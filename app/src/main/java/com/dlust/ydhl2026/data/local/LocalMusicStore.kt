package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 本地音乐扫描结果的内存缓存。
 * 本地音乐页写入，首页入口卡读取歌曲数；App 进程存活期间有效，
 * 接 Room 后由数据库表替换。
 */
class LocalMusicStore {

  private val _songs = MutableStateFlow<List<Song>>(emptyList())
  val songs: StateFlow<List<Song>> = _songs.asStateFlow()

  fun replace(songs: List<Song>) {
    _songs.value = songs
  }
}
