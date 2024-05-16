package app.vitune.android.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.vitune.android.lib.core.ui.LocalAppearance

@Composable
fun Slider(
    state: Float,
    setState: (Float) -> Unit,
    onSlideCompleted: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    @IntRange(from = 0) steps: Int = 0
) {
    val (colorPalette) = LocalAppearance.current

    androidx.compose.material3.Slider(
        value = state,
        onValueChange = setState,
        onValueChangeFinished = onSlideCompleted,
        valueRange = range,
        modifier = modifier,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = colorPalette.onAccent,
            activeTrackColor = colorPalette.accent,
            inactiveTrackColor = colorPalette.text.copy(alpha = 0.75f)
        )
    )
}
