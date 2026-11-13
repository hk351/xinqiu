package com.dlust.ydhl2026.ui.player

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.ui.components.ArrowBackIcon
import com.dlust.ydhl2026.ui.components.SparkleIcon
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.player.lyrics.GlyphPhase
import com.dlust.ydhl2026.ui.player.lyrics.activeLyricIndex
import com.dlust.ydhl2026.ui.player.lyrics.buildScatterGlyphs
import com.dlust.ydhl2026.ui.player.lyrics.glyphVisualAt
import com.dlust.ydhl2026.ui.player.lyrics.rememberLyricPosition
import com.dlust.ydhl2026.ui.player.lyrics.scatterFontSizeSp
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoInk
import com.dlust.ydhl2026.ui.theme.EchoStageWarm
import kotlinx.coroutines.delay

/**
 * 全屏散字歌词舞台。
 *
 * 和普通逐行滚动的歌词不同：整屏只有一句，字随机散落，唱到哪个字哪个字就亮起来，
 * 换句时整句淡出、下一句再逐字浮现，不做纵向滚动。
 *
 * 交互：
 * - 3.2 秒无操作后收起控件，点屏幕唤回；
 * - 顶部返回箭头或下滑超过 90dp 回到播放面板；
 * - 顶部「正在匹配最佳歌词」胶囊 2.6 秒后自动淡出。
 */
@Composable
fun ScatterLyricStage(
  lines: List<LyricLine>,
  positionMs: Long,
  durationMs: Long,
  isPlaying: Boolean,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val smoothPosition = rememberLyricPosition(positionMs = positionMs, isPlaying = isPlaying)

  // 舞台是沉浸层：进入时隐藏状态栏，离开时恢复，避免黑底上出现深色状态栏图标
  val view = LocalView.current
  DisposableEffect(Unit) {
    val window = (view.context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    controller?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller?.hide(WindowInsetsCompat.Type.systemBars())
    onDispose {
      controller?.show(WindowInsetsCompat.Type.systemBars())
    }
  }

  var controlsVisible by remember { mutableStateOf(true) }
  var controlsTick by remember { mutableIntStateOf(0) }
  var pillVisible by remember { mutableStateOf(true) }
  var dragged by remember { mutableFloatStateOf(0f) }
  val swipeThreshold = with(LocalDensity.current) { 90.dp.toPx() }

  // 每次点屏都重置自动隐藏的计时
  LaunchedEffect(controlsTick) {
    if (controlsVisible) {
      delay(3200)
      controlsVisible = false
    }
  }
  LaunchedEffect(Unit) {
    delay(2600)
    pillVisible = false
  }

  val controlsAlpha by animateFloatAsState(
    targetValue = if (controlsVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 260),
    label = "stageControls"
  )
  val pillAlpha by animateFloatAsState(
    targetValue = if (pillVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 260),
    label = "stagePill"
  )

  fun wakeControls() {
    controlsVisible = true
    controlsTick++
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(EchoInk)
      .pointerInput(Unit) {
        detectVerticalDragGestures(
          onDragEnd = {
            if (dragged > swipeThreshold) onBack()
            dragged = 0f
          },
          onDragCancel = { dragged = 0f },
          onVerticalDrag = { _, amount ->
            // 只统计向下的位移，向上滑不触发返回
            dragged = (dragged + amount).coerceAtLeast(0f)
          }
        )
      }
      .pointerInput(Unit) {
        detectTapGestures { wakeControls() }
      }
  ) {
    DustField()
    // 暗角：把视觉焦点收到屏幕中央的句子
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
          center = Offset(size.width * 0.5f, size.height * 0.45f),
          radius = size.width * 1.1f
        )
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 26.dp),
      contentAlignment = Alignment.Center
    ) {
      if (lines.isEmpty()) {
        Text(text = "这首歌还没有歌词", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
      } else {
        val activeIndex = activeLyricIndex(lines, smoothPosition)
        AnimatedContent(
          targetState = activeIndex,
          transitionSpec = {
            fadeIn(tween(420)).togetherWith(fadeOut(tween(420)))
          },
          label = "scatterLine"
        ) { index ->
          val line = lines.getOrNull(index)
          if (line == null) {
            Box(modifier = Modifier.size(0.dp))
          } else {
            ScatterLine(line = line, lineIndex = index, positionMs = smoothPosition)
          }
        }
      }
    }

    // 返回：独立点击区域，避免和「点屏唤回」冲突
    Box(
      modifier = Modifier
        .align(Alignment.TopStart)
        .statusBarsPadding()
        .padding(start = 12.dp, top = 6.dp)
        .size(38.dp)
        .clip(CircleShape)
        .graphicsLayer { alpha = controlsAlpha }
        .clickable(onClick = onBack),
      contentAlignment = Alignment.Center
    ) {
      ArrowBackIcon(modifier = Modifier.size(18.dp), color = Color.White.copy(alpha = 0.6f))
    }

    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 22.dp)
        .graphicsLayer { alpha = pillAlpha }
        .clip(RoundedCornerShape(17.dp))
        .background(Color(0xFF1C1E26).copy(alpha = 0.82f))
        .padding(horizontal = 15.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SparkleIcon(modifier = Modifier.size(13.dp), color = EchoAccent)
      Spacer(Modifier.width(7.dp))
      Text(
        text = "正在匹配最佳歌词…",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp
      )
    }

    StageProgress(
      positionMs = smoothPosition,
      durationMs = durationMs,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .graphicsLayer { alpha = controlsAlpha }
    )
  }
}

/** 一句散字歌词：每个字有固定的偏移和旋转，颜色随演唱进度推进 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScatterLine(
  line: LyricLine,
  lineIndex: Int,
  positionMs: Long
) {
  val glyphs = remember(lineIndex, line.text) { buildScatterGlyphs(line.text, lineIndex) }
  val fontSize = remember(glyphs.size) { scatterFontSizeSp(glyphs.size) }

  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    glyphs.forEachIndexed { index, glyph ->
      val visual = glyphVisualAt(
        positionMs = positionMs,
        startMs = line.timeMs,
        endMs = line.endMs,
        index = index,
        count = glyphs.size
      )
      val color = when (visual.phase) {
        GlyphPhase.SINGING -> EchoStageWarm
        GlyphPhase.SUNG -> Color.White
        GlyphPhase.WAITING -> Color.White.copy(alpha = 0.55f)
      }
      Text(
        text = glyph.text,
        color = color,
        fontSize = fontSize.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        softWrap = false,
        style = TextStyle(
          shadow = if (visual.phase == GlyphPhase.SINGING) {
            Shadow(color = EchoStageWarm.copy(alpha = 0.85f), blurRadius = 26f)
          } else {
            Shadow(color = Color.White.copy(alpha = 0.12f), blurRadius = 8f)
          }
        ),
        modifier = Modifier.graphicsLayer {
          translationX = glyph.dx.dp.toPx()
          translationY = glyph.dy.dp.toPx()
          rotationZ = glyph.rotation
          alpha = visual.alpha
          scaleX = visual.scale
          scaleY = visual.scale
        }
      )
    }
  }
}

/** 底部：时间 + 细进度线，颜色从品牌蓝过渡到暖橙 */
@Composable
private fun StageProgress(
  positionMs: Long,
  durationMs: Long,
  modifier: Modifier = Modifier
) {
  val fraction = if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
  } else {
    0f
  }
  Column(
    modifier = modifier
      .navigationBarsPadding()
      .padding(start = 26.dp, end = 26.dp, bottom = 26.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = formatDuration(positionMs),
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
      )
      Spacer(Modifier.weight(1f))
      Text(
        text = formatDuration(durationMs),
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
      )
    }
    Spacer(Modifier.height(8.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .clip(RoundedCornerShape(1.dp))
        .background(Color.White.copy(alpha = 0.14f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fraction)
          .height(2.dp)
          .background(Brush.horizontalGradient(listOf(EchoAccent, EchoStageWarm)))
      )
    }
  }
}

/** 背景星尘：白色为主，少量暖色余烬，缓慢上浮 */
private data class Dust(
  val x: Float,
  val y: Float,
  val radius: Float,
  val speed: Float,
  val drift: Float,
  val alpha: Float,
  val warm: Boolean
)

@Composable
private fun DustField(modifier: Modifier = Modifier) {
  val dusts = remember {
    val random = kotlin.random.Random(20261113)
    List(70) {
      Dust(
        x = random.nextFloat(),
        y = random.nextFloat(),
        radius = 0.4f + random.nextFloat() * 1.4f,
        speed = 0.01f + random.nextFloat() * 0.05f,
        drift = (random.nextFloat() - 0.5f) * 0.02f,
        alpha = 0.18f + random.nextFloat() * 0.55f,
        warm = random.nextFloat() < 0.16f
      )
    }
  }
  var clock by remember { mutableLongStateOf(0L) }

  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos { frame -> clock = frame }
    }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    // 读取 clock 让 Canvas 每帧重绘，但不会触发整棵 UI 树重组
    val seconds = clock / 1_000_000_000f
    dusts.forEach { dust ->
      val y = ((dust.y - seconds * dust.speed) % 1f + 1f) % 1f
      val x = ((dust.x + seconds * dust.drift) % 1f + 1f) % 1f
      drawCircle(
        color = if (dust.warm) {
          EchoStageWarm.copy(alpha = dust.alpha)
        } else {
          Color.White.copy(alpha = dust.alpha)
        },
        radius = dust.radius.dp.toPx(),
        center = Offset(x * size.width, y * size.height)
      )
    }
  }
}
