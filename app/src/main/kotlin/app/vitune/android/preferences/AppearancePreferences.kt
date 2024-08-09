package app.vitune.android.preferences

import app.vitune.android.GlobalPreferencesHolder
import app.vitune.android.preferences.OldPreferences.ColorPaletteMode
import app.vitune.android.preferences.OldPreferences.ColorPaletteName
import app.vitune.core.ui.BuiltInFontFamily
import app.vitune.core.ui.ColorMode
import app.vitune.core.ui.ColorSource
import app.vitune.core.ui.Darkness
import app.vitune.core.ui.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorSource by enum(
        when (OldPreferences.oldColorPaletteName) {
            ColorPaletteName.Default, ColorPaletteName.PureBlack -> ColorSource.Default
            ColorPaletteName.Dynamic, ColorPaletteName.AMOLED -> ColorSource.Dynamic
            ColorPaletteName.MaterialYou -> ColorSource.MaterialYou
        }
    )
    var colorMode by enum(
        when (OldPreferences.oldColorPaletteMode) {
            ColorPaletteMode.Light -> ColorMode.Light
            ColorPaletteMode.Dark -> ColorMode.Dark
            ColorPaletteMode.System -> ColorMode.System
        }
    )
    var darkness by enum(
        when (OldPreferences.oldColorPaletteName) {
            ColorPaletteName.Default, ColorPaletteName.Dynamic, ColorPaletteName.MaterialYou -> Darkness.Normal
            ColorPaletteName.PureBlack -> Darkness.PureBlack
            ColorPaletteName.AMOLED -> Darkness.AMOLED
        }
    )
    var thumbnailRoundness by enum(ThumbnailRoundness.Medium)
    var fontFamily by enum(BuiltInFontFamily.Poppins)
    var applyFontPadding by boolean(false)
    val isShowingThumbnailInLockscreenProperty = boolean(true)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
    var swipeToHideSongConfirm by boolean(true)
    var maxThumbnailSize by int(1920)
    var lyricsKeepScreenAwake by boolean(false)
}
