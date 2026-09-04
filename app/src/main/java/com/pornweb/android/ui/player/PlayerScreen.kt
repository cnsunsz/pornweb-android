package com.pornweb.android.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

private enum class OrientMode { Sensor, Landscape, Portrait }

@Composable
fun PlayerScreen(
    id: Long,
    part: Int,
    resume: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as PornWebApp
    val c = app.container
    val prefs = c.playerPrefs
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
            val allowResume = resume && prefs.resumeOnOpen
            startPositionMs = if (allowResume && p > 5 && !nearEnd) (p * 1000).toLong() else 0L
            if (d.progressPart != null && allowResume) currentPart = d.progressPart
        } catch (e: Exception) {
            loadError = c.parseError(e)
        } finally {
            ready = true
        }
    }

    DisposableEffect(Unit) {
        val prev = activity.requestedOrientation
        activity.requestedOrientation = if (prefs.startLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
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
        onBack = onBack,
        onOpenSettings = onOpenSettings
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerBody(
    id: Long,
    part: Int,
    extras: List<ExtraFile>,
    title: String,
    startPositionMs: Long,
    onPart: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as PornWebApp
    val c = app.container
    val prefs = c.playerPrefs
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val url = remember(id, part, c.tokenStore.token, c.serverStore.baseUrl) { c.streamUrl(id, part) }
    val token = c.tokenStore.token.orEmpty()
    val startMsState = rememberUpdatedState(startPositionMs)

    val defaultSpeed = prefs.defaultSpeed
    val longPressSpeed = prefs.longPressSpeed
    val skipMs = prefs.skipSeconds * 1000L
    val swipeSeekSeconds = prefs.swipeSeekSeconds
    val doubleTapEnabled = prefs.doubleTapSeek
    val leftRewind = prefs.leftLongPressRewind

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
            .setUserAgent("PornWeb-Android/1.0.6")
            .setDefaultRequestProperties(
                buildMap {
                    if (token.isNotBlank()) put("Authorization", "Bearer $token")
                    put("Accept", "*/*")
                }
            )
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
            .setEnableAudioFloatOutput(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(50_000, 120_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .build()
            .apply {
                playWhenReady = true
                setPlaybackSpeed(defaultSpeed)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var orientMode by remember {
        mutableStateOf(if (prefs.startLandscape) OrientMode.Landscape else OrientMode.Sensor)
    }
    var playing by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var playError by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(false) }
    var swipeHint by remember { mutableStateOf<String?>(null) }
    var speedHint by remember { mutableStateOf<String?>(null) }
    var dragAccumPx by remember { mutableFloatStateOf(0f) }
    var dragBasePos by remember { mutableLongStateOf(0L) }
    var boostActive by remember { mutableStateOf(false) }
    var rewindJob by remember { mutableStateOf<Job?>(null) }

    fun applyOrient(mode: OrientMode, lockControls: Boolean) {
        if (lockControls) {
            val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            activity.requestedOrientation = if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            return
        }
        activity.requestedOrientation = when (mode) {
            OrientMode.Sensor -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            OrientMode.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientMode.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    fun restoreSpeed() {
        boostActive = false
        speedHint = null
        rewindJob?.cancel()
        rewindJob = null
        player.setPlaybackSpeed(defaultSpeed)
    }

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
        player.setPlaybackSpeed(defaultSpeed)
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
            if (!seeking) positionMs = player.currentPosition.coerceAtLeast(0)
            playing = player.isPlaying
            delay(200)
        }
    }

    LaunchedEffect(controlsVisible, playing, locked, boostActive) {
        if (controlsVisible && playing && playError == null && !locked && !boostActive) {
            delay(4_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(swipeHint) {
        if (swipeHint != null) {
            delay(900)
            swipeHint = null
        }
    }

    LaunchedEffect(locked, orientMode) {
        applyOrient(orientMode, locked)
    }

    val durationForSlider = durationMs.coerceAtLeast(1L).toFloat()
    val sliderPos = if (seeking) seekValue else positionMs.toFloat().coerceIn(0f, durationForSlider)
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)

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
                .pointerInput(locked, doubleTapEnabled, skipMs, longPressSpeed, leftRewind, defaultSpeed, durationMs) {
                    if (locked) {
                        detectTapGestures { controlsVisible = true }
                        return@pointerInput
                    }
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (!doubleTapEnabled) return@detectTapGestures
                            val right = offset.x >= size.width / 2f
                            val cur = player.currentPosition
                            val target = if (right) {
                                (cur + skipMs).coerceAtMost(if (player.duration > 0) player.duration else cur + skipMs)
                            } else {
                                (cur - skipMs).coerceAtLeast(0L)
                            }
                            player.seekTo(target)
                            positionMs = target
                            swipeHint = if (right) "+${prefs.skipSeconds}s" else "-${prefs.skipSeconds}s"
                            controlsVisible = true
                        },
                        onTap = { controlsVisible = !controlsVisible },
                        onPress = { offset ->
                            val right = offset.x >= size.width / 2f
                            val job = scope.launch {
                                delay(380)
                                if (right || !leftRewind) {
                                    boostActive = true
                                    player.play()
                                    player.setPlaybackSpeed(longPressSpeed)
                                    speedHint = String.format("%.1fx", longPressSpeed)
                                    controlsVisible = true
                                } else {
                                    boostActive = true
                                    speedHint = "倒退"
                                    controlsVisible = true
                                    rewindJob = scope.launch {
                                        while (isActive) {
                                            val cur = player.currentPosition
                                            val target = (cur - 2_000).coerceAtLeast(0L)
                                            player.seekTo(target)
                                            positionMs = target
                                            delay(200)
                                        }
                                    }
                                }
                            }
                            tryAwaitRelease()
                            job.cancel()
                            restoreSpeed()
                        }
                    )
                }
                .pointerInput(locked, durationMs, swipeSeekSeconds) {
                    if (locked) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragAccumPx = 0f
                            dragBasePos = player.currentPosition.coerceAtLeast(0)
                            seeking = true
                            controlsVisible = true
                        },
                        onDragEnd = {
                            val target = seekValue.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                            player.seekTo(target)
                            positionMs = target
                            seeking = false
                            dragAccumPx = 0f
                        },
                        onDragCancel = {
                            seeking = false
                            dragAccumPx = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumPx += dragAmount
                            val maxSeekMs = swipeSeekSeconds * 1000.0
                            val deltaMs = (dragAccumPx / screenWidthPx) * maxSeekMs
                            val target = (dragBasePos + deltaMs.roundToLong())
                                .coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE / 4)
                            seekValue = target.toFloat()
                            val signed = target - dragBasePos
                            val sign = if (signed >= 0) "+" else "-"
                            swipeHint = "$sign${formatTime(abs(signed))} → ${formatTime(target)}"
                        }
                    )
                }
        )

        if (playError != null) {
            Text(
                playError!!,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        } else if (buffering && swipeHint == null && speedHint == null) {
            Text("缓冲中…", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        if (swipeHint != null) {
            Text(
                swipeHint!!,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        if (speedHint != null) {
            Text(
                speedHint!!,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        if (locked) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = {
                        locked = false
                        controlsVisible = true
                        applyOrient(orientMode, false)
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "解锁", tint = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent))
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
                            modifier = Modifier.padding(end = 4.dp).weight(1f)
                        )
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "播放设置", tint = Color.White)
                        }
                        IconButton(onClick = {
                            orientMode = when (orientMode) {
                                OrientMode.Sensor -> OrientMode.Landscape
                                OrientMode.Landscape -> OrientMode.Portrait
                                OrientMode.Portrait -> OrientMode.Sensor
                            }
                            applyOrient(orientMode, false)
                            controlsVisible = true
                        }) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "旋转", tint = Color.White)
                        }
                        IconButton(onClick = {
                            locked = true
                            controlsVisible = true
                            applyOrient(orientMode, true)
                        }) {
                            Icon(Icons.Default.Lock, contentDescription = "锁定", tint = Color.White)
                        }
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
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val barFraction = (sliderPos / durationForSlider).coerceIn(0f, 1f)
                    var barWidthPx by remember { mutableFloatStateOf(1f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                            .pointerInput(durationMs) {
                                detectTapGestures { offset ->
                                    val x = offset.x.coerceIn(0f, barWidthPx)
                                    val target = ((x / barWidthPx) * durationForSlider).toLong()
                                    seekValue = target.toFloat()
                                    player.seekTo(target)
                                    positionMs = target
                                    controlsVisible = true
                                }
                            }
                            .pointerInput(durationMs) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        seeking = true
                                        controlsVisible = true
                                    },
                                    onDragEnd = {
                                        player.seekTo(seekValue.toLong().coerceAtLeast(0))
                                        positionMs = seekValue.toLong()
                                        seeking = false
                                    },
                                    onDragCancel = { seeking = false },
                                    onHorizontalDrag = { change, _ ->
                                        val x = change.position.x.coerceIn(0f, barWidthPx)
                                        seekValue = (x / barWidthPx) * durationForSlider
                                        seeking = true
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(barFraction)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF22D3EE))
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(barFraction)
                                .height(14.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22D3EE))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTime(if (seeking) seekValue.toLong() else positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause() else player.play()
                                controlsVisible = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatTime(durationMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
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
