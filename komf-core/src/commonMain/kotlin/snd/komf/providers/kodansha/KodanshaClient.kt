package snd.komf.providers.kodansha

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import snd.komf.model.Image
import snd.komf.providers.kodansha.model.KodanshaChaptersResponse
import snd.komf.providers.kodansha.model.KodanshaSearchResult
import snd.komf.providers.kodansha.model.KodanshaSeries
import snd.komf.providers.kodansha.model.KodanshaSeriesId
import snd.komf.providers.kodansha.model.KodanshaVolume

/**
 * Kodansha retired api.kodansha.us; kodansha.us is now a storefront in front of
 * Azuki. All catalog data comes from the Azuki API, which scopes requests to a
 * publisher via a public organization key taken from the kodansha.us frontend.
 */
private const val apiUrl = "https://production.api.azuki.co"
private const val kodanshaOrganizationKey = "fff36c1f-9b3d-418e-b3a9-d2a537ddac06"

class KodanshaClient(private val ktor: HttpClient) {

    suspend fun search(name: String, limit: Int): KodanshaSearchResult {
        return ktor.get("$apiUrl/mangas/v1") {
            azukiHeaders()
            parameter("search_string", name)
            parameter("count", limit)
            parameter("sort", "popular")
        }.body()
    }

    suspend fun getSeries(seriesId: KodanshaSeriesId): KodanshaSeries {
        return ktor.get("$apiUrl/manga/${seriesId.id}/v0") {
            azukiHeaders()
        }.body()
    }

    suspend fun getAllSeriesBooks(seriesId: KodanshaSeriesId): List<KodanshaVolume> {
        val response: KodanshaChaptersResponse = ktor.get("$apiUrl/mangas/${seriesId.id}/chapters/v4") {
            azukiHeaders()
            parameter("order", "ascending")
            parameter("count", 1000)
        }.body()
        return response.orderedVolumes()
    }

    suspend fun getThumbnail(url: String): Image {
        val bytes: ByteArray = ktor.get(url).body()
        return Image(bytes)
    }

    private fun HttpRequestBuilder.azukiHeaders() {
        header("azuki-organization-key", kodanshaOrganizationKey)
    }
}
