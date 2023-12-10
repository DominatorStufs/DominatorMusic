package it.vfsfitvnm.vimusic.enums

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.roundedShape

@Suppress("unused")
enum class ThumbnailRoundness(val dp: Dp) {
    None(0.dp),
    Light(2.dp),
    Medium(8.dp),
    Heavy(12.dp),
    Heavier(16.dp),
    Heaviest(16.dp);

    val shape get() = dp.roundedShape
}
