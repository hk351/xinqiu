package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.local.PlayHistoryStore
import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.remote.ChartCache
import com.dlust.ydhl2026.data.remote.NeteaseApi
import com.dlust.ydhl2026.data.remote.NeteaseJson
import kotlinx.coroutines.flow.Flow

/**
 * 接入网易云榜单的真实数据源。
 *
 * 数据只走一条链路：先看今天的缓存 → 没有就请求接口 → 请求失败退回最近一份缓存。
 * 这样离线也能看到「上一次成功拉到的榜」，不会白屏。
 */
class NeteaseMusicRepository(
  private val api: NeteaseApi,
  private val cache: ChartCache,
  private val history: PlayHistoryStore
) : MusicRepository {

  private val chartPlaylists: List<Playlist> = listOf(
    playlist(NeteaseApi.CHART_HOT, "热歌榜", 200),
    playlist(NeteaseApi.CHART_RISING, "飙升榜", 100),
    playlist(NeteaseApi.CHART_NEW, "新歌榜", 100)
  )

  override suspend fun charts(): List<Playlist> = chartPlaylists

  override suspend fun chartTracks(chartId: Long): List<ChartTrack> {
    cache.loadToday(chartId)?.let { cached ->
      NeteaseJson.parseChart(cached)?.let { return it.tracks }
    }
    val raw = api.chartRaw(chartId)
    if (raw != null) {
      cache.save(chartId, raw)
      NeteaseJson.parseChart(raw)?.let { return it.tracks }
    }
    // 断网或接口挂了：退回最近一份缓存，仍解析失败就当没有数据
    return cache.loadLatest(chartId)
      ?.let(NeteaseJson::parseChart)
      ?.tracks
      .orEmpty()
  }

  override suspend fun recommended(): List<ChartTrack> =
    chartTracks(NeteaseApi.CHART_HOT).take(RECOMMEND_LIMIT)

  override fun recentSongs(): Flow<List<Song>> = history.recent

  override suspend fun search(query: String): List<Song> = api.search(query)

  override suspend fun playUrl(song: Song): String? =
    if (song.source == SOURCE_NETEASE) api.playUrl(song.id) else song.sourceUrl

  private fun playlist(id: Long, name: String, songCount: Int) = Playlist(
    id = id,
    name = name,
    coverUrl = null,
    songCount = songCount,
    createdAt = 0L,
    lastModifiedAt = 0L
  )

  private companion object {
    const val SOURCE_NETEASE = "netease"

    /**
     * 推荐页只取前 60 名：画布是无限补卡的，给太多只会让同一批歌在屏幕上重复得更密。
     * 60 首足够铺满好几屏不重样。
     */
    const val RECOMMEND_LIMIT = 60
  }
}
