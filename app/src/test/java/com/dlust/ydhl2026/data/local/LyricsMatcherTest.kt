package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 歌词配对测试：按文件名（忽略扩展名与大小写）把 .lrc 挂到歌曲上。
 */
class LyricsMatcherTest {

  private fun song(fileName: String?, lyrics: List<LyricLine> = emptyList()) = Song(
    id = "local-1",
    title = "十年",
    artist = "陈奕迅",
    durationMs = 205_000L,
    source = "local",
    sourceUrl = "content://media/external/audio/media/1",
    lastModifiedAt = 0L,
    fileName = fileName,
    lyrics = lyrics
  )

  private val lrc = listOf(LyricLine(timeMs = 1_000L, text = "如果那两个字没有颤抖"))

  @Test
  fun `文件名去扩展名并忽略大小写后配对`() {
    val result = attachLyrics(listOf(song("ShiNian.MP3")), mapOf("shinian" to lrc))
    assertEquals(lrc, result.single().lyrics)
  }

  @Test
  fun `目录层级不同但文件名相同也能配上`() {
    val index = mapOf("demo-a" to lrc)
    val result = attachLyrics(listOf(song("Demo-A.wav")), index)
    assertEquals(lrc, result.single().lyrics)
  }

  @Test
  fun `没有对应歌词时保留歌曲原有歌词`() {
    val origin = listOf(LyricLine(timeMs = 0L, text = "原有歌词"))
    val result = attachLyrics(listOf(song("demo-b.wav", origin)), mapOf("demo-a" to lrc))
    assertEquals(origin, result.single().lyrics)
  }

  @Test
  fun `歌曲没有文件名时不参与配对`() {
    val result = attachLyrics(listOf(song(null)), mapOf("shinian" to lrc))
    assertTrue(result.single().lyrics.isEmpty())
  }

  @Test
  fun `索引为空时原样返回`() {
    val songs = listOf(song("demo-a.wav"))
    assertEquals(songs, attachLyrics(songs, emptyMap()))
  }

  @Test
  fun `文件名提取规则`() {
    assertEquals("demo-a", songFileNameKey("Demo-A.wav"))
    assertEquals("带 空格", songFileNameKey("带 空格.flac"))
    assertNull(songFileNameKey(".lrc"))
    assertNull(songFileNameKey(null))
  }
}
