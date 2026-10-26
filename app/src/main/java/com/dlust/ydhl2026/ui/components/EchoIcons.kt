package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 图标全部用 Canvas 绘制，不引入图标库、不使用图片资源（与启动页/吉祥物风格一致）。
 */

/** 播放：三角形 */
@Composable
fun PlayIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.24f, h * 0.12f)
      lineTo(w * 0.86f, h * 0.50f)
      lineTo(w * 0.24f, h * 0.88f)
      close()
    }
    drawPath(path = path, color = color)
  }
}

/** 暂停：两根圆角竖条 */
@Composable
fun PauseIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.26f
    val corner = CornerRadius(barWidth * 0.35f, barWidth * 0.35f)
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.16f, h * 0.14f),
      size = Size(barWidth, h * 0.72f),
      cornerRadius = corner
    )
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.58f, h * 0.14f),
      size = Size(barWidth, h * 0.72f),
      cornerRadius = corner
    )
  }
}

/** 向上箭头：展开全屏播放器 */
@Composable
fun ChevronUpIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.20f, h * 0.62f)
      lineTo(w * 0.50f, h * 0.30f)
      lineTo(w * 0.80f, h * 0.62f)
    }
    drawPath(path = path, color = color, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
  }
}
