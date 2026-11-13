package com.dlust.ydhl2026.ui.player.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.absoluteValue

/** 散字舞台的纯逻辑测试：布局稳定、字号递减、逐字三态推进 */
class ScatterLineTest {

  @Test
  fun `同一句的散落布局每次都一样`() {
    val first = buildScatterGlyphs("把回声留在夜里", lineIndex = 3)
    val second = buildScatterGlyphs("把回声留在夜里", lineIndex = 3)

    assertEquals(first, second)
    assertEquals("把回声留在夜里".length, first.size)
    assertEquals("把", first.first().text)
  }

  @Test
  fun `散落偏移落在设计范围内`() {
    val glyphs = buildScatterGlyphs("屏幕亮起又暗像潮水反复", lineIndex = 7)

    glyphs.forEach { glyph ->
      assertTrue(glyph.dx.absoluteValue <= 8f)
      assertTrue(glyph.dy.absoluteValue <= 11f)
      assertTrue(glyph.rotation.absoluteValue <= 6f)
    }
  }

  @Test
  fun `字号随字数递减且不为零`() {
    var previous = Float.MAX_VALUE
    (2..20).forEach { count ->
      val size = scatterFontSizeSp(count)
      assertTrue(size > 0f)
      assertTrue(size <= previous + 0.001f)
      previous = size
    }
  }

  @Test
  fun `还没到第一句时不显示任何内容`() {
    val lines = listOf(
      LyricLine(timeMs = 1_000, text = "第一句", endMs = 2_000),
      LyricLine(timeMs = 2_000, text = "第二句", endMs = 3_000)
    )

    assertEquals(-1, activeLyricIndex(lines, positionMs = 500))
    assertEquals(0, activeLyricIndex(lines, positionMs = 1_000))
    assertEquals(1, activeLyricIndex(lines, positionMs = 2_500))
    assertEquals(1, activeLyricIndex(lines, positionMs = 9_999))
  }

  @Test
  fun `句首之前每个字都不可见`() {
    val visual = glyphVisualAt(
      positionMs = 900,
      startMs = 1_000,
      endMs = 2_000,
      index = 0,
      count = 4
    )

    assertEquals(GlyphPhase.WAITING, visual.phase)
    assertEquals(0f, visual.alpha, 0.001f)
  }

  @Test
  fun `唱到第几个字就把第几个字点亮`() {
    // 4 个字、总时长 4000ms：每个字占 1000ms
    val first = glyphVisualAt(1_100, 1_000, 5_000, index = 0, count = 4)
    val second = glyphVisualAt(2_100, 1_000, 5_000, index = 1, count = 4)
    val third = glyphVisualAt(2_100, 1_000, 5_000, index = 2, count = 4)

    assertEquals(GlyphPhase.SINGING, first.phase)
    assertEquals(GlyphPhase.SINGING, second.phase)
    assertEquals(GlyphPhase.WAITING, third.phase)
    // 句首 100ms 时第一个字还在淡入，但已经明显可见
    assertTrue(first.alpha > 0.3f)
    assertTrue(first.alpha < 1f)
  }

  @Test
  fun `唱完的字保持已唱并回落到原尺寸`() {
    val visual = glyphVisualAt(4_600, 1_000, 5_000, index = 0, count = 4)

    assertEquals(GlyphPhase.SUNG, visual.phase)
    assertEquals(1f, visual.alpha, 0.001f)
    assertEquals(1f, visual.scale, 0.001f)
  }

  @Test
  fun `正在唱的字有轻微放大`() {
    val visual = glyphVisualAt(1_500, 1_000, 5_000, index = 0, count = 4)

    assertTrue(visual.scale > 1f)
    assertTrue(visual.scale <= 1.06f)
  }

  @Test
  fun `句尾时间缺失时按每字固定时长兜底`() {
    val visual = glyphVisualAt(
      positionMs = 2_500,
      startMs = 1_000,
      endMs = 1_000,
      index = 0,
      count = 4
    )

    assertEquals(GlyphPhase.SUNG, visual.phase)
  }
}
