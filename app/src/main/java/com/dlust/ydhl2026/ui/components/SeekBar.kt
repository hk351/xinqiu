package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** 把 0..1 的拖动比例换算成毫秒；时长未知时返回 0，越界比例会被夹住 */
fun progressToMs(fraction: Float, durationMs: Long): Long =
  if (durationMs <= 0L) 0L else (fraction.coerceIn(0f, 1f) * durationMs).toLong()

/**
 * 可拖动的播放进度条。
 *
 * 细线只负责视觉，真正的触摸区域是 [touchHeight] 高的透明带，手指不用瞄准 2dp。
 * 拖动过程中用本地比例渲染，避免引擎每 250ms 回传的位置把滑块拽回去；
 * 松手（或单击）时把最终比例回调出去，由调用方换算成毫秒。
 *
 * [onPreview] 在拖动期间持续回报当前比例（结束时回报 null），
 * 让调用方可以把时间文字也换成手指所指的位置。
 */
@Composable
fun SeekBar(
  progress: Float,
  onSeek: (Float) -> Unit,
  trackColor: Color,
  progressBrush: Brush,
  modifier: Modifier = Modifier,
  trackHeight: Dp = 2.dp,
  touchHeight: Dp = 24.dp,
  thumbColor: Color = Color.White,
  onPreview: (Float?) -> Unit = {}
) {
  var dragFraction by remember { mutableStateOf<Float?>(null) }
  val shown = (dragFraction ?: progress).coerceIn(0f, 1f)
  // 手势块只创建一次，用最新的回调引用，避免时长变化后仍按旧值换算
  val latestOnSeek by rememberUpdatedState(onSeek)
  val latestOnPreview by rememberUpdatedState(onPreview)
  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .height(touchHeight)
      .pointerInput(Unit) {
        awaitEachGesture {
          val width = size.width.toFloat()
          if (width <= 0f) return@awaitEachGesture
          val down = awaitFirstDown(requireUnconsumed = false)
          var current = (down.position.x / width).coerceIn(0f, 1f)
          var dragging = false
          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            current = (change.position.x / width).coerceIn(0f, 1f)
            if (!dragging && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
              dragging = true
            }
            if (dragging) {
              if (dragFraction != current) {
                dragFraction = current
                latestOnPreview(current)
              }
              change.consume()
            }
          }
          dragFraction = null
          latestOnPreview(null)
          // 单击同样按最终位置跳转
          latestOnSeek(current)
        }
      },
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(trackHeight)
        .clip(RoundedCornerShape(trackHeight / 2))
        .background(trackColor)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(shown)
          .height(trackHeight)
          .background(progressBrush)
      )
    }
    // 拖动时才出现的滑块，给手指一个落点反馈
    val dragging = dragFraction
    if (dragging != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth(dragging)
          .height(trackHeight)
      ) {
        Box(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = 4.dp)
            .size(9.dp)
            .clip(CircleShape)
            .background(thumbColor)
        )
      }
    }
  }
}
