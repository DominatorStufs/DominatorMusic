package app.vitune.android.domain.material

import app.vitune.android.domain.value.SongAlbumEntry

class Album(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long? = null,
    var bookmarkedAt: Long? = null,
    val otherInfo: String? = null,
    val songReferenceIds: List<SongAlbumEntry> = mutableListOf()
) {
    fun toggleBookmark() {
        if (this.bookmarkedAt == null) {
            this.bookmarkedAt = System.currentTimeMillis()
        } else {
            this.bookmarkedAt = null
        }
    }
}
