package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.preferences.AppearancePreferences
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13

val ColorPaletteName.nameLocalized @Composable get() = when (this) {
    ColorPaletteName.Default -> stringResource(R.string.theme_name_default)
    ColorPaletteName.Dynamic -> stringResource(R.string.theme_name_dynamic)
    ColorPaletteName.PureBlack -> stringResource(R.string.theme_name_pureblack)
}

val ColorPaletteMode.nameLocalized @Composable get() = when (this) {
    ColorPaletteMode.Light -> stringResource(R.string.theme_mode_light)
    ColorPaletteMode.Dark -> stringResource(R.string.theme_mode_dark)
    ColorPaletteMode.System -> stringResource(R.string.theme_mode_system)
}

val ThumbnailRoundness.nameLocalized @Composable get() = when (this) {
    ThumbnailRoundness.None -> stringResource(R.string.thumbnail_roundness_none)
    ThumbnailRoundness.Light -> stringResource(R.string.thumbnail_roundness_light)
    ThumbnailRoundness.Medium -> stringResource(R.string.thumbnail_roundness_medium)
    ThumbnailRoundness.Heavy -> stringResource(R.string.thumbnail_roundness_heavy)
    ThumbnailRoundness.Heavier -> stringResource(R.string.thumbnail_roundness_heavier)
    ThumbnailRoundness.Heaviest -> stringResource(R.string.thumbnail_roundness_heaviest)
}

@Route
@Composable
fun AppearanceSettings() {
    val (colorPalette) = LocalAppearance.current

    with(AppearancePreferences) {
        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                        .asPaddingValues()
                )
        ) {
            Header(title = stringResource(R.string.appearance))

            SettingsEntryGroupText(title = stringResource(R.string.colors))

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.theme),
                selectedValue = colorPaletteName,
                onValueSelected = { colorPaletteName = it },
                valueText = { it.nameLocalized }
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.theme_mode),
                selectedValue = colorPaletteMode,
                isEnabled = colorPaletteName != ColorPaletteName.PureBlack,
                onValueSelected = { colorPaletteMode = it },
                valueText = { it.nameLocalized }
            )

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = stringResource(R.string.shapes))

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

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = stringResource(R.string.text))

            SwitchSettingEntry(
                title = stringResource(R.string.use_system_font),
                text = stringResource(R.string.use_system_font_description),
                isChecked = useSystemFont,
                onCheckedChange = { useSystemFont = it }
            )

            SwitchSettingEntry(
                title = stringResource(R.string.apply_font_padding),
                text = stringResource(R.string.apply_font_padding_description),
                isChecked = applyFontPadding,
                onCheckedChange = { applyFontPadding = it }
            )

            if (!isAtLeastAndroid13) {
                SettingsGroupSpacer()

                SettingsEntryGroupText(title = stringResource(R.string.lockscreen))

                SwitchSettingEntry(
                    title = stringResource(R.string.show_song_cover),
                    text = stringResource(R.string.show_song_cover_description),
                    isChecked = isShowingThumbnailInLockscreen,
                    onCheckedChange = { isShowingThumbnailInLockscreen = it }
                )
            }

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = stringResource(R.string.player))

            SwitchSettingEntry(
                title = stringResource(R.string.previous_button_while_collapsed),
                text = stringResource(R.string.previous_button_while_collapsed_description),
                isChecked = PlayerPreferences.isShowingPrevButtonCollapsed,
                onCheckedChange = { PlayerPreferences.isShowingPrevButtonCollapsed = it }
            )

            SwitchSettingEntry(
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
                SwitchSettingEntry(
                    title = stringResource(R.string.show_like_button),
                    text = stringResource(R.string.show_like_button_description),
                    isChecked = PlayerPreferences.showLike,
                    onCheckedChange = { PlayerPreferences.showLike = it }
                )
            }

            SwitchSettingEntry(
                title = stringResource(R.string.swipe_to_remove_item),
                text = stringResource(R.string.swipe_to_remove_item_description),
                isChecked = PlayerPreferences.horizontalSwipeToRemoveItem,
                onCheckedChange = { PlayerPreferences.horizontalSwipeToRemoveItem = it }
            )
        }
    }
}
