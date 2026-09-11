package com.pornweb.android.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
private fun RailEdgeChevron(
    expanded: Boolean,
    towardEnd: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val icon = when {
        towardEnd && expanded -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        towardEnd && !expanded -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        !towardEnd && expanded -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        else -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(width = 28.dp, height = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.9f),
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
    collapsible: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    Row(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical)
            )
            .padding(start = 6.dp, top = 48.dp, bottom = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (collapsible) {
            RailEdgeChevron(
                expanded = expanded,
                towardEnd = false,
                onClick = { expanded = !expanded },
                contentDescription = if (expanded) "收起左侧栏" else "展开左侧栏"
            )
        }
        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
        ) {
            Column(
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
    collapsible: Boolean = true,
    startCollapsed: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember(startCollapsed) { mutableStateOf(!startCollapsed) }
    Row(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical)
            )
            .padding(end = 6.dp, top = 48.dp, bottom = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
        ) {
            Column(
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
        if (collapsible) {
            RailEdgeChevron(
                expanded = expanded,
                towardEnd = true,
                onClick = { expanded = !expanded },
                contentDescription = if (expanded) "收起右侧栏" else "展开右侧栏"
            )
        }
    }
}
