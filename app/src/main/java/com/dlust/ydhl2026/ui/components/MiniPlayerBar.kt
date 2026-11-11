package com.dlust.ydhl2026.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.theme.EchoSurfaceDark

/**
 * 全局迷你播放条。
 * 必须挂在 Scaffold 的 bottomBar 槽位，不能在页面内部，否则切换 Tab 会丢失。
 *
 * @param progress 当前播放进度 0f..1f，用来画底部细进度线
 */
@Composable
fun MiniPlayerBar(
  song: Song,
  isPlaying: Boolean,
  progress: Float,
  onTogglePlay: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(12.dp)
  val interactionSource = remember { MutableInteractionSource() }
  val indication = LocalIndication.current

  // 引擎每 250ms 推一次位置，用等速补间把跳变抹平成连续推进
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 260, easing = LinearEasing),
    label = "miniProgress"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .height(56.dp)
      .clip(shape)
      .background(color = EchoSurfaceDark)
      .clickable(
        interactionSource = interactionSource,
        indication = indication,
        onClick = onClick
      )
      .pressScale(interactionSource, pressedScale = 0.985f)
      // 进度线直接画在卡片最上层：先铺一条 12% 白的轨道，再按当前进度覆盖品牌蓝
      .drawWithContent {
        drawContent()
        // 2.5dp 比 2dp 更能压住黑色背景；颜色比品牌浅蓝再亮一档，否则在深色卡上发灰
        val lineHeight = 2.5.dp.toPx()
        val top = size.height - lineHeight
        drawRect(
          color = Color.White.copy(alpha = 0.14f),
          topLeft = Offset(0f, top),
          size = Size(size.width, lineHeight)
        )
        drawRect(
          color = Color(0xFF6E8CE8),
          topLeft = Offset(0f, top),
          size = Size(size.width * animatedProgress, lineHeight)
        )
      }
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 10.dp, end = 10.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      CoverArt(
        seed = song.id,
        label = song.title.take(1),
        modifier = Modifier.size(40.dp),
        cornerRadius = 8.dp,
        labelSize = 16.sp
      )
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = song.artist,
          color = Color(0xFF9AA3AD),
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      // 播放 / 暂停，独立点击区域，避免触发展开
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .clickable(onClick = onTogglePlay),
        contentAlignment = Alignment.Center
      ) {
        if (isPlaying) {
          PauseIcon(modifier = Modifier.size(18.dp))
        } else {
          PlayIcon(modifier = Modifier.size(18.dp))
        }
      }
      Spacer(Modifier.width(4.dp))
      ChevronUpIcon(modifier = Modifier.size(14.dp), color = Color(0xFF9AA3AD))
    }
  }
}
