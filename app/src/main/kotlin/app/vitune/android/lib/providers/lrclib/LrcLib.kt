package app.vitune.android.lib.providers.lrclib

import app.vitune.android.lib.providers.common.runCatchingCancellable
import app.vitune.android.lib.providers.lrclib.models.Track
import app.vitune.android.lib.providers.lrclib.models.bestMatchingFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration

object LrcLib {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            defaultRequest {
                url("https://lrclib.net")
            }

            install(UserAgent) {
                agent = "ViTune (https://github.com/25huizengek1/ViTune)"
            }

            expectSuccess = true
        }
    }

    private suspend fun queryLyrics(
        artist: String,
        title: String,
        album: String? = null
    ) = client.get("/api/search") {
        parameter("track_name", title)
        parameter("artist_name", artist)
        if (album != null) parameter("album_name", album)
    }.body<List<Track>>()

    private suspend fun queryLyrics(query: String) = client.get("/api/search") {
        parameter("q", query)
    }.body<List<Track>>()

    suspend fun lyrics(
        artist: String,
        title: String,
        album: String? = null,
        synced: Boolean = true
    ) = runCatchingCancellable {
        queryLyrics(
            artist = artist,
            title = title,
            album = album
        ).let { list ->
            list.filter { if (synced) it.syncedLyrics != null else it.plainLyrics != null }
        }
    }

    suspend fun lyrics(
        query: String,
        synced: Boolean = true
    ) = runCatchingCancellable {
        queryLyrics(query = query).let { list ->
            list.filter { if (synced) it.syncedLyrics != null else it.plainLyrics != null }
        }
    }

    suspend fun bestLyrics(
        artist: String,
        title: String,
        duration: Duration,
        album: String? = null,
        synced: Boolean = true
    ) = lyrics(
        artist = artist,
        title = title,
        album = album,
        synced = synced
    )?.mapCatching { tracks ->
        tracks.bestMatchingFor(title, duration)
            ?.let { if (synced) it.syncedLyrics else it.plainLyrics }
            ?.let(LrcLib::Lyrics)
    }

    @JvmInline
    value class Lyrics(val text: String) {
        val sentences
            get() = runCatching {
                buildMap {
                    put(0L, "")

                    // TODO: fix this mess
                    text.trim().lines().filter { it.length >= 10 }.forEach {
                        put(
                            it[8].digitToInt() * 10L +
                                    it[7].digitToInt() * 100 +
                                    it[5].digitToInt() * 1000 +
                                    it[4].digitToInt() * 10000 +
                                    it[2].digitToInt() * 60 * 1000 +
                                    it[1].digitToInt() * 600 * 1000,
                            it.substring(10)
                        )
                    }
                }
            }.getOrNull()
    }
}
