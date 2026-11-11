package com.dlust.ydhl2026.ui.player.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.lyrics.activeLineIndex
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.ui.player.PlayerPalette
import kotlin.math.absoluteValue

/**
 * 「静帧」样式：三行居中，当前行最大最亮，上下相邻两行缩小变淡。
 * 视觉参照 Folia 的 Still 样式，但排版尺寸按我们的手机版面板比例缩放。
 */
@Composable
internal fun StillLyrics(
  lines: List<LyricLine>,
  positionMs: Long,
  palette: PlayerPalette,
  modifier: Modifier = Modifier
) {
  val activeIndex = lines.activeLineIndex(positionMs)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // 多渲染两层当过渡缓冲：切换行时能看到旧行缩小淡出的过程
    for (offset in -2..2) {
      val index = activeIndex + offset
      if (index !in lines.indices) {
        Spacer(Modifier.height(StillLineHeight))
        continue
      }
      StillLine(
        line = lines[index],
        distance = offset.absoluteValue,
        palette = palette
      )
    }
  }
}

private val StillLineHeight = 44.dp

@Composable
private fun StillLine(
  line: LyricLine,
  distance: Int,
  palette: PlayerPalette
) {
  val targetAlpha = when (distance) {
    0 -> 1f
    1 -> 0.3f
    // 再远的行只占位不显示，保证切换行时是「淡出」而不是「突然消失」
    else -> 0f
  }
  val targetSize = when (distance) {
    0 -> 26f
    1 -> 19f
    else -> 16f
  }
  val alpha by animateFloatAsState(
    targetValue = targetAlpha,
    animationSpec = tween(durationMillis = 220)
  )
  val fontSize by animateFloatAsState(
    targetValue = targetSize,
    animationSpec = tween(durationMillis = 220)
  )
  val weight = if (distance == 0) FontWeight.Bold else FontWeight.SemiBold

  Column(
    modifier = Modifier.height(StillLineHeight),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = line.text.ifBlank { "…" },
      color = palette.text.copy(alpha = alpha),
      fontSize = fontSize.sp,
      fontWeight = weight,
      textAlign = TextAlign.Center,
      maxLines = 2
    )
    if (distance == 0 && !line.translation.isNullOrBlank()) {
      Text(
        text = line.translation,
        color = palette.text.copy(alpha = alpha * 0.8f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
  }
}
