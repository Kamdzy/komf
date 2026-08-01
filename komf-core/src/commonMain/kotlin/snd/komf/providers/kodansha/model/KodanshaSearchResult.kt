package snd.komf.providers.kodansha.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodanshaSearchResult(
    val mangas: List<KodanshaSeries> = emptyList(),
    @SerialName("total_count")
    val totalCount: String? = null,
    val offset: Int = 0,
)
