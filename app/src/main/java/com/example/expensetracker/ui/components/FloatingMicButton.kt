package com.example.expensetracker.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全屏浮层：底部居中一个按住说话 / 松开结束的麦克风按钮。
 *
 * @param isVisible  是否显示（仅在 Idle/Preparing/Listening 时可见）
 * @param onPressStart 按下时回调 → 启动语音识别
 * @param onPressEnd   松开时回调 → 停止语音识别
 * @param bottomOffset 距屏幕底部偏移
 */
@Composable
fun FloatingMicButton(
    isVisible: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    bottomOffset: Dp = 80.dp
) {
    if (!isVisible) return

    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "micPressScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (pressed) 12.dp else 6.dp,
        animationSpec = tween(durationMillis = 200),
        label = "micElevation"
    )

    val micColor = if (pressed) Color(0xFFE53935) else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .offset(y = -bottomOffset)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape
                )
                .size(72.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(micColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            onPressStart()
                            tryAwaitRelease()
                            pressed = false
                            onPressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "按住说话",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
