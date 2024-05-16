package app.vitune.android.lib.providers.innertube.models.bodies

import app.vitune.android.lib.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context = Context.DefaultAndroid,
    val videoId: String,
    val playlistId: String? = null
)
