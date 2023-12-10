package it.vfsfitvnm.vimusic.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.R

object PlayerPreferences : GlobalPreferencesHolder() {
    var isInvincibilityEnabled by boolean(false)
    var trackLoopEnabled by boolean(false)
    var queueLoopEnabled by boolean(true)
    var skipSilence by boolean(false)
    var minimumSilence by long(2_000_000L)
    var volumeNormalization by boolean(false)
    var volumeNormalizationBaseGain by float(5.00f)
    var bassBoost by boolean(false)
    var bassBoostLevel by int(5)
    var resumePlaybackWhenDeviceConnected by boolean(false)
    var persistentQueue by boolean(false)
    var isShowingLyrics by boolean(false)
    var isShowingSynchronizedLyrics by boolean(false)
    var speed by float(1f)
    var isShowingPrevButtonCollapsed by boolean(false)
    var stopWhenClosed by boolean(false)
    var horizontalSwipeToClose by boolean(false)
    var horizontalSwipeToRemoveItem by boolean(false)
    var playerLayout by enum(PlayerLayout.New)
    var showLike by boolean(false)

    enum class PlayerLayout(val displayName: @Composable () -> String) {
        Classic(displayName = { stringResource(R.string.classic_player_layout_name) }),
        New(displayName = { stringResource(R.string.new_player_layout_name) })
    }

    val volumeNormalizationBaseGainRounded get() = (volumeNormalizationBaseGain * 100).toInt()
}
