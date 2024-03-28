package app.vitune.android.domain.material

import app.vitune.providers.piped.models.authenticatedWith
import io.ktor.http.*

data class PipedSession(
    val id: Long, val apiBaseUrl: Url, val token: String, val username: String // the username should never change on piped
) {
    companion object {
        fun new(
            apiBaseUrl: Url, token: String, username: String
        ): PipedSession {
            return PipedSession(0, apiBaseUrl, token, username)
        }
    }

    fun toApiSession() = apiBaseUrl authenticatedWith token
}
