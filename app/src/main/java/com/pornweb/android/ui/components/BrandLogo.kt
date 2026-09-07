package com.pornweb.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pornweb.android.R
import com.pornweb.android.ui.theme.PwAccent

@Composable
fun BrandLogo(
    size: Dp = 40.dp,
    breathe: Boolean = false,
    showWordmark: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "pwBrandBreathe")
    val animatedScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pwBrandScale"
    )
    val scale = if (breathe) animatedScale else 1f
    val glowAlpha = if (breathe) {
        ((scale - 1f) / 0.045f).coerceIn(0f, 1f) * 0.35f
    } else {
        0f
    }

    val gap = when {
        size >= 36.dp -> 12.dp
        size >= 24.dp -> 8.dp
        else -> 7.dp
    }
    val wordSize = (size.value * 0.8f).sp
    val underlineH = if (size >= 36.dp) 2.5.dp else 2.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (glowAlpha > 0.01f) {
                Box(
                    Modifier
                        .size(size * 1.15f)
                        .scale(scale)
                        .background(
                            PwAccent.copy(alpha = glowAlpha),
                            RoundedCornerShape(size * 0.28f)
                        )
                )
            }
            Image(
                painter = painterResource(R.drawable.pw_brand_mark),
                contentDescription = "PornWeb",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .scale(scale)
            )
        }
        if (showWordmark) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Porn",
                    color = Color(0xFFF5F5F5),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = wordSize,
                    letterSpacing = (-0.3).sp,
                    lineHeight = wordSize
                )
                Box {
                    Text(
                        text = "Web",
                        color = PwAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = wordSize,
                        letterSpacing = (-0.3).sp,
                        lineHeight = wordSize,
                        modifier = Modifier.padding(bottom = underlineH + 1.dp)
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(y = (-1).dp)
                            .width((size.value * 0.8f * 1.55f).dp)
                            .height(underlineH)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PwAccent.copy(alpha = 0.85f), Color.Transparent)
                                ),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}
