package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song

/** 测试替身：按歌名返回预设歌词，不联网 */
class FakeOnlineLyricsSource(
  private val byTitle: Map<String, List<LyricLine>> = emptyMap(),
  private val error: Throwable? = null
) : OnlineLyricsSource {

  val lookedUpTitles = mutableListOf<String>()

  override suspend fun find(song: Song): List<LyricLine> {
    lookedUpTitles += song.title
    error?.let { throw it }
    return byTitle[song.title] ?: emptyList()
  }
}
