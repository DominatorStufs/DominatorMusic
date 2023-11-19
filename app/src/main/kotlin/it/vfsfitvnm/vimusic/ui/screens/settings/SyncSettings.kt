package it.vfsfitvnm.vimusic.ui.screens.settings

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.piped.Piped
import it.vfsfitvnm.piped.models.Instance
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.PipedSession
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.themed.DefaultDialog
import it.vfsfitvnm.vimusic.ui.components.themed.DialogTextButton
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.IconButton
import it.vfsfitvnm.vimusic.ui.components.themed.TextField
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.launch

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
                text = "There was an unknown error linking your Piped account. Please try again.",
                style = typography.xs.semiBold.center,
                modifier = Modifier.padding(all = 24.dp)
            )

            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(all = 8.dp))
            else -> {
                var instances: List<Instance> by persistList(tag = "settings/sync/piped/instances")
                var loadingInstances by rememberSaveable { mutableStateOf(true) }
                var selectedInstance: Int? by rememberSaveable { mutableStateOf(null) }
                var username by rememberSaveable { mutableStateOf("") }
                var password by rememberSaveable { mutableStateOf("") }
                var canSelect by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    Piped.getInstances()?.getOrNull()?.let {
                        selectedInstance = null
                        instances = it
                        canSelect = true
                    } ?: run { hasError = true }
                    loadingInstances = false
                }

                BasicText(
                    text = "Piped",
                    style = typography.m.semiBold
                )

                ValueSelectorSettingsEntry(
                    title = "Instance",
                    selectedValue = selectedInstance,
                    values = instances.indices.toList(),
                    onValueSelected = { selectedInstance = it },
                    valueText = { idx ->
                        idx?.let { instances.getOrNull(it)?.name } ?: "Click to select"
                    },
                    isEnabled = canSelect,
                    trailingContent = if (loadingInstances) {
                        { CircularProgressIndicator() }
                    } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    hintText = "Username",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    hintText = "Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogTextButton(
                    text = "Login",
                    primary = true,
                    enabled = selectedInstance != null,
                    onClick = {
                        coroutineScope.launch {
                            selectedInstance?.let { idx ->
                                isLoading = true
                                val session = Piped.login(
                                    apiBaseUrl = instances[idx].apiBaseUrl,
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

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = "Sync")

        SettingsDescription(text = "You can host playlists elsewhere and synchronize them with ViMusic. Currently only supports Piped.")
        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "PIPED")

        SettingsEntry(
            title = "Add account",
            text = "Link a Piped account with your instance, username and password.",
            onClick = { linkingPiped = true }
        )
        SettingsEntry(
            title = "Learn more",
            text = "Don't know what Piped is or don't have an account? Click here to get redirected to their docs",
            onClick = { uriHandler.openUri("https://github.com/TeamPiped/Piped/blob/master/README.md") }
        )

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = "PIPED SESSIONS")

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