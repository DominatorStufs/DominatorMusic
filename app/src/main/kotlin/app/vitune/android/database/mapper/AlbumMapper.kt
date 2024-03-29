package app.vitune.android.database.mapper

import app.vitune.android.database.entity.AlbumEntity
import app.vitune.android.database.entity.SongAlbumCrossRefEntity
import app.vitune.android.domain.material.Album
import app.vitune.android.domain.value.SongAlbumEntry

class AlbumMapper {
    companion object {
        fun map(album: Album): AlbumEntity {
            return AlbumEntity(
                album.id,
                album.title,
                album.description,
                album.thumbnailUrl,
                album.year,
                album.authorsText,
                album.shareUrl,
                album.timestamp,
                album.bookmarkedAt,
                album.otherInfo
            )
        }

        fun map(albumEntityMapEntry: Map.Entry<AlbumEntity, List<SongAlbumCrossRefEntity>>): Album {
            return map(albumEntityMapEntry.key, albumEntityMapEntry.value)
        }

        fun map(albumEntity: AlbumEntity, songAlbumCrossRefEntities: List<SongAlbumCrossRefEntity>): Album {
            return Album(
                albumEntity.id,
                albumEntity.title,
                albumEntity.description,
                albumEntity.thumbnailUrl,
                albumEntity.year,
                albumEntity.authorsText,
                albumEntity.shareUrl,
                albumEntity.timestamp,
                albumEntity.bookmarkedAt,
                albumEntity.otherInfo,
                songAlbumCrossRefEntities.map {
                    SongAlbumEntry(it.songId, it.position)
                }
            )
        }

        fun mapCrossRefs(album: Album): List<SongAlbumCrossRefEntity> {
            return album.songReferenceIds.map {
                SongAlbumCrossRefEntity(
                    it.songId,
                    album.id,
                    it.position
                )
            }
        }
    }
}