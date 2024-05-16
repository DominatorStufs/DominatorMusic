package app.vitune.android.lib.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

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
    val isDefault: Boolean,
    val isDark: Boolean
) {
    object Saver : androidx.compose.runtime.saveable.Saver<ColorPalette, List<Any>> {
        override fun restore(value: List<Any>) = ColorPalette(
            background0 = Color(value[0] as Int),
            background1 = Color(value[1] as Int),
            background2 = Color(value[2] as Int),
            accent = Color(value[3] as Int),
            onAccent = Color(value[4] as Int),
            red = Color(value[5] as Int),
            blue = Color(value[6] as Int),
            text = Color(value[7] as Int),
            textSecondary = Color(value[8] as Int),
            textDisabled = Color(value[9] as Int),
            isDefault = value[10] as Boolean,
            isDark = value[11] as Boolean
        )

        override fun SaverScope.save(value: ColorPalette) = listOf(
            value.background0.toArgb(),
            value.background1.toArgb(),
            value.background2.toArgb(),
            value.accent.toArgb(),
            value.onAccent.toArgb(),
            value.red.toArgb(),
            value.blue.toArgb(),
            value.text.toArgb(),
            value.textSecondary.toArgb(),
            value.textDisabled.toArgb(),
            value.isDefault,
            value.isDark
        )
    }
}

private val defaultAccentColor = Color(0xff3e44ce).hsl

val defaultLightPalette = lightColorPalette(null)
val defaultDarkPalette = darkColorPalette(null, Darkness.Normal)

fun lightColorPalette(accent: Hsl? = null) = if (accent == null) ColorPalette(
    background0 = Color(0xfffdfdfe),
    background1 = Color(0xfff8f8fc),
    background2 = Color(0xffeaeaf5),
    text = Color(0xff212121),
    textSecondary = Color(0xff656566),
    textDisabled = Color(0xff9d9d9d),
    accent = defaultAccentColor.color,
    onAccent = Color.White,
    isDefault = true,
    isDark = false
) else {
    val (hue, saturation) = accent

    ColorPalette(
        background0 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.925f
        ),
        background1 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.90f
        ),
        background2 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.85f
        ),
        text = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.02f),
            lightness = 0.12f
        ),
        textSecondary = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.40f
        ),
        textDisabled = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.2f),
            lightness = 0.65f
        ),
        accent = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.5f),
            lightness = 0.5f
        ),
        onAccent = Color.White,
        isDefault = false,
        isDark = false
    )
}

fun darkColorPalette(
    accent: Hsl? = null,
    darkness: Darkness
) = if (accent == null) ColorPalette(
    background0 = Color(0xff16171d),
    background1 = Color(0xff1f2029),
    background2 = Color(0xff2b2d3b),
    text = Color(0xffe1e1e2),
    textSecondary = Color(0xffa3a4a6),
    textDisabled = Color(0xff6f6f73),
    accent = defaultAccentColor.color,
    onAccent = Color.White,
    isDefault = true,
    isDark = true
) else {
    val (hue, saturation) = accent

    ColorPalette(
        background0 = if (darkness == Darkness.Normal) Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.10f
        ) else Color.Black,
        background1 = if (darkness == Darkness.Normal) Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.15f
        ) else Color.Black,
        background2 = if (darkness == Darkness.Normal) Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.2f
        ) else Color.Black,
        text = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.02f),
            lightness = 0.88f
        ),
        textSecondary = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.65f
        ),
        textDisabled = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.2f),
            lightness = 0.40f
        ),
        accent = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(if (darkness == Darkness.AMOLED) 0.4f else 0.5f),
            lightness = 0.5f
        ),
        onAccent = Color.White,
        isDefault = false,
        isDark = true
    )
}

fun accentColorOf(
    source: ColorSource,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
) = when (source) {
    ColorSource.Default -> null
    ColorSource.Dynamic -> sampleBitmap?.let { dynamicAccentColorOf(it, isDark) }
        ?: defaultAccentColor

    ColorSource.MaterialYou -> materialAccentColor?.hsl ?: defaultAccentColor
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

fun ColorPalette.amoled() = if (isDark) {
    val (hue, saturation) = accent.hsl

    copy(
        background0 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.10f
        ),
        background1 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.15f
        ),
        background2 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.2f
        )
    )
} else this

fun colorPaletteOf(
    source: ColorSource,
    darkness: Darkness,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
): ColorPalette {
    val accentColor = accentColorOf(
        source = source,
        isDark = isDark,
        materialAccentColor = materialAccentColor,
        sampleBitmap = sampleBitmap
    )

    return if (isDark) darkColorPalette(accentColor, darkness) else lightColorPalette(accentColor)
}

inline val ColorPalette.isPureBlack get() = background0 == Color.Black
inline val ColorPalette.collapsedPlayerProgressBar
    get() = if (isPureBlack) defaultDarkPalette.background0 else background2
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent
inline val ColorPalette.primaryButton get() = if (isPureBlack) Color(0xff272727) else background2

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.overlay get() = Color.Black.copy(alpha = 0.75f)

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlay get() = defaultDarkPalette.text

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlayShimmer get() = defaultDarkPalette.shimmer
