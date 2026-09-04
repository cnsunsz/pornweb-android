package com.pornweb.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.ui.theme.PwMuted

@Composable
fun PlaybackSettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val prefs = app.container.playerPrefs

    var defaultSpeed by remember { mutableFloatStateOf(prefs.defaultSpeed) }
    var longPressSpeed by remember { mutableFloatStateOf(prefs.longPressSpeed) }
    var skipSeconds by remember { mutableIntStateOf(prefs.skipSeconds) }
    var swipeSeek by remember { mutableIntStateOf(prefs.swipeSeekSeconds) }
    var startLandscape by remember { mutableStateOf(prefs.startLandscape) }
    var doubleTap by remember { mutableStateOf(prefs.doubleTapSeek) }
    var leftRewind by remember { mutableStateOf(prefs.leftLongPressRewind) }
    var resumeOnOpen by remember { mutableStateOf(prefs.resumeOnOpen) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("播放设置", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "参考 MX Player / KMPlayer / Emby / Jellyfin",
            color = PwMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
        )

        SectionTitle("默认倍速")
        ChipRow(
            options = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
            selected = defaultSpeed,
            label = { if (it == 1.0f) "1.0x" else "${it}x" }
        ) {
            defaultSpeed = it
            prefs.defaultSpeed = it
        }

        SectionTitle("长按倍速（MX / KMPlayer）")
        Text("按住屏幕右侧临时加速；松开恢复", color = PwMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        ChipRow(
            options = listOf(2.0f, 3.0f, 4.0f),
            selected = longPressSpeed,
            label = { "${it.toInt()}x" }
        ) {
            longPressSpeed = it
            prefs.longPressSpeed = it
        }

        SectionTitle("左右双击跳过")
        ChipRow(
            options = listOf(5, 10, 15, 30),
            selected = skipSeconds,
            label = { "${it}s" }
        ) {
            skipSeconds = it
            prefs.skipSeconds = it
        }

        SectionTitle("横向滑动灵敏度")
        Text("滑过整屏大约跳过的秒数", color = PwMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        ChipRow(
            options = listOf(60, 90, 120, 180),
            selected = swipeSeek,
            label = { "${it}s" }
        ) {
            swipeSeek = it
            prefs.swipeSeekSeconds = it
        }

        Spacer(Modifier.height(16.dp))
        SwitchRow("启用双击快进/快退", doubleTap) {
            doubleTap = it
            prefs.doubleTapSeek = it
        }
        SwitchRow("左侧长按倒退（右侧长按加速）", leftRewind) {
            leftRewind = it
            prefs.leftLongPressRewind = it
        }
        SwitchRow("打开播放器时优先横屏", startLandscape) {
            startLandscape = it
            prefs.startLandscape = it
        }
        SwitchRow("有进度时自动续播", resumeOnOpen) {
            resumeOnOpen = it
            prefs.resumeOnOpen = it
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "提示：播放页也可点右上角齿轮进入本页。锁定后手势会暂停，需先解锁。",
            color = PwMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(label(opt)) }
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
