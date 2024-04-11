package app.vitune.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.models.PipedSession
import app.vitune.android.transaction
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.DefaultDialog
import app.vitune.android.ui.components.themed.DialogTextButton
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.TextField
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.center
import app.vitune.android.utils.semiBold
import app.vitune.compose.persist.persistList
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.piped.Piped
import app.vitune.providers.piped.models.Instance
import io.ktor.http.Url
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Route
@Composable
fun SyncSettings() {
    val coroutineScope = rememberCoroutineScope()

    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current

    val pipedSessions by Database.pipedSessions().collectAsState(initial = listOf())

    var linkingPiped by remember { mutableStateOf(false) }
    if (linkingPiped) DefaultDialog(
        onDismiss = { linkingPiped = false },
        horizontalAlignment = Alignment.Start
    ) {
        var isLoading by rememberSaveable { mutableStateOf(false) }
        var hasError by rememberSaveable { mutableStateOf(false) }

        when {
            hasError -> BasicText(
                text = stringResource(R.string.error_piped_link),
                style = typography.xs.semiBold.center,
                modifier = Modifier.padding(all = 24.dp)
            )

            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(all = 8.dp))

            else -> Column(modifier = Modifier.fillMaxWidth()) {
                var instances: List<Instance> by persistList(tag = "settings/sync/piped/instances")
                var loadingInstances by rememberSaveable { mutableStateOf(true) }
                var selectedInstance: Int? by rememberSaveable { mutableStateOf(null) }
                var username by rememberSaveable { mutableStateOf("") }
                var password by rememberSaveable { mutableStateOf("") }
                var canSelect by rememberSaveable { mutableStateOf(false) }
                var instancesUnavailable by rememberSaveable { mutableStateOf(false) }
                var customInstance: String? by rememberSaveable { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    Piped.getInstances()?.getOrNull()?.let {
                        selectedInstance = null
                        instances = it
                        canSelect = true
                    } ?: run { instancesUnavailable = true }
                    loadingInstances = false
                }

                BasicText(
                    text = stringResource(R.string.piped),
                    style = typography.m.semiBold
                )

                if (customInstance == null) ValueSelectorSettingsEntry(
                    title = stringResource(R.string.instance),
                    selectedValue = selectedInstance,
                    values = instances.indices.toImmutableList(),
                    onValueSelected = { selectedInstance = it },
                    valueText = { idx ->
                        idx?.let { instances.getOrNull(it)?.name }
                            ?: if (instancesUnavailable) stringResource(R.string.error_piped_instances_unavailable)
                            else stringResource(R.string.click_to_select)
                    },
                    isEnabled = !instancesUnavailable && canSelect,
                    usePadding = false,
                    trailingContent = if (loadingInstances) {
                        { CircularProgressIndicator() }
                    } else null
                )
                SwitchSettingsEntry(
                    title = stringResource(R.string.custom_instance),
                    text = null,
                    isChecked = customInstance != null,
                    onCheckedChange = { customInstance = if (customInstance == null) "" else null },
                    usePadding = false
                )
                customInstance?.let { instance ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = instance,
                        onValueChange = { customInstance = it },
                        hintText = stringResource(R.string.base_api_url),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    hintText = stringResource(R.string.username),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    hintText = stringResource(R.string.password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogTextButton(
                    text = stringResource(R.string.login),
                    primary = true,
                    enabled = (customInstance?.isNotBlank() == true || selectedInstance != null) &&
                            username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        @Suppress("Wrapping") // thank you ktlint
                        (customInstance?.let {
                            runCatching {
                                Url(it)
                            }.getOrNull() ?: runCatching {
                                Url("https://$it")
                            }.getOrNull()
                        } ?: selectedInstance?.let { instances[it].apiBaseUrl })?.let { url ->
                            coroutineScope.launch {
                                isLoading = true
                                val session = Piped.login(
                                    apiBaseUrl = url,
                                    username = username,
                                    password = password
                                )?.getOrNull().run {
                                    isLoading = false
                                    if (this == null) {
                                        hasError = true
                                        return@launch
                                    }
                                    this
                                }
                                transaction {
                                    Database.insert(
                                        PipedSession(
                                            apiBaseUrl = session.apiBaseUrl,
                                            username = username,
                                            token = session.token
                                        )
                                    )
                                }
                                linkingPiped = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    SettingsCategoryScreen(title = stringResource(R.string.sync)) {
        SettingsDescription(text = stringResource(R.string.sync_description))

        SettingsGroup(title = stringResource(R.string.piped)) {
            SettingsEntry(
                title = stringResource(R.string.add_account),
                text = stringResource(R.string.add_account_description),
                onClick = { linkingPiped = true }
            )
            SettingsEntry(
                title = stringResource(R.string.learn_more),
                text = stringResource(R.string.learn_more_description),
                onClick = { uriHandler.openUri("https://github.com/TeamPiped/Piped/blob/master/README.md") }
            )
        }
        SettingsGroup(title = stringResource(R.string.piped_sessions)) {
            pipedSessions.forEach {
                SettingsEntry(
                    title = it.username,
                    text = it.apiBaseUrl.toString(),
                    onClick = { },
                    trailingContent = {
                        IconButton(
                            onClick = { transaction { Database.delete(it) } },
                            icon = R.drawable.delete,
                            color = colorPalette.text
                        )
                    }
                )
            }
        }
    }
}
