package app.vitune.android.database.repository

import app.vitune.android.database.Database
import app.vitune.android.database.mapper.AlbumMapper
import app.vitune.android.domain.material.Album
import app.vitune.core.data.enums.AlbumSortBy
import app.vitune.core.data.enums.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepository {
    companion object {

        fun album(albumId: String): Album? {
            val album = Database.album(albumId)
            val crossRefEntities = Database.songAlbumCrossReferences(albumId)
            return album?.let { AlbumMapper.map(it, crossRefEntities) }
        }

        fun albumFlow(albumId: String): Flow<Album?> {
            return Database.albumFlow(albumId)
                .map {
                    val entry = it.entries.firstOrNull()
                    entry?.let(AlbumMapper::map)
                }
        }

        fun albumsByRowId(sortOrder: SortOrder): Flow<List<Album>> {
            return when (sortOrder) {
                SortOrder.Ascending -> Database.albumsByRowIdAsc()
                SortOrder.Descending -> Database.albumsByRowIdDesc()
            }.map { it.map(AlbumMapper::map) }
        }

        fun albumsByTitle(sortOrder: SortOrder): Flow<List<Album>> {
            return when (sortOrder) {
                SortOrder.Ascending -> Database.albumsByTitleAsc()
                SortOrder.Descending -> Database.albumsByTitleDesc()
            }.map { it.map(AlbumMapper::map) }
        }

        fun albumsByYear(sortOrder: SortOrder): Flow<List<Album>> {
            return when (sortOrder) {
                SortOrder.Ascending -> Database.albumsByYearAsc()
                SortOrder.Descending -> Database.albumsByYearDesc()
            }.map { it.map(AlbumMapper::map) }
        }

        fun albums(sortBy: AlbumSortBy, sortOrder: SortOrder) = when (sortBy) {
            AlbumSortBy.Title -> albumsByTitle(sortOrder)
            AlbumSortBy.Year -> albumsByYear(sortOrder)
            AlbumSortBy.DateAdded -> albumsByRowId(sortOrder)
        }

        fun save(album: Album) {
            val albumEntity = AlbumMapper.map(album)
            val songAlbumCrossRefEntities = AlbumMapper.mapCrossRefs(album)
            Database.clearAlbum(album.id)
            Database.upsert(albumEntity, songAlbumCrossRefEntities)
        }
    }
}