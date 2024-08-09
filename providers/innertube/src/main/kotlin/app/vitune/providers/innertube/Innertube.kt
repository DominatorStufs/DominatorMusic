package app.vitune.providers.innertube

import app.vitune.providers.innertube.models.MusicNavigationButtonRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Runs
import app.vitune.providers.innertube.models.Thumbnail
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.brotli
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Innertube {
    val client = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            @OptIn(ExperimentalSerializationApi::class)
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }
            )
        }

        install(ContentEncoding) {
            brotli(1.0f)
            gzip(0.9f)
            deflate(0.8f)
        }

        defaultRequest {
            url(scheme = "https", host = "music.youtube.com") {
                contentType(ContentType.Application.Json)
                headers {
                    append("X-Goog-Api-Key", API_KEY)
                    append("x-origin", ORIGIN)
                }
                parameters {
                    append("prettyPrint", "false")
                    append("key", API_KEY)
                }
            }
        }
    }

    private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    private const val ORIGIN = "https://music.youtube.com"

    internal const val BROWSE = "/youtubei/v1/browse"
    internal const val NEXT = "/youtubei/v1/next"
    internal const val PLAYER = "/youtubei/v1/player"
    internal const val QUEUE = "/youtubei/v1/music/get_queue"
    internal const val SEARCH = "/youtubei/v1/search"
    internal const val SEARCH_SUGGESTIONS = "/youtubei/v1/music/get_search_suggestions"
    internal const val MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK =
        "musicResponsiveListItemRenderer(flexColumns,fixedColumns,thumbnail,navigationEndpoint,badges)"
    internal const val MUSIC_TWO_ROW_ITEM_RENDERER_MASK =
        "musicTwoRowItemRenderer(thumbnailRenderer,title,subtitle,navigationEndpoint)"

    @Suppress("MaximumLineLength")
    internal const val PLAYLIST_PANEL_VIDEO_RENDERER_MASK =
        "playlistPanelVideoRenderer(title,navigationEndpoint,longBylineText,shortBylineText,thumbnail,lengthText,badges)"

    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)

    @Serializable
    data class Info<T : NavigationEndpoint.Endpoint>(
        val name: String?,
        val endpoint: T?
    ) {
        @Suppress("UNCHECKED_CAST")
        constructor(run: Runs.Run) : this(
            name = run.text,
            endpoint = run.navigationEndpoint?.endpoint as T?
        )
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val Song = SearchFilter("EgWKAQIIAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Video = SearchFilter("EgWKAQIQAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Album = SearchFilter("EgWKAQIYAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Artist = SearchFilter("EgWKAQIgAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val CommunityPlaylist = SearchFilter("EgeKAQQoAEABag4QAxAEEAkQChAFEBAQFQ%3D%3D")
        }
    }

    sealed class Item {
        abstract val thumbnail: Thumbnail?
        abstract val key: String
    }

    @Serializable
    data class SongItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val album: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?,
        val explicit: Boolean,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        companion object
    }

    data class VideoItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val viewsText: String?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        val isOfficialMusicVideo: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"

        companion object
    }

    @Serializable
    data class AlbumItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    @Serializable
    data class ArtistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val subscribersCountText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    @Serializable
    data class PlaylistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val channel: Info<NavigationEndpoint.Endpoint.Browse>?,
        val songCount: Int?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    data class ArtistPage(
        val name: String?,
        val description: String?,
        val thumbnail: Thumbnail?,
        val shuffleEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val radioEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val songs: List<SongItem>?,
        val songsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val albums: List<AlbumItem>?,
        val albumsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val singles: List<AlbumItem>?,
        val singlesEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val subscribersCountText: String?
    )

    data class PlaylistOrAlbumPage(
        val title: String?,
        val description: String?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val thumbnail: Thumbnail?,
        val url: String?,
        val songsPage: ItemsPage<SongItem>?,
        val otherVersions: List<AlbumItem>?,
        val otherInfo: String?
    )

    data class NextPage(
        val itemsPage: ItemsPage<SongItem>?,
        val playlistId: String?,
        val params: String? = null,
        val playlistSetVideoId: String? = null
    )

    @Serializable
    data class RelatedPage(
        val songs: List<SongItem>? = null,
        val playlists: List<PlaylistItem>? = null,
        val albums: List<AlbumItem>? = null,
        val artists: List<ArtistItem>? = null
    )

    data class DiscoverPage(
        val newReleaseAlbums: List<AlbumItem>,
        val moods: List<Mood.Item>,
        val trending: Trending
    ) {
        data class Trending(
            val songs: List<SongItem>,
            val endpoint: NavigationEndpoint.Endpoint.Browse?
        )
    }

    data class Mood(
        val title: String,
        val items: List<Item>
    ) {
        data class Item(
            val title: String,
            val stripeColor: Long,
            val endpoint: NavigationEndpoint.Endpoint.Browse
        ) : Innertube.Item() {
            override val thumbnail get() = null
            override val key
                get() = "${endpoint.browseId.orEmpty()}${endpoint.params?.let { "/$it" }.orEmpty()}"

            companion object
        }
    }

    @Suppress("ReturnCount")
    fun MusicNavigationButtonRenderer.toMood(): Mood.Item? {
        return Mood.Item(
            title = buttonText.runs.firstOrNull()?.text ?: return null,
            stripeColor = solid?.leftStripeColor ?: return null,
            endpoint = clickCommand.browseEndpoint ?: return null
        )
    }

    data class ItemsPage<T : Item>(
        val items: List<T>?,
        val continuation: String?
    )
}
