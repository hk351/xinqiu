package com.dlust.ydhl2026.ui.player.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.absoluteValue

/** 逐字动画节奏的单元测试：三态切换、缩放、辉光衰减 */
class LyricAnimTest {

  private val start = 10_000L
  private val end = 10_400L
  private val lookahead = 150L

  @Test
  fun `未开始时处于等待态且不可见`() {
    val visual = wordVisualAt(start, end, positionMs = 9_000, lookaheadMs = lookahead)

    assertEquals(WordPhase.WAITING, visual.phase)
    assertEquals(0f, visual.appear, 0.001f)
    assertEquals(0f, visual.glow, 0.001f)
    assertEquals(WordPhase.WAITING, wordVisualAt(start, end, 9_900, lookahead).phase)
  }

  @Test
  fun `提前量内开始入场`() {
    // 9500 时完全不可见，9950 时应该已经露出大半
    val visual = wordVisualAt(start, end, positionMs = 9_950, lookaheadMs = lookahead)

    assertEquals(WordPhase.WAITING, visual.phase)
    assertTrue(visual.appear > 0.5f)
    assertTrue(visual.appear < 1f)
  }

  @Test
  fun `唱到时放大高亮并发光`() {
    val visual = wordVisualAt(start, end, positionMs = 10_300, lookaheadMs = lookahead)

    assertEquals(WordPhase.ACTIVE, visual.phase)
    assertEquals(1f, visual.appear, 0.001f)
    assertEquals(1f, visual.emphasis, 0.001f)
    assertEquals(1f, visual.glow, 0.001f)
    assertEquals(1.4f, wordScale(visual), 0.001f)
    assertEquals(1f, wordAlpha(visual), 0.001f)
  }

  @Test
  fun `唱完后缩放回落、透明度降到常态`() {
    val visual = wordVisualAt(start, end, positionMs = 11_000, lookaheadMs = lookahead)

    assertEquals(WordPhase.PASSED, visual.phase)
    assertEquals(0f, visual.emphasis, 0.001f)
    assertEquals(1f, wordScale(visual), 0.001f)
    // 结束 600ms 后已经淡到接近常态的 0.82
    assertEquals(0.88f, wordAlpha(visual), 0.005f)
    // 辉光进入收尾阶段，已经比刚唱完时弱
    assertTrue(visual.glow < 0.5f)
  }

  @Test
  fun `辉光在结束后彻底消失`() {
    val visual = wordVisualAt(start, end, positionMs = end + 1_000, lookaheadMs = lookahead)

    assertEquals(0f, visual.glow, 0.001f)
    assertEquals(1f, visual.passedFor, 0.001f)
  }

  @Test
  fun `等待偏移固定且落在正负一之间`() {
    val first = wordWaitingOffset(3)
    assertEquals(first, wordWaitingOffset(3), 0f)
    assertTrue(first in -1f..1f)
    (0..20).forEach { assertTrue(wordWaitingOffset(it).absoluteValue <= 1f) }
  }
}
