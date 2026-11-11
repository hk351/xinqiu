package com.dlust.ydhl2026.ui.player.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.lyrics.activeLineIndex
import com.dlust.ydhl2026.data.lyrics.lineEndMs
import com.dlust.ydhl2026.data.lyrics.wordsOrSpread
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.LyricWord
import com.dlust.ydhl2026.ui.player.PlayerPalette

/** 逐字时间段：词在开始前 150ms 开始入场 */
private const val LOOKAHEAD_MS = 150L

/** 主歌词字号，一行大约放得下 12 个汉字 */
private val LineFontSize = 26.sp

/**
 * 「逐字」样式：当前行按词播放，未唱的词隐藏，正在唱的词放大并发光，唱过的词回落到常态。
 * 视觉参照 Folia Classic，用 Compose 逐帧计算而不是照搬它的代码。
 */
@Composable
internal fun ClassicLyrics(
  lines: List<LyricLine>,
  positionMs: Long,
  palette: PlayerPalette,
  modifier: Modifier = Modifier
) {
  val activeIndex = lines.activeLineIndex(positionMs)

  // 整屏歌词缓慢上下浮动，避免画面完全静止
  val breath by rememberInfiniteTransition(label = "classicBreath").animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7_000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "classicBreathValue"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer {
        translationY = breath * 7.dp.toPx()
        val breathScale = 1f + 0.01f * breath
        scaleX = breathScale
        scaleY = breathScale
      },
    contentAlignment = Alignment.Center
  ) {
    AnimatedContent(
      targetState = activeIndex,
      transitionSpec = {
        (
          fadeIn(tween(durationMillis = 220)) +
            scaleIn(initialScale = 0.9f, animationSpec = tween(durationMillis = 220))
          ) togetherWith (
          fadeOut(tween(durationMillis = 300)) +
            scaleOut(targetScale = 1.08f, animationSpec = tween(durationMillis = 300))
          )
      },
      label = "classicActiveLine"
    ) { index ->
      if (index in lines.indices) {
        ClassicLine(
          line = lines[index],
          lineEnd = lines.lineEndMs(index),
          positionMs = positionMs,
          palette = palette
        )
      } else {
        // 前奏/间奏没有歌词可显示，占位保持高度稳定
        Spacer(Modifier.height(120.dp))
      }
    }
  }
}

@Composable
private fun ClassicLine(
  line: LyricLine,
  lineEnd: Long,
  positionMs: Long,
  palette: PlayerPalette
) {
  val words = remember(line, lineEnd) { line.wordsOrSpread(lineEnd) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    WordRow(words = words, positionMs = positionMs, palette = palette)

    if (!line.translation.isNullOrBlank()) {
      Spacer(Modifier.height(16.dp))
      Text(
        text = line.translation,
        color = palette.textSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRow(
  words: List<LyricWord>,
  positionMs: Long,
  palette: PlayerPalette
) {
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.Center,
    verticalArrangement = Arrangement.Center
  ) {
    words.forEachIndexed { index, word ->
      key(index) {
        ClassicWord(
          word = word,
          wordIndex = index,
          visual = wordVisualAt(
            startMs = word.startMs,
            endMs = word.endMs,
            positionMs = positionMs,
            lookaheadMs = LOOKAHEAD_MS
          ),
          palette = palette
        )
      }
    }
  }
}

@Composable
private fun ClassicWord(
  word: LyricWord,
  wordIndex: Int,
  visual: WordVisual,
  palette: PlayerPalette
) {
  val scale = wordScale(visual)
  val alpha = wordAlpha(visual)
  // 未唱时有一点随机偏移，入场时归位；偏移按词序号固定，不会逐帧抖动
  val offset = wordWaitingOffset(wordIndex) * 8f * (1f - visual.appear)
  val color = lerp(palette.text, palette.primary, visual.emphasis)

  Box(
    modifier = Modifier.graphicsLayer {
      scaleX = scale
      scaleY = scale
      this.alpha = alpha
      translationX = offset.dp.toPx()
    }
  ) {
    if (visual.glow > 0.01f) {
      // 辉光层：同文本模糊一圈垫在下面，模拟 Folia 的文字外发光。
      // 必须用 Unbounded，默认的 Rectangle 会把模糊层裁成方形色块；
      // 也不用 TextStyle.shadow，大半径阴影在模拟器上同样会出现方块。
      Text(
        text = word.text,
        color = palette.primary.copy(alpha = 0.65f * visual.glow),
        fontSize = LineFontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.blur(
          radius = 10.dp * visual.glow,
          edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
      )
    }
    Text(
      text = word.text,
      color = color,
      fontSize = LineFontSize,
      fontWeight = FontWeight.Bold
    )
  }
}
