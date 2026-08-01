package snd.komf.providers.kodansha.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KodanshaSeries(
    val uuid: String,
    val name: String,
    val slug: String? = null,
    @SerialName("short_description")
    val shortDescription: String? = null,
    val image: KodanshaImage? = null,
    val creators: List<KodanshaCreator> = emptyList(),
    val genres: List<KodanshaGenre> = emptyList(),
    val tags: List<String> = emptyList(),
    val publisher: KodanshaPublisher? = null,
    @SerialName("is_complete")
    val isComplete: Boolean? = null,
    @SerialName("age_rating")
    val ageRating: KodanshaAgeRating? = null,
    @SerialName("alt_titles")
    val altTitles: List<KodanshaAltTitle> = emptyList(),
)

@JvmInline
value class KodanshaSeriesId(val id: String)

@Serializable
data class KodanshaGenre(
    val name: String,
    val slug: String? = null,
)

@Serializable
data class KodanshaPublisher(
    val name: String,
    val uuid: String? = null,
    val slug: String? = null,
)

@Serializable
data class KodanshaAgeRating(
    val rating: Int? = null,
    val label: String? = null,
)

@Serializable
data class KodanshaAltTitle(
    val name: String,
    val locale: String? = null,
    @SerialName("is_promoted")
    val isPromoted: Boolean = false,
)
