package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import app.vitune.android.domain.material.Song
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.px
import app.vitune.providers.innertube.Innertube
import coil.compose.AsyncImage

@Composable
fun SongItem(
    song: Innertube.SongItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier
) = SongItem(
    modifier = modifier,
    thumbnailUrl = song.thumbnail?.size(thumbnailSize.px),
    title = song.info?.name,
    authors = song.authors?.joinToString("") { it.name.orEmpty() },
    duration = song.durationText,
    thumbnailSize = thumbnailSize
)

@Composable
fun SongItem(
    song: MediaItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) = SongItem(
    modifier = modifier,
    thumbnailUrl = song.mediaMetadata.artworkUri.thumbnail(thumbnailSize.px)?.toString(),
    title = song.mediaMetadata.title?.toString(),
    authors = song.mediaMetadata.artist?.toString(),
    duration = song.mediaMetadata.extras?.getString("durationText"),
    thumbnailSize = thumbnailSize,
    onThumbnailContent = onThumbnailContent,
    trailingContent = trailingContent
)

@Composable
fun SongItem(
    song: Song,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    index: Int? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) = SongItem(
    modifier = modifier,
    index = index,
    thumbnailUrl = song.thumbnailUrl?.thumbnail(thumbnailSize.px),
    title = song.title,
    authors = song.artistsText,
    duration = song.durationText,
    thumbnailSize = thumbnailSize,
    onThumbnailContent = onThumbnailContent,
    trailingContent = trailingContent
)

@Composable
fun SongItem(
    thumbnailUrl: String?,
    title: String?,
    authors: String?,
    duration: String?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    index: Int? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current

    SongItem(
        title = title,
        authors = authors,
        duration = duration,
        thumbnailSize = thumbnailSize,
        thumbnailContent = {
            Box(
                modifier = Modifier
                    .clip(LocalAppearance.current.thumbnailShape)
                    .background(colorPalette.background1)
                    .fillMaxSize()
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (index != null) {
                    Box(
                        modifier = Modifier
                            .background(color = Color.Black.copy(alpha = 0.75f))
                            .fillMaxSize()
                    )
                    BasicText(
                        text = "${index + 1}",
                        style = typography.xs.semiBold.copy(color = Color.White),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            onThumbnailContent?.invoke(this)
        },
        modifier = modifier,
        trailingContent = trailingContent
    )
}

@Composable
fun SongItem(
    title: String?,
    authors: String?,
    duration: String?,
    thumbnailSize: Dp,
    thumbnailContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val (_, typography) = LocalAppearance.current

    ItemContainer(
        alternative = false,
        thumbnailSize = thumbnailSize,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(thumbnailSize),
            content = thumbnailContent
        )

        ItemInfoContainer {
            trailingContent?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BasicText(
                        text = title.orEmpty(),
                        style = typography.xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    it()
                }
            } ?: BasicText(
                text = title.orEmpty(),
                style = typography.xs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                authors?.let {
                    BasicText(
                        text = authors,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                duration?.let {
                    BasicText(
                        text = duration,
                        style = typography.xxs.secondary.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier
) = ItemContainer(
    alternative = false,
    thumbnailSize = thumbnailSize,
    modifier = modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(thumbnailSize)
    )

    ItemInfoContainer {
        TextPlaceholder()
        TextPlaceholder()
    }
}
