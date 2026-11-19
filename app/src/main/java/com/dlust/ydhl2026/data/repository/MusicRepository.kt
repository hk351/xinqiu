package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据访问的统一出口。
 * 上层（ViewModel）只依赖这个接口，不关心数据来自榜单接口、本地扫描还是播放历史。
 */
interface MusicRepository {

  /** 歌单 Tab：目前接入的是三个官方榜单 */
  suspend fun charts(): List<Playlist>

  /** 某个榜单的曲目，带名次 */
  suspend fun chartTracks(chartId: Long): List<ChartTrack>

  /** 推荐页：热歌榜按名次排好的曲目 */
  suspend fun recommended(): List<ChartTrack>

  /** 最近播放（本地记录，按时间倒序） */
  fun recentSongs(): Flow<List<Song>>

  /** 搜索歌曲（按歌名 / 歌手 / 专辑） */
  suspend fun search(query: String): List<Song>

  /** 解析可播放地址；本地歌曲直接返回自身路径，网络歌曲需要现取 */
  suspend fun playUrl(song: Song): String?
}
