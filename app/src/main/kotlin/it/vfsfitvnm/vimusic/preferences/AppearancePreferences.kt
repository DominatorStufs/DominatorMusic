package it.vfsfitvnm.vimusic.preferences

import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorPaletteName by enum(ColorPaletteName.Dynamic)
    var colorPaletteMode by enum(ColorPaletteMode.System)
    var thumbnailRoundness by enum(ThumbnailRoundness.Light)
    var useSystemFont by boolean(false)
    var applyFontPadding by boolean(false)
    var isShowingThumbnailInLockscreen by boolean(false)
}
