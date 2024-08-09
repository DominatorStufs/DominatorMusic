@file:Suppress("TooManyFunctions")

package app.vitune.android.ui.screens.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.NumberFieldDialog
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.Slider
import app.vitune.android.ui.components.themed.Switch
import app.vitune.android.ui.components.themed.ValueSelectorDialog
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.color
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Route
@Composable
fun SettingsScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    PersistMapCleanup("settings/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChange = onTabChanged,
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
    noinline onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    noinline valueText: @Composable (T) -> String = { it.name },
    noinline trailingContent: (@Composable () -> Unit)? = null
) = ValueSelectorSettingsEntry(
    title = title,
    selectedValue = selectedValue,
    values = enumValues<T>().toList().toImmutableList(),
    onValueSelect = onValueSelect,
    modifier = modifier,
    isEnabled = isEnabled,
    valueText = valueText,
    trailingContent = trailingContent
)

@Composable
fun <T> ValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    valueText: @Composable (T) -> String = { it.toString() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) ValueSelectorDialog(
        onDismiss = { isShowingDialog = false },
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelect = onValueSelect,
        valueText = valueText
    )

    SettingsEntry(
        modifier = modifier,
        title = title,
        text = text ?: valueText(selectedValue),
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        trailingContent = trailingContent,
        usePadding = usePadding
    )
}

@Composable
fun SwitchSettingsEntry(
    title: String,
    text: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    usePadding: Boolean = true
) = SettingsEntry(
    modifier = modifier,
    title = title,
    text = text,
    onClick = { onCheckedChange(!isChecked) },
    isEnabled = isEnabled,
    usePadding = usePadding
) {
    Switch(isChecked = isChecked)
}

@Composable
fun SliderSettingsEntry(
    title: String,
    text: String,
    state: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onSlide: (Float) -> Unit = { },
    onSlideComplete: () -> Unit = { },
    toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    isEnabled: Boolean = true,
    usePadding: Boolean = true
) = Column(modifier = modifier) {
    SettingsEntry(
        title = title,
        text = "$text (${toDisplay(state)})",
        onClick = {},
        isEnabled = isEnabled,
        usePadding = usePadding
    )

    Slider(
        state = state,
        setState = onSlide,
        onSlideComplete = onSlideComplete,
        range = range,
        steps = steps,
        modifier = Modifier
            .height(36.dp)
            .alpha(if (isEnabled) 1f else 0.5f)
            .let { if (usePadding) it.padding(start = 32.dp, end = 16.dp) else it }
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    )
}

@Composable
inline fun IntSettingsEntry(
    title: String,
    text: String,
    currentValue: Int,
    crossinline setValue: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    defaultValue: Int = 0,
    isEnabled: Boolean = true,
    usePadding: Boolean = true
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) NumberFieldDialog(
        onDismiss = { isShowingDialog = false },
        onAccept = {
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
        isEnabled = isEnabled,
        usePadding = usePadding
    )
}

@Composable
fun SettingsEntry(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .clickable(enabled = isEnabled, onClick = onClick)
        .alpha(if (isEnabled) 1f else 0.5f)
        .let { if (usePadding) it.padding(start = 32.dp, end = 16.dp) else it }
        .padding(vertical = 16.dp)
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
            style = typography.xs.semiBold.secondary
        )
    }

    trailingContent?.invoke()
}

@Composable
fun SettingsDescription(
    text: String,
    modifier: Modifier = Modifier,
    important: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = if (important) typography.xxs.semiBold.color(colorPalette.red)
        else typography.xxs.secondary,
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

@Composable
fun SettingsCategoryScreen(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    scrollState: ScrollState? = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .let { if (scrollState != null) it.verticalScroll(state = scrollState) else it }
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = title) {
            description?.let { description ->
                BasicText(
                    text = description,
                    style = typography.s.secondary
                )
                SettingsGroupSpacer()
            }
        }

        content()
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    important: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) = Column(modifier = modifier) {
    SettingsEntryGroupText(title = title)

    description?.let { description ->
        SettingsDescription(
            text = description,
            important = important
        )
    }

    content()

    SettingsGroupSpacer()
}
