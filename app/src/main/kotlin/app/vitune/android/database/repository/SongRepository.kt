package app.vitune.android.database.repository

import app.vitune.android.database.Database
import app.vitune.android.database.mapper.FormatMapper
import app.vitune.android.database.mapper.LyricsMapper
import app.vitune.android.database.mapper.SongMapper
import app.vitune.android.domain.material.Song
import app.vitune.android.domain.material.Lyrics
import app.vitune.android.domain.material.Format
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepository {
    companion object {
        fun song(songId: String): Song? {
            return Database.song(songId)
                ?.let(SongMapper::map)
        }

        fun songFlow(songId: String): Flow<Song?> {
            return Database.songFlow(songId)
                .map { if (it == null) null else SongMapper.map(it) }
        }

        fun songsByPlayTime(sortOrder: SortOrder, limit: Int = -1, isLocal: Boolean = false): Flow<List<Song>> {
            return when (sortOrder) {
                SortOrder.Ascending -> if (isLocal) Database.localSongsByPlayTimeAsc() else Database.songsByPlayTimeAsc()
                SortOrder.Descending -> if (isLocal) Database.localSongsByPlayTimeDesc() else Database.songsByPlayTimeDesc(limit)
            }.map { it.map(SongMapper::map) }
        }

        fun songsByTitle(sortOrder: SortOrder, isLocal: Boolean = false): Flow<List<Song>> {
            return when (sortOrder) {
                SortOrder.Ascending -> if (isLocal) Database.localSongsByTitleAsc() else Database.songsByTitleAsc()
                SortOrder.Descending -> if (isLocal) Database.localSongsByTitleDesc() else Database.songsByTitleDesc()
            }.map { it.map(SongMapper::map) }
        }

        fun songsByRowId(sortOrder: SortOrder, isLocal: Boolean = false): Flow<List<Song>> {
            return when (sortOrder) {
                SortOrder.Ascending -> if (isLocal) Database.localSongsByRowIdAsc() else Database.songsByRowIdAsc()
                SortOrder.Descending -> if (isLocal) Database.localSongsByRowIdDesc() else Database.songsByRowIdDesc()
            }.map { it.map(SongMapper::map) }
        }

        fun songs(sortBy: SongSortBy, sortOrder: SortOrder, isLocal: Boolean = false) = when (sortBy) {
            SongSortBy.PlayTime -> songsByPlayTime(sortOrder, -1, isLocal)
            SongSortBy.Title -> songsByTitle(sortOrder, isLocal)
            SongSortBy.DateAdded -> songsByRowId(sortOrder, isLocal)
        }

        fun favorites(): Flow<List<Song>> {
            return Database.favorites().map { it.map(SongMapper::map) }
        }

        fun albumSongs(albumId: String): Flow<List<Song>> {
            return Database.albumSongs(albumId).map { it.map(SongMapper::map) }
        }

        fun artistSongs(artistId: String): Flow<List<Song>> {
            return Database.artistSongs(artistId).map { it.map(SongMapper::map) }
        }

        fun search(query: String): Flow<List<Song>> {
            return Database.search(query).map { it.map(SongMapper::map) }
        }

        // TODO: Move trending stuff somewhere else?
        fun trending(limit: Int = 3): Flow<List<Song>> {
            return Database.trending(limit).map { it.map(SongMapper::map) }
        }
        fun trending(
            limit: Int = 3,
            now: Long = System.currentTimeMillis(),
            period: Long
        ): Flow<List<Song>> {
            return Database.trending(limit, now, period).map { it.map(SongMapper::map) }
        }

        // TODO: Move somewhere else
        fun playlistWithSongs(id: Long): Flow<List<Song>> {
            return Database.playlistWithSongs(id)
                .map { it?.songs ?: emptyList() }
                .map { it.map(SongMapper::map) }
        }

        // TODO: Merge into song eventually
        fun lyrics(songId: String): Flow<Lyrics?> {
            return Database.lyrics(songId)
                .map { it?.let(LyricsMapper::map) }
        }

        // TODO: Merge into song eventually
        fun format(songId: String): Flow<Format?> {
            return Database.format(songId)
                .map { it?.let(FormatMapper::map) }
        }

        // TODO: Use Song for that later
        fun loudnessDb(songId: String): Flow<Float?> {
            return Database.loudnessDb(songId)
        }

        fun save(lyrics: Lyrics) {
            Database.upsert(LyricsMapper.map(lyrics))
        }

        fun save(format: Format) {
            Database.upsert(FormatMapper.map(format))
        }

        fun save(song: Song) {
            // TODO: Handle Artist References
            Database.upsert(SongMapper.map(song))
        }

        fun delete(song: Song) {
            Database.delete(SongMapper.map(song))
        }

    }
}