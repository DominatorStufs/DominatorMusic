package app.vitune.android.usecase

import app.vitune.android.database.repository.AlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumUseCase {
    companion object {
        fun toggleBookmark(albumId: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val album = AlbumRepository.album(albumId)
                album?.toggleBookmark()
                album?.let(AlbumRepository::save)
            }
        }
    }
}