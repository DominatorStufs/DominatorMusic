package app.vitune.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import app.vitune.core.ui.enums.ColorPaletteMode
import app.vitune.core.ui.enums.ColorPaletteName

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDark: Boolean,
    val isAmoled: Boolean
) {
    companion object : Saver<ColorPalette, List<Any>> {
        override fun restore(value: List<Any>) = when (val accent = value[0] as Int) {
            0 -> DefaultDarkColorPalette
            1 -> DefaultLightColorPalette
            2 -> PureBlackColorPalette()
            else -> dynamicColorPaletteOf(
                accentColor = Color(accent),
                isDark = value[1] as Boolean,
                isAmoled = value[2] as Boolean
            )
        }

        override fun SaverScope.save(value: ColorPalette) = listOf(
            when {
                value === DefaultDarkColorPalette -> 0
                value === DefaultLightColorPalette -> 1
                value === PureBlackColorPalette() -> 2
                else -> value.accent.toArgb()
            },
            value.isDark,
            value.isAmoled
        )
    }
}

val DefaultDarkColorPalette = ColorPalette(
    background0 = Color(0xff16171d),
    background1 = Color(0xff1f2029),
    background2 = Color(0xff2b2d3b),
    text = Color(0xffe1e1e2),
    textSecondary = Color(0xffa3a4a6),
    textDisabled = Color(0xff6f6f73),
    accent = Color(0xff3e44ce),
    onAccent = Color.White,
    isDark = true,
    isAmoled = false
)

val DefaultLightColorPalette = ColorPalette(
    background0 = Color(0xfffdfdfe),
    background1 = Color(0xfff8f8fc),
    background2 = Color(0xffeaeaf5),
    text = Color(0xff212121),
    textSecondary = Color(0xff656566),
    textDisabled = Color(0xff9d9d9d),
    accent = Color(0xff3e44ce),
    onAccent = Color.White,
    isDark = false,
    isAmoled = false
)

fun PureBlackColorPalette(
    p: ColorPalette = DefaultDarkColorPalette
) = p.copy(
    background0 = Color.Black,
    background1 = Color.Black,
    background2 = Color.Black
)

fun colorPaletteOf(
    name: ColorPaletteName,
    mode: ColorPaletteMode,
    isDark: Boolean
) = when (name) {
    ColorPaletteName.Default,
    ColorPaletteName.Dynamic, ColorPaletteName.MaterialYou -> when (mode) {
        ColorPaletteMode.Light -> DefaultLightColorPalette
        ColorPaletteMode.Dark -> DefaultDarkColorPalette
        ColorPaletteMode.System -> if (isDark) DefaultDarkColorPalette else DefaultLightColorPalette
    }

    ColorPaletteName.PureBlack -> PureBlackColorPalette()
    ColorPaletteName.AMOLED -> PureBlackColorPalette().copy(isAmoled = true)
}

fun dynamicAccentColorOf(
    bitmap: Bitmap,
    isDark: Boolean
): Hsl? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(8)
        .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 36f..100f }) else null)
        .generate()

    val hsl = if (isDark) {
        palette.dominantSwatch ?: Palette
            .from(bitmap)
            .maximumColorCount(8)
            .generate()
            .dominantSwatch
    } else {
        palette.dominantSwatch
    }?.hsl ?: return null

    val arr = if (hsl[1] < 0.08)
        palette.swatches
            .map(Palette.Swatch::getHsl)
            .sortedByDescending(FloatArray::component2)
            .find { it[1] != 0f }
            ?: hsl
    else hsl

    return arr.hsl
}

@Suppress("CyclomaticComplexMethod")
fun dynamicColorPaletteOf(
    hsl: Hsl,
    isDark: Boolean,
    isAmoled: Boolean
) = hsl.let { (hue, saturation) ->
    val accentColor = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(if (isAmoled) 0.4f else 0.5f),
        lightness = 0.5f
    )

    if (isAmoled) PureBlackColorPalette().copy(
        isAmoled = true,
        accent = accentColor
    ) else colorPaletteOf(
        name = ColorPaletteName.Dynamic,
        mode = if (isDark) ColorPaletteMode.Dark else ColorPaletteMode.Light,
        isDark = isDark
    ).copy(
        background0 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = if (isDark) 0.10f else 0.925f
        ),
        background1 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = if (isDark) 0.15f else 0.90f
        ),
        background2 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = if (isDark) 0.2f else 0.85f
        ),
        accent = accentColor,
        text = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.02f),
            lightness = if (isDark) 0.88f else 0.12f
        ),
        textSecondary = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = if (isDark) 0.65f else 0.40f
        ),
        textDisabled = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.2f),
            lightness = if (isDark) 0.40f else 0.65f
        )
    )
}

fun dynamicColorPaletteOf(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean
) = dynamicColorPaletteOf(
    hsl = accentColor.hsl,
    isDark = isDark,
    isAmoled = isAmoled
)

inline val ColorPalette.isDefault
    get() =
        this === DefaultDarkColorPalette || this === DefaultLightColorPalette || this === PureBlackColorPalette()

inline val ColorPalette.collapsedPlayerProgressBar get() =
    if (this === PureBlackColorPalette()) DefaultDarkColorPalette.background0 else background2
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent
inline val ColorPalette.primaryButton
    get() = if (this === PureBlackColorPalette() || isAmoled) Color(0xFF272727) else background2

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.overlay get() = PureBlackColorPalette().background0.copy(alpha = 0.75f)

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlay get() = PureBlackColorPalette().text

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlayShimmer get() = PureBlackColorPalette().shimmer
