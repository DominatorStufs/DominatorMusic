package app.banafsh.android.lib.providers.innertube.models.bodies

import app.banafsh.android.lib.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody(
    val context: Context = Context.DefaultWeb,
    val input: String
)
