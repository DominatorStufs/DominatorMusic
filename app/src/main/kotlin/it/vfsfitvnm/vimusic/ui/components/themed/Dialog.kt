package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.drawCircle
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

@Composable
fun TextFieldDialog(
    hintText: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    doneText: String = stringResource(R.string.done),
    initialTextInput: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onCancel: () -> Unit = onDismiss,
    isTextInputValid: (String) -> Boolean = { it.isNotEmpty() }
) {
    val focusRequester = remember { FocusRequester() }
    val (colorPalette, typography) = LocalAppearance.current

    var textFieldValue by rememberSaveable(initialTextInput, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            textStyle = typography.xs.semiBold.center,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.None),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isTextInputValid(textFieldValue.text)) {
                        onDismiss()
                        onDone(textFieldValue.text)
                    }
                }
            ),
            cursorBrush = SolidColor(colorPalette.text),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = textFieldValue.text.isEmpty(),
                        enter = fadeIn(tween(100)),
                        exit = fadeOut(tween(100))
                    ) {
                        BasicText(
                            text = hintText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.xs.semiBold.secondary
                        )
                    }

                    innerTextField()
                }
            },
            modifier = Modifier
                .padding(all = 16.dp)
                .weight(weight = 1f, fill = false)
                .focusRequester(focusRequester)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            DialogTextButton(
                text = cancelText,
                onClick = onCancel
            )

            DialogTextButton(
                primary = true,
                text = doneText,
                onClick = {
                    if (isTextInputValid(textFieldValue.text)) {
                        onDismiss()
                        onDone(textFieldValue.text)
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
}

@Composable
fun <T> NumberFieldDialog(
    onDismiss: () -> Unit,
    onDone: (T) -> Unit,
    initialValue: T,
    defaultValue: T,
    convert: (String) -> T?,
    range: ClosedRange<T>,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    doneText: String = stringResource(R.string.done),
    onCancel: () -> Unit = onDismiss
) where T : Number, T : Comparable<T> = TextFieldDialog(
    hintText = "",
    onDismiss = onDismiss,
    onDone = { onDone((convert(it) ?: defaultValue).coerceIn(range)) },
    modifier = modifier,
    cancelText = cancelText,
    doneText = doneText,
    initialTextInput = initialValue.toString(),
    onCancel = onCancel,
    isTextInputValid = { true }
)

fun <T : Comparable<T>> T.coerceIn(range: ClosedRange<T>) = when {
    this < range.start -> range.start
    this > range.endInclusive -> range.endInclusive
    else -> this
}

@Composable
fun ConfirmationDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onCancel: () -> Unit = onDismiss
) {
    val (_, typography) = LocalAppearance.current

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        BasicText(
            text = text,
            style = typography.xs.medium.center,
            modifier = Modifier.padding(all = 16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            DialogTextButton(
                text = cancelText,
                onClick = onCancel
            )

            DialogTextButton(
                text = confirmText,
                primary = true,
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
inline fun DefaultDialog(
    noinline onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    crossinline content: @Composable ColumnScope.() -> Unit
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
            .padding(all = 48.dp)
            .background(
                color = LocalAppearance.current.colorPalette.background1,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
inline fun <T> ValueSelectorDialog(
    noinline onDismiss: () -> Unit,
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    crossinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    crossinline valueText: @Composable (T) -> String = { it.toString() }
) = Dialog(onDismissRequest = onDismiss) {
    ValueSelectorDialogBody(
        onDismiss = onDismiss,
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelected = onValueSelected,
        modifier = modifier
            .padding(all = 48.dp)
            .background(color = LocalAppearance.current.colorPalette.background1, shape = RoundedCornerShape(8.dp))
            .padding(vertical = 16.dp),
        valueText = valueText
    )
}

@Composable
inline fun <T> ValueSelectorDialogBody(
    noinline onDismiss: () -> Unit,
    title: String,
    selectedValue: T?,
    values: ImmutableList<T>,
    crossinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    crossinline valueText: @Composable (T) -> String = { it.toString() }
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(modifier = modifier) {
        BasicText(
            text = title,
            style = typography.s.semiBold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            values.forEach { value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                onDismiss()
                                onValueSelected(value)
                            }
                        )
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    if (selectedValue == value) Canvas(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = colorPalette.accent,
                                shape = CircleShape
                            )
                    ) {
                        drawCircle(
                            color = colorPalette.onAccent,
                            radius = 4.dp.toPx(),
                            center = size.center,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.4f),
                                blurRadius = 4.dp.toPx(),
                                offset = Offset(x = 0f, y = 1.dp.toPx())
                            )
                        )
                    } else Spacer(
                        modifier = Modifier
                            .size(18.dp)
                            .border(
                                width = 1.dp,
                                color = colorPalette.textDisabled,
                                shape = CircleShape
                            )
                    )

                    BasicText(
                        text = valueText(value),
                        style = typography.xs.medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 24.dp)
        ) {
            DialogTextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier
            )
        }
    }
}

@Composable
inline fun SliderDialog(
    noinline onDismiss: () -> Unit,
    title: String,
    initialValue: Float,
    crossinline onSlide: (Float) -> Unit,
    crossinline onSlideCompleted: (Float) -> Unit,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    crossinline toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    crossinline content: @Composable () -> Unit = { }
) {
    var state by rememberSaveable { mutableFloatStateOf(initialValue) }

    SliderDialog(
        onDismiss = onDismiss,
        title = title,
        state = state,
        setState = { state = it },
        onSlide = onSlide,
        onSlideCompleted = onSlideCompleted,
        modifier = modifier,
        toDisplay = toDisplay,
        min = min,
        max = max,
        steps = steps,
        content = content
    )
}

@Composable
inline fun SliderDialog(
    noinline onDismiss: () -> Unit,
    title: String,
    state: Float,
    crossinline setState: (Float) -> Unit,
    crossinline onSlide: (Float) -> Unit,
    crossinline onSlideCompleted: (Float) -> Unit,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    crossinline toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    crossinline content: @Composable () -> Unit = { }
) {
    val (colorPalette, typography) = LocalAppearance.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .padding(all = 48.dp)
                .background(color = colorPalette.background1, shape = RoundedCornerShape(8.dp))
                .padding(vertical = 16.dp)
        ) {
            BasicText(
                text = title,
                style = typography.s.semiBold,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
            )

            Slider(
                value = state,
                onValueChange = {
                    setState(it)
                    onSlide(it)
                },
                onValueChangeFinished = { onSlideCompleted(state) },
                modifier = Modifier
                    .height(36.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = colorPalette.onAccent,
                    activeTrackColor = colorPalette.accent,
                    inactiveTrackColor = colorPalette.text.copy(alpha = 0.75f)
                ),
                valueRange = min..max,
                steps = steps
            )

            BasicText(
                text = toDisplay(state),
                style = typography.s.semiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            content()

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 24.dp)
            ) {
                DialogTextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier
                )
            }
        }
    }
}
