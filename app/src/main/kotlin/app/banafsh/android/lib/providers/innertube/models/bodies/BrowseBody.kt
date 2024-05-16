package app.banafsh.android.lib.providers.innertube.models.bodies

import app.banafsh.android.lib.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context = Context.DefaultWeb,
    val browseId: String,
    val params: String? = null
)
