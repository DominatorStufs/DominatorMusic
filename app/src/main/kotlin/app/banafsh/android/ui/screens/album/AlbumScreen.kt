package app.banafsh.android.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.banafsh.android.Database
import app.banafsh.android.R
import app.banafsh.android.lib.compose.persist.PersistMapCleanup
import app.banafsh.android.lib.compose.persist.persist
import app.banafsh.android.lib.compose.persist.persistList
import app.banafsh.android.lib.compose.routing.RouteHandler
import app.banafsh.android.lib.core.ui.Dimensions
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.models.Album
import app.banafsh.android.models.Song
import app.banafsh.android.models.SongAlbumMap
import app.banafsh.android.query
import app.banafsh.android.ui.components.themed.Header
import app.banafsh.android.ui.components.themed.HeaderIconButton
import app.banafsh.android.ui.components.themed.HeaderPlaceholder
import app.banafsh.android.ui.components.themed.PlaylistInfo
import app.banafsh.android.ui.components.themed.Scaffold
import app.banafsh.android.ui.components.themed.adaptiveThumbnailContent
import app.banafsh.android.ui.items.AlbumItem
import app.banafsh.android.ui.items.AlbumItemPlaceholder
import app.banafsh.android.ui.screens.GlobalRoutes
import app.banafsh.android.ui.screens.Route
import app.banafsh.android.ui.screens.albumRoute
import app.banafsh.android.ui.screens.searchresult.ItemsPage
import app.banafsh.android.utils.asMediaItem
import app.banafsh.android.lib.core.ui.utils.stateFlowSaver
import app.banafsh.android.lib.providers.innertube.Innertube
import app.banafsh.android.lib.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.lib.providers.innertube.requests.albumPage
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Route
@Composable
fun AlbumScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val tabIndexState = rememberSaveable(saver = stateFlowSaver()) { MutableStateFlow(0) }
    val tabIndex by tabIndexState.collectAsState()

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")
    var songs by persistList<Song>("album/$browseId/songs")

    PersistMapCleanup(prefix = "album/$browseId/")

    LaunchedEffect(Unit) {
        Database
            .album(browseId)
            .distinctUntilChanged()
            .combine(
                Database
                    .albumSongs(browseId)
                    .distinctUntilChanged()
                    .cancellable()
            ) { currentAlbum, currentSongs ->
                album = currentAlbum
                songs = currentSongs

                if (currentAlbum?.timestamp != null && currentSongs.isNotEmpty()) return@combine

                withContext(Dispatchers.IO) {
                    Innertube.albumPage(BrowseBody(browseId = browseId))
                        ?.onSuccess { newAlbumPage ->
                            albumPage = newAlbumPage

                            Database.clearAlbum(browseId)

                            Database.upsert(
                                album = Album(
                                    id = browseId,
                                    title = newAlbumPage.title,
                                    description = newAlbumPage.description,
                                    thumbnailUrl = newAlbumPage.thumbnail?.url,
                                    year = newAlbumPage.year,
                                    authorsText = newAlbumPage.authors
                                        ?.joinToString("") { it.name.orEmpty() },
                                    shareUrl = newAlbumPage.url,
                                    timestamp = System.currentTimeMillis(),
                                    bookmarkedAt = album?.bookmarkedAt,
                                    otherInfo = newAlbumPage.otherInfo
                                ),
                                songAlbumMaps = newAlbumPage
                                    .songsPage
                                    ?.items
                                    ?.map { it.asMediaItem }
                                    ?.onEach { Database.insert(it) }
                                    ?.mapIndexed { position, mediaItem ->
                                        SongAlbumMap(
                                            songId = mediaItem.mediaId,
                                            albumId = browseId,
                                            position = position
                                        )
                                    } ?: emptyList()
                            )
                        }?.exceptionOrNull()?.printStackTrace()
                }
            }.collect()
    }

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            val headerContent: @Composable (
                beforeContent: (@Composable () -> Unit)?,
                afterContent: (@Composable () -> Unit)?
            ) -> Unit = { beforeContent, afterContent ->
                if (album?.timestamp == null) HeaderPlaceholder(modifier = Modifier.shimmer())
                else {
                    val (colorPalette) = LocalAppearance.current
                    val context = LocalContext.current

                    Header(title = album?.title ?: stringResource(R.string.unknown)) {
                        beforeContent?.invoke()

                        Spacer(modifier = Modifier.weight(1f))

                        afterContent?.invoke()

                        HeaderIconButton(
                            icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline
                            else R.drawable.bookmark,
                            color = colorPalette.accent,
                            onClick = {
                                val bookmarkedAt =
                                    if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                                query {
                                    album
                                        ?.copy(bookmarkedAt = bookmarkedAt)
                                        ?.let(Database::update)
                                }
                            }
                        )

                        HeaderIconButton(
                            icon = R.drawable.share_social,
                            color = colorPalette.text,
                            onClick = {
                                album?.shareUrl?.let { url ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(sendIntent, null)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = album?.timestamp == null,
                url = album?.thumbnailUrl
            )

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = { newTab -> tabIndexState.update { newTab } },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(1, stringResource(R.string.other_versions), R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> AlbumSongs(
                            songs = songs,
                            headerContent = headerContent,
                            thumbnailContent = thumbnailContent,
                            afterHeaderContent = {
                                if (album == null) PlaylistInfo(playlist = albumPage)
                                else PlaylistInfo(playlist = album)
                            }
                        )

                        1 -> {
                            ItemsPage(
                                tag = "album/$browseId/alternatives",
                                header = headerContent,
                                initialPlaceholderCount = 1,
                                continuationPlaceholderCount = 1,
                                emptyItemsText = stringResource(R.string.no_alternative_version),
                                provider = albumPage?.let {
                                    {
                                        Result.success(
                                            Innertube.ItemsPage(
                                                items = albumPage?.otherVersions,
                                                continuation = null
                                            )
                                        )
                                    }
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSize = Dimensions.thumbnails.album,
                                        modifier = Modifier.clickable { albumRoute(album.key) }
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
