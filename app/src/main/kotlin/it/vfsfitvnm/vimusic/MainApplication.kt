package it.vfsfitvnm.vimusic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.util.DebugLogger
import it.vfsfitvnm.compose.persist.PersistMap
import it.vfsfitvnm.compose.persist.PersistMapOwner
import it.vfsfitvnm.vimusic.preferences.DataPreferences

val globalPersistMap = PersistMap()

class MainApplication : Application(), ImageLoaderFactory, PersistMapOwner {
    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
        DatabaseInitializer()
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes(DataPreferences.coilDiskCacheMaxSize.bytes)
                .build()
        )
        .let { if (BuildConfig.DEBUG) it.logger(DebugLogger()) else it }
        .build()

    override val persistMap = globalPersistMap
}
