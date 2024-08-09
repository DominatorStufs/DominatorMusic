package app.vitune.core.ui

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
            colorPalette = ColorPalette.Saver.restore(value[0] as List<Any>),
            typography = Typography.restore(value[1] as List<Any>),
            thumbnailShapeCorners = (value[2] as Float).dp
        )

        override fun SaverScope.save(value: Appearance) = listOf(
            with(ColorPalette.Saver) { save(value.colorPalette) },
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
    source: ColorSource,
    mode: ColorMode,
    darkness: Darkness,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean,
    thumbnailRoundness: Dp,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme()
): Appearance {
    val isDark = remember(mode, isSystemInDarkTheme) {
        mode == ColorMode.Dark || (mode == ColorMode.System && isSystemInDarkTheme)
    }

    val colorPalette = remember(
        source,
        darkness,
        isDark,
        materialAccentColor,
        sampleBitmap
    ) {
        colorPaletteOf(
            source = source,
            darkness = darkness,
            isDark = isDark,
            materialAccentColor = materialAccentColor,
            sampleBitmap = sampleBitmap
        )
    }

    return rememberAppearance(
        colorPalette,
        fontFamily,
        applyFontPadding,
        thumbnailRoundness,
        isDark = isDark
    ) {
        Appearance(
            colorPalette = colorPalette,
            typography = typographyOf(
                color = colorPalette.text,
                fontFamily = fontFamily,
                applyFontPadding = applyFontPadding
            ),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }.value
}

fun Activity.setSystemBarAppearance(isDark: Boolean) {
    with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }

    val color = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()

    // TODO: Android now expects a background behind the system bars as well
    @Suppress("DEPRECATION")
    if (!isAtLeastAndroid6) window.statusBarColor = color
    @Suppress("DEPRECATION")
    if (!isAtLeastAndroid8) window.navigationBarColor = color
}

@Composable
fun Activity.SystemBarAppearance(palette: ColorPalette) = LaunchedEffect(palette) {
    withContext(Dispatchers.Main) {
        setSystemBarAppearance(palette.isDark)
    }
}
