package com.pornweb.android.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pornweb.android.PornWebApp
import com.pornweb.android.R
import com.pornweb.android.data.ExtraFile
import com.pornweb.android.data.ProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        try {
            val d = c.api.detail(id)
            extras = d.extraFileList()
            title = d.displayTitle()
            val p = d.progress ?: 0.0
            val dur = d.duration ?: 0.0
            val nearEnd = dur > 0 && p >= dur * 0.95
            startPositionMs = if (resume && p > 5 && !nearEnd) (p * 1000).toLong() else 0L
            if (d.progressPart != null && resume) currentPart = d.progressPart
        } catch (e: Exception) {
            loadError = c.parseError(e)
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
    if (loadError != null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(loadError!!, color = Color.White, modifier = Modifier.padding(24.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
            }
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
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val factory = OkHttpDataSource.Factory(okHttp)
            .setUserAgent("PornWeb-Android/1.0.3")
            .setDefaultRequestProperties(
                buildMap {
                    if (token.isNotBlank()) put("Authorization", "Bearer $token")
                    put("Accept", "*/*")
                }
            )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .build()
            .apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var playError by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose {
            saveProgress(c, id, part, player)
            player.release()
        }
    }

    LaunchedEffect(url) {
        playError = null
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        val start = startMsState.value
        if (start > 0) player.seekTo(start)
        player.play()
    }

    DisposableEffect(player, id, part) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    saveProgress(c, id, part, player)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) saveProgress(c, id, part, player)
            }

            override fun onPlayerError(error: PlaybackException) {
                playError = error.message ?: "播放失败 (${error.errorCode})"
                controlsVisible = true
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

    LaunchedEffect(player) {
        while (isActive) {
            val dur = player.duration
            durationMs = if (dur > 0) dur else 0L
            if (!seeking) {
                positionMs = player.currentPosition.coerceAtLeast(0)
            }
            playing = player.isPlaying
            delay(200)
        }
    }

    LaunchedEffect(controlsVisible, playing) {
        if (controlsVisible && playing && playError == null) {
            delay(4_000)
            controlsVisible = false
        }
    }

    val durationForSlider = durationMs.coerceAtLeast(1L).toFloat()
    val sliderPos = if (seeking) seekValue else positionMs.toFloat().coerceIn(0f, durationForSlider)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.player_view, null, false) as PlayerView
                view.player = player
                view.useController = false
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.keepScreenOn = true
                view
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        controlsVisible = !controlsVisible
                    }
                }
        )
        if (playError != null) {
            Text(
                playError!!,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            )
        } else if (buffering) {
            Text(
                "缓冲中…",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            modifier = Modifier.padding(end = 12.dp).weight(1f)
                        )
                    }
                    if (extras.size > 1) {
                        Row(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
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
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Slider(
                        value = sliderPos,
                        onValueChange = { v ->
                            seeking = true
                            controlsVisible = true
                            seekValue = v
                        },
                        onValueChangeFinished = {
                            player.seekTo(seekValue.toLong().coerceAtLeast(0))
                            positionMs = seekValue.toLong()
                            seeking = false
                        },
                        valueRange = 0f..durationForSlider,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF22D3EE),
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(if (seeking) seekValue.toLong() else positionMs), color = Color.White)
                        IconButton(onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                            controlsVisible = true
                        }) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "暂停" else "播放",
                                tint = Color.White
                            )
                        }
                        Text(formatTime(durationMs), color = Color.White)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = (ms / 1000).toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
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
