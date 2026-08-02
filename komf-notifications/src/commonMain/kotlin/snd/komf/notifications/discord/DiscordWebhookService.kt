package snd.komf.notifications.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import snd.komf.model.Image
import snd.komf.notifications.discord.model.Embed
import snd.komf.notifications.discord.model.EmbedFooter
import snd.komf.notifications.discord.model.EmbedImage
import snd.komf.notifications.NotificationContext
import snd.komf.notifications.discord.model.Webhook
import snd.komf.notifications.discord.model.WebhookExecuteRequest

private val logger = KotlinLogging.logger {}

private const val baseUrl = "https://discord.com/api"

// Discord JSON error code returned when its media scanner flags an attachment.
// https://discord.com/developers/docs/topics/opcodes-and-status-codes#json
private const val explicitContentErrorCode = 20009

class DiscordWebhookService(
    private val ktor: HttpClient,
    private val json: Json,
    private val templateRenderer: DiscordVelocityTemplates,
    private val seriesCover: Boolean,
    private val webhooks: Collection<String>,
    embedColor: String,
) {
    private val embedColor = embedColor.toInt(16)

    suspend fun send(
        context: NotificationContext,
        templates: DiscordStringTemplates? = null,
    ) {
        if (webhooks.isEmpty()) return

        val webhookRequest = toRequest(context, templates) ?: return
        webhooks.map { getWebhook(it) }.forEach { webhook ->
            executeWebhook(
                webhook = webhook,
                webhookRequest = webhookRequest,
                image = if (seriesCover) context.seriesCover else null
            )
        }
    }

    private fun toRequest(
        context: NotificationContext,
        templates: DiscordStringTemplates? = null,
    ): WebhookExecuteRequest? {
        val renderResult =
            templates?.let { templateRenderer.render(context, it) } ?: templateRenderer.render(context)
        if (renderResult.description == null &&
            renderResult.fields.isEmpty() &&
            renderResult.footer == null &&
            renderResult.title == null
            && !seriesCover
        ) {
            logger.warn { "empty discord message for series ${context.series.name}. Skipping notification" }
            return null
        }

        val image = if (seriesCover && context.seriesCover != null) {
            val contentType = context.seriesCover.mimeType?.replace("image/", "") ?: "jpeg"
            EmbedImage(url = "attachment://cover.$contentType")
        } else null

        val embed = Embed(
            title = renderResult.title,
            url = renderResult.titleUrl,
            description = renderResult.description,
            fields = renderResult.fields,
            footer = renderResult.footer?.let { EmbedFooter(text = it) },
            color = embedColor,
            image = image
        )
        return WebhookExecuteRequest(embeds = listOf(embed))
    }

    private suspend fun getWebhook(webhookUrl: String): Webhook {
        return ktor.get(webhookUrl).body()
    }

    private suspend fun executeWebhook(webhook: Webhook, webhookRequest: WebhookExecuteRequest, image: Image? = null) {
        try {
            postWebhook(webhook, webhookRequest, image)
        } catch (e: ClientRequestException) {
            if (image == null || !e.isExplicitContentRejection()) throw e

            // Discord scans attachments and rejects the whole message when the cover is flagged as
            // explicit. Resending the same image can never succeed, so drop it and keep the
            // notification rather than losing it entirely.
            val withoutImage = webhookRequest.withoutEmbedImages()
            if (withoutImage.embeds?.all { it.isEmpty() } != false) {
                logger.warn { "discord rejected the series cover as explicit content, and there is nothing else to send. Skipping notification" }
                return
            }
            logger.warn { "discord rejected the series cover as explicit content. Sending notification without it" }
            postWebhook(webhook, withoutImage, null)
        }
    }

    private suspend fun postWebhook(webhook: Webhook, webhookRequest: WebhookExecuteRequest, image: Image?) {
        val jsonPayload = json.encodeToString(webhookRequest)
        logger.debug { "discord webhook body: $jsonPayload" }
        ktor.post("$baseUrl/webhooks/${webhook.id}/${webhook.token}") {
            if (image == null) {
                contentType(ContentType.Application.Json)
                setBody(webhookRequest)
            } else {
                val filename = "cover.${image.mimeType?.replace("image/", "") ?: "jpeg"}"
                contentType(ContentType.MultiPart.FormData)
                setBody(
                    MultiPartFormDataContent(formData {
                        append(
                            "cover",
                            image.bytes,
                            Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"$filename\"") }
                        )
                        append("payload_json", jsonPayload)
                    })
                )
            }

        }
    }

    private suspend fun ClientRequestException.isExplicitContentRejection(): Boolean {
        if (response.status != HttpStatusCode.BadRequest) return false
        // ktor's default response validation saves the call before building the exception,
        // so the body can be read again here
        val code = runCatching {
            json.parseToJsonElement(response.bodyAsText()).jsonObject["code"]?.jsonPrimitive?.intOrNull
        }.getOrNull()
        return code == explicitContentErrorCode
    }

    private fun WebhookExecuteRequest.withoutEmbedImages() =
        copy(embeds = embeds?.map { it.copy(image = null) })

    private fun Embed.isEmpty() =
        title == null && description == null && footer == null && fields.isNullOrEmpty() && image == null
}
