package app.vitune.android.database.entity

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class SongAggregate(
    @Embedded val song: SongEntity,
    @Relation(
        entity = FormatEntity::class,
        parentColumn = "id",
        entityColumn = "songId",
    )
    val format: FormatEntity?
)