package com.pornweb.android.ui.actors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.ActorItem
import com.pornweb.android.ui.theme.PwMuted
import com.pornweb.android.ui.theme.PwPlaceholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorsScreen(onOpenActor: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ActorItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query, reloadKey) {
        val q = query.trim()
        if (reloadKey == 0 || q.isNotEmpty()) {
            // debounce search; initial / pull-refresh still waits briefly when query empty
            delay(300)
        }
        loading = true
        error = null
        try {
            val resp = c.api.actors(search = q.ifBlank { null })
            items = resp.items.orEmpty()
        } catch (e: Exception) {
            error = c.parseError(e)
            items = emptyList()
        } finally {
            loading = false
            refreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                reloadKey++
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "演员",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("搜索演员…") }
            )
            when {
                loading && items.isEmpty() && !refreshing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && items.isEmpty() -> {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items.isEmpty() && !loading -> {
                    Text(
                        if (query.isBlank()) "暂无演员" else "没有匹配的演员",
                        color = PwMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.displayName() }) { actor ->
                        ActorPosterCard(
                            actor = actor,
                            imageUrl = c.resolveAssetPath(actor.posterUrl),
                            onClick = {
                                val name = actor.displayName()
                                if (name.isNotBlank()) onOpenActor(name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActorPosterCard(
    actor: ActorItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
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
                .aspectRatio(2f / 3f)
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
                    contentDescription = actor.displayName(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = actor.displayName(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            text = "${actor.workCount()} 部",
            style = MaterialTheme.typography.bodySmall,
            color = PwMuted,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
        )
    }
}
