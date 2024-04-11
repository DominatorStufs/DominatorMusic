package app.vitune.compose.routing

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val globalRouteFlow = MutableSharedFlow<Pair<Route, Array<Any?>>>(extraBufferCapacity = 1)

private val backMutex = Mutex()
private var canCapture by mutableStateOf(true)

@Composable
fun GlobalPredictiveBackHandler(
    enabled: Boolean,
    onStart: () -> Unit,
    onProgress: (Float) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    var isCaptured by remember { mutableStateOf(false) }

    BackHandler((canCapture || isCaptured) && enabled) {
        onFinish()
    }

    PredictiveBackHandler(
        enabled = (canCapture || isCaptured) && enabled
    ) { backEvent ->
        backMutex.withLock {
            isCaptured = true
            canCapture = false

            onStart()

            try {
                backEvent.collect {
                    onProgress(it.progress)
                }
                onFinish()
            } catch (e: CancellationException) {
                onCancel()
            }

            canCapture = true
            isCaptured = false
        }
    }
}

@Composable
fun OnGlobalRoute(block: suspend (Pair<Route, Array<Any?>>) -> Unit) = LaunchedEffect(Unit) {
    globalRouteFlow.collect(block)
}
