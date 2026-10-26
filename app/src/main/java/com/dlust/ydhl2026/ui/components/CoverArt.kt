package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 预设色板：每个色对是一组“深 → 浅”的同色系渐变 */
private val CoverGradients = listOf(
  Color(0xFF1B3FA0) to Color(0xFF4C6FD1), // 品牌蓝
  Color(0xFF7B3FA0) to Color(0xFFB06FD1), // 紫
  Color(0xFFC1503F) to Color(0xFFE08A6F), // 砖红
  Color(0xFF1F7A6B) to Color(0xFF4FB3A0), // 青绿
  Color(0xFFA07820) to Color(0xFFD9B44A), // 金
  Color(0xFF2F6F3F) to Color(0xFF6BAF7A)  // 草绿
)

/**
 * 通用封面：没有图片素材，用 seed 派生一个稳定的渐变色块 + 可选文字。
 * 同一个 seed 每次渲染颜色都一致，不会闪烁。
 */
@Composable
fun CoverArt(
  seed: String,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 12.dp,
  label: String = "♪",
  labelSize: TextUnit = 20.sp
) {
  val gradient = remember(seed) {
    CoverGradients[(seed.hashCode() and 0x7FFFFFFF) % CoverGradients.size]
  }
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(cornerRadius))
      .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.92f),
      fontSize = labelSize,
      fontWeight = FontWeight.Bold
    )
  }
}
