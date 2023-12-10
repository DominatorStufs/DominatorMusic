package it.vfsfitvnm.vimusic.ui.screens.settings

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheSize
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

@kotlin.OptIn(ExperimentalCoilApi::class)
@OptIn(UnstableApi::class)
@Route
@Composable
fun CacheSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    with(DataPreferences) {
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
            Header(title = stringResource(R.string.cache))

            SettingsDescription(text = stringResource(R.string.cache_description))

            Coil.imageLoader(context).diskCache?.let { diskCache ->
                val diskCacheSize = remember(diskCache) { diskCache.size }

                SettingsGroupSpacer()

                SettingsEntryGroupText(title = stringResource(R.string.image_cache))

                SettingsDescription(
                    text = stringResource(
                        R.string.format_cache_space_used,
                        Formatter.formatShortFileSize(context, diskCacheSize),
                        diskCacheSize * 100 / coilDiskCacheMaxSize.bytes.coerceAtLeast(1)
                    )
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.max_size),
                    selectedValue = coilDiskCacheMaxSize,
                    onValueSelected = { coilDiskCacheMaxSize = it }
                )
            }

            binder?.cache?.let { cache ->
                val diskCacheSize by remember { derivedStateOf { cache.cacheSpace } }

                SettingsGroupSpacer()

                SettingsEntryGroupText(title = stringResource(R.string.song_cache))

                SettingsDescription(
                    text = buildString {
                        append(Formatter.formatShortFileSize(context, diskCacheSize))
                        append(" ${stringResource(R.string.used_word)}")
                        when (val size = exoPlayerDiskCacheMaxSize) {
                            ExoPlayerDiskCacheSize.Unlimited -> {}
                            else -> append(" (${diskCacheSize * 100 / size.bytes}%)")
                        }
                    }
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.max_size),
                    selectedValue = exoPlayerDiskCacheMaxSize,
                    onValueSelected = { exoPlayerDiskCacheMaxSize = it }
                )
            }
        }
    }
}
