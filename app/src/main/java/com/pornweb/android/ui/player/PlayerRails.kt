package com.pornweb.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pornweb.android.ui.theme.PwAccent

@Composable
fun RailIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Icon(
            imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else Color.Gray,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun LeftPlayerRail(
    locked: Boolean,
    orientLandscape: Boolean,
    showParts: Boolean,
    onToggleLock: () -> Unit,
    onToggleOrient: () -> Unit,
    onParts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(start = 10.dp, top = 72.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RailIconButton(
            imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = if (locked) "解锁" else "锁定",
            onClick = onToggleLock,
            tint = if (locked) PwAccent else Color.White
        )
        if (!locked) {
            RailIconButton(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "旋转",
                onClick = onToggleOrient,
                tint = if (orientLandscape) PwAccent else Color.White
            )
            if (showParts) {
                RailIconButton(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "选集",
                    onClick = onParts
                )
            }
        }
    }
}

@Composable
fun RightPlayerRail(
    fillMode: Boolean,
    subtitleActive: Boolean,
    currentSpeedLabel: String,
    showSpeedMenu: Boolean,
    onDismissSpeed: () -> Unit,
    onSpeedClick: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    speedOptions: List<Float>,
    selectedSpeed: Float,
    onScreenshot: () -> Unit,
    onToggleFill: () -> Unit,
    onSubtitles: () -> Unit,
    gifEnabled: Boolean = false,
    onGif: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 10.dp, top = 72.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RailIconButton(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "截图",
            onClick = onScreenshot
        )
        RailIconButton(
            imageVector = Icons.Default.Gif,
            contentDescription = "GIF（暂未开放）",
            onClick = onGif,
            tint = Color.Gray,
            enabled = gifEnabled
        )
        RailIconButton(
            imageVector = if (fillMode) Icons.Default.FitScreen else Icons.Default.CropFree,
            contentDescription = if (fillMode) "适应" else "铺满",
            onClick = onToggleFill,
            tint = if (fillMode) PwAccent else Color.White
        )
        RailIconButton(
            imageVector = Icons.Default.ClosedCaption,
            contentDescription = "字幕",
            onClick = onSubtitles,
            tint = if (subtitleActive) PwAccent else Color.White
        )
        Box {
            RailIconButton(
                imageVector = Icons.Default.Speed,
                contentDescription = "倍速 $currentSpeedLabel",
                onClick = onSpeedClick,
                tint = if (selectedSpeed != 1.0f) PwAccent else Color.White
            )
            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = onDismissSpeed) {
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
                                color = if (sp == selectedSpeed) PwAccent else Color.Unspecified
                            )
                        },
                        onClick = { onSelectSpeed(sp) }
                    )
                }
            }
        }
    }
}
