package app.vitune.android.database.mapper

import app.vitune.android.database.entity.ArtistEntity
import app.vitune.android.domain.material.Artist

class ArtistMapper {
    companion object {
        fun map(artist: Artist): ArtistEntity {
            return ArtistEntity(
                artist.id,
                artist.name,
                artist.thumbnailUrl,
                artist.timestamp,
                artist.bookmarkedAt,
            )
        }

        fun map(artistEntity: ArtistEntity): Artist {
            return Artist(
                artistEntity.id,
                artistEntity.name,
                artistEntity.thumbnailUrl,
                artistEntity.timestamp,
                artistEntity.bookmarkedAt
            )
        }
    }
}