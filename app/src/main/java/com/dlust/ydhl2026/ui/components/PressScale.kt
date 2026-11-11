package com.dlust.ydhl2026.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 按压回弹：手指按下时整块轻微缩小，抬起后弹回。
 * 全站列表卡片共用同一套手感，调用方只要把同一个 interactionSource 交给 clickable。
 */
fun Modifier.pressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 0.97f
): Modifier = composed {
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed) pressedScale else 1f,
    animationSpec = tween(durationMillis = 120),
    label = "pressScale"
  )
  graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}
