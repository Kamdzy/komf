package snd.komf.providers.kodansha.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/**
 * Azuki models a series as a flat chapter list, with volumes returned alongside it
 * as a lookup map. Komf matches at volume granularity, so only the volume portion
 * of the response is modelled here.
 */
@Serializable
data class KodanshaChaptersResponse(
    @SerialName("volume_uuid_to_volume")
    val volumeUuidToVolume: Map<String, KodanshaVolume> = emptyMap(),
    @SerialName("volume_uuid_order")
    val volumeUuidOrder: List<String> = emptyList(),
) {
    fun orderedVolumes(): List<KodanshaVolume> =
        volumeUuidOrder.mapNotNull { volumeUuidToVolume[it] }
            .ifEmpty { volumeUuidToVolume.values.toList() }
}

@Serializable
data class KodanshaVolume(
    val uuid: String,
    @SerialName("series_uuid")
    val seriesUuid: String? = null,
    @SerialName("series_name")
    val seriesName: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("short_name")
    val shortName: String? = null,
    val label: String? = null,
    @SerialName("order_number")
    val orderNumber: Int? = null,
    @SerialName("page_count")
    val pageCount: Int? = null,
    val description: String? = null,
    val isbn: String? = null,
    val image: KodanshaImage? = null,
    val product: KodanshaProduct? = null,
    @SerialName("product_print")
    val productPrint: KodanshaProduct? = null,
) {
    // `label` carries decimal volumes such as "3.5"; `orderNumber` is always a whole
    // number, so it is only a fallback.
    fun number(): Double? = label?.toDoubleOrNull() ?: orderNumber?.toDouble()
}

@JvmInline
value class KodanshaBookId(val id: String)

@Serializable
data class KodanshaProduct(
    val uuid: String? = null,
    val isbn: String? = null,
    @SerialName("release_date")
    val releaseDate: Instant? = null,
)
