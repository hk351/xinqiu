package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 在线歌词匹配的纯逻辑测试：搜索词拼接、候选挑选、制作信息行过滤。
 */
class OnlineLyricMatchTest {

  private fun candidate(id: Long, name: String, durationMs: Long, artist: String = "周杰伦") =
    LyricCandidate(id, name, artist, durationMs)

  private fun pick(
    candidates: List<LyricCandidate>,
    title: String,
    durationMs: Long,
    artist: String = "周杰伦"
  ) = pickBestMatch(candidates, title, artist, durationMs)

  @Test
  fun `搜索词包含歌名和歌手`() {
    assertEquals("晴天 周杰伦", buildSearchKeyword("晴天", "周杰伦"))
  }

  @Test
  fun `歌手缺失或为占位值时只搜歌名`() {
    assertEquals("晴天", buildSearchKeyword("晴天", "未知歌手"))
    assertEquals("晴天", buildSearchKeyword("晴天", "  "))
    assertEquals("晴天", buildSearchKeyword("晴天", "<unknown>"))
  }

  @Test
  fun `歌名为空时不搜索`() {
    assertNull(buildSearchKeyword("   ", "周杰伦"))
  }

  @Test
  fun `歌名完全相同的候选优先于包含匹配`() {
    val candidates = listOf(
      candidate(1, "晴天 (Live)", 269_000L),
      candidate(2, "晴天", 270_000L),
      candidate(3, "晴天娃娃", 200_000L)
    )
    assertEquals(2L, pick(candidates, "晴天", 269_000L)?.id)
  }

  @Test
  fun `同名候选里选时长最接近的`() {
    val candidates = listOf(
      candidate(1, "十年", 205_000L),
      candidate(2, "十年", 205_600L),
      candidate(3, "十年", 300_000L)
    )
    assertEquals(2L, pick(candidates, "十年", 205_500L)?.id)
  }

  @Test
  fun `忽略空格标点后仍能匹配`() {
    val candidates = listOf(candidate(7, "七里香-周杰伦", 299_000L))
    // 本地文件名没有歌手信息，包含匹配不因此被拒
    assertEquals(7L, pick(candidates, "七里香", 299_000L, artist = "未知歌手")?.id)
  }

  @Test
  fun `完全没有同名候选时返回空`() {
    val candidates = listOf(candidate(1, "稻香", 223_000L))
    assertNull(pick(candidates, "晴天", 269_000L))
  }

  @Test
  fun `候选为空时返回空`() {
    assertNull(pick(emptyList(), "晴天", 269_000L))
  }

  @Test
  fun `文件名只多出一个字母不算包含匹配`() {
    // demo-a 归一化后是 demoa，与 demo 只差一个字母，属于误配高发区
    val candidates = listOf(candidate(1, "Demo", 200_000L, artist = "群星"))
    assertNull(pick(candidates, "demo-a", 200_000L, artist = "未知歌手"))
  }

  @Test
  fun `歌手对不上时包含匹配被拒绝`() {
    val candidates = listOf(candidate(1, "晴天-某某翻唱", 269_000L, artist = "别人"))
    assertNull(pick(candidates, "晴天", 269_000L, artist = "周杰伦"))
  }

  @Test
  fun `时长差超过容忍值的候选被排除`() {
    // 40 秒的片段不配整首 3 分 46 秒的歌词
    val candidates = listOf(candidate(1, "夜曲", 226_000L))
    assertNull(pick(candidates, "夜曲", 40_000L))
  }

  @Test
  fun `任一方时长未知时不做时长排除`() {
    val candidates = listOf(candidate(1, "夜曲", 226_000L))
    assertEquals(1L, pick(candidates, "夜曲", 0L)?.id)
    val unknownCandidate = listOf(candidate(1, "夜曲", 0L))
    assertEquals(1L, pick(unknownCandidate, "夜曲", 40_000L)?.id)
  }

  @Test
  fun `作词作曲编曲等制作信息行会被过滤`() {
    assertTrue(isCreditLine("作词 : 某某"))
    assertTrue(isCreditLine("编曲：某某"))
    assertTrue(isCreditLine("  制作人: 某某"))
    assertTrue(isCreditLine("混音：某某"))

    val lines = listOf(
      LyricLine(timeMs = 0L, text = "作词 : 某某"),
      LyricLine(timeMs = 0L, text = "编曲：某某"),
      LyricLine(timeMs = 1_000L, text = "故事的小黄花")
    )
    assertEquals(listOf("故事的小黄花"), dropCreditLines(lines).map { it.text })
  }

  @Test
  fun `歌词正文里的冒号不会被误删`() {
    assertFalse(isCreditLine("他说：别走"))
    assertFalse(isCreditLine("晴天"))
    assertEquals(
      listOf("他说：别走"),
      dropCreditLines(listOf(LyricLine(timeMs = 1_000L, text = "他说：别走"))).map { it.text }
    )
  }
}
