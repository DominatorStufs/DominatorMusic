package app.vitune.android.ui.screens.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.Player
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Info
import app.vitune.android.models.Song
import app.vitune.android.models.ui.UiMedia
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.service.PlayerService
import app.vitune.android.transaction
import app.vitune.android.ui.components.FadingRow
import app.vitune.android.ui.components.SeekBar
import app.vitune.android.ui.components.themed.BigIconButton
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.utils.bold
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.favoritesIcon
import app.vitune.core.ui.utils.px
import app.vitune.core.ui.utils.roundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FORWARD_BACKWARD_OFFSET = 16f

@Composable
fun Controls(
    media: UiMedia?,
    binder: PlayerService.Binder?,
    shouldBePlaying: Boolean,
    position: Long,
    modifier: Modifier = Modifier,
    layout: PlayerPreferences.PlayerLayout = PlayerPreferences.playerLayout
) {
    var likedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(media) {
        if (media == null) likedAt = null
        else Database
            .likedAt(media.id)
            .distinctUntilChanged()
            .collect { likedAt = it }
    }

    val shouldBePlayingTransition = updateTransition(
        targetState = shouldBePlaying,
        label = "shouldBePlaying"
    )

    val playButtonRadius by shouldBePlayingTransition.animateDp(
        transitionSpec = { tween(durationMillis = 100, easing = LinearEasing) },
        label = "playPauseRoundness",
        targetValueByState = { if (it) 16.dp else 32.dp }
    )

    if (media != null && binder != null) when (layout) {
        PlayerPreferences.PlayerLayout.Classic -> ClassicControls(
            media = media,
            binder = binder,
            shouldBePlaying = shouldBePlaying,
            position = position,
            likedAt = likedAt,
            playButtonRadius = playButtonRadius,
            modifier = modifier
        )

        PlayerPreferences.PlayerLayout.New -> ModernControls(
            media = media,
            binder = binder,
            shouldBePlaying = shouldBePlaying,
            position = position,
            likedAt = likedAt,
            playButtonRadius = playButtonRadius,
            modifier = modifier
        )
    }
}

@Composable
private fun ClassicControls(
    media: UiMedia,
    binder: PlayerService.Binder,
    shouldBePlaying: Boolean,
    position: Long,
    likedAt: Long?,
    playButtonRadius: Dp,
    modifier: Modifier = Modifier
) = with(PlayerPreferences) {
    val (colorPalette) = LocalAppearance.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        MediaInfo(media)
        Spacer(modifier = Modifier.weight(1f))
        SeekBar(
            binder = binder,
            position = position,
            media = media,
            alwaysShowDuration = true
        )
        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                color = colorPalette.favoritesIcon,
                onClick = {
                    val currentMediaItem = binder.player.currentMediaItem

                    query {
                        if (
                            Database.like(
                                media.id,
                                if (likedAt == null) System.currentTimeMillis() else null
                            ) == 0
                        ) {
                            currentMediaItem
                                ?.takeIf { it.mediaId == media.id }
                                ?.let {
                                    Database.insert(currentMediaItem, Song::toggleLike)
                                }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            IconButton(
                icon = R.drawable.play_skip_back,
                color = colorPalette.text,
                onClick = binder.player::forceSeekToPrevious,
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(playButtonRadius.roundedShape)
                    .clickable {
                        if (shouldBePlaying) binder.player.pause()
                        else {
                            if (binder.player.playbackState == Player.STATE_IDLE) binder.player.prepare()
                            binder.player.play()
                        }
                    }
                    .background(colorPalette.background2)
                    .size(64.dp)
            ) {
                AnimatedPlayPauseButton(
                    playing = shouldBePlaying,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                icon = R.drawable.play_skip_forward,
                color = colorPalette.text,
                onClick = binder.player::forceSeekToNext,
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            IconButton(
                icon = R.drawable.infinite,
                enabled = trackLoopEnabled,
                onClick = { trackLoopEnabled = !trackLoopEnabled },
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ModernControls(
    media: UiMedia,
    binder: PlayerService.Binder,
    shouldBePlaying: Boolean,
    position: Long,
    likedAt: Long?,
    playButtonRadius: Dp,
    modifier: Modifier = Modifier,
    controlHeight: Dp = 64.dp
) {
    val previousButtonContent: @Composable RowScope.() -> Unit = {
        SkipButton(
            iconId = R.drawable.play_skip_back,
            onClick = binder.player::forceSeekToPrevious,
            modifier = Modifier.weight(1f),
            offsetOnPress = -FORWARD_BACKWARD_OFFSET
        )
    }

    val likeButtonContent: @Composable RowScope.() -> Unit = {
        BigIconButton(
            iconId = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
            onClick = {
                transaction {
                    Database.like(
                        songId = media.id,
                        likedAt = if (likedAt == null) System.currentTimeMillis() else null
                    )
                }
            },
            modifier = Modifier.weight(1f)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        MediaInfo(media)
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (PlayerPreferences.showLike) 4.dp else 8.dp)
        ) {
            if (PlayerPreferences.showLike) previousButtonContent()
            PlayButton(
                radius = playButtonRadius,
                shouldBePlaying = shouldBePlaying,
                modifier = Modifier
                    .height(controlHeight)
                    .weight(if (PlayerPreferences.showLike) 3f else 4f)
            )
            SkipButton(
                iconId = R.drawable.play_skip_forward,
                onClick = binder.player::forceSeekToNext,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (PlayerPreferences.showLike) likeButtonContent() else previousButtonContent()

            Column(modifier = Modifier.weight(4f)) {
                SeekBar(
                    binder = binder,
                    position = position,
                    media = media
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SkipButton(
    @DrawableRes iconId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offsetOnPress: Float = FORWARD_BACKWARD_OFFSET
) {
    val scope = rememberCoroutineScope()
    val offsetDp = remember { Animatable(0f) }
    val density = LocalDensity.current

    BigIconButton(
        iconId = iconId,
        onClick = {
            onClick()
            scope.launch { offsetDp.animateTo(offsetOnPress) }
        },
        onPress = { scope.launch { offsetDp.animateTo(offsetOnPress) } },
        onCancel = { scope.launch { offsetDp.animateTo(0f) } },
        modifier = modifier.graphicsLayer {
            with(density) {
                translationX = offsetDp.value.dp.toPx()
            }
        }
    )
}

@Composable
private fun PlayButton(
    radius: Dp,
    shouldBePlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    Box(
        modifier = modifier
            .clip(radius.roundedShape)
            .clickable {
                if (shouldBePlaying) binder?.player?.pause() else {
                    if (binder?.player?.playbackState == Player.STATE_IDLE) binder.player.prepare()
                    binder?.player?.play()
                }
            }
            .background(colorPalette.accent)
    ) {
        AnimatedPlayPauseButton(
            playing = shouldBePlaying,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        )
    }
}

@Composable
private fun MediaInfo(media: UiMedia) {
    val (_, typography) = LocalAppearance.current

    var artistInfo: List<Info>? by remember { mutableStateOf(null) }
    var maxHeight by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(media) {
        withContext(Dispatchers.IO) {
            artistInfo = Database
                .songArtistInfo(media.id)
                .takeIf { it.isNotEmpty() }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = media.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = ""
        ) { title ->
            FadingRow(modifier = Modifier.fillMaxWidth(0.75f)) {
                BasicText(
                    text = title,
                    style = typography.l.bold,
                    maxLines = 1
                )
            }
        }

        AnimatedContent(
            targetState = artistInfo,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = ""
        ) { state ->
            state?.let { artists ->
                FadingRow(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .heightIn(maxHeight.px.dp)
                ) {
                    artists.fastForEachIndexed { i, artist ->
                        if (i == artists.lastIndex && artists.size > 1) BasicText(
                            text = " & ",
                            style = typography.s.semiBold.secondary
                        )
                        BasicText(
                            text = artist.name.orEmpty(),
                            style = typography.s.semiBold.secondary,
                            modifier = Modifier.clickable { artistRoute.global(artist.id) }
                        )
                        if (i != artists.lastIndex && i + 1 != artists.lastIndex) BasicText(
                            text = ", ",
                            style = typography.s.semiBold.secondary
                        )
                    }
                }
            } ?: FadingRow(modifier = Modifier.fillMaxWidth(0.75f)) {
                BasicText(
                    text = media.artist,
                    style = typography.s.semiBold.secondary,
                    maxLines = 1,
                    modifier = Modifier.onGloballyPositioned { maxHeight = it.size.height }
                )
            }
        }
    }
}
