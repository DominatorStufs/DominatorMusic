package app.vitune.android.database.mapper

import app.vitune.android.database.entity.PipedSessionEntity
import app.vitune.android.domain.material.PipedSession

class PipedSessionMapper {
    companion object {
        fun map(pipedSession: PipedSession): PipedSessionEntity {
            return PipedSessionEntity(
                pipedSession.id,
                pipedSession.apiBaseUrl,
                pipedSession.token,
                pipedSession.username
            )
        }

        fun map(pipedSessionEntity: PipedSessionEntity): PipedSession {
            return PipedSession(
                pipedSessionEntity.id,
                pipedSessionEntity.apiBaseUrl,
                pipedSessionEntity.token,
                pipedSessionEntity.username
            )
        }
    }
}