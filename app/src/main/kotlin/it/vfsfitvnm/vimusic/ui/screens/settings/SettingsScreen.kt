@file:Suppress("TooManyFunctions")

package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.themed.NumberFieldDialog
import it.vfsfitvnm.vimusic.ui.components.themed.Scaffold
import it.vfsfitvnm.vimusic.ui.components.themed.Switch
import it.vfsfitvnm.vimusic.ui.components.themed.ValueSelectorDialog
import it.vfsfitvnm.vimusic.ui.screens.GlobalRoutes
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalAnimationApi::class)
@Route
@Composable
fun SettingsScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabChanged,
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.appearance), R.drawable.color_palette)
                    item(1, stringResource(R.string.player), R.drawable.play)
                    item(2, stringResource(R.string.cache), R.drawable.server)
                    item(3, stringResource(R.string.database), R.drawable.server)
                    item(4, stringResource(R.string.sync), R.drawable.sync)
                    item(5, stringResource(R.string.other), R.drawable.shapes)
                    item(6, stringResource(R.string.about), R.drawable.information)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> AppearanceSettings()
                        1 -> PlayerSettings()
                        2 -> CacheSettings()
                        3 -> DatabaseSettings()
                        4 -> SyncSettings()
                        5 -> OtherSettings()
                        6 -> About()
                    }
                }
            }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> EnumValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    crossinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    crossinline valueText: @Composable (T) -> String = { it.name },
    noinline trailingContent: (@Composable () -> Unit)? = null
) = ValueSelectorSettingsEntry(
    title = title,
    selectedValue = selectedValue,
    values = enumValues<T>().toList().toImmutableList(),
    onValueSelected = onValueSelected,
    modifier = modifier,
    isEnabled = isEnabled,
    valueText = valueText,
    trailingContent = trailingContent
)

@Composable
inline fun <T> ValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    crossinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    crossinline valueText: @Composable (T) -> String = { it.toString() },
    noinline trailingContent: (@Composable () -> Unit)? = null
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) ValueSelectorDialog(
        onDismiss = { isShowingDialog = false },
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelected = onValueSelected,
        valueText = valueText
    )

    SettingsEntry(
        modifier = modifier,
        title = title,
        text = valueText(selectedValue),
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        trailingContent = trailingContent
    )
}

@Composable
fun SwitchSettingEntry(
    title: String,
    text: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) = SettingsEntry(
    modifier = modifier,
    title = title,
    text = text,
    onClick = { onCheckedChange(!isChecked) },
    isEnabled = isEnabled
) {
    Switch(isChecked = isChecked)
}

@Composable
fun SliderSettingEntry(
    title: String,
    text: String,
    initialValue: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onSlide: (Float) -> Unit = { },
    onSlideCompleted: (Float) -> Unit = { },
    toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    isEnabled: Boolean = true
) = Column(modifier = modifier) {
    val (colorPalette) = LocalAppearance.current

    var state by rememberSaveable { mutableFloatStateOf(initialValue) }

    SettingsEntry(
        title = title,
        text = "$text (${toDisplay(state)})",
        onClick = {},
        isEnabled = isEnabled
    )

    Slider(
        value = state,
        onValueChange = {
            state = it
            onSlide(it)
        },
        onValueChangeFinished = { onSlideCompleted(state) },
        modifier = Modifier
            .height(36.dp)
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(start = 16.dp)
            .padding(all = 16.dp)
            .fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = colorPalette.onAccent,
            activeTrackColor = colorPalette.accent,
            inactiveTrackColor = colorPalette.text.copy(alpha = 0.75f)
        ),
        valueRange = min..max,
        steps = steps
    )
}

@Composable
inline fun IntSettingEntry(
    title: String,
    text: String,
    currentValue: Int,
    crossinline setValue: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    defaultValue: Int = 0,
    isEnabled: Boolean = true
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) NumberFieldDialog(
        onDismiss = { isShowingDialog = false },
        onDone = {
            setValue(it)
            isShowingDialog = false
        },
        initialValue = currentValue,
        defaultValue = defaultValue,
        convert = { it.toIntOrNull() },
        range = range
    )

    SettingsEntry(
        modifier = modifier,
        title = title,
        text = text,
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled
    )
}

@Composable
fun SettingsEntry(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .clickable(enabled = isEnabled, onClick = onClick)
        .alpha(if (isEnabled) 1f else 0.5f)
        .padding(start = 16.dp)
        .padding(all = 16.dp)
        .fillMaxWidth()
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(modifier = Modifier.weight(1f)) {
        BasicText(
            text = title,
            style = typography.xs.semiBold.copy(color = colorPalette.text)
        )

        if (text != null) BasicText(
            text = text,
            style = typography.xs.semiBold.copy(color = colorPalette.textSecondary)
        )
    }

    trailingContent?.invoke()
}

@Composable
fun SettingsDescription(
    text: String,
    modifier: Modifier = Modifier
) = BasicText(
    text = text,
    style = LocalAppearance.current.typography.xxs.secondary,
    modifier = modifier
        .padding(start = 16.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp)
)

@Composable
fun ImportantSettingsDescription(
    text: String,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = typography.xxs.semiBold.color(colorPalette.red),
        modifier = modifier
            .padding(start = 16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsEntryGroupText(
    title: String,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = title.uppercase(),
        style = typography.xxs.semiBold.copy(colorPalette.accent),
        modifier = modifier
            .padding(start = 16.dp)
            .padding(horizontal = 16.dp)
            .semantics { text = AnnotatedString(text = title) }
    )
}

@Composable
fun SettingsGroupSpacer(modifier: Modifier = Modifier) = Spacer(modifier = modifier.height(24.dp))
