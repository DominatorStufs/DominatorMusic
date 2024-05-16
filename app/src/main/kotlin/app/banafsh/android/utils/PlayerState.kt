package app.banafsh.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import app.banafsh.android.LocalPlayerServiceBinder
import app.banafsh.android.service.PlayerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Suppress("LambdaParameterInRestartableEffect") // Invalid: crossinline param
@Composable
inline fun Player.DisposableListener(
    key: Any = this,
    crossinline listenerProvider: () -> Player.Listener
) = DisposableEffect(key) {
    val listener = listenerProvider()

    addListener(listener)
    onDispose { removeListener(listener) }
}

@Composable
fun Player.positionAndDurationState(): State<Pair<Long, Long>> {
    val state = remember { mutableStateOf(currentPosition to duration) }

    LaunchedEffect(this) {
        var isSeeking = false

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isSeeking = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                state.value = currentPosition to state.value.second
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    isSeeking = true
                    state.value = currentPosition to duration
                }
            }
        }

        addListener(listener)

        val pollJob = launch {
            while (isActive) {
                delay(500)
                if (!isSeeking) state.value = currentPosition to duration
            }
        }

        try {
            suspendCancellableCoroutine<Nothing> { }
        } finally {
            pollJob.cancel()
            removeListener(listener)
        }
    }

    return state
}

typealias WindowState = Pair<Timeline.Window?, PlaybackException?>

@Composable
fun windowState(
    binder: PlayerService.Binder? = LocalPlayerServiceBinder.current
): WindowState {
    val player = binder?.player ?: return null to null
    var window by remember { mutableStateOf(player.currentWindow) }
    var error by remember { mutableStateOf<PlaybackException?>(player.playerError) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                window = player.currentWindow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                error = player.playerError
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                error = playbackException
            }
        }
    }

    return window to error
}
