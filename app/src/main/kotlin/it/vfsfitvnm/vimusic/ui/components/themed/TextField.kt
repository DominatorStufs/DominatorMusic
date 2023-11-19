package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold

@Composable
fun ColumnScope.TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    singleLine: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.None),
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = { },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hintText: String? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    val style by remember { derivedStateOf { textStyle ?: typography.xs.semiBold } }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = style,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colorPalette.text),
        decorationBox = { innerTextField ->
            hintText?.let { text ->
                Box(modifier = Modifier.weight(1f)) {
                    this@TextField.AnimatedVisibility(
                        visible = value.isEmpty(),
                        enter = fadeIn(tween(100)),
                        exit = fadeOut(tween(100))
                    ) {
                        BasicText(
                            text = text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = style.secondary,
                        )
                    }
                }
            }

            innerTextField()
        }
    )
}