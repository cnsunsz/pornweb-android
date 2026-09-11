package com.pornweb.android.ui.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pornweb.android.BuildConfig
import com.pornweb.android.PornWebApp
import com.pornweb.android.R
import com.pornweb.android.data.ExtraFile
import com.pornweb.android.data.ProgressRequest
import com.pornweb.android.data.SubtitleTrack
import com.pornweb.android.ui.theme.PwAccent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

private enum class OrientMode { Sensor, Landscape, Portrait }

private data class AudioTrackOpt(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean
)

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

    // True immersive fullscreen while on the player; restore bars when leaving.
    DisposableEffect(Unit) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
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


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
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
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }

    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    var subtitleTracksLoading by remember { mutableStateOf(false) }
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var cachedSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    var overlayCues by remember { mutableStateOf<List<WebVttCue>>(emptyList()) }
    var activeSubtitleText by remember { mutableStateOf<String?>(null) }
    var subtitleLoading by remember { mutableStateOf(false) }
    var subtitleHint by remember { mutableStateOf<String?>(null) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var subtitlePrefetchJob by remember { mutableStateOf<Job?>(null) }

    val initialSpeed = remember { prefs.effectiveSpeed() }
    var playbackSpeed by remember { mutableFloatStateOf(initialSpeed) }
    val longPressSpeed = prefs.longPressSpeed
    val skipMs = prefs.skipSeconds * 1000L
    val swipeSeekSeconds = prefs.swipeSeekSeconds
    val doubleTapEnabled = prefs.doubleTapSeek
    val leftRewind = prefs.leftLongPressRewind
    val autoHideMs = prefs.autoHideControlsMs.toLong()
    val showRemaining = prefs.showRemainingTime
    val continuousNext = prefs.continuousPlayNextPart

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
            .setUserAgent("PornWeb-Android/${BuildConfig.VERSION_NAME}")
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
            .setBufferDurationsMs(20_000, 120_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .build()
            .apply {
                playWhenReady = true
                setPlaybackSpeed(initialSpeed)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioMimeTypes(
                        MimeTypes.AUDIO_AAC,
                        MimeTypes.AUDIO_AC3,
                        MimeTypes.AUDIO_E_AC3,
                        MimeTypes.AUDIO_OPUS,
                        MimeTypes.AUDIO_FLAC,
                        MimeTypes.AUDIO_MPEG
                    )
                    .build()
            }
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var resizeZoom by remember { mutableStateOf(false) }
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }

    fun subtitleCacheFile(trackId: String): File {
        val dir = File(context.cacheDir, "subs")
        if (!dir.exists()) dir.mkdirs()
        val safe = trackId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "${id}_${part}_${safe}.vtt")
    }

    fun buildMediaItem(): MediaItem {
        // Video-only: subtitles are rendered via Compose overlay from parsed VTT cues.
        // Only reload when stream url/part changes — never just to attach/clear subs.
        return MediaItem.Builder().setUri(url).build()
    }

    fun reloadMediaKeepingPosition() {
        val keepPos = player.currentPosition.takeIf { it > 0 && player.mediaItemCount > 0 } ?: 0L
        val start = if (keepPos > 0) keepPos else startMsState.value
        player.setMediaItem(buildMediaItem(), /* resetPosition= */ false)
        player.prepare()
        if (start > 0) player.seekTo(start)
        player.setPlaybackSpeed(playbackSpeed)
        // Keep text tracks disabled; overlay path does not use ExoPlayer SubtitleConfiguration.
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
        player.playWhenReady = true
        player.play()
    }

    fun clearOverlaySubtitles() {
        selectedTrackId = null
        cachedSubtitleUri = null
        overlayCues = emptyList()
        activeSubtitleText = null
    }

    fun loadOverlayCuesFromUri(local: Uri): List<WebVttCue> {
        val file = when {
            local.scheme == "file" && !local.path.isNullOrBlank() -> File(local.path!!)
            else -> null
        }
        val content = file?.takeIf { it.isFile && it.length() > 0L }?.readText(Charsets.UTF_8)
            ?: return emptyList()
        return WebVttParser.parse(content)
    }

    suspend fun prefetchSubtitleVtt(trackId: String): Uri? = withContext(Dispatchers.IO) {
        val dest = subtitleCacheFile(trackId)
        if (dest.isFile && dest.length() > 0L) {
            return@withContext Uri.fromFile(dest)
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun writeBodyToDest(bodyBytes: okhttp3.ResponseBody): Uri? {
            val tmp = File(dest.absolutePath + ".tmp")
            try {
                tmp.outputStream().use { outStream -> bodyBytes.byteStream().copyTo(outStream) }
                if (!tmp.isFile || tmp.length() <= 0L) {
                    tmp.delete()
                    return null
                }
                if (dest.exists()) dest.delete()
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                if (!dest.isFile || dest.length() <= 0L) return null
                return Uri.fromFile(dest)
            } catch (e: Exception) {
                tmp.delete()
                throw e
            }
        }

        fun fetchOnce(async: Boolean): Pair<Int, Uri?> {
            val req = Request.Builder()
                .url(c.subtitleUrl(id, trackId, part, async = async))
                .header("Accept", "text/vtt,application/json,*/*")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val code = resp.code
                if (code == 202) return code to null
                if (!resp.isSuccessful) return code to null
                val body = resp.body ?: return code to null
                return code to writeBodyToDest(body)
            }
        }

        val (code, uri) = fetchOnce(async = true)
        if (uri != null) return@withContext uri

        if (code == 202) {
            val deadline = System.currentTimeMillis() + 120_000L
            while (isActive && System.currentTimeMillis() < deadline) {
                delay(1_250L)
                if (!isActive) return@withContext null
                val status = try {
                    c.api.subtitleStatus(id, trackId, part).status?.trim()?.lowercase()
                } catch (_: Exception) {
                    null
                }
                when (status) {
                    "ready" -> {
                        val (_, readyUri) = fetchOnce(async = true)
                        if (readyUri != null) return@withContext readyUri
                        val (_, syncUri) = fetchOnce(async = false)
                        return@withContext syncUri
                    }
                    "error", "unavailable" -> return@withContext null
                    else -> Unit
                }
            }
            return@withContext null
        }

        val (_, syncUri) = fetchOnce(async = false)
        syncUri
    }

    fun applySubtitleSelection(trackId: String?) {
        subtitlePrefetchJob?.cancel()
        subtitlePrefetchJob = null
        if (trackId == null) {
            subtitleLoading = false
            subtitleHint = null
            clearOverlaySubtitles()
            return
        }
        if (trackId == selectedTrackId && overlayCues.isNotEmpty()) {
            return
        }
        subtitleLoading = true
        subtitleHint = null
        val job = scope.launch {
            try {
                val local = prefetchSubtitleVtt(trackId)
                if (!isActive) return@launch
                if (local == null) {
                    Toast.makeText(context, "字幕加载失败", Toast.LENGTH_SHORT).show()
                    subtitleHint = "字幕加载失败"
                    delay(1800)
                    if (subtitleHint == "字幕加载失败") subtitleHint = null
                    return@launch
                }
                val cues = withContext(Dispatchers.IO) { loadOverlayCuesFromUri(local) }
                if (!isActive) return@launch
                if (cues.isEmpty()) {
                    Toast.makeText(context, "字幕解析失败", Toast.LENGTH_SHORT).show()
                    subtitleHint = "字幕解析失败"
                    delay(1800)
                    if (subtitleHint == "字幕解析失败") subtitleHint = null
                    return@launch
                }
                selectedTrackId = trackId
                cachedSubtitleUri = local
                overlayCues = cues
                // Drive active line immediately from current position (no ExoPlayer reprepare).
                activeSubtitleText = WebVttParser.activeText(cues, player.currentPosition)
                subtitleHint = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, "字幕加载失败", Toast.LENGTH_SHORT).show()
                subtitleHint = "字幕加载失败"
                delay(1800)
                if (subtitleHint == "字幕加载失败") subtitleHint = null
            } finally {
                if (subtitlePrefetchJob === coroutineContext[Job]) {
                    subtitleLoading = false
                }
            }
        }
        subtitlePrefetchJob = job
    }

    fun applyUserSpeed(speed: Float) {
        playbackSpeed = speed
        player.setPlaybackSpeed(speed)
        if (prefs.rememberSpeed) {
            prefs.lastSpeed = speed
        }
    }

    fun listAudioTracks(): List<AudioTrackOpt> {
        val outList = mutableListOf<AudioTrackOpt>()
        val groups = player.currentTracks.groups
        for (gi in 0 until groups.size) {
            val group = groups[gi]
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (j in 0 until group.length) {
                if (!group.isTrackSupported(j)) continue
                val format = group.getTrackFormat(j)
                val label = format.label
                    ?: format.language?.uppercase()
                    ?: "音轨 ${outList.size + 1}"
                val detail = buildString {
                    append(label)
                    format.codecs?.let { append(" · ").append(it) }
                    if (format.channelCount > 0) append(" · ").append(format.channelCount).append("ch")
                }
                outList += AudioTrackOpt(gi, j, detail, group.isTrackSelected(j))
            }
        }
        return outList
    }

    fun selectAudioTrack(opt: AudioTrackOpt) {
        val groups = player.currentTracks.groups
        if (opt.groupIndex !in 0 until groups.size) return
        val group = groups[opt.groupIndex]
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(opt.trackIndex)))
            .build()
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
    var externalHint by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(false) }
    var swipeHint by remember { mutableStateOf<String?>(null) }
    var speedHint by remember { mutableStateOf<String?>(null) }
    var brightnessHint by remember { mutableStateOf<String?>(null) }
    var volumeHint by remember { mutableStateOf<String?>(null) }
    var dragAccumPx by remember { mutableFloatStateOf(0f) }
    var dragBasePos by remember { mutableLongStateOf(0L) }
    var boostActive by remember { mutableStateOf(false) }
    var rewindJob by remember { mutableStateOf<Job?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showPartsSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var audioTracks by remember { mutableStateOf<List<AudioTrackOpt>>(emptyList()) }
    var subtitleOffsetMs by remember { mutableIntStateOf(0) }

    var brightness01 by remember {
        mutableFloatStateOf(
            activity.window.attributes.screenBrightness
                .takeIf { it in 0f..1f } ?: 0.5f
        )
    }

    fun setWindowBrightness(v: Float) {
        val clamped = v.coerceIn(0.01f, 1f)
        brightness01 = clamped
        val lp = activity.window.attributes
        lp.screenBrightness = clamped
        activity.window.attributes = lp
    }

    fun adjustVolumePercent(deltaFraction: Float): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (cur + deltaFraction * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        return ((next.toFloat() / max) * 100f).toInt()
    }

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
        player.setPlaybackSpeed(playbackSpeed)
    }

    fun seekBy(deltaMs: Long) {
        val cur = player.currentPosition
        val dur = player.duration
        val target = if (deltaMs >= 0) {
            (cur + deltaMs).coerceAtMost(if (dur > 0) dur else cur + deltaMs)
        } else {
            (cur + deltaMs).coerceAtLeast(0L)
        }
        player.seekTo(target)
        positionMs = target
    }


    DisposableEffect(player) {
        onDispose {
            subtitlePrefetchJob?.cancel()
            saveProgress(c, id, part, player)
            player.release()
        }
    }

    LaunchedEffect(id, part) {
        subtitleTracksLoading = true
        try {
            val resp = c.api.subtitles(id, part)
            subtitleTracks = resp.tracks.orEmpty().filter { it.trackId().isNotBlank() }
        } catch (_: Exception) {
            subtitleTracks = emptyList()
        } finally {
            subtitleTracksLoading = false
        }
    }

    LaunchedEffect(url) {
        playError = null
        subtitlePrefetchJob?.cancel()
        subtitlePrefetchJob = null
        clearOverlaySubtitles()
        subtitleLoading = false
        subtitleHint = null
        videoScale = 1f
        videoOffset = Offset.Zero
        reloadMediaKeepingPosition()
    }

    DisposableEffect(player, id, part, subtitleTracks) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    saveProgress(c, id, part, player)
                }
                if (playbackState == Player.STATE_ENDED && continuousNext && part + 1 < extras.size) {
                    onPart(part + 1)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) saveProgress(c, id, part, player)
            }

            override fun onPlayerError(error: PlaybackException) {
                val raw = listOfNotNull(error.message, error.cause?.message).joinToString(" ")
                val lower = raw.lowercase()
                playError = if (
                    "audio" in lower ||
                    "decoder" in lower ||
                    "soundtrack" in lower ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
                ) {
                    "音轨解码失败，可试外部播放器"
                } else {
                    error.message ?: "播放失败 (${error.errorCode})"
                }
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

    LaunchedEffect(player, overlayCues, subtitleOffsetMs) {
        while (isActive) {
            val dur = player.duration
            durationMs = if (dur > 0) dur else 0L
            val pos = player.currentPosition.coerceAtLeast(0)
            if (!seeking) positionMs = pos
            playing = player.isPlaying
            activeSubtitleText = if (overlayCues.isEmpty()) {
                null
            } else {
                WebVttParser.activeText(overlayCues, pos + subtitleOffsetMs)
            }
            delay(200)
        }
    }

    LaunchedEffect(controlsVisible, playing, locked, boostActive, autoHideMs) {
        if (controlsVisible && playing && playError == null && !locked && !boostActive) {
            delay(autoHideMs)
            controlsVisible = false
        }
    }

    LaunchedEffect(swipeHint) {
        if (swipeHint != null) { delay(900); swipeHint = null }
    }
    LaunchedEffect(brightnessHint) {
        if (brightnessHint != null) { delay(800); brightnessHint = null }
    }
    LaunchedEffect(volumeHint) {
        if (volumeHint != null) { delay(800); volumeHint = null }
    }
    LaunchedEffect(externalHint) {
        if (externalHint != null) { delay(2500); externalHint = null }
    }
    LaunchedEffect(locked, orientMode) {
        applyOrient(orientMode, locked)
    }
    LaunchedEffect(resizeZoom) {
        playerViewRef?.resizeMode = if (resizeZoom) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    val durationForSlider = durationMs.coerceAtLeast(1L).toFloat()
    val sliderPos = if (seeking) seekValue else positionMs.toFloat().coerceIn(0f, durationForSlider)
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    val orientLandscape = orientMode == OrientMode.Landscape ||
        (orientMode == OrientMode.Sensor && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

    val rightTimeLabel = if (showRemaining && durationMs > 0) {
        val rem = (durationMs - (if (seeking) seekValue.toLong() else positionMs)).coerceAtLeast(0L)
        "-${formatTime(rem)}"
    } else {
        formatTime(durationMs)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.player_view, null, false) as PlayerView
                view.player = player
                view.useController = false
                view.resizeMode = if (resizeZoom) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                view.keepScreenOn = true
                view.subtitleView?.apply {
                    // Overlay path: disable ExoPlayer SubtitleView to avoid double-draw if any text track appears.
                    setCues(emptyList())
                    visibility = android.view.View.GONE
                }
                playerViewRef = view
                view
            },
            update = {
                it.player = player
                playerViewRef = it
                it.resizeMode = if (resizeZoom) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = videoScale
                    scaleY = videoScale
                    translationX = videoOffset.x
                    translationY = videoOffset.y
                }
        )


        // Gestures overlay
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked, doubleTapEnabled, skipMs, longPressSpeed, leftRewind, playbackSpeed, durationMs) {
                    if (locked) {
                        detectTapGestures { controlsVisible = true }
                        return@pointerInput
                    }
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (!doubleTapEnabled) return@detectTapGestures
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val zone = offset.x / w
                            when {
                                zone < 1f / 3f -> {
                                    seekBy(-skipMs)
                                    swipeHint = "-${prefs.skipSeconds}s"
                                }
                                zone > 2f / 3f -> {
                                    seekBy(skipMs)
                                    swipeHint = "+${prefs.skipSeconds}s"
                                }
                                else -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                    swipeHint = if (player.isPlaying) "播放" else "暂停"
                                }
                            }
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
                                            seekBy(-2_000)
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
                // Unified 1-finger drag: horizontal = seek; left vertical = brightness; right vertical = volume.
                // Angle/threshold keeps vertical and horizontal from fighting.
                .pointerInput(locked, durationMs, swipeSeekSeconds, screenWidthPx, screenHeightPx) {
                    if (locked) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var mode = 0 // 0 undecided, 1 seek, 2 brightness, 3 volume
                        var baseBright = brightness01
                        var decided = false
                        val startX = down.position.x
                        val third = size.width / 3f
                        val slop = 28f
                        dragBasePos = player.currentPosition.coerceAtLeast(0)
                        dragAccumPx = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size > 1) {
                                // Hand off to pinch; do not consume so zoom can run.
                                break
                            }
                            val change = pressed.firstOrNull() ?: break
                            val dx = change.positionChange().x
                            val dy = change.positionChange().y
                            totalX += dx
                            totalY += dy
                            if (!decided && (abs(totalX) > slop || abs(totalY) > slop)) {
                                val ax = abs(totalX)
                                val ay = abs(totalY)
                                when {
                                    // Clear horizontal → seek (works across full width)
                                    ax >= ay * 1.2f -> {
                                        decided = true
                                        mode = 1
                                        seeking = true
                                        controlsVisible = true
                                    }
                                    // Clear vertical → brightness (left) / volume (right)
                                    ay >= ax * 1.2f -> {
                                        decided = true
                                        mode = when {
                                            startX < third -> 2
                                            startX > size.width - third -> 3
                                            else -> 0 // middle vertical: ignore
                                        }
                                        if (mode == 2) baseBright = brightness01
                                    }
                                    // Ambiguous angle: wait for a clearer direction
                                    else -> Unit
                                }
                            }
                            when (mode) {
                                1 -> {
                                    dragAccumPx += dx
                                    val maxSeekMs = swipeSeekSeconds * 1000.0
                                    val deltaMs = (dragAccumPx / screenWidthPx) * maxSeekMs
                                    val target = (dragBasePos + deltaMs.roundToLong())
                                        .coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE / 4)
                                    seekValue = target.toFloat()
                                    val signed = target - dragBasePos
                                    val sign = if (signed >= 0) "+" else "-"
                                    swipeHint = "$sign${formatTime(abs(signed))} → ${formatTime(target)}"
                                    controlsVisible = true
                                    change.consume()
                                }
                                2 -> {
                                    val delta = -totalY / screenHeightPx
                                    setWindowBrightness(baseBright + delta)
                                    brightnessHint = "亮度 ${(brightness01 * 100).toInt()}%"
                                    change.consume()
                                }
                                3 -> {
                                    val delta = -dy / screenHeightPx
                                    val pct = adjustVolumePercent(delta)
                                    volumeHint = "音量 $pct%"
                                    change.consume()
                                }
                            }
                        }
                        if (mode == 1) {
                            val target = seekValue.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                            player.seekTo(target)
                            positionMs = target
                            seeking = false
                            dragAccumPx = 0f
                        }
                    }
                }
                // Pinch-zoom only (2+ fingers). Must not consume single-finger pans or seek dies.
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var lastSpan = -1f
                        var lastCentroid = Offset.Zero
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size < 2) {
                                lastSpan = -1f
                                continue
                            }
                            val c = Offset(
                                pressed.map { it.position.x }.average().toFloat(),
                                pressed.map { it.position.y }.average().toFloat()
                            )
                            val span = kotlin.math.hypot(
                                (pressed[0].position.x - pressed[1].position.x).toDouble(),
                                (pressed[0].position.y - pressed[1].position.y).toDouble()
                            ).toFloat().coerceAtLeast(1f)
                            if (lastSpan > 0f) {
                                val zoom = span / lastSpan
                                if (zoom != 1f) {
                                    val next = (videoScale * zoom).coerceIn(1f, 4f)
                                    videoScale = next
                                    if (next <= 1.01f) {
                                        videoScale = 1f
                                        videoOffset = Offset.Zero
                                    }
                                }
                                if (videoScale > 1.01f) {
                                    videoOffset += (c - lastCentroid)
                                }
                            }
                            lastSpan = span
                            lastCentroid = c
                            pressed.forEach { it.consume() }
                        }
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
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        } else if (buffering && !subtitleLoading && swipeHint == null && speedHint == null &&
            brightnessHint == null && volumeHint == null
        ) {
            Text("缓冲中…", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        // Tiny corner chip only — never a large center/bottom card blocking faces.
        val subtitleChip = when {
            subtitleLoading && activeSubtitleText == null -> "准备中…"
            !subtitleHint.isNullOrBlank() && activeSubtitleText == null -> subtitleHint
            else -> null
        }
        if (subtitleChip != null) {
            Text(
                subtitleChip,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(end = 10.dp, bottom = 118.dp)
            )
        }

        // Compose subtitle overlay (WebVTT cues): white + thin black outline, no box.
        val cueText = activeSubtitleText
        if (!cueText.isNullOrBlank()) {
            val cueStyle = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 1.2f),
                    blurRadius = 2.5f
                )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 28.dp, end = 28.dp, bottom = 112.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    cueText,
                    style = cueStyle.copy(
                        color = Color.Black,
                        drawStyle = Stroke(width = 4.5f, miter = 2f, join = StrokeJoin.Round)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    cueText,
                    style = cueStyle.copy(color = Color.White),
                    textAlign = TextAlign.Center
                )
            }
        }

        val centerHint = brightnessHint ?: volumeHint ?: swipeHint
        if (centerHint != null) {
            Text(
                centerHint,
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
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        )
                    )
                    .padding(top = 36.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
        if (externalHint != null) {
            Text(
                externalHint!!,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }


        // Locked: single unlock affordance (no vertical rail).
        if (locked) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(start = 12.dp, bottom = 20.dp)
            ) {
                IconButton(
                    onClick = {
                        locked = false
                        controlsVisible = true
                        applyOrient(orientMode, false)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "解锁", tint = PwAccent)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top: back + title + ⋯ only
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            )
                        )
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                        IconButton(onClick = {
                            audioTracks = listAudioTracks()
                            showMoreSheet = true
                            controlsVisible = true
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White)
                        }
                    }
                }

                // Subtitle dropdown anchored near bottom chips
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp)
                ) {
                    DropdownMenu(
                        expanded = showSubtitleMenu,
                        onDismissRequest = { showSubtitleMenu = false }
                    ) {
                        when {
                            subtitleTracksLoading -> {
                                DropdownMenuItem(
                                    text = { Text("加载字幕…") },
                                    onClick = { },
                                    enabled = false
                                )
                            }
                            subtitleTracks.isEmpty() -> {
                                DropdownMenuItem(
                                    text = { Text("无字幕") },
                                    onClick = { showSubtitleMenu = false },
                                    enabled = false
                                )
                            }
                            else -> {
                                if (subtitleLoading) {
                                    DropdownMenuItem(
                                        text = { Text("准备中…") },
                                        onClick = { },
                                        enabled = false
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "无字幕",
                                            color = if (selectedTrackId == null) PwAccent else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        applySubtitleSelection(null)
                                        showSubtitleMenu = false
                                        controlsVisible = true
                                    }
                                )
                                subtitleTracks.forEach { track ->
                                    val tid = track.trackId()
                                    val supported = track.isSupported()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                track.displayLabel(),
                                                color = when {
                                                    !supported -> Color.Gray
                                                    selectedTrackId == tid -> PwAccent
                                                    else -> Color.Unspecified
                                                }
                                            )
                                        },
                                        enabled = supported,
                                        onClick = {
                                            if (!supported) {
                                                Toast.makeText(context, "该轨暂不支持", Toast.LENGTH_SHORT).show()
                                                return@DropdownMenuItem
                                            }
                                            applySubtitleSelection(tid)
                                            showSubtitleMenu = false
                                            controlsVisible = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom: scrubber + centered transport + secondary chips (MX/KM style)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            )
                        )
                        .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    val barFraction = (sliderPos / durationForSlider).coerceIn(0f, 1f)
                    var barWidthPx by remember { mutableFloatStateOf(1f) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTime(if (seeking) seekValue.toLong() else positionMs),
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .padding(horizontal = 6.dp)
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
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color.White.copy(alpha = 0.28f))
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(barFraction)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(PwAccent)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(barFraction)
                                    .height(28.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(PwAccent)
                                )
                            }
                        }
                        Text(
                            rightTimeLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(52.dp)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                locked = true
                                controlsVisible = true
                                applyOrient(orientMode, true)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "锁定", tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                seekBy(-skipMs)
                                swipeHint = "-${prefs.skipSeconds}s"
                                controlsVisible = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "快退",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause() else player.play()
                                controlsVisible = true
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "暂停" else "播放",
                                tint = PwAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                seekBy(skipMs)
                                swipeHint = "+${prefs.skipSeconds}s"
                                controlsVisible = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "快进",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                resizeZoom = !resizeZoom
                                controlsVisible = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (resizeZoom) Icons.Default.FitScreen else Icons.Default.CropFree,
                                contentDescription = if (resizeZoom) "适应" else "铺满",
                                tint = if (resizeZoom) PwAccent else Color.White
                            )
                        }
                    }

                    // Secondary row: 字幕 / 倍速 / 截图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showSubtitleMenu = true
                                controlsVisible = true
                            }
                        ) {
                            Icon(
                                Icons.Default.ClosedCaption,
                                contentDescription = null,
                                tint = if (selectedTrackId != null) PwAccent else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "字幕",
                                color = if (selectedTrackId != null) PwAccent else Color.White
                            )
                        }
                        Box {
                            TextButton(
                                onClick = {
                                    showSpeedMenu = true
                                    controlsVisible = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (playbackSpeed != 1.0f) PwAccent else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    String.format("%.2fx", playbackSpeed).trimEnd('0').trimEnd('.'),
                                    color = if (playbackSpeed != 1.0f) PwAccent else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                speedOptions.forEach { sp ->
                                    val label = when (sp) {
                                        0.5f -> "0.5x"
                                        0.75f -> "0.75x"
                                        1.0f -> "1.0x"
                                        1.25f -> "1.25x"
                                        1.5f -> "1.5x"
                                        1.75f -> "1.75x"
                                        2.0f -> "2.0x"
                                        2.5f -> "2.5x"
                                        3.0f -> "3.0x"
                                        else -> "${sp}x"
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (sp == playbackSpeed) PwAccent else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            applyUserSpeed(sp)
                                            showSpeedMenu = false
                                            controlsVisible = true
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                PlayerScreenshot.captureAndSave(context, playerViewRef)
                                controlsVisible = true
                            }
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("截图", color = Color.White)
                        }
                    }
                }
            }
        }

        if (showPartsSheet && extras.size > 1) {
            ModalBottomSheet(
                onDismissRequest = { showPartsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Text(
                    "选集",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 24.dp)
                ) {
                    extras.forEachIndexed { i, extra ->
                        FilterChip(
                            selected = part == i,
                            onClick = {
                                onPart(i)
                                showPartsSheet = false
                            },
                            label = { Text(extra.label ?: "第 ${i + 1} 集") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
                ) {
                    Text("更多", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            try {
                                player.pause()
                                c.openExternalPlayer(context, id, part, title)
                            } catch (_: ActivityNotFoundException) {
                                externalHint = "没有可用的外部播放器"
                            }
                            showMoreSheet = false
                            controlsVisible = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("外部播放器", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showMoreSheet = false
                            onOpenSettings()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("播放设置", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            locked = true
                            showMoreSheet = false
                            controlsVisible = true
                            applyOrient(orientMode, true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("锁定控件", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            orientMode = when (orientMode) {
                                OrientMode.Sensor -> OrientMode.Landscape
                                OrientMode.Landscape -> OrientMode.Portrait
                                OrientMode.Portrait -> OrientMode.Sensor
                            }
                            applyOrient(orientMode, false)
                            controlsVisible = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = if (orientLandscape) PwAccent else Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        val orientLabel = when (orientMode) {
                            OrientMode.Sensor -> "旋转：自动"
                            OrientMode.Landscape -> "旋转：横屏"
                            OrientMode.Portrait -> "旋转：竖屏"
                        }
                        Text(orientLabel, modifier = Modifier.weight(1f))
                    }
                    if (extras.size > 1) {
                        TextButton(
                            onClick = {
                                showMoreSheet = false
                                showPartsSheet = true
                                controlsVisible = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("选集", modifier = Modifier.weight(1f))
                        }
                    }
                    TextButton(
                        onClick = {
                            resizeZoom = !resizeZoom
                            controlsVisible = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (resizeZoom) Icons.Default.FitScreen else Icons.Default.CropFree,
                            contentDescription = null,
                            tint = if (resizeZoom) PwAccent else Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (resizeZoom) "画面：铺满" else "画面：适应",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Gif, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("GIF（暂未开放）", color = Color.Gray, modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("音轨", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (audioTracks.isEmpty()) {
                        Text("暂无多音轨", color = Color.Gray)
                    } else {
                        audioTracks.forEach { opt ->
                            TextButton(
                                onClick = {
                                    selectAudioTrack(opt)
                                    audioTracks = listAudioTracks()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    opt.label,
                                    color = if (opt.selected) PwAccent else Color.Unspecified,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("字幕", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            showMoreSheet = false
                            showSubtitleMenu = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedTrackId != null) "已选字幕 · 点击切换" else "选择字幕",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "字幕时间轴偏移: ${subtitleOffsetMs} ms",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { subtitleOffsetMs -= 100 }) { Text("-100ms") }
                        TextButton(onClick = { subtitleOffsetMs = 0 }) { Text("复位") }
                        TextButton(onClick = { subtitleOffsetMs += 100 }) { Text("+100ms") }
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
