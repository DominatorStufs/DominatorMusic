package app.vitune.android.ui.screens

import androidx.compose.runtime.Composable
import app.vitune.android.models.Mood
import app.vitune.android.ui.screens.album.AlbumScreen
import app.vitune.android.ui.screens.artist.ArtistScreen
import app.vitune.android.ui.screens.pipedplaylist.PipedPlaylistScreen
import app.vitune.android.ui.screens.playlist.PlaylistScreen
import app.vitune.android.ui.screens.searchresult.SearchResultScreen
import app.vitune.android.ui.screens.settings.SettingsScreen
import app.vitune.compose.routing.Route0
import app.vitune.compose.routing.Route1
import app.vitune.compose.routing.Route3
import app.vitune.compose.routing.Route4
import app.vitune.compose.routing.RouteHandlerScope
import app.vitune.core.data.enums.BuiltInPlaylist
import io.ktor.http.Url
import java.util.UUID

/**
 * Marker class for linters that a composable is a route and should not be handled like a regular
 * composable, but rather as an entrypoint.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Route

val albumRoute = Route1<String>("albumRoute")
val artistRoute = Route1<String>("artistRoute")
val builtInPlaylistRoute = Route1<BuiltInPlaylist>("builtInPlaylistRoute")
val localPlaylistRoute = Route1<Long>("localPlaylistRoute")
val pipedPlaylistRoute = Route3<String, String, String>("pipedPlaylistRoute")
val playlistRoute = Route4<String, String?, Int?, Boolean>("playlistRoute")
val moodRoute = Route1<Mood>("moodRoute")
val searchResultRoute = Route1<String>("searchResultRoute")
val searchRoute = Route1<String>("searchRoute")
val settingsRoute = Route0("settingsRoute")

@Composable
fun RouteHandlerScope.GlobalRoutes() {
    albumRoute { browseId ->
        AlbumScreen(browseId = browseId)
    }

    artistRoute { browseId ->
        ArtistScreen(browseId = browseId)
    }

    pipedPlaylistRoute { apiBaseUrl, sessionToken, playlistId ->
        PipedPlaylistScreen(
            apiBaseUrl = runCatching { Url(apiBaseUrl) }.getOrNull()
                ?: error("Invalid apiBaseUrl: $apiBaseUrl is not a valid Url"),
            sessionToken = sessionToken,
            playlistId = runCatching {
                UUID.fromString(playlistId)
            }.getOrNull() ?: error("Invalid playlistId: $playlistId is not a valid UUID")
        )
    }

    playlistRoute { browseId, params, maxDepth, shouldDedup ->
        PlaylistScreen(
            browseId = browseId,
            params = params,
            maxDepth = maxDepth,
            shouldDedup = shouldDedup
        )
    }

    settingsRoute {
        SettingsScreen()
    }

    searchResultRoute { query ->
        SearchResultScreen(
            query = query,
            onSearchAgain = { searchRoute(query) }
        )
    }
}
