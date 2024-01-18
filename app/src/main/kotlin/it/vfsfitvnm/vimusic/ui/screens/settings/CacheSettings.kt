package it.vfsfitvnm.vimusic.ui.screens.settings

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheSize
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.ui.screens.Route

@kotlin.OptIn(ExperimentalCoilApi::class)
@OptIn(UnstableApi::class)
@Route
@Composable
fun CacheSettings() = with(DataPreferences) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    SettingsCategoryScreen(title = stringResource(R.string.cache)) {
        SettingsDescription(text = stringResource(R.string.cache_description))

        Coil.imageLoader(context).diskCache?.let { diskCache ->
            val diskCacheSize = remember(diskCache) { diskCache.size }

            SettingsGroup(
                title = stringResource(R.string.image_cache),
                description = stringResource(
                    R.string.format_cache_space_used,
                    Formatter.formatShortFileSize(context, diskCacheSize),
                    diskCacheSize * 100 / coilDiskCacheMaxSize.bytes.coerceAtLeast(1)
                )
            ) {
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.max_size),
                    selectedValue = coilDiskCacheMaxSize,
                    onValueSelected = { coilDiskCacheMaxSize = it }
                )
            }
        }
        binder?.cache?.let { cache ->
            val diskCacheSize by remember { derivedStateOf { cache.cacheSpace } }

            SettingsGroup(
                title = stringResource(R.string.song_cache),
                description = buildString {
                    append(Formatter.formatShortFileSize(context, diskCacheSize))
                    append(" ${stringResource(R.string.used_word)}")
                    when (val size = exoPlayerDiskCacheMaxSize) {
                        ExoPlayerDiskCacheSize.Unlimited -> {}
                        else -> append(" (${diskCacheSize * 100 / size.bytes}%)")
                    }
                }
            ) {
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.max_size),
                    selectedValue = exoPlayerDiskCacheMaxSize,
                    onValueSelected = { exoPlayerDiskCacheMaxSize = it }
                )

                SwitchSettingsEntry(
                    title = stringResource(R.string.progressive_cache),
                    text = stringResource(R.string.progressive_cache_description),
                    isChecked = PlayerPreferences.progressiveCache,
                    onCheckedChange = {
                        PlayerPreferences.progressiveCache = it
                    }
                )
            }
        }
    }
}
