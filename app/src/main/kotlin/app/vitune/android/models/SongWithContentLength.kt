package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import app.vitune.android.database.entity.SongEntity
import app.vitune.android.domain.material.Song

@Immutable
data class SongWithContentLength(
    @Embedded val song: SongEntity,
    val contentLength: Long?
)
