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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/**
 * 全局迷你播放条。
 * 必须挂在 Scaffold 的 bottomBar 槽位或页面底部，不能在页面内部，否则切换 Tab 会丢失。
 *
 * 视觉上只保留一条进度细线 + 一行文字，不再用大色块，
 * 这样海报墙的封面可以一直铺到屏幕底部，不被打断。
 *
 * @param progress 当前播放进度 0f..1f，用来画顶部细进度线
 */
@Composable
fun MiniPlayerBar(
  song: Song,
  isPlaying: Boolean,
  progress: Float,
  onTogglePlay: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDark: Boolean = true,
  /** 进度线颜色；首页会传系统主题色，其它页面沿用品牌蓝 */
  accent: Color = EchoAccent
) {
  val interactionSource = remember { MutableInteractionSource() }
  val indication = LocalIndication.current
  val primary = if (isDark) EchoTextPrimaryDark else EchoTextPrimary
  val background = if (isDark) EchoBackgroundDark else EchoBackground

  // 引擎每 250ms 推一次位置，用等速补间把跳变抹平成连续推进
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 260, easing = LinearEasing),
    label = "miniProgress"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      // 用页面底色做渐变，让迷你条从海报墙里「浮」出来，而不是压一块色板；
      // 过渡只发生在顶部 12dp 留白内，正文区域必须完全不透明，否则列表文字会透出来
      .background(
        Brush.verticalGradient(
          0f to Color.Transparent,
          0.16f to background,
          1f to background
        )
      )
      .clickable(
        interactionSource = interactionSource,
        indication = indication,
        onClick = onClick
      )
      .pressScale(interactionSource, pressedScale = 0.99f)
  ) {
    // 顶部留一段渐变过渡区，让后面滚动的封面文字自然淡出
    Spacer(Modifier.height(12.dp))
    // 顶部细节进度线：底色区分深/浅色，进度用品牌蓝
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.5.dp)
        .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedProgress)
          .height(1.5.dp)
          .background(accent)
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
        .height(44.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      CoverArt(
        seed = song.id,
        // 与本地/歌单列表的缩略图保持同一套规则：没有真实封面时用歌名首字
        label = song.title.take(1),
        labelSize = 13.sp,
        modifier = Modifier.size(34.dp),
        cornerRadius = 3.dp,
        artUrl = song.albumArtUrl
      )
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          color = primary,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = song.artist,
          color = EchoTextSecondary,
          fontSize = 10.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      // 播放 / 暂停，独立点击区域，避免触发展开
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .clickable(onClick = onTogglePlay),
        contentAlignment = Alignment.Center
      ) {
        if (isPlaying) {
          PauseIcon(modifier = Modifier.size(16.dp), color = primary)
        } else {
          PlayIcon(modifier = Modifier.size(16.dp), color = primary)
        }
      }
      Spacer(Modifier.width(4.dp))
      ChevronUpIcon(modifier = Modifier.size(13.dp), color = EchoTextSecondary)
    }
  }
}
