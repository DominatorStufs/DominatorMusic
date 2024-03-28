package app.vitune.android.database.repository

import app.vitune.android.database.Database
import app.vitune.android.database.mapper.PipedSessionMapper
import app.vitune.android.domain.material.PipedSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PipedSessionRepository {
    companion object {
        fun pipedSessions(): Flow<List<PipedSession>> {
            return Database.pipedSessions()
                .map { it.map(PipedSessionMapper::map) }
        }

        fun insert(pipedSession: PipedSession) {
            Database.insert(PipedSessionMapper.map(pipedSession))
        }

        fun delete(pipedSession: PipedSession) {
            Database.delete(PipedSessionMapper.map(pipedSession))
        }
    }
}