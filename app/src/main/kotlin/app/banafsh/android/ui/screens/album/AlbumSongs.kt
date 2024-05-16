package app.banafsh.android.ui.screens.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.banafsh.android.LocalPlayerAwareWindowInsets
import app.banafsh.android.LocalPlayerServiceBinder
import app.banafsh.android.R
import app.banafsh.android.lib.core.ui.Dimensions
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.models.Song
import app.banafsh.android.ui.components.LocalMenuState
import app.banafsh.android.ui.components.ShimmerHost
import app.banafsh.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.banafsh.android.ui.components.themed.LayoutWithAdaptiveThumbnail
import app.banafsh.android.ui.components.themed.NonQueuedMediaItemMenu
import app.banafsh.android.ui.components.themed.SecondaryTextButton
import app.banafsh.android.ui.items.SongItem
import app.banafsh.android.ui.items.SongItemPlaceholder
import app.banafsh.android.utils.PlaylistDownloadIcon
import app.banafsh.android.utils.asMediaItem
import app.banafsh.android.utils.enqueue
import app.banafsh.android.utils.forcePlayAtIndex
import app.banafsh.android.utils.forcePlayFromBeginning
import app.banafsh.android.lib.core.ui.utils.isLandscape
import kotlinx.collections.immutable.toImmutableList

// TODO: migrate to single impl for all 'song lists'
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumSongs(
    songs: List<Song>,
    headerContent: @Composable (
        beforeContent: (@Composable () -> Unit)?,
        afterContent: (@Composable () -> Unit)?
    ) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    afterHeaderContent: (@Composable () -> Unit)? = null
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val lazyListState = rememberLazyListState()

    LayoutWithAdaptiveThumbnail(
        thumbnailContent = thumbnailContent,
        modifier = modifier
    ) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent(
                            {
                                SecondaryTextButton(
                                    text = stringResource(R.string.enqueue),
                                    enabled = songs.isNotEmpty(),
                                    onClick = {
                                        binder?.player?.enqueue(songs.map(Song::asMediaItem))
                                    }
                                )
                            },
                            {
                                PlaylistDownloadIcon(
                                    songs = songs.map(Song::asMediaItem).toImmutableList()
                                )
                            }
                        )

                        if (!isLandscape) thumbnailContent()
                        afterHeaderContent?.invoke()
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItem(
                        song = song,
                        index = index,
                        thumbnailSize = Dimensions.thumbnails.song,
                        modifier = Modifier.combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = song.asMediaItem
                                    )
                                }
                            },
                            onClick = {
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(
                                    items = songs.map(Song::asMediaItem),
                                    index = index
                                )
                            }
                        )
                    )
                }

                if (songs.isEmpty()) item(key = "loading") {
                    ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            )
        }
    }
}
