package com.thuku.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** This service intentionally does not inspect or persist notification contents. It only unlocks media-session access. */
class ThukuNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("ThukuMedia", "Media-session listener connected")
    }
}

private data class MediaSnapshot(
    val controller: MediaController,
    val title: String,
    val artist: String,
    val playing: Boolean
)

private fun hasThukuMediaAccess(context: Context): Boolean {
    val raw = runCatching {
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    }.getOrDefault("")
    return raw.split(':').any { entry ->
        val component = ComponentName.unflattenFromString(entry)
        component?.packageName == context.packageName && component.className == ThukuNotificationListener::class.java.name
    }
}

private fun activeMedia(context: Context): MediaSnapshot? = runCatching {
    val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return null
    val component = ComponentName(context, ThukuNotificationListener::class.java)
    manager.getActiveSessions(component).firstNotNullOfOrNull { controller ->
        val metadata = controller.metadata ?: return@firstNotNullOfOrNull null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().trim()
        if (title.isBlank()) return@firstNotNullOfOrNull null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
        if (state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) return@firstNotNullOfOrNull null
        MediaSnapshot(controller, title.take(120), artist.take(120), state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING)
    }
}.getOrNull()

@Composable
internal fun MusicControlsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(hasThukuMediaAccess(context)) }
    var media by remember { mutableStateOf<MediaSnapshot?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            access = hasThukuMediaAccess(context)
            media = if (access) activeMedia(context) else null
            delay(2_500)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("NOW PLAYING", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (!access) {
                    Text("Connect your music player", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Optional notification access enables media controls only; Thuku does not save or upload notification text.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        access = hasThukuMediaAccess(context)
                    }) { Text("Enable access") }
                } else if (media == null) {
                    Text("Nothing playing", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Start playback in a music app to see controls here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(media!!.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media!!.artist.ifBlank { "Media player" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            media?.let { now ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { runCatching { now.controller.transportControls.skipToPrevious() } }) { Icon(Icons.Filled.SkipPrevious, "Previous track") }
                        IconButton(onClick = {
                            runCatching { if (now.playing) now.controller.transportControls.pause() else now.controller.transportControls.play() }
                            media = now.copy(playing = !now.playing)
                        }) { Icon(if (now.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (now.playing) "Pause" else "Play") }
                        IconButton(onClick = { runCatching { now.controller.transportControls.skipToNext() } }) { Icon(Icons.Filled.SkipNext, "Next track") }
                    }
                }
            }
        }
    }
}
