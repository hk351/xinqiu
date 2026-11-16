package com.dlust.ydhl2026.ui.player.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import kotlin.math.floor
import kotlin.math.sin

/**
 * 散字歌词舞台的纯计算部分。
 *
 * 和 [LyricClock] 一样刻意不依赖 Compose：字号、字偏移、逐字明暗都在这里算好，
 * UI 层只负责画，测试可以固定住「唱到第几个字时应该是什么状态」。
 */

/** 一个字在屏幕上的固定散落参数，单位是 dp / 度 */
data class ScatterGlyph(
  val text: String,
  val dx: Float,
  val dy: Float,
  val rotation: Float
)

/** 逐字三态：还没唱 / 正在唱 / 已唱完 */
enum class GlyphPhase { WAITING, SINGING, SUNG }

/** 某个字在当前时刻的视觉参数 */
data class GlyphVisual(
  val phase: GlyphPhase,
  val alpha: Float,
  val scale: Float
)

/** 用「句序号 + 字序号」生成 0..1 的稳定伪随机，避免每帧乱跳 */
internal fun seededUnit(value: Int): Float {
  val raw = sin(value * 12.9898 + 78.233) * 43758.5453
  return (raw - floor(raw)).toFloat()
}

/**
 * 长句的随机偏移要收敛：字数越多，单字可用宽度越小，
 * 保持原幅度会让相邻字互相压叠、整行被挤出屏幕。
 */
internal fun scatterOffsetScale(charCount: Int): Float = when {
  charCount <= 8 -> 1f
  charCount <= 12 -> 0.7f
  else -> 0.5f
}

/**
 * 把一句歌词拆成逐字散落布局。
 * 同一句每次得到的位置完全一致，所以重绘、旋转屏幕都不会抖。
 */
fun buildScatterGlyphs(text: String, lineIndex: Int): List<ScatterGlyph> {
  val scale = scatterOffsetScale(text.length)
  return text.mapIndexed { index, char ->
    ScatterGlyph(
      text = char.toString(),
      dx = (seededUnit(lineIndex * 31 + index * 7) - 0.5f) * 16f * scale,
      dy = (seededUnit(lineIndex * 17 + index * 13) - 0.5f) * 22f * scale,
      rotation = (seededUnit(lineIndex * 11 + index * 5) - 0.5f) * 12f * scale
    )
  }
}

/** 字号随字数递减，保证长句也能落在屏幕内 */
fun scatterFontSizeSp(charCount: Int): Float = when {
  charCount <= 4 -> 62f
  charCount == 5 -> 56f
  charCount == 6 -> 48f
  charCount == 7 -> 42f
  charCount == 8 -> 38f
  charCount == 9 -> 34f
  charCount <= 10 -> 29f
  charCount <= 12 -> 25f
  charCount <= 15 -> 22f
  else -> 20f
}

/**
 * 当前句在整份歌词里的下标。
 * 返回 -1 表示还没到第一句，UI 此时留空而不是强行显示第一句。
 */
fun activeLyricIndex(lines: List<LyricLine>, positionMs: Long): Int =
  lines.indexOfLast { it.timeMs <= positionMs }

/**
 * 逐字状态推进。
 *
 * - 整句所有字在句首后的 45ms 间隔内依次淡入（预览态），不需要等到唱到才出现；
 * - 每个字的演唱时间 = 整句时长 / 字数，用于把「正在唱」推进到下一个字；
 * - 句尾时间缺失时按每字 600ms 兜底，避免时长 0 导致全部瞬间变成已唱。
 */
fun glyphVisualAt(
  positionMs: Long,
  startMs: Long,
  endMs: Long,
  index: Int,
  count: Int
): GlyphVisual {
  if (count <= 0) return GlyphVisual(GlyphPhase.WAITING, 0f, 1f)

  val duration = if (endMs > startMs) endMs - startMs else 600L * count
  val slot = duration.toFloat() / count
  val charStart = startMs + (slot * index).toLong()
  val charEnd = startMs + (slot * (index + 1)).toLong()

  // 依次浮现：最后几个字可能在一秒内全部出现，符合「从虚空中逐字浮现」的观感
  val appearStart = startMs + index * 45L
  val alpha = ((positionMs - appearStart).toFloat() / 280f).coerceIn(0f, 1f)

  val phase = when {
    positionMs >= charEnd -> GlyphPhase.SUNG
    positionMs >= charStart -> GlyphPhase.SINGING
    else -> GlyphPhase.WAITING
  }
  val scale = if (phase == GlyphPhase.SINGING) 1f + 0.06f * alpha else 1f

  return GlyphVisual(phase = phase, alpha = alpha, scale = scale)
}
