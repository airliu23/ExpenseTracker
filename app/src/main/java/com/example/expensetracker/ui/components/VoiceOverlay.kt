package com.example.expensetracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 语音录入全屏动画遮罩
 *
 * @param isVisible 是否可见
 * @param isProcessing 是否正在识别中（停止动画，显示处理中）
 * @param partialText 实时部分识别文字
 * @param onDismiss 用户点击取消
 */
@Composable
fun VoiceOverlay(
    isVisible: Boolean,
    isProcessing: Boolean,
    partialText: String,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "voice")

    // 麦克风缩放脉冲
    val micScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    // 外圈涟漪 1
    val ripple1Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    val ripple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1Alpha"
    )

    // 外圈涟漪 2 (延迟)
    val ripple2Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2Alpha"
    )

    // 光点旋转
    val dotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot"
    )

    val accentColor = Color(0xFFE53935)  // 红色麦克风

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        // 底部取消按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "取消",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 麦克风区域
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // 涟漪 1
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(ripple1Scale)
                        .alpha(ripple1Alpha)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                )
                // 涟漪 2 (视觉延迟 — offset slightly)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(
                            if (ripple2Scale < 1.4f) 1.0f
                            else ripple2Scale
                        )
                        .alpha(
                            if (ripple2Alpha > 0.15f) ripple2Alpha
                            else 0f
                        )
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.08f))
                )

                // 麦克风图标 (脉冲)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (isProcessing) 1f else micScale)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // 旋转光点
                val rad = Math.toRadians(dotAngle.toDouble()).toFloat()
                val dotRadius = 72
                val dx = (dotRadius * kotlin.math.cos(rad)).dp
                val dy = (dotRadius * kotlin.math.sin(rad)).dp
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dx.roundToPx(), dy.roundToPx()) }
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.7f))
                )
            }

            Spacer(Modifier.height(32.dp))

            // 状态文字
            Text(
                text = if (isProcessing) "识别中…" else "正在聆听",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            // 实时部分识别文字
            if (partialText.isNotBlank()) {
                Text(
                    text = partialText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            } else {
                Text(
                    text = "请说出消费内容",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp
                )
            }
        }
    }
}
