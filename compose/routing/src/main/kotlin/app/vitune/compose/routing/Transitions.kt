package app.vitune.compose.routing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut

@ExperimentalAnimationApi
val defaultStacking = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    targetContentEnter = fadeIn(),
    targetContentZIndex = 1f
)

@ExperimentalAnimationApi
val defaultUnstacking = ContentTransform(
    initialContentExit = scaleOut(targetScale = 1.1f) + fadeOut(),
    targetContentEnter = EnterTransition.None,
    targetContentZIndex = 0f
)

@ExperimentalAnimationApi
val defaultStill = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    targetContentEnter = fadeIn(),
    targetContentZIndex = 1f
)

@ExperimentalAnimationApi
val AnimatedContentTransitionScope<Route?>.isStacking: Boolean
    get() = initialState == null && targetState != null

@ExperimentalAnimationApi
val AnimatedContentTransitionScope<Route?>.isUnstacking: Boolean
    get() = initialState != null && targetState == null

@ExperimentalAnimationApi
val AnimatedContentTransitionScope<Route?>.isStill: Boolean
    get() = initialState == null && targetState == null

@ExperimentalAnimationApi
val AnimatedContentTransitionScope<Route?>.isUnknown: Boolean
    get() = initialState != null && targetState != null
