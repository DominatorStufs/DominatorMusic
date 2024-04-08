package app.vitune.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.work.Configuration
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.service.PlayerService
import app.vitune.android.service.downloadState
import app.vitune.android.ui.components.BottomSheetMenu
import app.vitune.android.ui.components.rememberBottomSheetState
import app.vitune.android.ui.components.themed.LinearProgressIndicator
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.home.HomeScreen
import app.vitune.android.ui.screens.player.Player
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.utils.DisposableListener
import app.vitune.android.utils.LocalMonetCompat
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.collectProvidedBitmapAsState
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.intent
import app.vitune.android.utils.invokeOnReady
import app.vitune.android.utils.setDefaultPalette
import app.vitune.android.utils.toast
import app.vitune.compose.persist.LocalPersistMap
import app.vitune.compose.persist.PersistMap
import app.vitune.compose.preferences.PreferencesHolder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.SystemBarAppearance
import app.vitune.core.ui.appearance
import app.vitune.core.ui.dynamicColorPaletteOf
import app.vitune.core.ui.enums.ColorPaletteName
import app.vitune.core.ui.isPureBlackAvailable
import app.vitune.core.ui.rippleTheme
import app.vitune.core.ui.shimmerTheme
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.requests.playlistPage
import app.vitune.providers.innertube.requests.song
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.util.DebugLogger
import com.kieronquinn.monetcompat.core.MonetActivityAccessException
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.valentinilk.shimmer.LocalShimmerTheme
import dev.kdrag0n.monet.theme.ColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity(), MonetColorsChangedListener {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) this@MainActivity.binder = service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            // Try to rebind, otherwise fail
            unbindService(this)
            bindService(intent<PlayerService>(), this, Context.BIND_AUTO_CREATE)
        }
    }

    private var _monet: MonetCompat? by mutableStateOf(null)
    private val monet get() = _monet ?: throw MonetActivityAccessException()

    private var binder by mutableStateOf<PlayerService.Binder?>(null)

    override fun onStart() {
        super.onStart()
        bindService(intent<PlayerService>(), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        MonetCompat.setup(this)
        _monet = MonetCompat.getInstance()
        monet.setDefaultPalette()
        monet.addMonetColorsChangedListener(
            listener = this,
            notifySelf = false
        )
        monet.updateMonetColors()
        monet.invokeOnReady {
            setContent()
        }

        onNewIntent(intent)
    }

    @Composable
    fun AppWrapper(
        modifier: Modifier = Modifier,
        content: @Composable BoxWithConstraintsScope.() -> Unit
    ) = with(AppearancePreferences) {
        val sampleBitmap by binder.collectProvidedBitmapAsState()
        val appearance = appearance(
            name = colorPaletteName,
            mode = colorPaletteMode,
            usePureBlack = usePureBlack &&
                isPureBlackAvailable(colorPaletteName, colorPaletteMode),
            fontFamily = fontFamily,
            materialAccentColor = Color(monet.getAccentColor(this@MainActivity)),
            sampleBitmap = sampleBitmap,
            applyFontPadding = applyFontPadding,
            thumbnailRoundness = thumbnailRoundness.dp
        )

        SystemBarAppearance(palette = appearance.colorPalette)

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(appearance.colorPalette.background0)
        ) {
            CompositionLocalProvider(LocalAppearance provides appearance) {
                content()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    @OptIn(ExperimentalLayoutApi::class)
    fun setContent() {
        val fromNotification = intent?.extras?.getBoolean("fromNotification") == true

        setContent {
            AppWrapper {
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

                val imeVisible = WindowInsets.isImeVisible
                val imeBottomDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }
                val animatedBottomDp by animateDpAsState(
                    targetValue = if (imeVisible) 0.dp else bottomDp,
                    label = ""
                )

                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.items.collapsedPlayerHeight + bottomDp,
                    expandedBound = maxHeight
                )

                val playerAwareWindowInsets = remember(
                    bottomDp,
                    animatedBottomDp,
                    playerBottomSheetState.value,
                    imeVisible,
                    imeBottomDp
                ) {
                    val bottom =
                        if (imeVisible) imeBottomDp.coerceAtLeast(playerBottomSheetState.value)
                        else playerBottomSheetState.value.coerceIn(
                            animatedBottomDp..playerBottomSheetState.collapsedBound
                        )

                    windowsInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(bottom = bottom))
                }

                CompositionLocalProvider(
                    LocalIndication provides rememberRipple(),
                    LocalRippleTheme provides rippleTheme(),
                    LocalShimmerTheme provides shimmerTheme(),
                    LocalPlayerServiceBinder provides binder,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalPersistMap provides Dependencies.application.persistMap,
                    LocalMonetCompat provides monet
                ) {
                    val isDownloading by downloadState.collectAsState()

                    Box {
                        HomeScreen(
                            onPlaylistUrl = { url ->
                                onNewIntent(Intent.parseUri(url, 0))
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isDownloading,
                        modifier = Modifier.padding(playerAwareWindowInsets.asPaddingValues())
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }

                    CompositionLocalProvider(
                        LocalAppearance provides LocalAppearance.current.let {
                            if (
                                AppearancePreferences.colorPaletteName == ColorPaletteName.AMOLED
                            ) it.copy(
                                colorPalette = dynamicColorPaletteOf(
                                    accentColor = it.colorPalette.accent,
                                    isDark = true,
                                    isAmoled = false
                                )
                            ) else it
                        }
                    ) {
                        Player(
                            layoutState = playerBottomSheetState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    BottomSheetMenu(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .imePadding()
                    )
                }

                LaunchedEffect(binder?.player) {
                    val player = binder?.player ?: return@LaunchedEffect

                    when {
                        player.currentMediaItem == null ->
                            if (!playerBottomSheetState.isDismissed) playerBottomSheetState.dismiss()

                        playerBottomSheetState.isDismissed -> if (fromNotification) {
                            intent.replaceExtras(null)
                            playerBottomSheetState.expandSoft()
                        } else playerBottomSheetState.collapseSoft()
                    }
                }

                binder?.player?.DisposableListener {
                    object : Player.Listener {
                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) = when {
                            reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED || mediaItem == null -> Unit
                            mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") == true ->
                                playerBottomSheetState.collapseSoft()

                            else -> playerBottomSheetState.expandSoft()
                        }
                    }
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri() ?: return

        intent?.data = null
        this.intent = null

        Log.d(TAG, "Opening url: $uri")

        lifecycleScope.launch(Dispatchers.IO) {
            when (val path = uri.pathSegments.firstOrNull()) {
                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                    val browseId = "VL$playlistId"

                    if (playlistId.startsWith("OLAK5uy_")) {
                        Innertube.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let {
                            it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                albumRoute.ensureGlobal(browseId)
                            }
                        }
                    } else {
                        playlistRoute.ensureGlobal(browseId, uri.getQueryParameter("params"), null)
                    }
                }

                "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                    artistRoute.ensureGlobal(channelId)
                }

                else -> when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> path
                    else -> {
                        toast(getString(R.string.error_url, uri))
                        null
                    }
                }?.let { videoId ->
                    Innertube.song(videoId)?.getOrNull()?.let { song ->
                        val binder = snapshotFlow { binder }.filterNotNull().first()

                        withContext(Dispatchers.Main) {
                            binder.player.forcePlay(song.asMediaItem)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monet.removeMonetColorsChangedListener(this)
        _monet = null
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onMonetColorsChanged(
        monet: MonetCompat,
        monetColors: ColorScheme,
        isInitialChange: Boolean
    ) {
        if (!isInitialChange) recreate()
    }
}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No player insets provided") }

class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    override fun onCreate() {
        MonetCompat.debugLog = BuildConfig.DEBUG
        super.onCreate()

        Dependencies.init(this)
        MonetCompat.enablePaletteCompat()
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes(DataPreferences.coilDiskCacheMaxSize.bytes)
                .build()
        )
        .let { if (BuildConfig.DEBUG) it.logger(DebugLogger()) else it }
        .build()

    val persistMap = PersistMap()

    override val workManagerConfiguration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
        .build()
}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
        DatabaseInitializer()
    }
}

open class GlobalPreferencesHolder : PreferencesHolder(Dependencies.application, "preferences")
