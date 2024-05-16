package app.vitune.android.ui.screens.localplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.lib.compose.persist.PersistMapCleanup
import app.vitune.android.lib.compose.persist.persist
import app.vitune.android.lib.compose.routing.RouteHandler
import app.vitune.android.models.Playlist
import app.vitune.android.models.Song
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.adaptiveThumbnailContent
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Route
@Composable
fun LocalPlaylistScreen(playlistId: Long) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "localPlaylist/$playlistId/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            var playlist by persist<Playlist?>("localPlaylist/$playlistId/playlist")
            var songs by persist<List<Song>?>("localPlaylist/$playlistId/Songs")

            LaunchedEffect(Unit) {
                Database
                    .playlist(playlistId)
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect { playlist = it }
            }

            LaunchedEffect(Unit) {
                Database
                    .playlistSongs(playlistId)
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect { songs = it }
            }

            val thumbnailContent = remember(playlist) {
                playlist?.thumbnail?.let { url ->
                    adaptiveThumbnailContent(
                        isLoading = false,
                        url = url
                    )
                } ?: { }
            }

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    playlist?.let {
                        when (currentTabIndex) {
                            0 -> LocalPlaylistSongs(
                                playlist = it,
                                songs = songs ?: emptyList(),
                                thumbnailContent = thumbnailContent,
                                onDelete = pop
                            )
                        }
                    }
                }
            }
        }
    }
}
