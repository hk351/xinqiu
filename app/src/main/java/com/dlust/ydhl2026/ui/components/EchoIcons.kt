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

/** 向下箭头：收起全屏播放器 */
@Composable
fun ChevronDownIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.20f, h * 0.34f)
      lineTo(w * 0.50f, h * 0.66f)
      lineTo(w * 0.80f, h * 0.34f)
    }
    drawPath(path = path, color = color, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
  }
}

/** 上一首：左侧竖条 + 左向三角 */
@Composable
fun SkipPreviousIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.14f, h * 0.18f),
      size = Size(w * 0.10f, h * 0.64f),
      cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    val path = Path().apply {
      moveTo(w * 0.84f, h * 0.18f)
      lineTo(w * 0.36f, h * 0.50f)
      lineTo(w * 0.84f, h * 0.82f)
      close()
    }
    drawPath(path = path, color = color)
  }
}

/** 下一首：右向三角 + 右侧竖条 */
@Composable
fun SkipNextIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.16f, h * 0.18f)
      lineTo(w * 0.64f, h * 0.50f)
      lineTo(w * 0.16f, h * 0.82f)
      close()
    }
    drawPath(path = path, color = color)
    drawRoundRect(
      color = color,
      topLeft = Offset(w * 0.76f, h * 0.18f),
      size = Size(w * 0.10f, h * 0.64f),
      cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
  }
}

/** 返回：左向箭头 */
@Composable
fun ArrowBackIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.74f, h * 0.16f)
      lineTo(w * 0.30f, h * 0.50f)
      lineTo(w * 0.74f, h * 0.84f)
    }
    drawPath(path = path, color = color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
  }
}

/** 拖拽手柄：三条短横线 */
@Composable
fun DragHandleIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    listOf(0.26f, 0.50f, 0.74f).forEach { y ->
      drawLine(
        color = color,
        start = Offset(w * 0.18f, h * y),
        end = Offset(w * 0.82f, h * y),
        strokeWidth = w * 0.10f,
        cap = StrokeCap.Round
      )
    }
  }
}

/** 删除：垃圾桶轮廓 */
@Composable
fun TrashIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeWidth = w * 0.075f
    drawLine(
      color = color,
      start = Offset(w * 0.20f, h * 0.28f),
      end = Offset(w * 0.80f, h * 0.28f),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.40f, h * 0.18f),
      end = Offset(w * 0.60f, h * 0.18f),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    val body = Path().apply {
      moveTo(w * 0.30f, h * 0.34f)
      lineTo(w * 0.35f, h * 0.86f)
      lineTo(w * 0.65f, h * 0.86f)
      lineTo(w * 0.70f, h * 0.34f)
    }
    drawPath(
      path = body,
      color = color,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
  }
}

/** 播放队列：三行列表 */
@Composable
fun QueueListIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    listOf(0.28f, 0.50f, 0.72f).forEach { y ->
      drawLine(
        color = color,
        start = Offset(w * 0.30f, h * y),
        end = Offset(w * 0.88f, h * y),
        strokeWidth = w * 0.09f,
        cap = StrokeCap.Round
      )
    }
    listOf(0.28f, 0.50f, 0.72f).forEach { y ->
      drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.13f, h * y))
    }
  }
}

/** 搜索：圆环 + 斜柄 */
@Composable
fun SearchIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.10f
    drawCircle(
      color = color,
      radius = w * 0.27f,
      center = Offset(w * 0.43f, h * 0.43f),
      style = Stroke(width = stroke)
    )
    drawLine(
      color = color,
      start = Offset(w * 0.63f, h * 0.63f),
      end = Offset(w * 0.83f, h * 0.83f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
  }
}

/** 收藏：实心/描边心形 */
@Composable
fun HeartIcon(
  modifier: Modifier = Modifier,
  color: Color = Color.White,
  filled: Boolean = false
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.50f, h * 0.84f)
      cubicTo(w * 0.12f, h * 0.58f, w * 0.10f, h * 0.19f, w * 0.30f, h * 0.15f)
      cubicTo(w * 0.40f, h * 0.13f, w * 0.46f, h * 0.19f, w * 0.50f, h * 0.25f)
      cubicTo(w * 0.54f, h * 0.19f, w * 0.60f, h * 0.13f, w * 0.70f, h * 0.15f)
      cubicTo(w * 0.90f, h * 0.19f, w * 0.88f, h * 0.58f, w * 0.50f, h * 0.84f)
      close()
    }
    if (filled) {
      drawPath(path = path, color = color)
    } else {
      drawPath(
        path = path,
        color = color,
        style = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
      )
    }
  }
}

/** 更多：三个圆点 */
@Composable
fun MoreIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    listOf(0.16f, 0.50f, 0.84f).forEach { x ->
      drawCircle(color = color, radius = w * 0.085f, center = Offset(w * x, h * 0.50f))
    }
  }
}

/** 列表循环：上下两条带箭头的循环线 */
@Composable
fun RepeatIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.09f
    drawLine(
      color = color,
      start = Offset(w * 0.18f, h * 0.28f),
      end = Offset(w * 0.82f, h * 0.28f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    val rightArrow = Path().apply {
      moveTo(w * 0.64f, h * 0.14f)
      lineTo(w * 0.82f, h * 0.28f)
      lineTo(w * 0.64f, h * 0.42f)
    }
    drawPath(path = rightArrow, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawLine(
      color = color,
      start = Offset(w * 0.82f, h * 0.72f),
      end = Offset(w * 0.18f, h * 0.72f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    val leftArrow = Path().apply {
      moveTo(w * 0.36f, h * 0.58f)
      lineTo(w * 0.18f, h * 0.72f)
      lineTo(w * 0.36f, h * 0.86f)
    }
    drawPath(path = leftArrow, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
  }
}

/** 单曲循环：循环线中间加一个「1」 */
@Composable
fun RepeatOneIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.09f
    drawLine(
      color = color,
      start = Offset(w * 0.18f, h * 0.28f),
      end = Offset(w * 0.82f, h * 0.28f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    val rightArrow = Path().apply {
      moveTo(w * 0.64f, h * 0.14f)
      lineTo(w * 0.82f, h * 0.28f)
      lineTo(w * 0.64f, h * 0.42f)
    }
    drawPath(path = rightArrow, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawLine(
      color = color,
      start = Offset(w * 0.82f, h * 0.72f),
      end = Offset(w * 0.18f, h * 0.72f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    val leftArrow = Path().apply {
      moveTo(w * 0.36f, h * 0.58f)
      lineTo(w * 0.18f, h * 0.72f)
      lineTo(w * 0.36f, h * 0.86f)
    }
    drawPath(path = leftArrow, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 中间的「1」：一竖 + 顶部小斜杠 + 底部短横
    drawLine(
      color = color,
      start = Offset(w * 0.50f, h * 0.42f),
      end = Offset(w * 0.50f, h * 0.58f),
      strokeWidth = stroke * 0.8f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.44f, h * 0.46f),
      end = Offset(w * 0.50f, h * 0.42f),
      strokeWidth = stroke * 0.8f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.44f, h * 0.58f),
      end = Offset(w * 0.56f, h * 0.58f),
      strokeWidth = stroke * 0.8f,
      cap = StrokeCap.Round
    )
  }
}

/** 随机播放：两条交叉箭头 */
@Composable
fun ShuffleIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.10f
    drawLine(
      color = color,
      start = Offset(w * 0.10f, h * 0.24f),
      end = Offset(w * 0.74f, h * 0.74f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.10f, h * 0.76f),
      end = Offset(w * 0.74f, h * 0.26f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    // 右上箭头
    drawLine(
      color = color,
      start = Offset(w * 0.58f, h * 0.74f),
      end = Offset(w * 0.74f, h * 0.74f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.74f, h * 0.74f),
      end = Offset(w * 0.74f, h * 0.58f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    // 右下箭头
    drawLine(
      color = color,
      start = Offset(w * 0.58f, h * 0.26f),
      end = Offset(w * 0.74f, h * 0.26f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
    drawLine(
      color = color,
      start = Offset(w * 0.74f, h * 0.26f),
      end = Offset(w * 0.74f, h * 0.42f),
      strokeWidth = stroke,
      cap = StrokeCap.Round
    )
  }
}
