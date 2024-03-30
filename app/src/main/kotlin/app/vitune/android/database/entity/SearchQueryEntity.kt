package app.vitune.android.database.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "SearchQuery",
    indices = [
        Index(
            value = ["query"],
            unique = true
        )
    ]
)
data class SearchQueryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String
)
