package app.vitune.android.domain.material

data class Song(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    var durationText: String?,
    val thumbnailUrl: String?,
    var likedAt: Long? = null,
    var totalPlayTimeMs: Long = 0,
    var loudnessBoost: Float? = null,
    val blacklisted: Boolean = false
) {
    fun toggleLike() {
        if (likedAt == null) {
            this.likedAt = System.currentTimeMillis()
        } else {
            likedAt = null
        }
    }

    fun isLiked(): Boolean {
        return this.likedAt != null
    }

    fun updateDurationText(durationText: String?) {
        this.durationText = durationText
    }

    fun incrementTotalPlayTime(playTimeInMs: Long) {
        this.totalPlayTimeMs += playTimeInMs
    }

    fun updateLoudnessBoost(loudnessBoost: Float?) {
        this.loudnessBoost = loudnessBoost
    }
}
