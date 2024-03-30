package app.vitune.android.database.mapper

import app.vitune.android.database.entity.SearchQueryEntity
import app.vitune.android.domain.material.SearchQuery

class SearchQueryMapper {
    companion object {
        fun map(searchQuery: SearchQuery): SearchQueryEntity {
            return SearchQueryEntity(
                searchQuery.id,
                searchQuery.query
            )
        }

        fun map(searchQueryEntity: SearchQueryEntity): SearchQuery {
            return SearchQuery(
                searchQueryEntity.id,
                searchQueryEntity.query
            )
        }
    }
}