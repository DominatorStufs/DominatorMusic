package app.vitune.android.ui.screens.builtinplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import app.vitune.android.R
import app.vitune.android.lib.compose.persist.PersistMapCleanup
import app.vitune.android.lib.compose.routing.RouteHandler
import app.vitune.android.lib.core.data.enums.BuiltInPlaylist
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route

@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableIntStateOf(
            when (builtInPlaylist) {
                BuiltInPlaylist.Favorites -> 0
                BuiltInPlaylist.Offline -> 1
                BuiltInPlaylist.Top -> 2
            }
        )
    }

    PersistMapCleanup(prefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.favorites), R.drawable.heart)
                    item(1, stringResource(R.string.offline), R.drawable.airplane)
                    item(
                        2,
                        stringResource(R.string.format_top_playlist, DataPreferences.topListLength),
                        R.drawable.trending_up
                    )
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Favorites)
                        1 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Offline)
                        2 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Top)
                    }
                }
            }
        }
    }
}
