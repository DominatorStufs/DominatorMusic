package app.vitune.android.lib.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class SubscribeButtonRenderer(
    val subscriberCountText: Runs?
)
