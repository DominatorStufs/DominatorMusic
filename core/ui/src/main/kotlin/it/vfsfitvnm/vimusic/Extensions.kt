package it.vfsfitvnm.vimusic

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Dp.roundedShape get() = if (this == 0.dp) RectangleShape else RoundedCornerShape(this)
