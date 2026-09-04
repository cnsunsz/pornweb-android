package com.pornweb.android.ui.actors

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.MediaItem
import com.pornweb.android.ui.components.PosterGridCard
import com.pornweb.android.ui.theme.PwBg
import com.pornweb.android.ui.theme.PwMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorMediaScreen(
    name: String,
    onBack: () -> Unit,
    onOpenMedia: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("newest") }
    var sortMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val pageSize = 20

    suspend fun fetch(reset: Boolean) {
        val nextPage = if (reset) 1 else page + 1
        if (!reset && items.size >= total && total > 0) return
        loading = true
        error = null
        try {
            val resp = c.api.actorMedia(
                name = name,
                page = nextPage,
                pageSize = pageSize,
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

    LaunchedEffect(name, sort) {
        fetch(true)
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

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(name, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PwBg)
        )
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
        }
        when {
            error != null && items.isEmpty() -> {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items.isEmpty() && loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() && !loading -> {
                Text("没有作品", color = PwMuted, modifier = Modifier.padding(16.dp))
            }
            else -> LazyVerticalGrid(
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
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
