package app.banafsh.android.lib.core.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Context.streamVolumeFlow(stream: Int = AudioManager.STREAM_MUSIC) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actualStream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 0)
            if (stream != actualStream) return
            trySend(intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0))
        }
    }

    ContextCompat.registerReceiver(
        /* context = */ this@Context,
        /* receiver = */ receiver,
        /* filter = */ IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
        /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
    )
    awaitClose { unregisterReceiver(receiver) }
}
