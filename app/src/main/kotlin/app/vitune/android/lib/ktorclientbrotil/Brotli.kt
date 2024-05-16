package app.vitune.android.lib.ktorclientbrotil

import io.ktor.client.plugins.compression.ContentEncoding

fun ContentEncoding.Config.brotli(quality: Float? = null) {
    customEncoder(BrotliEncoder, quality)
}
