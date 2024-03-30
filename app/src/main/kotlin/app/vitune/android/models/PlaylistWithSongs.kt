package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.vitune.android.database.entity.SongAggregate
import app.vitune.android.database.entity.SongEntity
import app.vitune.android.domain.material.Song

@Immutable
data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SortedSongPlaylistMap::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongAggregate>
)
