package app.vitune.compose.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class RouteHandlerScope(
    val route: Route?,
    val parameters: Array<Any?>,
    val push: (Route?) -> Unit,
    val pop: () -> Unit
) {
    @Composable
    inline fun NavHost(content: @Composable () -> Unit) {
        if (route == null) content()
    }

    operator fun Route0.invoke() = push(this)

    operator fun <P0> Route1<P0>.invoke(p0: P0) {
        parameters[0] = p0
        push(this)
    }

    operator fun <P0, P1> Route2<P0, P1>.invoke(p0: P0, p1: P1) {
        parameters[0] = p0
        parameters[1] = p1
        push(this)
    }

    operator fun <P0, P1, P2> Route3<P0, P1, P2>.invoke(p0: P0, p1: P1, p2: P2) {
        parameters[0] = p0
        parameters[1] = p1
        parameters[2] = p2
        push(this)
    }

    operator fun <P0, P1, P2, P3> Route4<P0, P1, P2, P3>.invoke(p0: P0, p1: P1, p2: P2, p3: P3) {
        parameters[0] = p0
        parameters[1] = p1
        parameters[2] = p2
        parameters[3] = p3
        push(this)
    }
}
