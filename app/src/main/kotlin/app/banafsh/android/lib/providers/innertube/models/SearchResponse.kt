package app.banafsh.android.lib.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?
    )
}
