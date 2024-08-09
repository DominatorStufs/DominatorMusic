package app.vitune.android.ui.screens.settings

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.rememberEqualizerLauncher
import app.vitune.core.ui.utils.isAtLeastAndroid6

@OptIn(UnstableApi::class)
@Route
@Composable
fun PlayerSettings() = with(PlayerPreferences) {
    val binder = LocalPlayerServiceBinder.current
    val launchEqualizer by rememberEqualizerLauncher(audioSessionId = { binder?.player?.audioSessionId })

    SettingsCategoryScreen(title = stringResource(R.string.player)) {
        SettingsGroup(title = stringResource(R.string.player)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.persistent_queue),
                text = stringResource(R.string.persistent_queue_description),
                isChecked = persistentQueue,
                onCheckedChange = { persistentQueue = it }
            )

            if (isAtLeastAndroid6) SwitchSettingsEntry(
                title = stringResource(R.string.resume_playback),
                text = stringResource(R.string.resume_playback_description),
                isChecked = resumePlaybackWhenDeviceConnected,
                onCheckedChange = {
                    resumePlaybackWhenDeviceConnected = it
                }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.stop_when_closed),
                text = stringResource(R.string.stop_when_closed_description),
                isChecked = stopWhenClosed,
                onCheckedChange = { stopWhenClosed = it }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.skip_on_error),
                text = stringResource(R.string.skip_on_error_description),
                isChecked = skipOnError,
                onCheckedChange = { skipOnError = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.audio)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.skip_silence),
                text = stringResource(R.string.skip_silence_description),
                isChecked = skipSilence,
                onCheckedChange = {
                    skipSilence = it
                }
            )

            AnimatedVisibility(visible = skipSilence) {
                val initialValue by remember { derivedStateOf { minimumSilence.toFloat() / 1000L } }
                var newValue by remember(initialValue) { mutableFloatStateOf(initialValue) }
                var changed by rememberSaveable { mutableStateOf(false) }

                Column {
                    SliderSettingsEntry(
                        title = stringResource(R.string.minimum_silence_length),
                        text = stringResource(R.string.minimum_silence_length_description),
                        state = newValue,
                        onSlide = { newValue = it },
                        onSlideComplete = {
                            minimumSilence = newValue.toLong() * 1000L
                            changed = true
                        },
                        toDisplay = { stringResource(R.string.format_ms, it.toLong()) },
                        range = 1f..2000f
                    )

                    AnimatedVisibility(visible = changed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingsDescription(
                                text = stringResource(R.string.minimum_silence_length_warning),
                                important = true,
                                modifier = Modifier.weight(2f)
                            )
                            SecondaryTextButton(
                                text = stringResource(R.string.restart_service),
                                onClick = {
                                    binder?.restartForegroundOrStop()?.let { changed = false }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 24.dp)
                            )
                        }
                    }
                }
            }

            SwitchSettingsEntry(
                title = stringResource(R.string.loudness_normalization),
                text = stringResource(R.string.loudness_normalization_description),
                isChecked = volumeNormalization,
                onCheckedChange = { volumeNormalization = it }
            )

            AnimatedVisibility(visible = volumeNormalization) {
                var newValue by remember(volumeNormalizationBaseGain) {
                    mutableFloatStateOf(volumeNormalizationBaseGain)
                }

                SliderSettingsEntry(
                    title = stringResource(R.string.loudness_base_gain),
                    text = stringResource(R.string.loudness_base_gain_description),
                    state = newValue,
                    onSlide = { newValue = it },
                    onSlideComplete = { volumeNormalizationBaseGain = newValue },
                    toDisplay = { stringResource(R.string.format_db, "%.2f".format(it)) },
                    range = -20f..20f,
                    steps = 79
                )
            }

            SwitchSettingsEntry(
                title = stringResource(R.string.bass_boost),
                text = stringResource(R.string.bass_boost_description),
                isChecked = bassBoost,
                onCheckedChange = { bassBoost = it }
            )

            AnimatedVisibility(visible = bassBoost) {
                var newValue by remember(bassBoostLevel) { mutableFloatStateOf(bassBoostLevel.toFloat()) }

                SliderSettingsEntry(
                    title = stringResource(R.string.bass_boost_level),
                    text = stringResource(R.string.bass_boost_level_description),
                    state = newValue,
                    onSlide = { newValue = it },
                    onSlideComplete = { bassBoostLevel = newValue.toInt() },
                    toDisplay = { (it * 1000f).toInt().toString() },
                    range = 0f..1f
                )
            }

            SettingsEntry(
                title = stringResource(R.string.equalizer),
                text = stringResource(R.string.equalizer_description),
                onClick = launchEqualizer
            )
        }
    }
}
