package app.vitune.android.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.lib.compose.persist.PersistMapCleanup
import app.vitune.android.lib.compose.routing.RouteHandler
import app.vitune.android.lib.compose.routing.defaultStacking
import app.vitune.android.lib.compose.routing.defaultStill
import app.vitune.android.lib.compose.routing.defaultUnstacking
import app.vitune.android.lib.compose.routing.isStacking
import app.vitune.android.lib.compose.routing.isUnstacking
import app.vitune.android.lib.compose.routing.isUnknown
import app.vitune.android.models.SearchQuery
import app.vitune.android.models.toUiMood
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.query
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.builtInPlaylistRoute
import app.vitune.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.vitune.android.ui.screens.localPlaylistRoute
import app.vitune.android.ui.screens.localplaylist.LocalPlaylistScreen
import app.vitune.android.ui.screens.moodRoute
import app.vitune.android.ui.screens.pipedPlaylistRoute
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.ui.screens.search.SearchScreen
import app.vitune.android.ui.screens.searchResultRoute
import app.vitune.android.ui.screens.searchRoute
import app.vitune.android.ui.screens.searchresult.SearchResultScreen
import app.vitune.android.ui.screens.settings.SettingsScreen
import app.vitune.android.ui.screens.settingsRoute

@Route
@Composable
fun HomeScreen(onPlaylistUrl: (String) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    RouteHandler(
        listenToGlobalEmitter = true,
        transitionSpec = {
            when {
                isStacking -> defaultStacking
                isUnstacking -> defaultUnstacking
                isUnknown -> when {
                    initialState.route == searchRoute && targetState.route == searchResultRoute -> defaultStacking
                    initialState.route == searchResultRoute && targetState.route == searchRoute -> defaultUnstacking
                    else -> defaultStill
                }

                else -> defaultStill
            }
        }
    ) {
        GlobalRoutes()

        settingsRoute {
            SettingsScreen()
        }

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(playlistId = playlistId)
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
        }

        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = { searchRoute(query) }
            )
        }

        searchRoute { initialTextInput ->
            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!DataPreferences.pauseSearchHistory) query {
                        Database.insert(SearchQuery(query = query))
                    }
                },
                onViewPlaylist = onPlaylistUrl
            )
        }

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.settings,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = UIStatePreferences.homeScreenTabIndex,
                onTabChanged = { UIStatePreferences.homeScreenTabIndex = it },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
                    item(1, stringResource(R.string.discover), R.drawable.globe)
                    item(2, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(3, stringResource(R.string.playlists), R.drawable.playlist)
                    item(4, stringResource(R.string.artists), R.drawable.person)
                    item(5, stringResource(R.string.albums), R.drawable.disc)
                    item(6, stringResource(R.string.local), R.drawable.download)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    val onSearchClick = { searchRoute("") }
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it.key) },
                            onArtistClick = { artistRoute(it.key) },
                            onPlaylistClick = {
                                playlistRoute(
                                    p0 = it.key,
                                    p1 = null,
                                    p2 = null,
                                    p3 = it.channel?.name == "YouTube Music"
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        1 -> HomeDiscovery(
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumRoute(it) },
                            onSearchClick = onSearchClick
                        )

                        2 -> HomeSongs(
                            onSearchClick = onSearchClick
                        )

                        3 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onPipedPlaylistClick = { session, playlist ->
                                pipedPlaylistRoute(
                                    p0 = session.apiBaseUrl.toString(),
                                    p1 = session.token,
                                    p2 = playlist.id.toString()
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        4 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        5 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        6 -> HomeLocalSongs(
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}
