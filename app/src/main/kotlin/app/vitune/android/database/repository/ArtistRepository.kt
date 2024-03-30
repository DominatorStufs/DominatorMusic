package app.vitune.android.database.repository

import app.vitune.android.database.Database
import app.vitune.android.database.mapper.ArtistMapper
import app.vitune.android.domain.material.Artist
import app.vitune.core.data.enums.ArtistSortBy
import app.vitune.core.data.enums.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtistRepository {
    companion object {
        fun artist(artistId: String): Artist? {
            return Database.artist(artistId)
                ?.let(ArtistMapper::map)
        }

        fun artistFlow(artistId: String): Flow<Artist?> {
            return Database.artistFlow(artistId)
                .map { it?.let(ArtistMapper::map) }
        }

        // TODO: Move to Song
        fun artistsBySongId(songId: String): Flow<List<Artist>> {
            return Database.artistsBySongId(songId)
                .map { it.map(ArtistMapper::map) }
        }

        fun artistsByName(sortOrder: SortOrder): Flow<List<Artist>> {
            return when (sortOrder) {
                SortOrder.Ascending -> Database.artistsByNameAsc()
                SortOrder.Descending -> Database.artistsByNameDesc()
            }.map { it.map(ArtistMapper::map) }
        }

        fun artistsByRowId(sortOrder: SortOrder): Flow<List<Artist>> {
            return when (sortOrder) {
                SortOrder.Ascending -> Database.artistsByRowIdAsc()
                SortOrder.Descending -> Database.artistsByRowIdDesc()
            }.map { it.map(ArtistMapper::map) }
        }

        fun artists(sortBy: ArtistSortBy, sortOrder: SortOrder) = when (sortBy) {
            ArtistSortBy.Name -> artistsByName(sortOrder)
            ArtistSortBy.DateAdded -> artistsByRowId(sortOrder)
        }

        fun save(artist: Artist) {
            Database.upsert(ArtistMapper.map(artist))
        }
    }
}