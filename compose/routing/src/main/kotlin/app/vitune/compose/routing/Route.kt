@file:Suppress("UNCHECKED_CAST")

package app.vitune.compose.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.SaverScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Immutable
open class Route internal constructor(val tag: String) {
    override fun equals(other: Any?) = when {
        this === other -> true
        other is Route -> tag == other.tag
        else -> false
    }

    override fun hashCode() = tag.hashCode()

    protected fun global(args: Array<Any?>) = globalRouteFlow.tryEmit(
        RouteRequest(
            route = this,
            args = args
        )
    )

    protected suspend fun ensureGlobal(args: Array<Any?>) {
        globalRouteFlow.subscriptionCount.filter { it > 0 }.first()
        globalRouteFlow.emit(
            RouteRequest(
                route = this,
                args = args
            )
        )
    }

    object Saver : androidx.compose.runtime.saveable.Saver<Route?, String> {
        override fun restore(value: String): Route? = value.takeIf(String::isNotEmpty)?.let(::Route)
        override fun SaverScope.save(value: Route?): String = value?.tag.orEmpty()
    }
}

@Immutable
class Route0(tag: String) : Route(tag) {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        if (this == child) content()
    }

    fun global() = global(emptyArray())
    suspend fun ensureGlobal() = ensureGlobal(emptyArray())
}

@Immutable
class Route1<P0>(tag: String) : Route(tag) {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0) -> Unit) {
        if (this == child) content(args[0] as P0)
    }

    fun global(p0: P0) = global(arrayOf(p0))
    suspend fun ensureGlobal(p0: P0) = ensureGlobal(arrayOf(p0))
}

@Immutable
class Route2<P0, P1>(tag: String) : Route(tag) {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1
        )
    }

    fun global(p0: P0, p1: P1) = global(arrayOf(p0, p1))
    suspend fun ensureGlobal(p0: P0, p1: P1) = ensureGlobal(arrayOf(p0, p1))
}

@Immutable
class Route3<P0, P1, P2>(tag: String) : Route(tag) {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1, P2) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1,
            args[2] as P2
        )
    }

    fun global(p0: P0, p1: P1, p2: P2) = global(arrayOf(p0, p1, p2))
    suspend fun ensureGlobal(p0: P0, p1: P1, p2: P2) = ensureGlobal(arrayOf(p0, p1, p2))
}

@Immutable
class Route4<P0, P1, P2, P3>(tag: String) : Route(tag) {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1, P2, P3) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1,
            args[2] as P2,
            args[3] as P3
        )
    }

    fun global(p0: P0, p1: P1, p2: P2, p3: P3) = global(arrayOf(p0, p1, p2, p3))
    suspend fun ensureGlobal(p0: P0, p1: P1, p2: P2, p3: P3) = ensureGlobal(arrayOf(p0, p1, p2, p3))
}
