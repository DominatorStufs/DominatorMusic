package app.vitune.android.database.repository

import app.vitune.android.database.Database
import app.vitune.android.database.mapper.SearchQueryMapper
import app.vitune.android.domain.material.SearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchQueryRepository {
    companion object {

        fun queries(query: String): Flow<List<SearchQuery>> {
            return Database.queries(query)
                .map { it.map(SearchQueryMapper::map) }
        }

        fun queriesCount(): Flow<Int> {
            return Database.queriesCount()
        }

        fun clearQueries() {
            Database.clearQueries()
        }

        fun save(searchQuery: SearchQuery) {
            Database.upsert(SearchQueryMapper.map(searchQuery))
        }

        fun delete(searchQuery: SearchQuery) {
            Database.delete(SearchQueryMapper.map(searchQuery))
        }
    }
}