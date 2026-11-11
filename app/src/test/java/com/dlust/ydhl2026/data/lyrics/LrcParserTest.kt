package com.dlust.ydhl2026.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LRC 解析与逐字时间轴的单元测试 */
class LrcParserTest {

  @Test
  fun `解析普通 LRC 并按时间排序`() {
    val lines = LrcParser.parse(
      """
      [ti:测试]
      [00:05.50]第二句
      [00:01.00]第一句
      """.trimIndent()
    )

    assertEquals(2, lines.size)
    assertEquals(1_000L, lines[0].timeMs)
    assertEquals("第一句", lines[0].text)
    assertEquals(5_500L, lines[1].timeMs)
    // 行结束时间自动补成下一行开始时间
    assertEquals(5_500L, lines[0].endMs)
  }

  @Test
  fun `一行多个时间标签会展开成多行`() {
    val lines = LrcParser.parse("[00:01.00][00:03.00]副歌")

    assertEquals(2, lines.size)
    assertEquals(listOf(1_000L, 3_000L), lines.map { it.timeMs })
    assertEquals(listOf("副歌", "副歌"), lines.map { it.text })
  }

  @Test
  fun `支持不带毫秒的时间标签`() {
    val lines = LrcParser.parse("[01:02]整秒")

    assertEquals(62_000L, lines.single().timeMs)
  }

  @Test
  fun `增强 LRC 解析出逐字时间`() {
    val lines = LrcParser.parse("[00:10.00]<00:10.00>你<00:10.50>好<00:11.20>世界")
    val line = lines.single()

    assertEquals("你好世界", line.text)
    assertEquals(3, line.words.size)
    assertEquals(10_000L, line.words[0].startMs)
    assertEquals(10_500L, line.words[0].endMs)
    assertEquals(10_500L, line.words[1].startMs)
    assertEquals(11_200L, line.words[1].endMs)
    // 最后一个词的结束时间对齐到行结束
    assertEquals(line.endMs, line.words[2].endMs)
  }

  @Test
  fun `同时间戳的第二行作为译文合并`() {
    val lines = LrcParser.parse(
      """
      [00:02.00]hello world
      [00:02.00]你好世界
      [00:02.00]hello world
      """.trimIndent()
    )

    assertEquals(1, lines.size)
    assertEquals("hello world", lines.single().text)
    assertEquals("你好世界", lines.single().translation)
  }

  @Test
  fun `offset 标签整体平移时间`() {
    val lines = LrcParser.parse(
      """
      [offset:-500]
      [00:01.00]提前半秒
      """.trimIndent()
    )

    assertEquals(500L, lines.single().timeMs)
  }

  @Test
  fun `空文本与纯元信息不会产出歌词`() {
    assertTrue(LrcParser.parse("").isEmpty())
    assertTrue(LrcParser.parse("[ar:歌手]\n[al:专辑]").isEmpty())
  }

  @Test
  fun `切词拼回去与原文一致`() {
    val text = "我们 via via，走吧！"
    assertEquals(text, splitTokens(text).joinToString(""))
  }

  @Test
  fun `没有逐字数据时按字数均分整行`() {
    val line = LrcParser.parse("[00:00.00]一二三四").single()
    val words = line.wordsOrSpread(line.endMs)

    assertEquals("一二三四", words.joinToString("") { it.text })
    assertEquals(0L, words.first().startMs)
    assertEquals(line.endMs, words.last().endMs)
    // 时间轴单调递增且每段都有正时长
    words.zipWithNext().forEach { (a, b) -> assertTrue(b.startMs >= a.endMs) }
    words.forEach { assertTrue(it.endMs > it.startMs) }
  }

  @Test
  fun `当前行索引定位`() {
    val lines = LrcParser.parse(
      """
      [00:01.00]一
      [00:05.00]二
      [00:09.00]三
      """.trimIndent()
    )

    assertEquals(-1, lines.activeLineIndex(0))
    assertEquals(0, lines.activeLineIndex(1_000))
    assertEquals(0, lines.activeLineIndex(4_999))
    assertEquals(1, lines.activeLineIndex(5_000))
    assertEquals(2, lines.activeLineIndex(60_000))
  }
}
