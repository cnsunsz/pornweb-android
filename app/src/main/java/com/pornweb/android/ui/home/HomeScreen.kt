package com.pornweb.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.LibraryItem
import com.pornweb.android.data.MediaItem
import com.pornweb.android.ui.components.PosterCard
import com.pornweb.android.ui.theme.PwMuted
import com.pornweb.android.ui.theme.PwPlaceholder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenMedia: (Long) -> Unit, onOpenLibrary: (String?) -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    var continueItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var latest by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        coroutineScope {
            error = null
            val cont = async {
                try { c.api.continueWatching().items.orEmpty() } catch (_: Exception) { emptyList() }
            }
            val late = async {
                try { c.api.mediaList(page = 1, pageSize = 20, sort = "newest").items.orEmpty() } catch (e: Exception) {
                    error = c.parseError(e)
                    emptyList()
                }
            }
            val libs = async {
                try { c.parseLibraries(c.api.libraries()) } catch (_: Exception) { emptyList() }
            }
            continueItems = cont.await()
            latest = late.await()
            libraries = libs.await()
        }
    }

    LaunchedEffect(Unit) {
        refreshing = true
        try { load() } finally { refreshing = false }
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = {
        scope.launch {
            refreshing = true
            try { load() } finally { refreshing = false }
        }
    }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    "PornWeb",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (error != null) {
                item {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
            item {
                HomeRow(
                    title = "继续观看",
                    empty = "暂无播放进度",
                    items = continueItems,
                    imageOf = { c.resolveImage(it) },
                    onOpen = onOpenMedia
                )
            }
            item {
                Text(
                    "媒体库",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            if (libraries.isEmpty()) {
                item {
                    Text("暂无媒体库", color = PwMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(libraries, key = { it.path ?: it.name ?: it.id.toString() }) { lib ->
                            LibraryTile(lib, poster = c.posterUrl(lib.posterId)) {
                                onOpenLibrary(lib.path)
                            }
                        }
                    }
                }
            }
            item {
                HomeRow(
                    title = "最近添加",
                    empty = "媒体库为空，请在网页端添加媒体库并扫描",
                    items = latest,
                    imageOf = { c.resolveImage(it) },
                    onOpen = onOpenMedia
                )
            }
        }
    }
}

@Composable
private fun HomeRow(
    title: String,
    empty: String,
    items: List<MediaItem>,
    imageOf: (MediaItem) -> String,
    onOpen: (Long) -> Unit
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (items.isEmpty()) {
            Text(empty, color = PwMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.mediaId() }) { item ->
                    PosterCard(item = item, imageUrl = imageOf(item), showProgress = true) {
                        onOpen(item.mediaId())
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTile(lib: LibraryItem, poster: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(PwPlaceholder)
        ) {
            if (poster.isNotBlank()) {
                AsyncImage(model = poster, contentDescription = lib.displayName(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(lib.displayName(), style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        Text("${lib.itemCount()} 部", color = PwMuted, style = MaterialTheme.typography.bodySmall)
    }
}
