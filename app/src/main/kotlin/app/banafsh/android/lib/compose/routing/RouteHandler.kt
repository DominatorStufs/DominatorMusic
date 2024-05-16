package app.banafsh.android.lib.compose.routing

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

typealias TransitionScope<T> = AnimatedContentTransitionScope<T>
typealias TransitionSpec<T> = TransitionScope<T>.() -> ContentTransform

private val defaultTransitionSpec: TransitionSpec<RouteHandlerScope> = {
    when {
        isStacking -> defaultStacking
        isStill -> defaultStill
        else -> defaultUnstacking
    }
}

@Composable
fun RouteHandler(
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    handleBackPress: Boolean = true,
    transitionSpec: TransitionSpec<RouteHandlerScope> = defaultTransitionSpec,
    content: @Composable RouteHandlerScope.() -> Unit
) {
    var route by rememberSaveable(stateSaver = Route.Saver) {
        mutableStateOf(null)
    }

    RouteHandler(
        route = route,
        onRouteChanged = { route = it },
        listenToGlobalEmitter = listenToGlobalEmitter,
        handleBackPress = handleBackPress,
        transitionSpec = transitionSpec,
        modifier = modifier,
        content = content
    )
}

@Composable
fun RouteHandler(
    route: Route?,
    onRouteChanged: (Route?) -> Unit,
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    handleBackPress: Boolean = true,
    transitionSpec: TransitionSpec<RouteHandlerScope> = defaultTransitionSpec,
    content: @Composable RouteHandlerScope.() -> Unit
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val parameters = rememberSaveable { arrayOfNulls<Any?>(4) }
    val scope = remember(route) {
        RouteHandlerScope(
            route = route,
            parameters = parameters,
            push = onRouteChanged,
            pop = { if (handleBackPress) backDispatcher?.onBackPressed() else onRouteChanged(null) }
        )
    }

    if (listenToGlobalEmitter && route == null) OnGlobalRoute { request ->
        request.args.forEachIndexed(parameters::set)
        onRouteChanged(request.route)
    }

    BackHandler(enabled = handleBackPress && route != null) {
        onRouteChanged(null)
    }

    updateTransition(targetState = scope, label = null).AnimatedContent(
        transitionSpec = transitionSpec,
        contentKey = RouteHandlerScope::route,
        modifier = modifier
    ) {
        it.content()
    }
}
