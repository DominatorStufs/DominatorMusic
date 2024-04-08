package app.vitune.android.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.currentLocale
import app.vitune.android.utils.findActivity
import app.vitune.android.utils.startLanguagePicker
import app.vitune.core.ui.BuiltInFontFamily
import app.vitune.core.ui.googleFontsAvailable
import app.vitune.core.ui.isPureBlackAvailable
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.enums.ColorPaletteMode
import app.vitune.core.ui.enums.ColorPaletteName
import app.vitune.core.ui.enums.ThumbnailRoundness
import app.vitune.core.ui.utils.isAtLeastAndroid13

@Route
@Composable
fun AppearanceSettings() = with(AppearancePreferences) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    SettingsCategoryScreen(title = stringResource(R.string.appearance)) {
        SettingsGroup(title = stringResource(R.string.colors)) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.theme),
                selectedValue = colorPaletteName,
                onValueSelected = { colorPaletteName = it },
                valueText = { it.nameLocalized }
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.theme_mode),
                selectedValue = colorPaletteMode,
                isEnabled = colorPaletteName != ColorPaletteName.AMOLED,
                onValueSelected = { colorPaletteMode = it },
                valueText = { it.nameLocalized }
            )

            AnimatedVisibility(
                visible = isPureBlackAvailable(colorPaletteName, colorPaletteMode),
                label = ""
            ) { SwitchSettingsEntry(
                title = stringResource(R.string.theme_name_pureblack),
                text = "",
                isChecked = usePureBlack,
                onCheckedChange = { usePureBlack = it }
            ) }
        }
        SettingsGroup(title = stringResource(R.string.shapes)) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.thumbnail_roundness),
                selectedValue = thumbnailRoundness,
                onValueSelected = { thumbnailRoundness = it },
                trailingContent = {
                    Spacer(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = colorPalette.accent,
                                shape = thumbnailRoundness.shape
                            )
                            .background(
                                color = colorPalette.background1,
                                shape = thumbnailRoundness.shape
                            )
                            .size(36.dp)
                    )
                },
                valueText = { it.nameLocalized }
            )
        }
        SettingsGroup(title = stringResource(R.string.text)) {
            if (isAtLeastAndroid13) SettingsEntry(
                title = stringResource(R.string.language),
                text = currentLocale()?.displayLanguage
                    ?: stringResource(R.string.theme_name_default),
                onClick = {
                    context.findActivity().startLanguagePicker()
                }
            )

            if (googleFontsAvailable()) EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.font),
                selectedValue = fontFamily,
                onValueSelected = { fontFamily = it },
                valueText = {
                    if (it == BuiltInFontFamily.System) stringResource(R.string.use_system_font) else it.name
                }
            ) else SwitchSettingsEntry(
                title = stringResource(R.string.use_system_font),
                text = stringResource(R.string.use_system_font_description),
                isChecked = fontFamily == BuiltInFontFamily.System,
                onCheckedChange = {
                    fontFamily = if (it) BuiltInFontFamily.System else BuiltInFontFamily.Poppins
                }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.apply_font_padding),
                text = stringResource(R.string.apply_font_padding_description),
                isChecked = applyFontPadding,
                onCheckedChange = { applyFontPadding = it }
            )
        }
        if (!isAtLeastAndroid13) SettingsGroup(title = stringResource(R.string.lockscreen)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.show_song_cover),
                text = stringResource(R.string.show_song_cover_description),
                isChecked = isShowingThumbnailInLockscreen,
                onCheckedChange = { isShowingThumbnailInLockscreen = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.player)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.previous_button_while_collapsed),
                text = stringResource(R.string.previous_button_while_collapsed_description),
                isChecked = PlayerPreferences.isShowingPrevButtonCollapsed,
                onCheckedChange = { PlayerPreferences.isShowingPrevButtonCollapsed = it }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.swipe_horizontally_to_close),
                text = stringResource(R.string.swipe_horizontally_to_close_description),
                isChecked = PlayerPreferences.horizontalSwipeToClose,
                onCheckedChange = { PlayerPreferences.horizontalSwipeToClose = it }
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.player_layout),
                selectedValue = PlayerPreferences.playerLayout,
                onValueSelected = { PlayerPreferences.playerLayout = it },
                valueText = { it.displayName() }
            )

            AnimatedVisibility(
                visible = PlayerPreferences.playerLayout == PlayerPreferences.PlayerLayout.New,
                label = ""
            ) {
                SwitchSettingsEntry(
                    title = stringResource(R.string.show_like_button),
                    text = stringResource(R.string.show_like_button_description),
                    isChecked = PlayerPreferences.showLike,
                    onCheckedChange = { PlayerPreferences.showLike = it }
                )
            }

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.seek_bar_style),
                selectedValue = PlayerPreferences.seekBarStyle,
                onValueSelected = { PlayerPreferences.seekBarStyle = it },
                valueText = { it.displayName() }
            )

            AnimatedVisibility(
                visible = PlayerPreferences.seekBarStyle == PlayerPreferences.SeekBarStyle.Wavy,
                label = ""
            ) {
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.seek_bar_quality),
                    selectedValue = PlayerPreferences.wavySeekBarQuality,
                    onValueSelected = { PlayerPreferences.wavySeekBarQuality = it },
                    valueText = { it.displayName() }
                )
            }

            SwitchSettingsEntry(
                title = stringResource(R.string.swipe_to_remove_item),
                text = stringResource(R.string.swipe_to_remove_item_description),
                isChecked = PlayerPreferences.horizontalSwipeToRemoveItem,
                onCheckedChange = { PlayerPreferences.horizontalSwipeToRemoveItem = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.songs)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.swipe_to_hide_song),
                text = stringResource(R.string.swipe_to_hide_song_description),
                isChecked = swipeToHideSong,
                onCheckedChange = { swipeToHideSong = it }
            )
            AnimatedVisibility(
                visible = swipeToHideSong,
                label = ""
            ) {
                SwitchSettingsEntry(
                    title = stringResource(R.string.swipe_to_hide_song_confirm),
                    text = stringResource(R.string.swipe_to_hide_song_confirm_description),
                    isChecked = swipeToHideSongConfirm,
                    onCheckedChange = { swipeToHideSongConfirm = it }
                )
            }
        }
    }
}

val ColorPaletteName.nameLocalized
    @Composable get() = stringResource(
        when (this) {
            ColorPaletteName.Default -> R.string.theme_name_default
            ColorPaletteName.Dynamic -> R.string.theme_name_dynamic
            ColorPaletteName.AMOLED -> R.string.theme_name_amoled
            ColorPaletteName.MaterialYou -> R.string.theme_name_materialyou
        }
    )

val ColorPaletteMode.nameLocalized
    @Composable get() = stringResource(
        when (this) {
            ColorPaletteMode.Light -> R.string.theme_mode_light
            ColorPaletteMode.Dark -> R.string.theme_mode_dark
            ColorPaletteMode.System -> R.string.theme_mode_system
        }
    )

val ThumbnailRoundness.nameLocalized
    @Composable get() = stringResource(
        when (this) {
            ThumbnailRoundness.None -> R.string.thumbnail_roundness_none
            ThumbnailRoundness.Light -> R.string.thumbnail_roundness_light
            ThumbnailRoundness.Medium -> R.string.thumbnail_roundness_medium
            ThumbnailRoundness.Heavy -> R.string.thumbnail_roundness_heavy
            ThumbnailRoundness.Heavier -> R.string.thumbnail_roundness_heavier
            ThumbnailRoundness.Heaviest -> R.string.thumbnail_roundness_heaviest
        }
    )
