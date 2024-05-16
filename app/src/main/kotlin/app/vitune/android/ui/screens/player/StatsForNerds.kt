package app.vitune.android.ui.screens.player

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.lib.core.ui.LocalAppearance
import app.vitune.android.lib.core.ui.onOverlay
import app.vitune.android.lib.core.ui.overlay
import app.vitune.android.lib.providers.innertube.Innertube
import app.vitune.android.models.Format
import app.vitune.android.utils.color
import app.vitune.android.utils.medium
import app.vitune.android.lib.providers.innertube.models.bodies.PlayerBody
import app.vitune.android.lib.providers.innertube.requests.player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun StatsForNerds(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var cachedBytes by remember(mediaId) {
            mutableLongStateOf(binder.cache.getCachedBytes(mediaId, 0, -1))
        }

        var format by remember { mutableStateOf<Format?>(null) }

        LaunchedEffect(mediaId) {
            Database.format(mediaId).distinctUntilChanged().collectLatest { currentFormat ->
                if (currentFormat?.itag == null) binder.player.currentMediaItem
                    ?.takeIf { it.mediaId == mediaId }
                    ?.let { mediaItem ->
                        withContext(Dispatchers.IO) {
                            delay(2000)
                            Innertube.player(PlayerBody(videoId = mediaId))
                                ?.onSuccess { response ->
                                    response.streamingData?.highestQualityFormat?.let { format ->
                                        Database.insert(mediaItem)
                                        Database.insert(
                                            Format(
                                                songId = mediaId,
                                                itag = format.itag,
                                                mimeType = format.mimeType,
                                                bitrate = format.bitrate,
                                                loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                                contentLength = format.contentLength,
                                                lastModified = format.lastModified
                                            )
                                        )
                                    }
                                }
                        }
                    } else format = currentFormat
            }
        }

        DisposableEffect(mediaId) {
            val listener = object : Cache.Listener {
                override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                    cachedBytes += span.length
                }

                override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                    cachedBytes -= span.length
                }

                override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) =
                    Unit
            }

            binder.cache.addListener(mediaId, listener)

            onDispose {
                binder.cache.removeListener(mediaId, listener)
            }
        }

        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
                .background(colorPalette.overlay)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(all = 16.dp)
            ) {
                @Composable
                fun Text(text: String) = BasicText(
                    text = text,
                    maxLines = 1,
                    style = typography.xs.medium.color(colorPalette.onOverlay)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.id))
                    Text(text = stringResource(R.string.itag))
                    Text(text = stringResource(R.string.bitrate))
                    Text(text = stringResource(R.string.size))
                    Text(text = stringResource(R.string.cached))
                    Text(text = stringResource(R.string.loudness))
                }

                Column {
                    Text(text = mediaId)
                    Text(text = format?.itag?.toString() ?: stringResource(R.string.unknown))
                    Text(
                        text = format?.bitrate?.let {
                            stringResource(
                                R.string.format_kbps,
                                it / 1000
                            )
                        } ?: stringResource(R.string.unknown)
                    )
                    Text(
                        text = format?.contentLength
                            ?.let { Formatter.formatShortFileSize(context, it) }
                            ?: stringResource(R.string.unknown)
                    )
                    Text(
                        text = buildString {
                            append(Formatter.formatShortFileSize(context, cachedBytes))

                            format?.contentLength?.let {
                                append(" (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)")
                            }
                        }
                    )
                    Text(
                        text = format?.loudnessDb?.let {
                            stringResource(
                                R.string.format_db,
                                "%.2f".format(it)
                            )
                        } ?: stringResource(R.string.unknown)
                    )
                }
            }
        }
    }
}
