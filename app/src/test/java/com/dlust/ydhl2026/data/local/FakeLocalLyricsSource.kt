package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.LyricLine

/** 测试替身：返回预设的 .lrc 索引，不触碰 SAF */
class FakeLocalLyricsSource(
  private val index: Map<String, List<LyricLine>> = emptyMap(),
  private val error: Throwable? = null
) : LocalLyricsSource {

  var loadCount = 0
    private set

  override suspend fun loadIndex(): Map<String, List<LyricLine>> {
    loadCount++
    error?.let { throw it }
    return index
  }
}
