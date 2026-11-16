package com.dlust.ydhl2026.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** 吉祥物视线目标：对应登录页各输入框 */
enum class MascotGaze { CENTER, PHONE, SMS, PASSWORD }

private val FaceBlue = Color(0xFF3B62D6)
private val HandBlue = Color(0xFF2A4CB0)
private val CupBlue = Color(0xFF22409E)
private val DarkNavy = Color(0xFF101E45)
private val BlushBlue = Color(0xFF9FB6FF)
private val EyeWhite = Color.White

/**
 * Echo 吉祥物：戴耳机的蓝色圆脸角色。
 * - 随机眨眼
 * - 视线跟随当前输入框（gaze）
 * - 每次打字眼睛放大一瞬（typePulseKey 变化）
 * - coveringEyes = true 时双手捂眼（密码模式）
 * - isSad = true 时嘴角向下（登录失败）
 * - faceColor 可跟随主题色变化，手脚/腮红自动派生
 */
@Composable
fun EchoMascot(
  gaze: MascotGaze,
  coveringEyes: Boolean,
  isSad: Boolean,
  typePulseKey: Int,
  faceColor: Color = FaceBlue,
  modifier: Modifier = Modifier
) {
  val blink = remember { Animatable(1f) }
  val pulse = remember { Animatable(1f) }
  val cover = remember { Animatable(0f) }
  val gazeX = remember { Animatable(0f) }
  val gazeY = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(Random.nextLong(2200, 4800))
      blink.animateTo(0.08f, tween(90))
      blink.animateTo(1f, tween(140))
    }
  }
  LaunchedEffect(typePulseKey) {
    if (typePulseKey > 0) {
      pulse.snapTo(1.18f)
      pulse.animateTo(1f, tween(180))
    }
  }
  LaunchedEffect(coveringEyes) {
    cover.animateTo(
      targetValue = if (coveringEyes) 1f else 0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 280f
      )
    )
  }
  LaunchedEffect(gaze) {
    val (tx, ty) = when (gaze) {
      MascotGaze.PHONE -> -0.45f to 0.5f
      MascotGaze.SMS -> 0.45f to 0.5f
      MascotGaze.PASSWORD -> 0f to 0.6f
      MascotGaze.CENTER -> 0f to 0f
    }
    launch { gazeX.animateTo(tx, tween(260)) }
    launch { gazeY.animateTo(ty, tween(260)) }
  }

  Canvas(modifier = modifier.size(width = 130.dp, height = 98.dp)) {
    val w = size.width
    val h = size.height
    val faceR = w * 0.26f
    val faceCx = w / 2f
    val faceCy = h * 0.52f
    val handColor = lerp(faceColor, Color.Black, 0.25f)
    val cupColor = lerp(faceColor, Color.Black, 0.42f)
    val blushColor = lerp(faceColor, Color.White, 0.55f)

    // 耳机头梁
    drawArc(
      color = DarkNavy,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(faceCx - faceR * 1.02f, faceCy - faceR * 1.02f),
      size = Size(faceR * 2.04f, faceR * 2.04f),
      style = Stroke(width = faceR * 0.12f, cap = StrokeCap.Round)
    )
    // 脸
    drawCircle(color = faceColor, radius = faceR, center = Offset(faceCx, faceCy))
    // 耳机罩
    drawCircle(color = cupColor, radius = faceR * 0.24f, center = Offset(faceCx - faceR, faceCy))
    drawCircle(color = cupColor, radius = faceR * 0.24f, center = Offset(faceCx + faceR, faceCy))
    // 腮红
    drawCircle(
      color = blushColor,
      radius = faceR * 0.12f,
      center = Offset(faceCx - faceR * 0.62f, faceCy + faceR * 0.30f)
    )
    drawCircle(
      color = blushColor,
      radius = faceR * 0.12f,
      center = Offset(faceCx + faceR * 0.62f, faceCy + faceR * 0.30f)
    )
    // 嘴巴：微笑 / 难过
    val mouthW = faceR * 0.72f
    val mouthH = faceR * 0.58f
    val mouthTopLeft = Offset(faceCx - mouthW / 2f, faceCy + faceR * 0.30f)
    drawArc(
      color = DarkNavy,
      startAngle = if (isSad) 210f else 30f,
      sweepAngle = 120f,
      useCenter = false,
      topLeft = mouthTopLeft,
      size = Size(mouthW, mouthH),
      style = Stroke(width = faceR * 0.09f, cap = StrokeCap.Round)
    )
    // 眼睛 + 瞳孔
    val eyeDx = faceR * 0.42f
    val eyeCy = faceCy - faceR * 0.15f
    val pupilDx = gazeX.value * faceR * 0.16f
    val pupilDy = gazeY.value * faceR * 0.13f
    for (s in listOf(-1f, 1f)) {
      val eyeCx = faceCx + s * eyeDx
      withTransform({
        scale(1f, blink.value, pivot = Offset(eyeCx, eyeCy))
      }) {
        drawCircle(
          color = EyeWhite,
          radius = faceR * 0.30f * pulse.value,
          center = Offset(eyeCx, eyeCy)
        )
        drawCircle(
          color = DarkNavy,
          radius = faceR * 0.13f * pulse.value,
          center = Offset(eyeCx + pupilDx, eyeCy + pupilDy)
        )
      }
    }
    // 双手：休息位 ↔ 捂眼位
    val handR = faceR * (0.30f + 0.08f * cover.value)
    val leftRest = Offset(faceCx - faceR * 0.95f, faceCy + faceR * 0.78f)
    val rightRest = Offset(faceCx + faceR * 0.95f, faceCy + faceR * 0.78f)
    val leftCover = Offset(faceCx - eyeDx, eyeCy)
    val rightCover = Offset(faceCx + eyeDx, eyeCy)
    val leftPos = Offset(
      leftRest.x + (leftCover.x - leftRest.x) * cover.value,
      leftRest.y + (leftCover.y - leftRest.y) * cover.value
    )
    val rightPos = Offset(
      rightRest.x + (rightCover.x - rightRest.x) * cover.value,
      rightRest.y + (rightCover.y - rightRest.y) * cover.value
    )
    drawCircle(color = handColor, radius = handR, center = leftPos)
    drawCircle(color = handColor, radius = handR, center = rightPos)
  }
}
