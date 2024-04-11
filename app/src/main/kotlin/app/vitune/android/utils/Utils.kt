@file:OptIn(UnstableApi::class)

package app.vitune.android.utils

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.vitune.android.models.Song
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.service.LOCAL_KEY_PREFIX
import app.vitune.android.service.isLocal
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.requests.playlistPage
import app.vitune.providers.piped.models.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

val Innertube.SongItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name.orEmpty() })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "albumId" to album?.endpoint?.browseId,
                        "durationText" to durationText,
                        "artistNames" to authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name },
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId }
                    )
                )
                .build()
        )
        .build()

val Innertube.VideoItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name.orEmpty() })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "artistNames" to if (isOfficialMusicVideo) authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name } else null,
                        "artistIds" to if (isOfficialMusicVideo) authors?.mapNotNull { it.endpoint?.browseId }
                        else null
                    )
                )
                .build()
        )
        .build()

val Playlist.Video.asMediaItem: MediaItem?
    get() {
        val key = id ?: return null

        return MediaItem.Builder()
            .setMediaId(key)
            .setUri(key)
            .setCustomCacheKey(key)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(uploaderName)
                    .setArtworkUri(Uri.parse(thumbnailUrl.toString()))
                    .setExtras(
                        bundleOf(
                            "durationText" to duration.toComponents { minutes, seconds, _ ->
                                "$minutes:${seconds.toString().padStart(2, '0')}"
                            },
                            "artistNames" to listOf(uploaderName),
                            "artistIds" to uploaderId?.let { listOf(it) }
                        )
                    )
                    .build()
            )
            .build()
    }

val Song.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (isLocal) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else id.toUri()
        )
        .setCustomCacheKey(id)
        .build()

fun String?.thumbnail(
    size: Int,
    maxSize: Int = AppearancePreferences.maxThumbnailSize
): String? {
    val actualSize = size.coerceAtMost(maxSize)
    return when {
        this?.startsWith("https://lh3.googleusercontent.com") == true -> "$this-w$actualSize-h$actualSize"
        this?.startsWith("https://yt3.ggpht.com") == true -> "$this-w$actualSize-h$actualSize-s$actualSize"
        else -> this
    }
}

fun Uri?.thumbnail(size: Int) = toString().thumbnail(size)?.toUri()

fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

@Suppress("LoopWithTooManyJumpStatements")
suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(
    maxDepth: Int = Int.MAX_VALUE
) = runCatching {
    val page = getOrThrow()
    val songs = page.songsPage?.items.orEmpty().toMutableSet()
    var continuation = page.songsPage?.continuation

    var depth = 0

    while (continuation != null && depth++ < maxDepth) {
        val newSongs = Innertube.playlistPage(
            body = ContinuationBody(continuation = continuation)
        )?.getOrNull()?.takeUnless { it.items.isNullOrEmpty() } ?: break

        if (newSongs.items?.any { it in songs } != false) break

        newSongs.items?.let { songs += it }
        continuation = newSongs.continuation
    }

    page.copy(
        songsPage = Innertube.ItemsPage(
            items = songs.toList(),
            continuation = null
        )
    )
}

fun <T> Flow<T>.onFirst(block: suspend (T) -> Unit): Flow<T> {
    var isFirst = true

    return onEach {
        if (!isFirst) return@onEach

        block(it)
        isFirst = false
    }
}
