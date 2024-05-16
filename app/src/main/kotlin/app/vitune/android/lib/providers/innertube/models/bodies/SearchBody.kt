package app.vitune.android.lib.providers.innertube.models.bodies

import app.vitune.android.lib.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context = Context.DefaultWeb,
    val query: String,
    val params: String
)
