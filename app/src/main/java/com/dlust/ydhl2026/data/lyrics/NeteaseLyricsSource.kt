package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 网易云音乐的公开搜索/歌词接口实现。
 *
 * 说明：这不是官方开放 API，只是网页端在用的接口，可能随时调整或限流；
 * 失败时一律返回空列表，界面表现为「这首歌还没有歌词」，不会影响播放。
 * 后续接上自建 Navidrome 后，可以换成 Subsonic 的 getLyricsBySongId 作为稳定来源。
 */
class NeteaseLyricsSource(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OnlineLyricsSource {

  override suspend fun find(song: Song): List<LyricLine> = withContext(ioDispatcher) {
    val keyword = buildSearchKeyword(song.title, song.artist) ?: return@withContext emptyList()
    val candidates = runCatching { search(keyword) }.getOrDefault(emptyList())
    val best = pickBestMatch(candidates, song.title, song.durationMs)
      ?: return@withContext emptyList()
    runCatching { fetchLyric(best.id) }.getOrDefault(emptyList())
  }

  private fun search(keyword: String): List<LyricCandidate> {
    val encoded = URLEncoder.encode(keyword, "UTF-8")
    val json = getJson("$SEARCH_URL?s=$encoded&type=1&limit=10&offset=0")
    val songs = JSONObject(json)
      .optJSONObject("result")
      ?.optJSONArray("songs")
      ?: return emptyList()
    return buildList {
      for (index in 0 until songs.length()) {
        val item = songs.optJSONObject(index) ?: continue
        val id = item.optLong("id")
        val name = item.optString("name").trim()
        if (id <= 0L || name.isEmpty()) continue
        val artist = item.optJSONArray("artists")
          ?.optJSONObject(0)
          ?.optString("name")
          .orEmpty()
        add(LyricCandidate(id, name, artist, item.optLong("duration")))
      }
    }
  }

  private fun fetchLyric(id: Long): List<LyricLine> {
    val json = getJson("$LYRIC_URL?id=$id&lv=-1&tv=-1&kv=-1")
    val raw = JSONObject(json).optJSONObject("lrc")?.optString("lyric").orEmpty()
    // 纯音乐没有可展示的歌词，直接当作没找到
    if (raw.isBlank() || raw.contains(PURE_MUSIC_MARK)) return emptyList()
    return LrcParser.parse(raw)
  }

  private fun getJson(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
      connection.connectTimeout = CONNECT_TIMEOUT_MS
      connection.readTimeout = READ_TIMEOUT_MS
      connection.setRequestProperty("User-Agent", USER_AGENT)
      connection.setRequestProperty("Referer", "https://music.163.com/")
      connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
      connection.disconnect()
    }
  }

  private companion object {
    const val SEARCH_URL = "https://music.163.com/api/search/get"
    const val LYRIC_URL = "https://music.163.com/api/song/lyric"
    const val PURE_MUSIC_MARK = "纯音乐"
    const val CONNECT_TIMEOUT_MS = 5_000
    const val READ_TIMEOUT_MS = 8_000
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) Echo/1.0"
  }
}
