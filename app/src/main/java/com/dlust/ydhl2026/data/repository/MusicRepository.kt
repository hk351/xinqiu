package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据访问的统一出口。
 * 上层（ViewModel）只依赖这个接口，不关心数据是 Mock、本地 MP3 还是网络接口。
 */
interface MusicRepository {

  /** 我的歌单 */
  fun playlists(): Flow<List<Playlist>>

  /** 最近播放 */
  fun recentSongs(): Flow<List<Song>>

  /** 某个歌单里的歌曲 */
  fun songsOf(playlistId: Long): Flow<List<Song>>

  /** 搜索歌曲（按歌名 / 歌手 / 专辑） */
  suspend fun search(query: String): List<Song>
}
