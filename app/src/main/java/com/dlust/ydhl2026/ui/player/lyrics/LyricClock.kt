package com.dlust.ydhl2026.ui.player.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

/**
 * 逐字动画需要按帧推进的时间轴。
 *
 * [PlaybackEngine] 每 250ms 才回传一次进度，直接拿它驱动词的高亮会出现 250ms 的台阶；
 * 这里用帧时钟在两次回调之间插值，并在跳转（拖动进度、切歌）时直接对齐。
 */
@Composable
fun rememberLyricPosition(positionMs: Long, isPlaying: Boolean): Long {
  val target by rememberUpdatedState(positionMs)
  val playing by rememberUpdatedState(isPlaying)
  var smooth by remember { mutableLongStateOf(positionMs) }

  LaunchedEffect(Unit) {
    var lastFrame = 0L
    while (true) {
      withFrameNanos { frame ->
        val wanted = target
        val elapsedMs = if (lastFrame == 0L) 0L else (frame - lastFrame) / 1_000_000L
        lastFrame = frame
        smooth = when {
          // 一次性跳了很远，说明是拖动或切歌，直接对齐而不是慢慢追
          abs(wanted - smooth) > 800 -> wanted
          playing -> {
            val advanced = smooth + elapsedMs
            advanced + ((wanted - advanced) * 0.15f).toLong()
          }
          else -> wanted
        }
      }
    }
  }
  return smooth
}
