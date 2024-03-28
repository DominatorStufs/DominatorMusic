package app.vitune.android.ui.screens.localplaylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.database.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Playlist
import app.vitune.android.models.Song
import app.vitune.android.models.SongPlaylistMap
import app.vitune.android.database.query
import app.vitune.android.database.transaction
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.ConfirmationDialog
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.InPlaylistMediaItemMenu
import app.vitune.android.ui.components.themed.Menu
import app.vitune.android.ui.components.themed.MenuEntry
import app.vitune.android.ui.components.themed.ReorderHandle
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.TextFieldDialog
import app.vitune.android.ui.items.SongItem
import app.vitune.android.utils.PlaylistDownloadIcon
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.completed
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.android.utils.launchYouTubeMusic
import app.vitune.android.utils.toast
import app.vitune.compose.persist.persist
import app.vitune.compose.reordering.animateItemPlacement
import app.vitune.compose.reordering.draggedItem
import app.vitune.compose.reordering.rememberReorderingState
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.requests.playlistPage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

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

    val lazyListState = rememberLazyListState()

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = songs ?: emptyList<Any>(),
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable { mutableStateOf(false) }

    if (isRenaming) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        initialTextInput = playlist?.name.orEmpty(),
        onDismiss = { isRenaming = false },
        onDone = { text ->
            query {
                playlist?.copy(name = text)?.let(Database::update)
            }
        }
    )

    var isDeleting by rememberSaveable { mutableStateOf(false) }

    if (isDeleting) ConfirmationDialog(
        text = stringResource(R.string.confirm_delete_playlist),
        onDismiss = { isDeleting = false },
        onConfirm = {
            query {
                playlist?.let(Database::delete)
            }
            onDelete()
        }
    )

    Box(modifier = modifier) {
        LookaheadScope {
            LazyColumn(
                state = reorderingState.lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Header(
                        title = playlist?.name
                            ?: stringResource(R.string.unknown),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        SecondaryTextButton(
                            text = stringResource(R.string.enqueue),
                            enabled = songs?.isNotEmpty() == true,
                            onClick = {
                                songs?.map(Song::asMediaItem)
                                    ?.let { mediaItems ->
                                        binder?.player?.enqueue(mediaItems)
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        songs?.map(Song::asMediaItem)
                            ?.let { PlaylistDownloadIcon(songs = it.toImmutableList()) }

                        HeaderIconButton(
                            icon = R.drawable.ellipsis_horizontal,
                            color = colorPalette.text,
                            onClick = {
                                menuState.display {
                                    Menu {
                                        playlist?.browseId?.let { browseId ->
                                            MenuEntry(
                                                icon = R.drawable.sync,
                                                text = stringResource(R.string.sync),
                                                onClick = {
                                                    menuState.hide()
                                                    transaction {
                                                        runBlocking(Dispatchers.IO) {
                                                            Innertube.playlistPage(
                                                                BrowseBody(
                                                                    browseId = browseId
                                                                )
                                                            )?.completed()
                                                        }?.getOrNull()?.let { remotePlaylist ->
                                                            Database.clearPlaylist(playlistId)

                                                            remotePlaylist.songsPage
                                                                ?.items
                                                                ?.map(Innertube.SongItem::asMediaItem)
                                                                ?.onEach(Database::insert)
                                                                ?.mapIndexed { position, mediaItem ->
                                                                    SongPlaylistMap(
                                                                        songId = mediaItem.mediaId,
                                                                        playlistId = playlistId,
                                                                        position = position
                                                                    )
                                                                }
                                                                ?.let(Database::insertSongPlaylistMaps)
                                                        }
                                                    }
                                                }
                                            )

                                            songs?.firstOrNull()?.id?.let { firstSongId ->
                                                MenuEntry(
                                                    icon = R.drawable.play,
                                                    text = stringResource(R.string.watch_playlist_on_youtube),
                                                    onClick = {
                                                        menuState.hide()
                                                        binder?.player?.pause()
                                                        uriHandler.openUri(
                                                            "https://youtube.com/watch?v=$firstSongId&list=${
                                                                playlist?.browseId
                                                                    ?.drop(2)
                                                            }"
                                                        )
                                                    }
                                                )

                                                MenuEntry(
                                                    icon = R.drawable.musical_notes,
                                                    text = stringResource(R.string.open_in_youtube_music),
                                                    onClick = {
                                                        menuState.hide()
                                                        binder?.player?.pause()
                                                        if (
                                                            !launchYouTubeMusic(
                                                                context = context,
                                                                endpoint = "watch?v=$firstSongId&list=${
                                                                    playlist?.browseId
                                                                        ?.drop(2)
                                                                }"
                                                            )
                                                        ) context.toast(
                                                            context.getString(R.string.youtube_music_not_installed)
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        MenuEntry(
                                            icon = R.drawable.pencil,
                                            text = stringResource(R.string.rename),
                                            onClick = {
                                                menuState.hide()
                                                isRenaming = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.trash,
                                            text = stringResource(R.string.delete),
                                            onClick = {
                                                menuState.hide()
                                                isDeleting = true
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                itemsIndexed(
                    items = songs ?: emptyList(),
                    key = { _, song -> song.id },
                    contentType = { _, song -> song }
                ) { index, song ->
                    SongItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        InPlaylistMediaItemMenu(
                                            playlistId = playlistId,
                                            positionInPlaylist = index,
                                            song = song,
                                            onDismiss = menuState::hide
                                        )
                                    }
                                },
                                onClick = {
                                    songs
                                        ?.map(Song::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            )
                            .animateItemPlacement(reorderingState)
                            .draggedItem(
                                reorderingState = reorderingState,
                                index = index
                            )
                            .background(colorPalette.background0),
                        song = song,
                        thumbnailSize = Dimensions.thumbnails.song
                    ) {
                        ReorderHandle(
                            reorderingState = reorderingState,
                            index = index
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            visible = !reorderingState.isDragging,
            onClick = {
                songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            }
        )
    }
}
