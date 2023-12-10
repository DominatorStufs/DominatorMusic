package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

@JvmInline
value class Px(val value: Int) {
    val dp @Composable get() = dp(LocalDensity.current)
    fun dp(density: Density) = with(density) { value.toDp() }
}

val Int.px get() = Px(value = this)
val Float.px get() = Px(value = roundToInt())
