package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import app.vitune.android.database.entity.SongEntity
import app.vitune.android.domain.material.Song

@Immutable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Lyrics(
    @PrimaryKey val songId: String,
    val fixed: String?,
    val synced: String?,
    val startTime: Long? = null
)
