package app.vitune.android.database.mapper

import app.vitune.android.database.entity.SongAggregate
import app.vitune.android.database.entity.SongEntity
import app.vitune.android.domain.material.Song

class SongMapper {
    companion object {
        fun map(song: Song): SongEntity {
            return SongEntity(
                song.id,
                song.title,
                song.artistsText,
                song.durationText,
                song.thumbnailUrl,
                song.likedAt,
                song.totalPlayTimeMs,
                song.loudnessBoost,
                song.blacklisted
            )
        }

        fun map(songAggregate: SongAggregate): Song {
            val songEntity = songAggregate.song
            return Song(
                songEntity.id,
                songEntity.title,
                songEntity.artistsText,
                songEntity.durationText,
                songEntity.thumbnailUrl,
                songEntity.likedAt,
                songEntity.totalPlayTimeMs,
                songEntity.loudnessBoost,
                songEntity.blacklisted,
                songAggregate.format?.let(FormatMapper::map)
            )
        }
    }
}