package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

/** 按 seed 取一组稳定的渐变色对，封面占位与播放背景共用同一套色板 */
fun coverGradientPair(seed: String): Pair<Color, Color> =
  CoverGradients[(seed.hashCode() and 0x7FFFFFFF) % CoverGradients.size]

/**
 * 通用封面：能取到真实专辑图就显示图片，取不到再用 seed 派生一个稳定的渐变色块 + 可选文字。
 * 同一个 seed 每次渲染颜色都一致，不会闪烁。
 *
 * 质感由三层叠出来：底色渐变 → 对角高光 → 右上角柔光，最上面再压一圈细内描边。
 */
@Composable
fun CoverArt(
  seed: String,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 12.dp,
  label: String = "♪",
  labelSize: TextUnit = 20.sp,
  artUrl: String? = null
) {
  val gradient = remember(seed) { coverGradientPair(seed) }
  val shape = RoundedCornerShape(cornerRadius)
  val context = LocalContext.current
  // 初值取同步缓存：列表滚动回来时同一张封面能立刻显示，不会先闪一下渐变
  val artwork by produceState(initialValue = artUrl?.let(::cachedArtwork), artUrl) {
    value = artUrl?.let { loadArtwork(context, it) }
  }
  Box(
    modifier = modifier
      .clip(shape)
      .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
    contentAlignment = Alignment.Center
  ) {
    val bitmap = artwork
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize()
      )
      Box(
        modifier = Modifier
          .matchParentSize()
          .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
      )
    } else {
      // 对角高光：左上提亮、右下压暗，让平面色块有体积感
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.linearGradient(
              listOf(
                Color.White.copy(alpha = 0.26f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.12f)
              )
            )
          )
      )
      // 右上角柔光，模拟光源
      Canvas(modifier = Modifier.matchParentSize()) {
        val radius = size.minDimension * 0.58f
        val center = Offset(size.width * 0.84f, size.height * 0.12f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
            center = center,
            radius = radius
          ),
          radius = radius,
          center = center
        )
      }
      Text(
        text = label,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = labelSize,
        fontWeight = FontWeight.Bold
      )
      // 细内描边压在最上层，避免纯色块边缘发虚
      Box(
        modifier = Modifier
          .matchParentSize()
          .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
      )
    }
  }
}
