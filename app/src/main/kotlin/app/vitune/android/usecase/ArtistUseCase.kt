package app.vitune.android.usecase

import app.vitune.android.database.repository.ArtistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistUseCase {
    companion object {
        fun toggleBookmark(artistId: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val artist = ArtistRepository.artist(artistId)
                artist?.toggleBookmark()
                artist?.let(ArtistRepository::save)
            }
        }
    }
}