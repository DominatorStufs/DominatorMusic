package app.banafsh.android.lib.providers.innertube.requests

import app.banafsh.android.lib.providers.common.runCatchingCancellable
import app.banafsh.android.lib.providers.innertube.Innertube
import app.banafsh.android.lib.providers.innertube.models.ContinuationResponse
import app.banafsh.android.lib.providers.innertube.models.MusicShelfRenderer
import app.banafsh.android.lib.providers.innertube.models.SearchResponse
import app.banafsh.android.lib.providers.innertube.models.bodies.ContinuationBody
import app.banafsh.android.lib.providers.innertube.models.bodies.SearchBody
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun <T : Innertube.Item> Innertube.searchPage(
    body: SearchBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingCancellable {
    val response = client.post(SEARCH) {
        setBody(body)
        @Suppress("all")
        mask(
            "contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)"
        )
    }.body<SearchResponse>()

    response
        .contents
        ?.tabbedSearchResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.lastOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(fromMusicShelfRendererContent)
}

suspend fun <T : Innertube.Item> Innertube.searchPage(
    body: ContinuationBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingCancellable {
    val response = client.post(SEARCH) {
        setBody(body)
        @Suppress("all")
        mask(
            "continuationContents.musicShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)"
        )
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(
    mapper: (MusicShelfRenderer.Content) -> T?
) = Innertube.ItemsPage(
    items = this
        ?.contents
        ?.mapNotNull(mapper),
    continuation = this
        ?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation
)
