package it.vfsfitvnm.piped.models

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatedPlaylist(
    @SerialName("playlistId")
    val id: UUIDString
)

@Serializable
data class PlaylistPreview(
    val id: UUIDString,
    val name: String,
    @SerialName("shortDescription")
    val description: String? = null,
    @SerialName("thumbnail")
    val thumbnailUrl: UrlString,
    @SerialName("videos")
    val videoCount: Int
)

@Serializable
data class Playlist(
    val name: String,
    val thumbnailUrl: UrlString,
    val description: String? = null,
    val bannerUrl: UrlString? = null,
    @SerialName("videos")
    val videoCount: Int,
    @SerialName("relatedStreams")
    val videos: List<Video>
) {
    @Serializable
    data class Video(
        val url: String, // not a real url, why?
        val title: String,
        @SerialName("thumbnail")
        val thumbnailUrl: UrlString,
        val uploaderName: String,
        val uploaderUrl: String, // not a real url either
        @SerialName("uploaderAvatar")
        val uploaderAvatarUrl: UrlString
    ) {
        val id
            get() = if (url.startsWith("/watch?v=")) url.substringAfter("/watch?v=")
            else Url(url).parameters["v"]?.firstOrNull()

        val uploaderId
            get() = if (url.startsWith("/channel/")) url.substringAfter("/channel/")
            else Url(url).pathSegments.lastOrNull()
    }
}