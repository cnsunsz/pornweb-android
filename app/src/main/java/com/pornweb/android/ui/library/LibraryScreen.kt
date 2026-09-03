package com.pornweb.android.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.FolderItem
import com.pornweb.android.data.LibraryItem
import com.pornweb.android.data.MediaItem
import com.pornweb.android.ui.components.PosterGridCard
import com.pornweb.android.ui.theme.PwMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(initialFolder: String?, onOpenMedia: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("newest") }
    var genre by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf(initialFolder.orEmpty()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var sortMenu by remember { mutableStateOf(false) }
    var genreMenu by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pageSize = 20

    suspend fun fetch(reset: Boolean) {
        val nextPage = if (reset) 1 else page + 1
        if (!reset && items.size >= total && total > 0) return
        loading = true
        error = null
        try {
            val resp = c.api.mediaList(
                page = nextPage,
                pageSize = pageSize,
                genre = genre.ifBlank { null },
                folder = folder.ifBlank { null },
                sort = sort
            )
            val list = resp.items.orEmpty()
            total = resp.total ?: list.size
            page = nextPage
            items = if (reset) list else items + list
        } catch (e: Exception) {
            error = c.parseError(e)
            if (reset) items = emptyList()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshing = true
        try {
            fetch(true)
            genres = try { c.parseStringList(c.api.genres()) } catch (_: Exception) { emptyList() }
            folders = try { c.parseFolders(c.api.folders()) } catch (_: Exception) { emptyList() }
            libraries = try { c.parseLibraries(c.api.libraries()) } catch (_: Exception) { emptyList() }
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(sort, genre, folder) {
        if (!refreshing) {
            fetch(true)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - 4 && items.size < total && !loading
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) fetch(false)
    }

    val sortLabel = when (sort) {
        "title" -> "标题"
        "rating" -> "评分"
        "year" -> "年份"
        else -> "最新"
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = {
        scope.launch {
            refreshing = true
            try { fetch(true) } finally { refreshing = false }
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            Text("媒体库", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilterChip(selected = true, onClick = { sortMenu = true }, label = { Text(sortLabel) })
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf("newest" to "最新", "title" to "标题", "rating" to "评分", "year" to "年份").forEach { (k, v) ->
                            DropdownMenuItem(text = { Text(v) }, onClick = { sort = k; sortMenu = false })
                        }
                    }
                }
                Box {
                    FilterChip(selected = genre.isNotBlank(), onClick = { genreMenu = true }, label = { Text(genre.ifBlank { "类型" }) })
                    DropdownMenu(expanded = genreMenu, onDismissRequest = { genreMenu = false }) {
                        DropdownMenuItem(text = { Text("全部类型") }, onClick = { genre = ""; genreMenu = false })
                        genres.forEach { g ->
                            DropdownMenuItem(text = { Text(g) }, onClick = { genre = g; genreMenu = false })
                        }
                    }
                }
                Box {
                    val folderLabel = folder.substringAfterLast('/').ifBlank { folder.ifBlank { "目录" } }
                    FilterChip(selected = folder.isNotBlank(), onClick = { folderMenu = true }, label = { Text(folderLabel, maxLines = 1) })
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(text = { Text("全部目录") }, onClick = { folder = ""; folderMenu = false })
                        libraries.forEach { lib ->
                            val p = lib.path.orEmpty()
                            if (p.isNotBlank()) {
                                DropdownMenuItem(text = { Text(lib.displayName()) }, onClick = { folder = p; folderMenu = false })
                            }
                        }
                        folders.forEach { f ->
                            DropdownMenuItem(text = { Text(f.displayName()) }, onClick = { folder = f.folderPath(); folderMenu = false })
                        }
                    }
                }
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            if (items.isEmpty() && !loading && !refreshing) {
                Text("没有内容", color = PwMuted, modifier = Modifier.padding(16.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                state = gridState,
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.mediaId() }) { item ->
                    PosterGridCard(item = item, imageUrl = c.resolveImage(item)) {
                        onOpenMedia(item.mediaId())
                    }
                }
                if (loading && items.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
