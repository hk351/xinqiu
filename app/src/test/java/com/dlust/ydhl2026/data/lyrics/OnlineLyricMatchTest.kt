package com.dlust.ydhl2026.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 在线歌词匹配的纯逻辑测试：搜索词拼接与候选挑选。
 */
class OnlineLyricMatchTest {

  private fun candidate(id: Long, name: String, durationMs: Long, artist: String = "周杰伦") =
    LyricCandidate(id, name, artist, durationMs)

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
    assertEquals(2L, pickBestMatch(candidates, "晴天", 269_000L)?.id)
  }

  @Test
  fun `同名候选里选时长最接近的`() {
    val candidates = listOf(
      candidate(1, "十年", 205_000L),
      candidate(2, "十年", 205_600L),
      candidate(3, "十年", 300_000L)
    )
    assertEquals(2L, pickBestMatch(candidates, "十年", 205_500L)?.id)
  }

  @Test
  fun `忽略空格标点后仍能匹配`() {
    val candidates = listOf(candidate(7, "七里香-周杰伦", 299_000L))
    assertEquals(7L, pickBestMatch(candidates, "七里香", 299_000L)?.id)
  }

  @Test
  fun `完全没有同名候选时返回空`() {
    val candidates = listOf(candidate(1, "稻香", 223_000L))
    assertNull(pickBestMatch(candidates, "晴天", 269_000L))
  }

  @Test
  fun `候选为空时返回空`() {
    assertNull(pickBestMatch(emptyList(), "晴天", 269_000L))
  }
}
