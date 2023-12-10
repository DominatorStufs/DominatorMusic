package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.roundedShape

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

val LocalAppearance by lazy { staticCompositionLocalOf<Appearance> { error("No appearance provided") } }
