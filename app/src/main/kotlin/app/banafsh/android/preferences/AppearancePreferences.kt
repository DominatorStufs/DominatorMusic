package app.banafsh.android.preferences

import app.banafsh.android.GlobalPreferencesHolder
import app.banafsh.android.lib.core.ui.BuiltInFontFamily
import app.banafsh.android.lib.core.ui.ColorMode
import app.banafsh.android.lib.core.ui.ColorSource
import app.banafsh.android.lib.core.ui.Darkness
import app.banafsh.android.lib.core.ui.ThumbnailRoundness
import app.banafsh.android.preferences.OldPreferences.ColorPaletteMode
import app.banafsh.android.preferences.OldPreferences.ColorPaletteName

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
}
