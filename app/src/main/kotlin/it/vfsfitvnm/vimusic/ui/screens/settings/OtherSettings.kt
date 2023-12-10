package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.DatabaseInitializer
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerMediaBrowserService
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.findActivity
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isIgnoringBatteryOptimizations
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Route
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val uriHandler = LocalUriHandler.current
    val (colorPalette) = LocalAppearance.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations }
    )

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = stringResource(R.string.other))

        SettingsEntryGroupText(title = stringResource(R.string.android_auto))

        SwitchSettingEntry(
            title = stringResource(R.string.android_auto),
            text = stringResource(R.string.android_auto_description),
            isChecked = isAndroidAutoEnabled,
            onCheckedChange = { isAndroidAutoEnabled = it }
        )

        AnimatedVisibility(visible = isAndroidAutoEnabled) {
            SettingsDescription(text = stringResource(R.string.android_auto_warning))
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.search_history))

        SwitchSettingEntry(
            title = stringResource(R.string.pause_search_history),
            text = stringResource(R.string.pause_search_history_description),
            isChecked = DataPreferences.pauseSearchHistory,
            onCheckedChange = { DataPreferences.pauseSearchHistory = it }
        )

        AnimatedVisibility(visible = !(DataPreferences.pauseSearchHistory && queriesCount == 0)) {
            SettingsEntry(
                title = stringResource(R.string.clear_search_history),
                text = if (queriesCount > 0) stringResource(
                    R.string.format_clear_search_history_amount,
                    queriesCount
                )
                else stringResource(R.string.empty_history),
                onClick = { query(Database::clearQueries) },
                isEnabled = queriesCount > 0
            )
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.built_in_playlists))

        IntSettingEntry(
            title = stringResource(R.string.top_list_length),
            text = stringResource(R.string.top_list_length_description),
            currentValue = DataPreferences.topListLength,
            setValue = { DataPreferences.topListLength = it },
            defaultValue = 10,
            range = 1..500
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.quick_picks))

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.quick_picks_source),
            selectedValue = DataPreferences.quickPicksSource,
            onValueSelected = { DataPreferences.quickPicksSource = it },
            valueText = { it.displayName() }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.service_lifetime))

        AnimatedVisibility(visible = !isIgnoringBatteryOptimizations) {
            ImportantSettingsDescription(
                text = stringResource(R.string.service_lifetime_warning)
            )
        }

        if (isAtLeastAndroid12) SettingsDescription(
            text = stringResource(R.string.service_lifetime_warning_android_12)
        )

        SettingsEntry(
            title = stringResource(R.string.ignore_battery_optimizations),
            text = if (isIgnoringBatteryOptimizations) stringResource(R.string.ignoring_battery_optimizations)
            else stringResource(R.string.ignore_battery_optimizations_action),
            onClick = {
                if (!isAtLeastAndroid6) return@SettingsEntry

                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e: ActivityNotFoundException) {
                        context.toast(context.getString(R.string.no_battery_optimization_settings_found))
                    }
                }
            },
            isEnabled = !isIgnoringBatteryOptimizations
        )

        AnimatedVisibility(!isAtLeastAndroid12 || isIgnoringBatteryOptimizations) {
            SwitchSettingEntry(
                title = stringResource(R.string.invincible_service),
                text = stringResource(R.string.invincible_service_description),
                isChecked = PlayerPreferences.isInvincibilityEnabled,
                onCheckedChange = { PlayerPreferences.isInvincibilityEnabled = it }
            )
        }

        SettingsEntry(
            title = stringResource(R.string.need_help),
            text = stringResource(R.string.need_help_description),
            onClick = {
                uriHandler.openUri("https://dontkillmyapp.com/")
            }
        )

        SettingsDescription(text = stringResource(R.string.service_lifetime_report_issue))

        SettingsGroupSpacer()

        var showTroubleshoot by rememberSaveable { mutableStateOf(false) }

        AnimatedContent(showTroubleshoot, label = "") { show ->
            if (show) Column {
                SettingsEntryGroupText(title = stringResource(R.string.troubleshooting))

                ImportantSettingsDescription(text = stringResource(R.string.troubleshooting_warning))

                val troubleshootScope = rememberCoroutineScope()
                var reloading by rememberSaveable { mutableStateOf(false) }

                SecondaryTextButton(
                    text = stringResource(R.string.reload_app_internals),
                    onClick = {
                        if (!reloading) troubleshootScope.launch {
                            reloading = true
                            binder?.restartForegroundOrStop()
                            DatabaseInitializer.reload()
                            reloading = false
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryTextButton(
                    text = stringResource(R.string.kill_app),
                    onClick = {
                        binder?.stopRadio()
                        binder?.invincible = false
                        context.findActivity().finishAndRemoveTask()
                        binder?.restartForegroundOrStop()
                        troubleshootScope.launch {
                            delay(500L)
                            Handler(Looper.getMainLooper()).postAtFrontOfQueue { exitProcess(0) }
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                SettingsGroupSpacer()
            } else SecondaryTextButton(
                text = stringResource(R.string.show_troubleshoot_section),
                onClick = {
                    coroutineScope.launch {
                        delay(500)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                    showTroubleshoot = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}
