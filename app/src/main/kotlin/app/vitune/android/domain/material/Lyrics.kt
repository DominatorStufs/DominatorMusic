package app.vitune.android.domain.material

data class Lyrics(
    val songId: String,
    val fixed: String?,
    val synced: String?,
    val startTime: Long? = null
)
