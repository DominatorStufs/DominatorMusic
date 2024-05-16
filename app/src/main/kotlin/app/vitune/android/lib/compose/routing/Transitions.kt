package app.vitune.android.lib.compose.routing

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut

val defaultStacking = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    targetContentEnter = fadeIn(),
    targetContentZIndex = 1f
)

val defaultUnstacking = ContentTransform(
    initialContentExit = fadeOut(),
    targetContentEnter = EnterTransition.None,
    targetContentZIndex = 0f
)

val defaultStill = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    targetContentEnter = fadeIn(),
    targetContentZIndex = 1f
)

val TransitionScope<RouteHandlerScope>.isStacking
    get() = initialState.route == null && targetState.route != null

val TransitionScope<RouteHandlerScope>.isUnstacking
    get() = initialState.route != null && targetState.route == null

val TransitionScope<RouteHandlerScope>.isStill
    get() = initialState.route == null && targetState.route == null

val TransitionScope<RouteHandlerScope>.isUnknown
    get() = initialState.route != null && targetState.route != null
