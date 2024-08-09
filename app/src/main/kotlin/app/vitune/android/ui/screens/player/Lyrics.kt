package app.vitune.android.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Lyrics
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.transaction
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.DefaultDialog
import app.vitune.android.ui.components.themed.Menu
import app.vitune.android.ui.components.themed.MenuEntry
import app.vitune.android.ui.components.themed.TextField
import app.vitune.android.ui.components.themed.TextFieldDialog
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.ui.components.themed.ValueSelectorDialogBody
import app.vitune.android.ui.modifiers.verticalFadingEdge
import app.vitune.android.utils.SynchronizedLyrics
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.disabled
import app.vitune.android.utils.medium
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.onOverlayShimmer
import app.vitune.core.ui.overlay
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.requests.lyrics
import app.vitune.providers.kugou.KuGou
import app.vitune.providers.lrclib.LrcLib
import app.vitune.providers.lrclib.models.Track
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuLaunch: () -> Unit = { },
    onOpenDialog: (() -> Unit)? = null,
    shouldShowSynchronizedLyrics: Boolean = PlayerPreferences.isShowingSynchronizedLyrics,
    setShouldShowSynchronizedLyrics: (Boolean) -> Unit = {
        PlayerPreferences.isShowingSynchronizedLyrics = it
    },
    shouldKeepScreenAwake: Boolean = AppearancePreferences.lyricsKeepScreenAwake,
    shouldUpdateLyrics: Boolean = true
) = AnimatedVisibility(
    visible = isDisplayed,
    enter = fadeIn(),
    exit = fadeOut()
) {
    val currentEnsureSongInserted by rememberUpdatedState(ensureSongInserted)
    val currentMediaMetadataProvider by rememberUpdatedState(mediaMetadataProvider)
    val currentDurationProvider by rememberUpdatedState(durationProvider)

    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val density = LocalDensity.current
    val view = LocalView.current

    var lyrics by remember { mutableStateOf<Lyrics?>(null) }

    val showSynchronizedLyrics = remember(shouldShowSynchronizedLyrics, lyrics) {
        shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() != true
    }

    var isEditing by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var isPicking by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var isError by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var invalidLrc by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }

    val text = remember(lyrics, showSynchronizedLyrics) {
        if (showSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
    }

    if (shouldKeepScreenAwake) DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(mediaId, shouldShowSynchronizedLyrics) {
        runCatching {
            withContext(Dispatchers.IO) {
                Database
                    .lyrics(mediaId)
                    .distinctUntilChanged()
                    .cancellable()
                    .collect { currentLyrics ->
                        if (
                            !shouldUpdateLyrics ||
                            (currentLyrics?.fixed != null && currentLyrics.synced != null)
                        ) lyrics = currentLyrics
                        else {
                            val mediaMetadata = currentMediaMetadataProvider()
                            var duration =
                                withContext(Dispatchers.Main) { currentDurationProvider() }

                            while (duration == C.TIME_UNSET) {
                                delay(100)
                                duration =
                                    withContext(Dispatchers.Main) { currentDurationProvider() }
                            }

                            val album = mediaMetadata.albumTitle?.toString()
                            val artist = mediaMetadata.artist?.toString().orEmpty()
                            val title = mediaMetadata.title?.toString().orEmpty()

                            lyrics = null
                            isError = false

                            Lyrics(
                                songId = mediaId,
                                fixed = (
                                        if (currentLyrics?.fixed == null)
                                            Innertube.lyrics(NextBody(videoId = mediaId))
                                                ?.getOrNull()
                                                ?: LrcLib.bestLyrics(
                                                    artist = artist,
                                                    title = title,
                                                    duration = duration.milliseconds,
                                                    album = album,
                                                    synced = false
                                                )?.map { it?.text }?.getOrNull()
                                        else currentLyrics.fixed
                                        ).orEmpty(),
                                synced = (
                                        if (currentLyrics?.synced == null)
                                            LrcLib.bestLyrics(
                                                artist = artist,
                                                title = title,
                                                duration = duration.milliseconds,
                                                album = album
                                            )?.map { it?.text }?.getOrNull()
                                                ?: KuGou.lyrics(
                                                    artist = artist,
                                                    title = title,
                                                    duration = duration / 1000
                                                )?.map { it?.value }?.getOrNull()
                                                ?: LrcLib.bestLyrics(
                                                    artist = artist,
                                                    title = title.split("(")[0].trim(),
                                                    duration = duration.milliseconds,
                                                    album = album
                                                )?.map { it?.text }?.getOrNull()
                                        else currentLyrics.synced
                                        ).orEmpty()
                            ).also {
                                transaction {
                                    runCatching {
                                        currentEnsureSongInserted()
                                        Database.upsert(it)
                                    }
                                }
                            }
                        }

                        isError =
                            (shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() == true) ||
                                    (!shouldShowSynchronizedLyrics && lyrics?.fixed?.isBlank() == true)
                    }
            }
        }.exceptionOrNull()
            ?.let { if (it is CancellationException) throw it else it.printStackTrace() }
    }

    if (isEditing) TextFieldDialog(
        hintText = stringResource(R.string.enter_lyrics),
        initialTextInput = (if (shouldShowSynchronizedLyrics) lyrics?.synced else lyrics?.fixed).orEmpty(),
        singleLine = false,
        maxLines = 10,
        isTextInputValid = { true },
        onDismiss = { isEditing = false },
        onAccept = {
            transaction {
                runCatching {
                    currentEnsureSongInserted()

                    Database.upsert(
                        if (shouldShowSynchronizedLyrics) Lyrics(
                            songId = mediaId,
                            fixed = lyrics?.fixed,
                            synced = it
                        ) else Lyrics(
                            songId = mediaId,
                            fixed = it,
                            synced = lyrics?.synced
                        )
                    )
                }
            }
        }
    )

    if (isPicking && shouldShowSynchronizedLyrics) DefaultDialog(
        onDismiss = { isPicking = false },
        horizontalPadding = 0.dp
    ) {
        val tracks = remember { mutableStateListOf<Track>() }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf(false) }

        var query by rememberSaveable {
            mutableStateOf(currentMediaMetadataProvider().title?.toString().orEmpty())
        }

        LaunchedEffect(query) {
            loading = true
            error = false

            delay(500)

            LrcLib.lyrics(query = query)?.onSuccess {
                tracks.clear()
                tracks.addAll(it)
                loading = false
                error = false
            }?.onFailure {
                loading = false
                error = true
            } ?: run { loading = false }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            maxLines = 1,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            error || tracks.isEmpty() -> BasicText(
                text = stringResource(R.string.no_lyrics_found),
                style = typography.s.semiBold.center,
                modifier = Modifier
                    .padding(all = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            else -> ValueSelectorDialogBody(
                onDismiss = { isPicking = false },
                title = stringResource(R.string.choose_lyric_track),
                selectedValue = null,
                values = tracks.toImmutableList(),
                onValueSelect = {
                    transaction {
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = lyrics?.fixed,
                                synced = it.syncedLyrics.orEmpty()
                            )
                        )
                        isPicking = false
                    }
                },
                valueText = {
                    "${it.artistName} - ${it.trackName} (${
                        it.duration.seconds.toComponents { minutes, seconds, _ ->
                            "$minutes:${seconds.toString().padStart(2, '0')}"
                        }
                    })"
                }
            )
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .fillMaxSize()
            .background(colorPalette.overlay)
    ) {
        val animatedHeight by animateDpAsState(
            targetValue = maxHeight,
            label = ""
        )

        AnimatedVisibility(
            visible = isError,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(
                    if (shouldShowSynchronizedLyrics) R.string.synchronized_lyrics_not_available
                    else R.string.lyrics_not_available
                ),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = invalidLrc && shouldShowSynchronizedLyrics,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(R.string.invalid_synchronized_lyrics),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth()
            )
        }

        AnimatedContent(
            targetState = showSynchronizedLyrics,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = ""
        ) { synchronized ->
            if (synchronized) {
                val lazyListState = rememberLazyListState()
                val synchronizedLyrics = remember(text) {
                    val lrc = lyrics?.synced ?: return@remember null
                    val sentences = LrcLib.Lyrics(lrc).sentences?.toImmutableMap()

                    invalidLrc = sentences == null
                    sentences?.let {
                        SynchronizedLyrics(sentences) {
                            binder?.player?.let { player ->
                                player.currentPosition + 50L - (lyrics?.startTime ?: 0L)
                            } ?: 0L
                        }
                    }
                }

                LaunchedEffect(synchronizedLyrics, density, animatedHeight) {
                    val currentSynchronizedLyrics = synchronizedLyrics ?: return@LaunchedEffect
                    val centerOffset = with(density) { (-animatedHeight / 3).roundToPx() }

                    lazyListState.animateScrollToItem(
                        index = currentSynchronizedLyrics.index + 1,
                        scrollOffset = centerOffset
                    )

                    while (true) {
                        delay(50)
                        if (!currentSynchronizedLyrics.update()) continue

                        lazyListState.animateScrollToItem(
                            index = currentSynchronizedLyrics.index + 1,
                            scrollOffset = centerOffset
                        )
                    }
                }

                if (synchronizedLyrics != null) LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = false,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .verticalFadingEdge()
                        .fillMaxWidth()
                ) {
                    item(key = "header", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight))
                    }
                    itemsIndexed(
                        items = synchronizedLyrics.sentences.values.toImmutableList()
                    ) { index, sentence ->
                        BasicText(
                            text = sentence,
                            style = typography.xs.center.medium.let {
                                if (index == synchronizedLyrics.index) it.color(Color.White)
                                else it.disabled
                            },
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp)
                        )
                    }
                    item(key = "footer", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight))
                    }
                }
            } else BasicText(
                text = lyrics?.fixed.orEmpty(),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .verticalFadingEdge()
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(vertical = maxHeight / 4, horizontal = 32.dp)
            )
        }

        if (text == null && !isError) Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.shimmer()
        ) {
            repeat(4) {
                TextPlaceholder(
                    color = colorPalette.onOverlayShimmer,
                    modifier = Modifier.alpha(1f - it * 0.2f)
                )
            }
        }

        if (onOpenDialog != null) Image(
            painter = painterResource(R.drawable.expand),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.onOverlay),
            modifier = Modifier
                .padding(all = 4.dp)
                .clickable(
                    indication = rememberRipple(bounded = false),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onOpenDialog()
                    }
                )
                .padding(all = 8.dp)
                .size(20.dp)
                .align(Alignment.BottomStart)
        )

        Image(
            painter = painterResource(R.drawable.ellipsis_horizontal),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.onOverlay),
            modifier = Modifier
                .padding(all = 4.dp)
                .clickable(
                    indication = rememberRipple(bounded = false),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onMenuLaunch()
                        menuState.display {
                            Menu {
                                MenuEntry(
                                    icon = R.drawable.time,
                                    text = stringResource(
                                        if (shouldShowSynchronizedLyrics) R.string.show_unsynchronized_lyrics
                                        else R.string.show_synchronized_lyrics
                                    ),
                                    secondaryText = if (shouldShowSynchronizedLyrics) null
                                    else stringResource(R.string.provided_lyrics_by),
                                    onClick = {
                                        menuState.hide()
                                        setShouldShowSynchronizedLyrics(!shouldShowSynchronizedLyrics)
                                    }
                                )

                                MenuEntry(
                                    icon = R.drawable.pencil,
                                    text = stringResource(R.string.edit_lyrics),
                                    onClick = {
                                        menuState.hide()
                                        isEditing = true
                                    }
                                )

                                MenuEntry(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.search_lyrics_online),
                                    onClick = {
                                        menuState.hide()
                                        val mediaMetadata = currentMediaMetadataProvider()

                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                    putExtra(
                                                        SearchManager.QUERY,
                                                        "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                    )
                                                }
                                            )
                                        } catch (e: ActivityNotFoundException) {
                                            context.toast(context.getString(R.string.no_browser_installed))
                                        }
                                    }
                                )

                                MenuEntry(
                                    icon = R.drawable.sync,
                                    text = stringResource(R.string.refetch_lyrics),
                                    enabled = lyrics != null,
                                    onClick = {
                                        menuState.hide()

                                        transaction {
                                            runCatching {
                                                currentEnsureSongInserted()

                                                Database.upsert(
                                                    if (shouldShowSynchronizedLyrics) Lyrics(
                                                        songId = mediaId,
                                                        fixed = lyrics?.fixed,
                                                        synced = null
                                                    ) else Lyrics(
                                                        songId = mediaId,
                                                        fixed = null,
                                                        synced = lyrics?.synced
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )

                                if (shouldShowSynchronizedLyrics) {
                                    MenuEntry(
                                        icon = R.drawable.download,
                                        text = stringResource(R.string.pick_from_lrclib),
                                        onClick = {
                                            menuState.hide()
                                            isPicking = true
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.play_skip_forward,
                                        text = stringResource(R.string.set_lyrics_start_offset),
                                        secondaryText = stringResource(
                                            R.string.set_lyrics_start_offset_description
                                        ),
                                        onClick = {
                                            menuState.hide()
                                            lyrics?.let {
                                                val startTime = binder?.player?.currentPosition
                                                query {
                                                    Database.upsert(it.copy(startTime = startTime))
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                .padding(all = 8.dp)
                .size(20.dp)
                .align(Alignment.BottomEnd)
        )
    }
}
