package snd.komf.providers.kodansha

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.providers.kodansha.model.KodanshaSeriesId
import snd.komf.util.NameSimilarityMatcher

private const val searchLimit = 50

class KodanshaMetadataProvider(
    private val client: KodanshaClient,
    private val metadataMapper: KodanshaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders {
        return CoreProviders.KODANSHA
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(KodanshaSeriesId(seriesId.value))
        val thumbnail = if (fetchSeriesCovers) getThumbnail(series.image?.largestUrl()) else null
        val volumes = client.getAllSeriesBooks(KodanshaSeriesId(series.uuid))
        return metadataMapper.toSeriesMetadata(series, volumes, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(KodanshaSeriesId(seriesId.value))
        return getThumbnail(series.image?.largestUrl())
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        // Azuki has no endpoint for a single volume; volumes are only returned as part
        // of the series' chapter listing, so the series has to be resolved first.
        val series = client.getSeries(KodanshaSeriesId(seriesId.value))
        val volume = client.getAllSeriesBooks(KodanshaSeriesId(seriesId.value))
            .firstOrNull { it.uuid == bookId.id }
            ?: throw IllegalStateException("Book ${bookId.id} not found in series ${seriesId.value}")
        val thumbnail = if (fetchBookCovers) getThumbnail(volume.image?.largestUrl()) else null

        return metadataMapper.toBookMetadata(volume, series.slug, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.search(sanitizeSearchInput(seriesName), limit)
        return searchResults.mangas.take(limit).map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.search(sanitizeSearchInput(seriesName), searchLimit)

        return searchResults.mangas
            .firstOrNull { nameMatcher.matches(seriesName, it.name) }
            ?.let { match ->
                val series = client.getSeries(KodanshaSeriesId(match.uuid))
                val thumbnail = if (fetchSeriesCovers) getThumbnail(series.image?.largestUrl()) else null
                val volumes = client.getAllSeriesBooks(KodanshaSeriesId(series.uuid))
                metadataMapper.toSeriesMetadata(series, volumes, thumbnail)
            }
    }

    private suspend fun getThumbnail(url: String?): Image? {
        if (url == null) return null
        return client.getThumbnail(url)
    }

    private fun sanitizeSearchInput(input: String): String {
        return input.take(300)
            .replace("\"", "")
    }
}
