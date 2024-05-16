package app.vitune.android.lib.providers.innertube.requests

import app.vitune.android.lib.providers.common.runCatchingCancellable
import app.vitune.android.lib.providers.innertube.Innertube
import app.vitune.android.lib.providers.innertube.models.GetQueueResponse
import app.vitune.android.lib.providers.innertube.models.bodies.QueueBody
import app.vitune.android.lib.providers.innertube.utils.from
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.queue(body: QueueBody) = runCatchingCancellable {
    val response = client.post(QUEUE) {
        setBody(body)
        mask("queueDatas.content.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK")
    }.body<GetQueueResponse>()

    response
        .queueData
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.playlistPanelVideoRenderer
                ?.let { Innertube.SongItem.from(it) }
        }
}

suspend fun Innertube.song(videoId: String): Result<Innertube.SongItem?>? =
    queue(QueueBody(videoIds = listOf(videoId)))?.map { it?.firstOrNull() }
