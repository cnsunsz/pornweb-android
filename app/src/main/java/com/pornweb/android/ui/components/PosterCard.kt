package com.pornweb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.MediaItem
import com.pornweb.android.ui.theme.PwAccent
import com.pornweb.android.ui.theme.PwMuted
import com.pornweb.android.ui.theme.PwPlaceholder
import kotlin.math.floor

fun formatMediaDuration(seconds: Double?): String? {
    if (seconds == null || seconds <= 0.0) return null
    val total = floor(seconds).toLong().coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun DurationBadge(durationSeconds: Double?, modifier: Modifier = Modifier) {
    val text = formatMediaDuration(durationSeconds) ?: return
    Text(
        text = text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
fun PosterCard(
    item: MediaItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    aspectRatio: Float = 2f / 3f,
    showProgress: Boolean = true,
    showDuration: Boolean = false,
    onClick: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val loader = app.container.imageLoader
    val context = LocalContext.current
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .background(PwPlaceholder)
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = loader,
                    contentDescription = item.displayTitle(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (showDuration) {
                DurationBadge(
                    durationSeconds = item.duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            if (showProgress && item.progressRatio() > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressRatio() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PwAccent,
                    trackColor = PwPlaceholder.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = item.displayTitle(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
        )
        if (item.year != null && item.year > 0) {
            Text(
                text = item.year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = PwMuted,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp)
            )
        }
    }
}

@Composable
fun PosterGridCard(
    item: MediaItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 2f / 3f,
    showProgress: Boolean = true,
    showDuration: Boolean = true,
    titleMaxLines: Int = 2,
    onClick: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val loader = app.container.imageLoader
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .background(PwPlaceholder)
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = loader,
                    contentDescription = item.displayTitle(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (showDuration) {
                DurationBadge(
                    durationSeconds = item.duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                )
            }
            if (showProgress && item.progressRatio() > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressRatio() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PwAccent,
                    trackColor = PwPlaceholder.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = item.displayTitle(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp, start = 1.dp, end = 1.dp)
        )
    }
}
