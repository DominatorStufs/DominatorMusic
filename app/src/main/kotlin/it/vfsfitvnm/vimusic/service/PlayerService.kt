package it.vfsfitvnm.vimusic.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.requests.player
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.MainActivity
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheSize
import it.vfsfitvnm.vimusic.models.Event
import it.vfsfitvnm.vimusic.models.QueuedMediaItem
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongWithContentLength
import it.vfsfitvnm.vimusic.preferences.AppearancePreferences
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.utils.ConditionalCacheDataSourceFactory
import it.vfsfitvnm.vimusic.utils.InvincibleService
import it.vfsfitvnm.vimusic.utils.RingBuffer
import it.vfsfitvnm.vimusic.utils.TimerJob
import it.vfsfitvnm.vimusic.utils.YouTubeRadio
import it.vfsfitvnm.vimusic.utils.activityPendingIntent
import it.vfsfitvnm.vimusic.utils.broadcastPendingIntent
import it.vfsfitvnm.vimusic.utils.findNextMediaItemById
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid10
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid8
import it.vfsfitvnm.vimusic.utils.mediaItems
import it.vfsfitvnm.vimusic.utils.shouldBePlaying
import it.vfsfitvnm.vimusic.utils.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import android.os.Binder as AndroidBinder

const val LOCAL_KEY_PREFIX = "local:"

@get:OptIn(UnstableApi::class)
val DataSpec.isLocal get() = key?.startsWith(LOCAL_KEY_PREFIX) == true

val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX)
val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX)

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass", "TooManyFunctions") // intended in this class: it is a service
@OptIn(UnstableApi::class)
class PlayerService : InvincibleService(), Player.Listener, PlaybackStatsListener.Callback {
    private lateinit var mediaSession: MediaSession
    private lateinit var cache: SimpleCache
    private lateinit var player: ExoPlayer

    private val stateBuilder
        get() = PlaybackState.Builder().setActions(
            PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_REWIND
        ).addCustomAction(
            /* action = */ "LIKE",
            /* name   = */ "Like",
            /* icon   = */ if (isLikedState.value) R.drawable.heart else R.drawable.heart_outline
        )

    private val playbackStateMutex = Mutex()

    private val metadataBuilder = MediaMetadata.Builder()

    private var notificationManager: NotificationManager? = null
    private var timerJob: TimerJob? = null
    private var radio: YouTubeRadio? = null

    private lateinit var bitmapProvider: BitmapProvider

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var preferenceUpdaterJob: Job? = null

    private var volumeNormalizationJob: Job? = null

    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null

    private val binder = Binder()

    private var isNotificationStarted = false

    override val notificationId get() = NOTIFICATION_ID

    private lateinit var notificationActionReceiver: NotificationActionReceiver

    private val mediaItemState = MutableStateFlow<MediaItem?>(null)
    private val isLikedState = mediaItemState
        .flatMapMerge { item ->
            item?.mediaId?.let { Database.likedAt(it).distinctUntilChanged() } ?: flowOf(null)
        }
        .map { it != null }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override fun onBind(intent: Intent?): AndroidBinder {
        super.onBind(intent)
        return binder
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onCreate() {
        super.onCreate()

        bitmapProvider = BitmapProvider(
            bitmapSize = (256 * resources.displayMetrics.density).roundToInt(),
            colorProvider = { isSystemInDarkMode ->
                if (isSystemInDarkMode) Color.BLACK else Color.WHITE
            }
        )

        createNotificationChannel()

        val cacheEvictor = when (val size = DataPreferences.exoPlayerDiskCacheMaxSize) {
            ExoPlayerDiskCacheSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }

        val directory = cacheDir.resolve("exoplayer").also { directory ->
            if (directory.exists()) return@also

            directory.mkdir()

            cacheDir.listFiles()?.forEach {
                @Suppress("ComplexCondition")
                if (
                    (it.isDirectory && it.name.length == 1 && it.name.isDigitsOnly() || it.extension == "uid") &&
                    !it.renameTo(directory.resolve(it.name))
                ) it.deleteRecursively()
            }

            filesDir.resolve("coil").deleteRecursively()
        }

        cache = SimpleCache(directory, cacheEvictor, StandaloneDatabaseProvider(this))

        player = ExoPlayer.Builder(this, createRendersFactory(), createMediaSourceFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()

        updateRepeatMode()

        player.skipSilenceEnabled = PlayerPreferences.skipSilence
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, this))

        maybeRestorePlayerQueue()

        mediaSession = MediaSession(baseContext, "PlayerService")
        mediaSession.setCallback(SessionCallback(player))
        mediaSession.setPlaybackState(stateBuilder.build())
        mediaSession.isActive = true

        coroutineScope.launch {
            var first = true
            mediaItemState.zip(isLikedState) { mediaItem, _ ->
                // work around NPE in other processes
                if (first) {
                    first = false
                    return@zip
                }
                if (mediaItem == null) return@zip
                withContext(Dispatchers.Main) {
                    updatePlaybackState()
                    // work around NPE in other processes
                    handler.post {
                        runCatching {
                            applicationContext.getSystemService<NotificationManager>()
                                ?.notify(NOTIFICATION_ID, notification())
                        }
                    }
                }
            }.collect()
        }

        notificationActionReceiver = NotificationActionReceiver()

        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
            addAction(Action.like.value)
        }

        ContextCompat.registerReceiver(
            /* context  = */ this,
            /* receiver = */ notificationActionReceiver,
            /* filter   = */ filter,
            /* flags    = */ ContextCompat.RECEIVER_NOT_EXPORTED
        )

        maybeResumePlaybackWhenDeviceConnected()

        fun <T> CoroutineScope.stateCallback(
            runner: suspend (suspend () -> Unit) -> Unit = { withContext(Dispatchers.Main) { it() } },
            state: () -> T,
            callback: suspend () -> Unit
        ) = launch { snapshotFlow(state).collectLatest { runner(callback) } }

        preferenceUpdaterJob = coroutineScope.launch {
            with(PlayerPreferences) {
                stateCallback(
                    state = { resumePlaybackWhenDeviceConnected },
                    callback = ::maybeResumePlaybackWhenDeviceConnected
                )
                stateCallback(
                    state = { AppearancePreferences.isShowingThumbnailInLockscreen },
                    callback = ::maybeShowSongCoverInLockScreen
                )
                stateCallback(state = { trackLoopEnabled }, callback = ::updateRepeatMode)
                stateCallback(state = { queueLoopEnabled }, callback = ::updateRepeatMode)
                stateCallback(state = { volumeNormalization }, callback = ::maybeNormalizeVolume)
                stateCallback(
                    state = { volumeNormalizationBaseGain },
                    callback = ::maybeNormalizeVolume
                )
                stateCallback(state = { bassBoost }, callback = ::maybeBassBoost)
                stateCallback(state = { bassBoostLevel }, callback = ::maybeBassBoost)
                stateCallback(
                    state = { speed },
                    callback = { player.setPlaybackSpeed(speed.coerceAtLeast(0.01f)) }
                )
                stateCallback(
                    state = { isInvincibilityEnabled },
                    callback = {
                        this@PlayerService.isInvincibilityEnabled = isInvincibilityEnabled
                    }
                )
                stateCallback(
                    state = { skipSilence },
                    callback = { player.skipSilenceEnabled = skipSilence },
                    runner = { handler.post { runBlocking { it() } } }
                )
            }
        }
    }

    private fun updateRepeatMode() {
        player.repeatMode = when {
            PlayerPreferences.trackLoopEnabled -> Player.REPEAT_MODE_ONE
            PlayerPreferences.queueLoopEnabled -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.shouldBePlaying || PlayerPreferences.stopWhenClosed)
            broadcastPendingIntent<NotificationDismissReceiver>().send()
        super.onTaskRemoved(rootIntent)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
        maybeSavePlayerQueue()

    override fun onDestroy() {
        maybeSavePlayerQueue()

        player.removeListener(this)
        player.stop()
        player.release()

        unregisterReceiver(notificationActionReceiver)

        mediaSession.isActive = false
        mediaSession.release()
        cache.release()

        loudnessEnhancer?.release()

        preferenceUpdaterJob?.cancel()

        coroutineScope.cancel()

        super.onDestroy()
    }

    override fun shouldBeInvincible() = !player.shouldBePlaying

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (bitmapProvider.setDefaultBitmap() && player.currentMediaItem != null)
            notificationManager?.notify(NOTIFICATION_ID, notification())
        super.onConfigurationChanged(newConfig)
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000 && !DataPreferences.pausePlaytime) query {
            Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
        }

        if (totalPlayTimeMs > 30000 && !DataPreferences.pauseHistory) query {
            runCatching {
                Database.insert(
                    Event(
                        songId = mediaItem.mediaId,
                        timestamp = System.currentTimeMillis(),
                        playTime = totalPlayTimeMs
                    )
                )
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItemState.update { mediaItem }

        maybeRecoverPlaybackError()
        maybeNormalizeVolume()
        maybeProcessRadio()

        when {
            mediaItem == null -> bitmapProvider.listener?.invoke(null)
            mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri ->
                bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
            updateMediaSessionQueue(player.currentTimeline)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) return
        updateMediaSessionQueue(timeline)
        maybeSavePlayerQueue()
    }

    private fun updateMediaSessionQueue(timeline: Timeline) {
        val builder = MediaDescription.Builder()

        val currentMediaItemIndex = player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) endIndex -= startIndex

        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        mediaSession.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSession.QueueItem(
                    builder
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(mediaItem.mediaMetadata.title)
                        .setSubtitle(mediaItem.mediaMetadata.artist)
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
    }

    private fun maybeRecoverPlaybackError() {
        if (player.playerError != null) player.prepare()
    }

    private fun maybeProcessRadio() = radio?.let { radio ->
        if (player.mediaItemCount - player.currentMediaItemIndex <= 3)
            coroutineScope.launch(Dispatchers.Main) {
                player.addMediaItems(radio.process())
            }
        Unit
    }

    private fun maybeSavePlayerQueue() {
        if (!PlayerPreferences.persistentQueue) return

        val mediaItems = player.currentTimeline.mediaItems
        val mediaItemIndex = player.currentMediaItemIndex
        val mediaItemPosition = player.currentPosition

        query {
            Database.clearQueue()
            Database.insert(
                mediaItems.mapIndexed { index, mediaItem ->
                    QueuedMediaItem(
                        mediaItem = mediaItem,
                        position = if (index == mediaItemIndex) mediaItemPosition else null
                    )
                }
            )
        }
    }

    private fun maybeRestorePlayerQueue() {
        if (!PlayerPreferences.persistentQueue) return

        query {
            val queuedSong = Database.queue()
            Database.clearQueue()

            if (queuedSong.isEmpty()) return@query

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            runBlocking(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )
                player.prepare()

                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                startForeground(NOTIFICATION_ID, notification())
            }
        }
    }

    private fun maybeNormalizeVolume() {
        if (!PlayerPreferences.volumeNormalization) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob?.invokeOnCompletion { volumeNormalizationJob = null }
            player.volume = 1f
            return
        }

        if (loudnessEnhancer == null) loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)

        val songId = player.currentMediaItem?.mediaId ?: return
        volumeNormalizationJob?.cancel()
        volumeNormalizationJob = coroutineScope.launch {
            runCatching {
                Database.loudnessDb(songId).cancellable().collectLatest { loudness ->
                    Database.loudnessBoost(songId).cancellable().collectLatest { boost ->
                        withContext(Dispatchers.Main) {
                            loudnessEnhancer?.setTargetGain(
                                PlayerPreferences.volumeNormalizationBaseGainRounded +
                                        ((boost ?: 0f) * 100).toInt() -
                                        ((loudness ?: 0f) * 100).toInt()
                            )
                            loudnessEnhancer?.enabled = true
                        }
                    }
                }
            }
        }
    }

    private fun maybeBassBoost() {
        if (!PlayerPreferences.bassBoost) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
            maybeNormalizeVolume()
            return
        }

        runCatching {
            if (bassBoost == null) bassBoost = BassBoost(0, player.audioSessionId)
            bassBoost?.setStrength(PlayerPreferences.bassBoostLevel.toShort())
            bassBoost?.enabled = true
        }.onFailure {
            Toast.makeText(this, R.string.error_bassboost_init, Toast.LENGTH_SHORT).show()
        }
    }

    private fun maybeShowSongCoverInLockScreen() = handler.post {
        val bitmap = if (isAtLeastAndroid13 || AppearancePreferences.isShowingThumbnailInLockscreen)
            bitmapProvider.bitmap else null

        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)

        if (isAtLeastAndroid13 && player.currentMediaItemIndex == 0) metadataBuilder.putText(
            MediaMetadata.METADATA_KEY_TITLE,
            "${player.mediaMetadata.title} "
        )

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun maybeResumePlaybackWhenDeviceConnected() {
        if (!isAtLeastAndroid6) return

        if (!PlayerPreferences.resumePlaybackWhenDeviceConnected) {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
            return
        }
        if (audioManager == null) audioManager = getSystemService<AudioManager>()

        audioDeviceCallback =
            @SuppressLint("NewApi")
            object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo) =
                    audioDeviceInfo.isSink && (
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                                    audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                    audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                            )
                        .let {
                            if (!isAtLeastAndroid8) it else
                                it || audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET
                        }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) player.play()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)
    }

    private fun sendOpenEqualizerIntent() = sendBroadcast(
        Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
    )

    private fun sendCloseEqualizerIntent() = sendBroadcast(
        Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
        }
    )

    private fun updatePlaybackState() = coroutineScope.launch {
        playbackStateMutex.withLock {
            withContext(Dispatchers.Main) {
                mediaSession.setPlaybackState(
                    stateBuilder
                        .setState(player.androidPlaybackState, player.currentPosition, 1f)
                        .setBufferedPosition(player.bufferedPosition)
                        .build()
                )
            }
        }
    }

    private val Player.androidPlaybackState
        get() = when (playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackState.STATE_BUFFERING else PlaybackState.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackState.STATE_NONE
            else -> PlaybackState.STATE_NONE
        }

    // legacy behavior may cause inconsistencies, but not available on sdk 24 or lower
    @Suppress("DEPRECATION")
    override fun onEvents(player: Player, events: Player.Events) {
        if (player.duration != C.TIME_UNSET) mediaSession.setMetadata(
            metadataBuilder
                .putText(MediaMetadata.METADATA_KEY_TITLE, player.mediaMetadata.title)
                .putText(MediaMetadata.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
                .putText(MediaMetadata.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
                .build()
        )

        updatePlaybackState()

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            val notification = notification()

            if (notification == null) {
                isNotificationStarted = false
                makeInvincible(false)
                stopForeground(false)
                sendCloseEqualizerIntent()
                notificationManager?.cancel(NOTIFICATION_ID)
                return
            }

            if (player.shouldBePlaying && !isNotificationStarted) {
                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                startForeground(NOTIFICATION_ID, notification)
                makeInvincible(false)
                sendOpenEqualizerIntent()
            } else {
                if (!player.shouldBePlaying) {
                    isNotificationStarted = false
                    stopForeground(false)
                    makeInvincible(true)
                    sendCloseEqualizerIntent()
                }
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun notification(): Notification? {
        if (player.currentMediaItem == null) return null

        val playIntent = Action.play.pendingIntent
        val pauseIntent = Action.pause.pendingIntent
        val nextIntent = Action.next.pendingIntent
        val prevIntent = Action.previous.pendingIntent
        val likeIntent = Action.like.pendingIntent

        val mediaMetadata = player.mediaMetadata

        @Suppress("DEPRECATION") // support for SDK < 26
        val builder = if (isAtLeastAndroid8) {
            Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        } else {
            Notification.Builder(applicationContext)
        }
            .setContentTitle(mediaMetadata.title)
            .setContentText(mediaMetadata.artist)
            .setSubText(player.playerError?.message)
            .setLargeIcon(bitmapProvider.bitmap)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(
                player.playerError?.let { R.drawable.alert_circle } ?: R.drawable.app_icon
            )
            .setOngoing(false)
            .setContentIntent(
                activityPendingIntent<MainActivity>(
                    flags = PendingIntent.FLAG_UPDATE_CURRENT
                ) {
                    putExtra("expandPlayerBottomSheet", true)
                }
            )
            .setDeleteIntent(broadcastPendingIntent<NotificationDismissReceiver>())
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .addAction(R.drawable.play_skip_back, getString(R.string.skip_back), prevIntent)
            .addAction(
                if (player.shouldBePlaying) R.drawable.pause else R.drawable.play,
                if (player.shouldBePlaying) getString(R.string.pause) else getString(R.string.play),
                if (player.shouldBePlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, getString(R.string.skip_forward), nextIntent)
            .addAction(
                if (isLikedState.value) R.drawable.heart else R.drawable.heart_outline,
                getString(R.string.like),
                likeIntent
            )

        bitmapProvider.load(mediaMetadata.artworkUri) { bitmap ->
            maybeShowSongCoverInLockScreen()
            notificationManager?.notify(NOTIFICATION_ID, builder.setLargeIcon(bitmap).build())
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService()

        if (!isAtLeastAndroid8) return

        notificationManager?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.now_playing),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                }
            )

            if (getNotificationChannel(SLEEP_TIMER_NOTIFICATION_CHANNEL_ID) == null)
                createNotificationChannel(
                    NotificationChannel(
                        SLEEP_TIMER_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.sleep_timer),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
        }
    }

    private fun createCacheDataSource() = ConditionalCacheDataSourceFactory(
        cacheDataSourceFactory = CacheDataSource.Factory().setCache(cache),
        upstreamDataSourceFactory = DefaultDataSource.Factory(
            applicationContext,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(16000)
                .setReadTimeoutMs(8000)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0")
        )
    ) { !it.isLocal }

    @Suppress("CyclomaticComplexMethod")
    private fun createDataSourceFactory(): DataSource.Factory {
        val chunkLength = 512 * 1024L
        val ringBuffer = RingBuffer<Pair<String, Uri>?>(2) { null }

        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val videoId = dataSpec.key ?: error("A key must be set")

            when {
                dataSpec.isLocal ||
                        cache.isCached(videoId, dataSpec.position, chunkLength) -> dataSpec

                videoId == ringBuffer.getOrNull(0)?.first ->
                    dataSpec.withUri(ringBuffer.getOrNull(0)!!.second)

                videoId == ringBuffer.getOrNull(1)?.first ->
                    dataSpec.withUri(ringBuffer.getOrNull(1)!!.second)

                else -> {
                    val body = runBlocking(Dispatchers.IO) {
                        Innertube.player(PlayerBody(videoId = videoId))
                    }?.getOrThrow()

                    if (body?.videoDetails?.videoId != videoId) throw VideoIdMismatchException()

                    val url = when (val status = body.playabilityStatus?.status) {
                        "OK" -> body.streamingData?.highestQualityFormat?.let { format ->
                            val mediaItem = runBlocking(Dispatchers.Main) {
                                player.findNextMediaItemById(videoId)
                            }

                            if (mediaItem?.mediaMetadata?.extras?.getString("durationText") == null)
                                format.approxDurationMs?.div(1000)
                                    ?.let(DateUtils::formatElapsedTime)?.removePrefix("0")
                                    ?.let { durationText ->
                                        mediaItem?.mediaMetadata?.extras?.putString(
                                            "durationText",
                                            durationText
                                        )
                                        Database.updateDurationText(videoId, durationText)
                                    }

                            query {
                                mediaItem?.let(Database::insert)

                                Database.insert(
                                    it.vfsfitvnm.vimusic.models.Format(
                                        songId = videoId,
                                        itag = format.itag,
                                        mimeType = format.mimeType,
                                        bitrate = format.bitrate,
                                        loudnessDb = body.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                        contentLength = format.contentLength,
                                        lastModified = format.lastModified
                                    )
                                )
                            }

                            format.url
                        } ?: throw PlayableFormatNotFoundException()

                        "UNPLAYABLE" -> throw UnplayableException()
                        "LOGIN_REQUIRED" -> throw LoginRequiredException()

                        else -> throw PlaybackException(
                            status,
                            null,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }

                    ringBuffer.append(videoId to url.toUri())
                    dataSpec.withUri(url.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)
                }
            }
        }
    }

    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
        /* dataSourceFactory = */ createDataSourceFactory(),
        /* extractorsFactory = */ createExtractorsFactory()
    )

    private fun createExtractorsFactory() = DefaultExtractorsFactory()

    private fun createRendersFactory(): RenderersFactory {
        val minimumSilenceDuration = PlayerPreferences.minimumSilence.coerceIn(1000L..2_000_000L)
        val audioSink = DefaultAudioSink.Builder(applicationContext)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(false)
            .setAudioOffloadSupportProvider(DefaultAudioOffloadSupportProvider(applicationContext))
            .setAudioProcessorChain(
                DefaultAudioProcessorChain(
                    arrayOf(),
                    SilenceSkippingAudioProcessor(
                        /* minimumSilenceDurationUs = */ minimumSilenceDuration,
                        /* paddingSilenceUs         = */ minimumSilenceDuration / 100L,
                        /* silenceThresholdLevel    = */ 256
                    ),
                    SonicAudioProcessor()
                )
            )
            .build()
            .apply {
                if (isAtLeastAndroid10) setOffloadMode(AudioSink.OFFLOAD_MODE_DISABLED)
            }

        return RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audioSink
                )
            )
        }
    }

    inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val cache: Cache
            get() = this@PlayerService.cache

        val mediaSession
            get() = this@PlayerService.mediaSession

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        var invincible
            get() = isInvincibilityEnabled
            set(value) {
                isInvincibilityEnabled = value
            }

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()

            timerJob = coroutineScope.timer(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@PlayerService, SLEEP_TIMER_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.sleep_timer_ended))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .build()

                notificationManager?.notify(SLEEP_TIMER_NOTIFICATION_ID, notification)

                stopSelf()
                exitProcess(0)
            }
        }

        fun cancelSleepTimer() {
            timerJob?.cancel()
            timerJob = null
        }

        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = true)

        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = false)

        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean) {
            radioJob?.cancel()
            radio = null
            YouTubeRadio(
                endpoint?.videoId,
                endpoint?.playlistId,
                endpoint?.playlistSetVideoId,
                endpoint?.params
            ).let { radioData ->
                isLoadingRadio = true
                radioJob = coroutineScope.launch(Dispatchers.Main) {
                    val items = radioData.process().let { Database.filterBlacklistedSongs(it) }
                    if (justAdd) player.addMediaItems(items.drop(1))
                    else player.forcePlayFromBeginning(items)

                    radio = radioData
                    isLoadingRadio = false
                }
            }
        }

        fun stopRadio() {
            isLoadingRadio = false
            radioJob?.cancel()
            radio = null
        }

        /**
         * This method should ONLY be called when the application (sc. activity) is in the foreground!
         */
        fun restartForegroundOrStop() {
            player.pause()
            isInvincibilityEnabled = false
            stopSelf()
        }

        fun isCached(song: SongWithContentLength) =
            song.contentLength?.let { cache.isCached(song.song.id, 0L, it) } ?: false
    }

    private fun likeAction() = mediaItemState.value?.let { mediaItem ->
        transaction {
            Database.like(
                mediaItem.mediaId,
                if (isLikedState.value) null else System.currentTimeMillis()
            )
        }
    }.let { }

    private inner class SessionCallback(private val player: Player) : MediaSession.Callback() {
        override fun onPlay() = player.play()
        override fun onPause() = player.pause()
        override fun onSkipToPrevious() = runCatching(player::forceSeekToPrevious).let { }
        override fun onSkipToNext() = runCatching(player::forceSeekToNext).let { }
        override fun onSeekTo(pos: Long) = player.seekTo(pos)
        override fun onStop() = player.pause()
        override fun onRewind() = player.seekToDefaultPosition()
        override fun onSkipToQueueItem(id: Long) =
            runCatching { player.seekToDefaultPosition(id.toInt()) }.let { }

        override fun onCustomAction(action: String, extras: Bundle?) {
            super.onCustomAction(action, extras)
            if (action == "LIKE") likeAction()
        }
    }

    inner class NotificationActionReceiver internal constructor() : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Action.pause.value -> player.pause()
                Action.play.value -> player.play()
                Action.next.value -> player.forceSeekToNext()
                Action.previous.value -> player.forceSeekToPrevious()
                Action.like.value -> likeAction()
            }
        }
    }

    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.stopService(context.intent<PlayerService>())
        }
    }

    @JvmInline
    private value class Action(val value: String) {
        context(Context)
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                this@Context,
                100,
                Intent(value).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {
            val pause = Action("it.vfsfitvnm.vimusic.pause")
            val play = Action("it.vfsfitvnm.vimusic.play")
            val next = Action("it.vfsfitvnm.vimusic.next")
            val previous = Action("it.vfsfitvnm.vimusic.previous")
            val like = Action("it.vfsfitvnm.vimusic.like")
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "default_channel_id"

        const val SLEEP_TIMER_NOTIFICATION_ID = 1002
        const val SLEEP_TIMER_NOTIFICATION_CHANNEL_ID = "sleep_timer_channel_id"
    }
}
