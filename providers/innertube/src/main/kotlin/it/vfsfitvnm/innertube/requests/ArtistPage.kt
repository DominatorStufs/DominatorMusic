package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.vfsfitvnm.extensions.runCatchingCancellable
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.BrowseResponse
import it.vfsfitvnm.innertube.models.Context
import it.vfsfitvnm.innertube.models.MusicCarouselShelfRenderer
import it.vfsfitvnm.innertube.models.MusicShelfRenderer
import it.vfsfitvnm.innertube.models.bodies.BrowseBody
import it.vfsfitvnm.innertube.utils.findSectionByTitle
import it.vfsfitvnm.innertube.utils.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext

suspend fun Innertube.artistPage(body: BrowseBody) = runCatchingCancellable {
    val ctx = currentCoroutineContext()
    val response = client.post(BROWSE) {
        setBody(body)
        mask("contents,header")
    }.body<BrowseResponse>()

    val responseNoLang by lazy {
        CoroutineScope(ctx).async(start = CoroutineStart.LAZY) {
            client.post(BROWSE) {
                setBody(body.copy(context = Context.DefaultWebNoLang))
                mask("contents,header")
            }.body<BrowseResponse>()
        }
    }

    suspend fun findSectionByTitle(text: String) = response
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.get(0)
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.findSectionByTitle(text) ?: responseNoLang.await()
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.get(0)
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.findSectionByTitle(text)

    val songsSection = findSectionByTitle("Songs")?.musicShelfRenderer
    val albumsSection = findSectionByTitle("Albums")?.musicCarouselShelfRenderer
    val singlesSection = findSectionByTitle("Singles")?.musicCarouselShelfRenderer

    Innertube.ArtistPage(
        name = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.title
            ?.text,
        description = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.description
            ?.text,
        thumbnail = (
                response
                    .header
                    ?.musicImmersiveHeaderRenderer
                    ?.foregroundThumbnail
                    ?: response
                        .header
                        ?.musicImmersiveHeaderRenderer
                        ?.thumbnail
                )
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.getOrNull(0),
        shuffleEndpoint = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.playButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchEndpoint,
        radioEndpoint = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.startRadioButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchEndpoint,
        songs = songsSection
            ?.contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        songsEndpoint = songsSection
            ?.bottomEndpoint
            ?.browseEndpoint,
        albums = albumsSection
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        albumsEndpoint = albumsSection
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.moreContentButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.browseEndpoint,
        singles = singlesSection
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        singlesEndpoint = singlesSection
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.moreContentButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.browseEndpoint
    )
}
