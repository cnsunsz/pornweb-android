package com.pornweb.android.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.ExtraFile
import com.pornweb.android.data.ProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(id: Long, part: Int, resume: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as PornWebApp
    val c = app.container
    var currentPart by remember { mutableIntStateOf(part) }
    var extras by remember { mutableStateOf<List<ExtraFile>>(emptyList()) }
    var startPositionMs by remember { mutableStateOf(0L) }
    var ready by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        try {
            val d = c.api.detail(id)
            extras = d.extraFiles.orEmpty()
            title = d.displayTitle()
            val p = d.progress ?: 0.0
            val dur = d.duration ?: 0.0
            val nearEnd = dur > 0 && p >= dur * 0.95
            startPositionMs = if (resume && p > 5 && !nearEnd) (p * 1000).toLong() else 0L
            if (d.progressPart != null && resume) currentPart = d.progressPart
        } catch (_: Exception) {
        } finally {
            ready = true
        }
    }

    DisposableEffect(Unit) {
        val prev = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.requestedOrientation = prev
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (!ready) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("正在准备播放…", color = Color.White)
        }
        return
    }

    PlayerBody(
        id = id,
        part = currentPart,
        extras = extras,
        title = title,
        startPositionMs = startPositionMs,
        onPart = { currentPart = it; startPositionMs = 0L },
        onBack = onBack
    )
}

@Composable
private fun PlayerBody(
    id: Long,
    part: Int,
    extras: List<ExtraFile>,
    title: String,
    startPositionMs: Long,
    onPart: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PornWebApp
    val c = app.container
    val url = remember(id, part, c.tokenStore.token, c.serverStore.baseUrl) { c.streamUrl(id, part) }
    val token = c.tokenStore.token.orEmpty()
    val startMsState = rememberUpdatedState(startPositionMs)

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)
            .setDefaultRequestProperties(
                buildMap {
                    if (token.isNotBlank()) put("Authorization", "Bearer $token")
                    put("User-Agent", "PornWeb-Android/1.0")
                }
            )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()
            .apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    DisposableEffect(player) {
        onDispose {
            saveProgress(c, id, part, player)
            player.release()
        }
    }

    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        val start = startMsState.value
        if (start > 0) player.seekTo(start)
        player.play()
    }

    DisposableEffect(player, id, part) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    saveProgress(c, id, part, player)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) saveProgress(c, id, part, player)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, id, part) {
        while (isActive) {
            delay(10_000)
            saveProgress(c, id, part, player)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    this.player = player
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        if (title.isNotBlank()) {
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        if (extras.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                extras.forEachIndexed { i, extra ->
                    FilterChip(
                        selected = part == i,
                        onClick = { onPart(i) },
                        label = { Text(extra.label ?: "${i + 1}") },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

private fun saveProgress(
    c: com.pornweb.android.data.AppContainer,
    id: Long,
    part: Int,
    player: ExoPlayer
) {
    val pos = player.currentPosition.coerceAtLeast(0) / 1000.0
    val dur = player.duration.let { if (it > 0) it / 1000.0 else 0.0 }
    if (pos < 1 && dur <= 0) return
    CoroutineScope(Dispatchers.IO).launch {
        try {
            c.api.saveProgress(id, ProgressRequest(position = pos, duration = dur, part = part))
        } catch (_: Exception) {
        }
    }
}
