package app.vitune.core.ui

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.vitune.core.ui.enums.ColorPaletteMode
import app.vitune.core.ui.enums.ColorPaletteName
import app.vitune.core.ui.utils.isAtLeastAndroid6
import app.vitune.core.ui.utils.isAtLeastAndroid8
import app.vitune.core.ui.utils.isCompositionLaunched
import app.vitune.core.ui.utils.roundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShapeCorners: Dp
) {
    val thumbnailShape = thumbnailShapeCorners.roundedShape
    operator fun component4() = thumbnailShape

    companion object AppearanceSaver : Saver<Appearance, List<Any>> {
        @Suppress("UNCHECKED_CAST")
        override fun restore(value: List<Any>) = Appearance(
            colorPalette = ColorPalette.restore(value[0] as List<Any>),
            typography = Typography.restore(value[1] as List<Any>),
            thumbnailShapeCorners = (value[2] as Float).dp
        )

        override fun SaverScope.save(value: Appearance) = listOf(
            with(ColorPalette.Companion) { save(value.colorPalette) },
            with(Typography.Companion) { save(value.typography) },
            value.thumbnailShapeCorners.value
        )
    }
}

val LocalAppearance = staticCompositionLocalOf<Appearance> { error("No appearance provided") }

@Composable
inline fun rememberAppearance(
    vararg keys: Any = arrayOf(Unit),
    saver: Saver<Appearance, out Any> = Appearance.AppearanceSaver,
    isDark: Boolean = isSystemInDarkTheme(),
    crossinline provide: (isSystemInDarkTheme: Boolean) -> Appearance
) = rememberSaveable(
    keys,
    isCompositionLaunched(),
    isDark,
    stateSaver = saver
) {
    mutableStateOf(provide(isDark))
}

@Composable
fun appearance(
    name: ColorPaletteName,
    mode: ColorPaletteMode,
    materialAccentColor: Color,
    fontFamily: BuiltInFontFamily,
    sampleBitmap: Bitmap?,
    applyFontPadding: Boolean,
    thumbnailRoundness: Dp,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    usePureBlack: Boolean
): Appearance {
    val isDark = remember(mode, isSystemInDarkTheme) {
        mode == ColorPaletteMode.Dark || (mode == ColorPaletteMode.System && isSystemInDarkTheme)
    }

    val defaultTheme = remember(
        isDark,
        mode,
        isSystemInDarkTheme,
        fontFamily,
        applyFontPadding,
        thumbnailRoundness
    ) {
        val colorPalette = colorPaletteOf(
            name = ColorPaletteName.Default,
            mode = mode,
            isDark = isSystemInDarkTheme
        )

        Appearance(
            colorPalette = colorPalette,
            typography = typographyOf(
                color = colorPalette.text,
                fontFamily = fontFamily,
                applyFontPadding = applyFontPadding
            ),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }

    var dynamicAccentColor by rememberSaveable(stateSaver = Hsl.Saver) {
        mutableStateOf(defaultTheme.colorPalette.accent.hsl)
    }

    LaunchedEffect(sampleBitmap, name) {
        if (!name.isDynamic) return@LaunchedEffect

        dynamicAccentColor = sampleBitmap?.let {
            dynamicAccentColorOf(
                bitmap = it,
                isDark = isDark
            )
        } ?: defaultTheme.colorPalette.accent.hsl
    }

    val colorPalette by remember(name, isDark) {
        derivedStateOf {
            when (name) {
                ColorPaletteName.Default -> defaultTheme.colorPalette
                ColorPaletteName.Dynamic -> dynamicColorPaletteOf(
                    hsl = dynamicAccentColor,
                    isDark = isDark,
                    isAmoled = false
                )

                ColorPaletteName.MaterialYou -> dynamicColorPaletteOf(
                    accentColor = materialAccentColor,
                    isDark = isDark,
                    isAmoled = false
                )

                ColorPaletteName.AMOLED -> dynamicColorPaletteOf(
                    hsl = dynamicAccentColor,
                    isDark = true,
                    isAmoled = true
                )
            }
        }
    }

    return rememberAppearance(
        colorPalette,
        defaultTheme,
        thumbnailRoundness,
        usePureBlack,
        isDark = isDark
    ) {
        Appearance(
            colorPalette = if (usePureBlack) PureBlackColorPalette else colorPalette,
            typography = defaultTheme.typography.copy(color = colorPalette.text),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }.value
}

@Composable
fun isPureBlackAvailable(
    colorPaletteName: ColorPaletteName,
    colorPaletteMode: ColorPaletteMode
) = colorPaletteName == ColorPaletteName.AMOLED
        || colorPaletteMode == ColorPaletteMode.Dark
        || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme())

fun Activity.setSystemBarAppearance(isDark: Boolean) {
    with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }

    val color = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()

    if (!isAtLeastAndroid6) window.statusBarColor = color
    if (!isAtLeastAndroid8) window.navigationBarColor = color
}

@Composable
fun Activity.SystemBarAppearance(palette: ColorPalette) = LaunchedEffect(palette) {
    withContext(Dispatchers.Main) {
        setSystemBarAppearance(palette.isDark)
    }
}
