package it.vfsfitvnm.lrclib.models

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Duration

@Serializable
data class Track(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val duration: Long,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

internal fun List<Track>.bestMatchingFor(title: String, duration: Duration) =
    (firstOrNull { it.duration == duration.inWholeSeconds }
        ?: minByOrNull { abs(it.trackName.length - title.length) })