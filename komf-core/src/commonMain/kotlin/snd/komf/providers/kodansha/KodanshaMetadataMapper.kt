package snd.komf.providers.kodansha

import com.fleeksoft.ksoup.Ksoup
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.PublisherType
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.kodansha.model.KodanshaSeries
import snd.komf.providers.kodansha.model.KodanshaVolume

const val kodanshaBaseUrl = "https://kodansha.us"

class KodanshaMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesMetadata(
        series: KodanshaSeries,
        volumes: List<KodanshaVolume>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val status = when (series.isComplete) {
            true -> SeriesStatus.ENDED
            false -> SeriesStatus.ONGOING
            null -> null
        }

        val titles = listOf(SeriesTitle(series.name, TitleType.LOCALIZED, "en")) +
                series.altTitles.map { SeriesTitle(it.name, titleTypeOf(it.locale), it.locale) }

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.shortDescription?.let { parseDescription(it) },
            publisher = series.publisher?.let { Publisher(it.name, PublisherType.LOCALIZED) },
            ageRating = series.ageRating?.rating,
            genres = series.genres.map { it.name },
            tags = series.tags,
            totalBookCount = volumes.size.takeIf { it > 0 },
            thumbnail = thumbnail,
            authors = series.creators.map { Author(it.name, AuthorRole.WRITER) },
            links = listOfNotNull(series.slug?.let { WebLink("Kodansha", seriesUrl(it)) })
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.uuid),
            metadata = metadata,
            books = volumes.map { volume ->
                SeriesBook(
                    id = ProviderBookId(volume.uuid),
                    number = volume.number()?.let { BookRange(it) },
                    name = volume.fullName ?: volume.shortName ?: "${series.name} ${volume.label}",
                    type = null,
                    edition = null
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(
        volume: KodanshaVolume,
        seriesSlug: String?,
        thumbnail: Image? = null
    ): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = volume.fullName ?: volume.shortName,
            summary = volume.description?.let { parseDescription(it) },
            number = volume.number()?.let { BookRange(it) },
            releaseDate = (volume.product?.releaseDate ?: volume.productPrint?.releaseDate)
                ?.toLocalDateTime(TimeZone.UTC)?.date,
            isbn = volume.isbn ?: volume.product?.isbn ?: volume.productPrint?.isbn,
            thumbnail = thumbnail,
            links = listOfNotNull(
                seriesSlug?.let { slug -> volume.label?.let { WebLink("Kodansha", volumeUrl(slug, it)) } }
            )
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(volume.uuid),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    fun toSeriesSearchResult(series: KodanshaSeries): SeriesSearchResult {
        return SeriesSearchResult(
            url = series.slug?.let { seriesUrl(it) },
            imageUrl = series.image?.largestUrl(),
            title = series.name,
            resultId = series.uuid,
            provider = CoreProviders.KODANSHA,
        )
    }

    private fun titleTypeOf(locale: String?) =
        if (locale != null && locale.startsWith("ja")) TitleType.NATIVE else TitleType.LOCALIZED

    private fun parseDescription(description: String): String {
        return Ksoup.parse(description).wholeText().replace("\n\n", "\n")
    }

    private fun seriesUrl(seriesSlug: String) = "$kodanshaBaseUrl/series/$seriesSlug"
    private fun volumeUrl(seriesSlug: String, volumeLabel: String) =
        "$kodanshaBaseUrl/series/$seriesSlug/volume-$volumeLabel"
}
