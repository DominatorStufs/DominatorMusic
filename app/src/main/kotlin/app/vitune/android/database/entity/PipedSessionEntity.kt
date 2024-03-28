package app.vitune.android.database.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.vitune.providers.piped.models.authenticatedWith
import io.ktor.http.Url

@Immutable
@Entity(
    tableName = "PipedSession",
    indices = [
        Index(
            value = ["apiBaseUrl", "username"],
            unique = true
        )
    ]
)
data class PipedSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apiBaseUrl: Url,
    val token: String,
    val username: String
) {
}
