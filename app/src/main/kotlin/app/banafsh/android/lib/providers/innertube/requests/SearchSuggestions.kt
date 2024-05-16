package app.banafsh.android.lib.providers.innertube.requests

import app.banafsh.android.lib.providers.common.runCatchingCancellable
import app.banafsh.android.lib.providers.innertube.Innertube
import app.banafsh.android.lib.providers.innertube.models.SearchSuggestionsResponse
import app.banafsh.android.lib.providers.innertube.models.bodies.SearchSuggestionsBody
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.searchSuggestions(body: SearchSuggestionsBody) = runCatchingCancellable {
    val response = client.post(SEARCH_SUGGESTIONS) {
        setBody(body)
        @Suppress("all")
        mask(
            "contents.searchSuggestionsSectionRenderer.contents.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query"
        )
    }.body<SearchSuggestionsResponse>()

    response
        .contents
        ?.firstOrNull()
        ?.searchSuggestionsSectionRenderer
        ?.contents
        ?.mapNotNull { content ->
            content
                .searchSuggestionRenderer
                ?.navigationEndpoint
                ?.searchEndpoint
                ?.query
        }
}
