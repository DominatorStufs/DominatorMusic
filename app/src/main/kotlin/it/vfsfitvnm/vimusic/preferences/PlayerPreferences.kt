package it.vfsfitvnm.vimusic.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.R

object PlayerPreferences : GlobalPreferencesHolder() {
    val isInvincibilityEnabledProperty = boolean(false)
    var isInvincibilityEnabled by isInvincibilityEnabledProperty
    val trackLoopEnabledProperty = boolean(false)
    var trackLoopEnabled by trackLoopEnabledProperty
    val queueLoopEnabledProperty = boolean(true)
    var queueLoopEnabled by queueLoopEnabledProperty
    val skipSilenceProperty = boolean(false)
    var skipSilence by skipSilenceProperty
    val minimumSilenceProperty = long(2_000_000L)
    var minimumSilence by minimumSilenceProperty
    val volumeNormalizationProperty = boolean(false)
    var volumeNormalization by volumeNormalizationProperty
    val volumeNormalizationBaseGainProperty = float(5.00f)
    var volumeNormalizationBaseGain by volumeNormalizationBaseGainProperty
    val bassBoostProperty = boolean(false)
    var bassBoost by bassBoostProperty
    val bassBoostLevelProperty = int(5)
    var bassBoostLevel by bassBoostLevelProperty
    val resumePlaybackWhenDeviceConnectedProperty = boolean(false)
    var resumePlaybackWhenDeviceConnected by resumePlaybackWhenDeviceConnectedProperty
    val persistentQueueProperty = boolean(false)
    var persistentQueue by persistentQueueProperty
    val isShowingLyricsProperty = boolean(false)
    var isShowingLyrics by isShowingLyricsProperty
    val isShowingSynchronizedLyricsProperty = boolean(false)
    var isShowingSynchronizedLyrics by isShowingSynchronizedLyricsProperty
    val speedProperty = float(1f)
    var speed by speedProperty
    val isShowingPrevButtonCollapsedProperty = boolean(false)
    var isShowingPrevButtonCollapsed by isShowingPrevButtonCollapsedProperty
    val stopWhenClosedProperty = boolean(false)
    var stopWhenClosed by stopWhenClosedProperty
    val horizontalSwipeToCloseProperty = boolean(false)
    var horizontalSwipeToClose by horizontalSwipeToCloseProperty
    val horizontalSwipeToRemoveItemProperty = boolean(false)
    var horizontalSwipeToRemoveItem by horizontalSwipeToRemoveItemProperty
    val playerLayoutProperty = enum(PlayerLayout.New)
    var playerLayout by playerLayoutProperty
    val seekBarStyleProperty = enum(SeekBarStyle.Wavy)
    var seekBarStyle by seekBarStyleProperty
    val showLikeProperty = boolean(false)
    var showLike by showLikeProperty
    val highQualityAudioProperty = boolean(true)
    var highQualityAudio by highQualityAudioProperty
    var progressiveCache by boolean(true)

    enum class PlayerLayout(val displayName: @Composable () -> String) {
        Classic(displayName = { stringResource(R.string.classic_player_layout_name) }),
        New(displayName = { stringResource(R.string.new_player_layout_name) })
    }

    enum class SeekBarStyle(val displayName: @Composable () -> String) {
        Static(displayName = { stringResource(R.string.static_seek_bar_name) }),
        Wavy(displayName = { stringResource(R.string.wavy_seek_bar_name) })
    }

    val volumeNormalizationBaseGainRounded get() = (volumeNormalizationBaseGain * 100).toInt()
}
