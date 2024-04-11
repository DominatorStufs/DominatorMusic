package app.vitune.compose.routing

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RouteHandler(
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    transitionSpec: AnimatedContentTransitionScope<Route?>.() -> ContentTransform = {
        when {
            isStacking -> defaultStacking
            isStill -> defaultStill
            else -> defaultUnstacking
        }
    },
    content: @Composable RouteHandlerScope.() -> Unit
) {
    var route by rememberSaveable(stateSaver = Route.Saver) {
        mutableStateOf(null)
    }

    RouteHandler(
        route = route,
        onRouteChanged = { route = it },
        listenToGlobalEmitter = listenToGlobalEmitter,
        transitionSpec = transitionSpec,
        modifier = modifier,
        content = content
    )
}

@ExperimentalAnimationApi
@Composable
fun RouteHandler(
    route: Route?,
    onRouteChanged: (Route?) -> Unit,
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    transitionSpec: AnimatedContentTransitionScope<Route?>.() -> ContentTransform = {
        when {
            isStacking -> defaultStacking
            isStill -> defaultStill
            else -> defaultUnstacking
        }
    },
    content: @Composable RouteHandlerScope.() -> Unit
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val parameters = rememberSaveable { arrayOfNulls<Any?>(3) }

    if (listenToGlobalEmitter && route == null) OnGlobalRoute { (newRoute, newParameters) ->
        newParameters.forEachIndexed(parameters::set)
        onRouteChanged(newRoute)
    }

    var predictiveBackProgress: Float? by remember { mutableStateOf(null) }
    GlobalPredictiveBackHandler(
        enabled = route != null,
        onStart = { predictiveBackProgress = 0f },
        onProgress = { predictiveBackProgress = it },
        onFinish = {
            onRouteChanged(null)
            predictiveBackProgress = null
        },
        onCancel = {
            predictiveBackProgress = null
        }
    )

    fun Route?.scope() = RouteHandlerScope(
        route = this,
        parameters = parameters,
        push = onRouteChanged,
        pop = {
            predictiveBackProgress = null
            onRouteChanged(null)
            backDispatcher?.onBackPressed()
        }
    )

    val transitionState = remember { SeekableTransitionState(route) }

    if (predictiveBackProgress == null) LaunchedEffect(route) {
        if (transitionState.currentState != route) transitionState.animateTo(route)
    } else LaunchedEffect(predictiveBackProgress) {
        transitionState.seekTo(
            fraction = predictiveBackProgress ?: 0f,
            targetState = null
        )
    }

    rememberTransition(
        transitionState = transitionState,
        label = null
    ).AnimatedContent(
        transitionSpec = transitionSpec,
        modifier = modifier
    ) {
        it.scope().content()
    }
}
