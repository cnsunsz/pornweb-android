package com.pornweb.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.MediaItem
import com.pornweb.android.ui.theme.PwMuted
import com.pornweb.android.ui.theme.PwPlaceholder

@Composable
fun DetailScreen(id: Long, onBack: () -> Unit, onPlay: (id: Long, part: Int, resume: Boolean) -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    var item by remember { mutableStateOf<MediaItem?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var part by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        loading = true
        error = null
        try {
            val d = c.api.detail(id)
            item = d
            part = d.progressPart ?: 0
        } catch (e: Exception) {
            error = c.parseError(e)
        } finally {
            loading = false
        }
    }

    val media = item
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            media != null -> {
                val fanart = c.resolveImage(media, "fanart")
                val poster = c.resolveImage(media, "poster")
                val progress = media.progress ?: 0.0
                val extras = media.extraFiles.orEmpty()
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(PwPlaceholder)
                    ) {
                        if (fanart.isNotBlank()) {
                            AsyncImage(model = fanart, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0B0D10))))
                        )
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        Box(
                            Modifier
                                .width(120.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PwPlaceholder)
                        ) {
                            if (poster.isNotBlank()) {
                                AsyncImage(model = poster, contentDescription = media.displayTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Column(Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(media.displayTitle(), style = MaterialTheme.typography.headlineMedium)
                            if (!media.originalTitle.isNullOrBlank() && media.originalTitle != media.title) {
                                Text(media.originalTitle, color = PwMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            val bits = buildList {
                                media.year?.takeIf { it > 0 }?.let { add(it.toString()) }
                                media.category?.let { add(if (it == "tvshow") "剧集" else "影片") }
                                media.rating?.takeIf { it > 0 }?.let { add("%.1f".format(it)) }
                            }
                            if (bits.isNotEmpty()) {
                                Text(bits.joinToString("  ·  "), color = PwMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    if (!media.genre.isNullOrBlank()) {
                        Text(media.genre, color = PwMuted, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp))
                    }
                    if (!media.director.isNullOrBlank()) {
                        Text("导演  ${media.director}", modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp))
                    }
                    val cast = media.parsedCast()
                    if (cast.isNotEmpty()) {
                        Text("演员  ${cast.joinToString("、")}", modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp))
                    }
                    if (!media.plot.isNullOrBlank()) {
                        Text("简介", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
                        Text(media.plot, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    if (extras.size > 1) {
                        Text("分集", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
                        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            extras.forEachIndexed { i, extra ->
                                FilterChip(
                                    selected = part == i,
                                    onClick = { part = i },
                                    label = { Text(extra.label ?: "第${i + 1}部") }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val canResume = progress > 5 && (media.duration ?: 0.0).let { d -> d <= 0 || progress < d * 0.95 }
                        Button(onClick = { onPlay(media.mediaId(), part, canResume) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (canResume) "继续播放 ${formatTime(progress)}" else "播放")
                        }
                        if (canResume) {
                            OutlinedButton(onClick = { onPlay(media.mediaId(), part, false) }) {
                                Icon(Icons.Default.Replay, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("从头播放")
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val s = seconds.toInt().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
