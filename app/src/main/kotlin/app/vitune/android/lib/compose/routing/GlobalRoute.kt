package app.vitune.android.lib.compose.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.MutableSharedFlow

typealias RouteRequestDefinition = Pair<Route, Array<Any?>>

// Same runtime type as before, but just syntactically nicer
@JvmInline
value class RouteRequest private constructor(private val def: RouteRequestDefinition) {
    constructor(route: Route, args: Array<Any?>) : this(route to args)

    val route get() = def.first
    val args get() = def.second
}

internal val globalRouteFlow = MutableSharedFlow<RouteRequest>(extraBufferCapacity = 1)

@Composable
fun OnGlobalRoute(block: suspend (RouteRequest) -> Unit) {
    val currentBlock by rememberUpdatedState(block)

    LaunchedEffect(Unit) {
        globalRouteFlow.collect {
            currentBlock(it)
        }
    }
}
