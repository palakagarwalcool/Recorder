package com.medium.cameraapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView

@Composable
fun VideoPreviewScreen(
    uri: String
) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()

        }
    }

    DisposableEffect(
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    StyledPlayerView(context).apply {
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxWidth().height(600.dp)
            )
        }
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}
