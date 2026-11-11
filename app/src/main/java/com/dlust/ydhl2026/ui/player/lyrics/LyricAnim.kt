package com.dlust.ydhl2026.ui.player.lyrics

/**
 * 逐字动画的纯计算部分。
 *
 * 与 Compose 无关，方便用单元测试固定节奏；样式层只负责把结果画出来。
 * 时间轴参照 Folia Classic：词在开始前一小段时间入场，开始后放大高亮，结束后回落并慢慢淡出。
 */

/** 词的三种状态 */
enum class WordPhase { WAITING, ACTIVE, PASSED }

/** 一个词在当前时刻的全部视觉参数，取值均已归一化 */
data class WordVisual(
  val phase: WordPhase,
  val appear: Float,
  val emphasis: Float,
  val glow: Float,
  val passedFor: Float
)

/** 入场动画时长：从「提前量」到完全可见 */
private const val APPEAR_MS = 400f
/** 高亮起势时长 */
private const val EMPHASIS_IN_MS = 120f
/** 高亮回落时长 */
private const val EMPHASIS_OUT_MS = 200f
/** 辉光起势与收尾时长 */
private const val GLOW_IN_MS = 160f
private const val GLOW_TAIL_MS = 900f

/**
 * @param lookaheadMs 提前多少毫秒开始做入场动画（节奏越快提前量越小）
 */
fun wordVisualAt(
  startMs: Long,
  endMs: Long,
  positionMs: Long,
  lookaheadMs: Long
): WordVisual {
  val phase = when {
    positionMs < startMs -> WordPhase.WAITING
    positionMs <= endMs -> WordPhase.ACTIVE
    else -> WordPhase.PASSED
  }

  val appearStart = startMs - lookaheadMs
  val appear = ((positionMs - appearStart).toFloat() / APPEAR_MS).coerceIn(0f, 1f)

  val emphasis = when (phase) {
    WordPhase.WAITING -> 0f
    WordPhase.ACTIVE -> ((positionMs - startMs).toFloat() / EMPHASIS_IN_MS).coerceIn(0f, 1f)
    WordPhase.PASSED -> 1f - ((positionMs - endMs).toFloat() / EMPHASIS_OUT_MS).coerceIn(0f, 1f)
  }

  val glow = when (phase) {
    WordPhase.WAITING -> 0f
    WordPhase.ACTIVE -> ((positionMs - startMs).toFloat() / GLOW_IN_MS).coerceIn(0f, 1f)
    WordPhase.PASSED -> 1f - ((positionMs - endMs).toFloat() / GLOW_TAIL_MS).coerceIn(0f, 1f)
  }

  val passedFor = if (phase == WordPhase.PASSED) {
    ((positionMs - endMs).toFloat() / GLOW_TAIL_MS).coerceIn(0f, 1f)
  } else {
    0f
  }

  return WordVisual(
    phase = phase,
    appear = easeOutCubic(appear),
    emphasis = easeOutCubic(emphasis),
    glow = glow,
    passedFor = passedFor
  )
}

/** 单词缩放：入场从 0.5 起，命中后放大到 1.4，结束后回到 1 */
fun wordScale(visual: WordVisual): Float =
  0.5f + 0.5f * visual.appear + 0.4f * visual.emphasis

/** 单词不透明度：未入场不可见，结束后回落到 0.82 */
fun wordAlpha(visual: WordVisual): Float =
  visual.appear * (1f - 0.18f * visual.passedFor)

/** 等待状态下的随机偏移，按词序号取固定值，重绘时不会跳动 */
fun wordWaitingOffset(wordIndex: Int): Float {
  val seeded = ((wordIndex * 12_989 + 78_233) % 1_000).toFloat() / 1_000f
  return (seeded - 0.5f) * 2f
}

private fun easeOutCubic(value: Float): Float {
  val inverse = 1f - value
  return 1f - inverse * inverse * inverse
}
