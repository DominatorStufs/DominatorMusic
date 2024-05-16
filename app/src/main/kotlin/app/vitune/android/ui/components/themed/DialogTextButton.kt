package app.vitune.android.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.vitune.android.lib.core.ui.LocalAppearance
import app.vitune.android.utils.disabled
import app.vitune.android.utils.medium
import app.vitune.android.utils.primary
import app.vitune.android.lib.core.ui.utils.roundedShape

@Composable
fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = typography.xs.medium.let {
            when {
                !enabled -> it.disabled
                primary -> it.primary
                else -> it
            }
        },
        modifier = modifier
            .clip(36.dp.roundedShape)
            .background(if (primary) colorPalette.accent else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    )
}
