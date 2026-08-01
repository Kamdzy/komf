package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

@Serializable
data class KodanshaImage(
    val uuid: String? = null,
    val jpg: List<KodanshaImageVariant> = emptyList(),
    val webp: List<KodanshaImageVariant> = emptyList(),
) {
    // Azuki publishes every image in a handful of fixed widths. Prefer webp,
    // which is offered at the same sizes as jpg but smaller.
    fun largestUrl(): String? {
        val variants = webp.ifEmpty { jpg }
        return variants.maxByOrNull { it.width ?: 0 }?.url
    }
}

@Serializable
data class KodanshaImageVariant(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)
