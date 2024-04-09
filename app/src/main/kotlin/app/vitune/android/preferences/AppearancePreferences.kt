package app.vitune.android.preferences

import app.vitune.android.GlobalPreferencesHolder
import app.vitune.core.ui.BuiltInFontFamily
import app.vitune.core.ui.enums.ColorPaletteMode
import app.vitune.core.ui.enums.ColorPaletteName
import app.vitune.core.ui.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorPaletteName by enum(ColorPaletteName.Dynamic)
    var colorPaletteMode by enum(ColorPaletteMode.System)
    var usePureBlack by boolean(if (colorPaletteName == ColorPaletteName.PureBlack) true else false)
    var thumbnailRoundness by enum(ThumbnailRoundness.Medium)
    var fontFamily by enum(BuiltInFontFamily.Poppins)
    var applyFontPadding by boolean(false)
    val isShowingThumbnailInLockscreenProperty = boolean(true)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
    var swipeToHideSongConfirm by boolean(true)
    var maxThumbnailSize by int(1920)
}
