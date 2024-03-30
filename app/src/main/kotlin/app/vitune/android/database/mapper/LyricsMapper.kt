package app.vitune.android.database.mapper

import app.vitune.android.database.entity.LyricsEntity
import app.vitune.android.domain.material.Lyrics

class LyricsMapper {
    companion object {
        fun map(lyrics: Lyrics): LyricsEntity {
            return LyricsEntity(
                lyrics.songId,
                lyrics.fixed,
                lyrics.synced,
                lyrics.startTime,
            )
        }

        fun map(lyricsEntity: LyricsEntity): Lyrics {
            return Lyrics(
                lyricsEntity.songId,
                lyricsEntity.fixed,
                lyricsEntity.synced,
                lyricsEntity.startTime
            )
        }
    }
}