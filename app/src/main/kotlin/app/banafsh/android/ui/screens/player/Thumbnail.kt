package app.banafsh.android.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.banafsh.android.Database
import app.banafsh.android.LocalPlayerServiceBinder
import app.banafsh.android.R
import app.banafsh.android.lib.core.ui.Dimensions
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.service.LoginRequiredException
import app.banafsh.android.service.PlayableFormatNotFoundException
import app.banafsh.android.service.UnplayableException
import app.banafsh.android.service.VideoIdMismatchException
import app.banafsh.android.service.isLocal
import app.banafsh.android.ui.modifiers.onSwipe
import app.banafsh.android.utils.forceSeekToNext
import app.banafsh.android.utils.forceSeekToPrevious
import app.banafsh.android.utils.thumbnail
import app.banafsh.android.utils.windowState
import app.banafsh.android.lib.core.ui.utils.px
import coil.compose.AsyncImage
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

@Composable
fun Thumbnail(
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    isShowingStatsForNerds: Boolean,
    onShowStatsForNerds: (Boolean) -> Unit,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current
    val thumbnailSize = Dimensions.thumbnails.player.song

    val (nullableWindow, error) = windowState()
    val window = nullableWindow ?: return

    AnimatedContent(
        targetState = window,
        transitionSpec = {
            if (initialState.mediaItem.mediaId == targetState.mediaItem.mediaId)
                return@AnimatedContent EnterTransition.None togetherWith ExitTransition.None

            val duration = 500
            val direction =
                if (targetState.firstPeriodIndex > initialState.firstPeriodIndex) Left else Right

            ContentTransform(
                targetContentEnter = slideIntoContainer(direction, tween(duration)) +
                        fadeIn(tween(duration)) +
                        scaleIn(tween(duration), 0.85f),
                initialContentExit = slideOutOfContainer(direction, tween(duration)) +
                        fadeOut(tween(duration)) +
                        scaleOut(tween(duration), 0.85f),
                sizeTransform = SizeTransform(clip = false) { _, _ ->
                    tween(
                        durationMillis = duration,
                        delayMillis = 500
                    )
                }
            )
        },
        modifier = modifier.onSwipe(
            onSwipeLeft = {
                binder.player.forceSeekToNext()
            },
            onSwipeRight = {
                binder.player.seekToDefaultPosition()
                binder.player.forceSeekToPrevious()
            }
        ),
        contentAlignment = Alignment.Center,
        label = ""
    ) { currentWindow ->
        val shadowElevation by animateDpAsState(
            targetValue = if (window == currentWindow) 8.dp else 0.dp,
            animationSpec = tween(500),
            label = ""
        )
        val blurRadius by animateDpAsState(
            targetValue = if (
                (isShowingLyrics && !currentWindow.mediaItem.isLocal) ||
                error != null || isShowingStatsForNerds
            ) 8.dp else 0.dp,
            animationSpec = tween(500),
            label = ""
        )

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(thumbnailSize)
                .shadow(
                    elevation = shadowElevation,
                    shape = thumbnailShape,
                    clip = false
                )
                .clip(thumbnailShape)
        ) {
            if (currentWindow.mediaItem.mediaMetadata.artworkUri != null) AsyncImage(
                model = currentWindow.mediaItem.mediaMetadata.artworkUri.thumbnail((thumbnailSize - 64.dp).px),
                error = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (!currentWindow.mediaItem.isLocal) onShowLyrics(true) },
                            onLongPress = { onShowStatsForNerds(true) }
                        )
                    }
                    .fillMaxSize()
                    .background(colorPalette.background0)
                    .let {
                        if (blurRadius == 0.dp) it else it.blur(radius = blurRadius)
                    }
            ) else Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onShowStatsForNerds(true) })
                    }
                    .fillMaxSize()
            )

            if (!currentWindow.mediaItem.isLocal) Lyrics(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingLyrics && error == null,
                onDismiss = { onShowLyrics(false) },
                ensureSongInserted = { Database.insert(currentWindow.mediaItem) },
                height = thumbnailSize,
                mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                durationProvider = player::getDuration,
                onOpenDialog = onOpenDialog
            )

            StatsForNerds(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingStatsForNerds && error == null,
                onDismiss = { onShowStatsForNerds(false) }
            )

            PlaybackError(
                isDisplayed = error != null,
                messageProvider = {
                    if (currentWindow.mediaItem.isLocal) stringResource(R.string.error_local_music_deleted)
                    else when (error?.cause?.cause) {
                        is UnresolvedAddressException, is UnknownHostException ->
                            stringResource(R.string.error_network)

                        is PlayableFormatNotFoundException -> stringResource(R.string.error_unplayable)
                        is UnplayableException -> stringResource(R.string.error_source_deleted)
                        is LoginRequiredException -> stringResource(R.string.error_server_restrictions)
                        is VideoIdMismatchException -> stringResource(R.string.error_id_mismatch)
                        else -> stringResource(R.string.error_unknown_playback)
                    }
                },
                onDismiss = player::prepare
            )
        }
    }
}
