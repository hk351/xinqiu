package com.dlust.ydhl2026.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 网易云歌词 JSON 的解析测试：制作信息行过滤、纯音乐与空响应的兜底。
 */
class NeteaseLyricJsonTest {

  @Test
  fun `解析普通歌词并过滤制作信息行`() {
    val json = """
      {"code":200,"lrc":{"lyric":"[00:00.000]作词 : 罗言\n[00:00.500]作词Lyrics by : 罗言\n[00:01.000]故事的小黄花\n[00:03.000]从出生那年就飘着"}}
    """.trimIndent()

    val lines = NeteaseLyricJson.parse(json)

    assertEquals(listOf("故事的小黄花", "从出生那年就飘着"), lines.map { it.text })
    assertEquals(1_000L, lines.first().timeMs)
  }

  @Test
  fun `纯音乐与空歌词都当作没有歌词`() {
    assertTrue(NeteaseLyricJson.parse("""{"lrc":{"lyric":"纯音乐，请欣赏"}}""").isEmpty())
    assertTrue(NeteaseLyricJson.parse("""{"lrc":{"lyric":""}}""").isEmpty())
    assertTrue(NeteaseLyricJson.parse("""{"code":200}""").isEmpty())
  }
}
