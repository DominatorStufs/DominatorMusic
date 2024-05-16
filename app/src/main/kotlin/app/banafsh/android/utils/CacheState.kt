package app.banafsh.android.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import app.banafsh.android.Database
import app.banafsh.android.LocalPlayerServiceBinder
import app.banafsh.android.R
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.models.Format
import app.banafsh.android.service.PrecacheService
import app.banafsh.android.service.downloadState
import app.banafsh.android.ui.components.themed.HeaderIconButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PlaylistDownloadIcon(
    songs: ImmutableList<MediaItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val isDownloading by downloadState.collectAsState()

    if (
        !songs.all {
            isCached(
                mediaId = it.mediaId,
                key = isDownloading
            )
        }
    ) HeaderIconButton(
        icon = R.drawable.download,
        color = colorPalette.text,
        onClick = {
            songs.forEach {
                PrecacheService.scheduleCache(context.applicationContext, it)
            }
        },
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
fun isCached(
    mediaId: String,
    key: Any? = Unit
): Boolean {
    val cache = LocalPlayerServiceBinder.current?.cache ?: return false
    var format: Format? by remember { mutableStateOf(null) }

    LaunchedEffect(mediaId, key) {
        Database.format(mediaId).distinctUntilChanged().collect { format = it }
    }

    return remember(key) {
        format?.contentLength?.let { len ->
            cache.isCached(mediaId, 0, len)
        } ?: false
    }
}

@OptIn(UnstableApi::class)
class ConditionalCacheDataSourceFactory(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val upstreamDataSourceFactory: DataSource.Factory,
    private val shouldCache: (DataSpec) -> Boolean
) : DataSource.Factory {
    init {
        cacheDataSourceFactory.setUpstreamDataSourceFactory(upstreamDataSourceFactory)
    }

    override fun createDataSource() = object : DataSource {
        private lateinit var selectedFactory: DataSource.Factory
        private val transferListeners = mutableListOf<TransferListener>()

        private val source by lazy {
            selectedFactory.createDataSource().apply {
                transferListeners.forEach { addTransferListener(it) }
                transferListeners.clear()
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int) =
            source.read(buffer, offset, length)

        override fun addTransferListener(transferListener: TransferListener) {
            if (::selectedFactory.isInitialized) source.addTransferListener(transferListener)
            else transferListeners += transferListener
        }

        override fun open(dataSpec: DataSpec): Long {
            selectedFactory =
                if (shouldCache(dataSpec)) cacheDataSourceFactory else upstreamDataSourceFactory

            return source.open(dataSpec)
        }

        override fun getUri() = source.uri
        override fun close() = source.close()
    }
}
