package app.vitune.android.domain.material

class Artist(
    val id: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long? = null,
    var bookmarkedAt: Long? = null
) {
    fun toggleBookmark() {
        if (this.bookmarkedAt == null) {
            this.bookmarkedAt = System.currentTimeMillis()
        } else {
            this.bookmarkedAt = null
        }
    }
}
