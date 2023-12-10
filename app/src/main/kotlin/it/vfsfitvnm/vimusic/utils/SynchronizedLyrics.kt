package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class SynchronizedLyrics(
    val sentences: Map<Long, String>,
    private val positionProvider: () -> Long
) {
    var index by mutableIntStateOf(currentIndex)
        private set

    private val currentIndex: Int
        get() {
            var index = -1
            for ((key) in sentences) {
                if (key >= positionProvider()) break
                index++
            }
            return if (index == -1) 0 else index
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        return if (newIndex != index) {
            index = newIndex
            true
        } else false
    }
}
