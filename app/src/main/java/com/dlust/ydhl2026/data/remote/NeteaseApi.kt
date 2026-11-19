package com.dlust.ydhl2026.data.remote

import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Song
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 一次榜单拉取的结果：榜单名、更新时间、按名次排好的曲目 */
data class ChartSnapshot(
  val name: String,
  val updatedAt: Long,
  val tracks: List<ChartTrack>
)

/**
 * 网易云网页端接口的极简封装：榜单、搜索、播放地址。
 *
 * 这不是官方开放 API，可能随时调整或限流，所以每个方法失败都返回 null/空列表，
 * 由上层决定是展示缓存还是提示「稍后再试」，不影响应用其它功能。
 */
class NeteaseApi(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  /**
   * 拉取榜单原文；[chartId] 是歌单 id，网易云的官方榜就是固定 id 的公开歌单。
   * 返回原文而不是模型，是为了把同一份 JSON 直接落盘缓存。
   */
  suspend fun chartRaw(chartId: Long): String? = withContext(ioDispatcher) {
    runCatching { getJson(chartUrl(chartId)) }.getOrNull()
  }

  suspend fun search(keyword: String, limit: Int = SEARCH_LIMIT): List<Song> =
    withContext(ioDispatcher) {
      val query = keyword.trim()
      if (query.isEmpty()) return@withContext emptyList()
      runCatching { getJson(searchUrl(query, limit)) }
        .getOrNull()
        ?.let(NeteaseJson::parseSearch)
        .orEmpty()
    }

  /**
   * 解析某首歌的播放地址。
   * 榜单接口只给元数据，地址要单独请求；返回的链接带时效，不能缓存太久。
   */
  suspend fun playUrl(songId: String): String? = withContext(ioDispatcher) {
    runCatching { getJson(playUrlEndpoint(songId)) }
      .getOrNull()
      ?.let(NeteaseJson::parsePlayUrl)
  }

  private fun getJson(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
      connection.connectTimeout = CONNECT_TIMEOUT_MS
      connection.readTimeout = READ_TIMEOUT_MS
      connection.setRequestProperty("User-Agent", USER_AGENT)
      // 不带 Referer 会被接口拒绝
      connection.setRequestProperty("Referer", REFERER)
      connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
      connection.disconnect()
    }
  }

  private fun chartUrl(chartId: Long) = "$CHART_URL?id=$chartId&n=$CHART_LIMIT"

  private fun searchUrl(keyword: String, limit: Int) =
    "$SEARCH_URL?s=${URLEncoder.encode(keyword, "UTF-8")}&type=1&limit=$limit&offset=0"

  private fun playUrlEndpoint(songId: String): String {
    val rawId = songId.removePrefix(SONG_ID_PREFIX)
    val encodedIds = URLEncoder.encode("[$rawId]", "UTF-8")
    return "$PLAY_URL?id=$rawId&ids=$encodedIds&br=$BITRATE"
  }

  companion object {
    /** 网络歌的 id 前缀：与本地 content:// 的歌曲区分开，也方便反查真实 id */
    const val SONG_ID_PREFIX = "ncm-"

    const val CHART_HOT = 3778678L      // 云音乐热歌榜
    const val CHART_RISING = 19723756L  // 云音乐飙升榜
    const val CHART_NEW = 3779629L      // 云音乐新歌榜

    private const val CHART_URL = "https://music.163.com/api/playlist/detail"
    private const val SEARCH_URL = "https://music.163.com/api/search/get"
    private const val PLAY_URL = "https://music.163.com/api/song/enhance/player/url"
    private const val CHART_LIMIT = 100
    private const val SEARCH_LIMIT = 30
    private const val BITRATE = 128000
    private const val REFERER = "https://music.163.com/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) Echo/1.0"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000
  }
}

/**
 * 接口 JSON 到数据模型的纯函数。
 * 单独抽出来是为了能在 JVM 单测里直接喂固定 JSON 验证解析，不依赖网络和设备。
 */
object NeteaseJson {

  fun parseChart(json: String): ChartSnapshot? {
    val result = JSONObject(json).optJSONObject("result") ?: return null
    // 接口有两种形态：老版把歌单包在 result.playlist 里，现在直接平铺在 result 上
    val playlist = result.optJSONObject("playlist") ?: result
    val updatedAt = playlist.optLong("updateTime")
    val array = playlist.optJSONArray("tracks") ?: return null
    val tracks = buildList {
      for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val song = toSong(item, updatedAt) ?: continue
        add(ChartTrack(rank = index + 1, song = song))
      }
    }
    if (tracks.isEmpty()) return null
    return ChartSnapshot(
      name = playlist.optString("name"),
      updatedAt = updatedAt,
      tracks = tracks
    )
  }

  fun parseSearch(json: String): List<Song> {
    val songs = JSONObject(json).optJSONObject("result")?.optJSONArray("songs")
      ?: return emptyList()
    return buildList {
      for (index in 0 until songs.length()) {
        songs.optJSONObject(index)?.let { toSong(it, updatedAt = 0L) }?.let { add(it) }
      }
    }
  }

  fun parsePlayUrl(json: String): String? {
    val data = JSONObject(json).optJSONArray("data") ?: return null
    val url = data.optJSONObject(0)?.optString("url").orEmpty()
    // 无版权时接口会返回空串或 null，此时当作没有播放地址
    return url.takeIf { it.startsWith("http") }
  }

  private fun toSong(item: JSONObject, updatedAt: Long): Song? {
    val ncmId = item.optLong("id")
    val title = item.optString("name").trim()
    // 没有 id 或歌名的条目在搜索/榜单里都是无效数据，直接跳过
    if (ncmId <= 0L || title.isEmpty()) return null
    val album = item.optJSONObject("album")
    val artist = item.optJSONArray("artists")
      ?.optJSONObject(0)
      ?.optString("name")
      ?.trim()
      .orEmpty()
    return Song(
      id = NeteaseApi.SONG_ID_PREFIX + ncmId,
      title = title,
      artist = artist.ifEmpty { "未知歌手" },
      album = album?.optString("name")?.trim()?.takeIf { it.isNotEmpty() },
      // 接口给的是 http 链接，已在 network_security_config 里放行网易云 CDN
      albumArtUrl = album?.optString("picUrl")?.takeIf { it.startsWith("http") },
      durationMs = item.optLong("duration"),
      source = "netease",
      // 播放地址带时效，等用户真的点播时再解析
      sourceUrl = "",
      lastModifiedAt = updatedAt
    )
  }
}
