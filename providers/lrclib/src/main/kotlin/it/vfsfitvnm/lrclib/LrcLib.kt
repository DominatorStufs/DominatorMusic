package it.vfsfitvnm.lrclib

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import it.vfsfitvnm.extensions.runCatchingCancellable
import it.vfsfitvnm.lrclib.models.Track
import it.vfsfitvnm.lrclib.models.bestMatchingFor
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

            expectSuccess = true
        }
    }

    private suspend fun queryLyrics(artist: String, title: String, album: String? = null) =
        client.get("/api/search") {
            parameter("track_name", title)
            parameter("artist_name", artist)
            if (album != null) parameter("album_name", album)
        }.body<List<Track>>().filter { it.syncedLyrics != null }

    suspend fun lyrics(
        artist: String,
        title: String,
        duration: Duration,
        album: String? = null
    ) = runCatchingCancellable {
        val tracks = queryLyrics(artist, title, album)

        tracks.bestMatchingFor(title, duration)?.syncedLyrics?.let(LrcLib::Lyrics)
    }

    suspend fun lyrics(artist: String, title: String) = runCatchingCancellable {
        queryLyrics(artist = artist, title = title, album = null)
    }

    @JvmInline
    value class Lyrics(val text: String) {
        val sentences
            get() = runCatching {
                buildMap {
                    put(0L, "")
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
