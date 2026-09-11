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
    var autoHideSec by remember { mutableIntStateOf((prefs.autoHideControlsMs / 1000).coerceIn(1, 30)) }
    var showRemaining by remember { mutableStateOf(prefs.showRemainingTime) }
    var rememberSpeed by remember { mutableStateOf(prefs.rememberSpeed) }
    var continuousNext by remember { mutableStateOf(prefs.continuousPlayNextPart) }

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
            "对齐 VidHub / MX Player / Emby",
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
            if (!prefs.rememberSpeed) prefs.lastSpeed = it
        }

        SectionTitle("长按倍速")
        Text("按住画面临时加速；松开恢复", color = PwMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        ChipRow(
            options = listOf(2.0f, 3.0f, 4.0f),
            selected = longPressSpeed,
            label = { "${it.toInt()}x" }
        ) {
            longPressSpeed = it
            prefs.longPressSpeed = it
        }

        SectionTitle("跳过间隔（双击 / 底栏按钮）")
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

        SectionTitle("控制栏自动隐藏")
        ChipRow(
            options = listOf(2, 3, 4, 5, 8, 10),
            selected = autoHideSec,
            label = { "${it}s" }
        ) {
            autoHideSec = it
            prefs.autoHideControlsMs = it * 1000
        }

        Spacer(Modifier.height(16.dp))
        SwitchRow("启用三分区双击（左退 / 中暂停 / 右进）", doubleTap) {
            doubleTap = it
            prefs.doubleTapSeek = it
        }
        SwitchRow("左侧长按倒退（右侧长按加速）", leftRewind) {
            leftRewind = it
            prefs.leftLongPressRewind = it
        }
        SwitchRow("记住上次播放倍速", rememberSpeed) {
            rememberSpeed = it
            prefs.rememberSpeed = it
        }
        SwitchRow("分集播完自动连播下一集", continuousNext) {
            continuousNext = it
            prefs.continuousPlayNextPart = it
        }
        SwitchRow("进度条右侧显示剩余时长", showRemaining) {
            showRemaining = it
            prefs.showRemainingTime = it
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
            "提示：播放页左栏锁定/旋转/选集，右栏截图/铺满/字幕/倍速；右上角 ⋯ 可切换音轨。锁定后仅可点解锁。",
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
