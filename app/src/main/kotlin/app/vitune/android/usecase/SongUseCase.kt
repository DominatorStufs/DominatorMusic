package app.vitune.android.usecase

import app.vitune.android.database.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SongUseCase {
    companion object {
        fun toggleLike(songId: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val song = SongRepository.song(songId)
                song?.toggleLike()
                song?.let(SongRepository::save)
            }
        }

        fun updateDurationText(songId: String, durationText: String?) {
            // TODO: What is an duration text and why is it a property of the song?
            CoroutineScope(Dispatchers.IO).launch {
                val song = SongRepository.song(songId)
                song?.updateDurationText(durationText)
                song?.let(SongRepository::save)
            }
        }

        fun isLiked(songId: String): Flow<Boolean> {
            return SongRepository.songFlow(songId)
                .map { it?.isLiked() == true }
                .distinctUntilChanged()
        }

        fun incrementTotalPlayTime(songId: String, playTime: Long) {
            CoroutineScope(Dispatchers.IO).launch {
                val song = SongRepository.song(songId)
                song?.incrementTotalPlayTime(playTime)
                song?.let(SongRepository::save)
            }
        }

        fun loudnessBoost(songId: String): Flow<Float> {
            return SongRepository.songFlow(songId)
                .map { it?.loudnessBoost ?: 0f }
                .distinctUntilChanged()
        }

        fun updateLoudnessBoost(songId: String, loudnessBoost: Float?) {
            CoroutineScope(Dispatchers.IO).launch {
                val song = SongRepository.song(songId)
                song?.updateLoudnessBoost(loudnessBoost)
                song?.let(SongRepository::save)
            }
        }
    }
}