package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.Song

/** 测试替身：返回预设的本地歌曲列表，不触碰 MediaStore */
class FakeLocalMusicSource(private val songs: List<Song>) : LocalMusicSource {

  var scanCount = 0
    private set

  override suspend fun scan(): List<Song> {
    scanCount++
    return songs
  }
}
