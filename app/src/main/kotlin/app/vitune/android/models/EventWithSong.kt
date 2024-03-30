package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import app.vitune.android.database.entity.SongAggregate
import app.vitune.android.database.entity.SongEntity
import app.vitune.android.domain.material.Song

@Immutable
data class EventWithSong(
    @Embedded val event: Event,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: SongAggregate
)
