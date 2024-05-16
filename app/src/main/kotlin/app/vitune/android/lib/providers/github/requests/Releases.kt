package app.vitune.android.lib.providers.github.requests

import app.vitune.android.lib.providers.common.runCatchingCancellable
import app.vitune.android.lib.providers.github.GitHub
import app.vitune.android.lib.providers.github.models.Release
import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun GitHub.releases(
    owner: String,
    repo: String,
    page: Int = 1,
    pageSize: Int = 30
) = runCatchingCancellable {
    httpClient.get("repos/$owner/$repo/releases") {
        withPagination(page = page, size = pageSize)
    }.body<List<Release>>()
}
