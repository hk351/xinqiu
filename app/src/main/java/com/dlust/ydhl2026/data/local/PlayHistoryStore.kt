package com.dlust.ydhl2026.data.local

import android.content.Context
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 最近播放的抽象。
 * 播放器只依赖这个接口，测试里可以塞一个纯内存实现，不必碰 SharedPreferences。
 */
interface PlayHistory {
  val recent: StateFlow<List<Song>>
  fun record(song: Song)
}

/**
 * 最近播放记录。
 *
 * 只存歌曲快照（不含歌词），够首页「最近」Tab 展示与再次播放；
 * 用 SharedPreferences 存一份 JSON 数组，避免为这点数据引入数据库。
 */
class PlayHistoryStore(context: Context) : PlayHistory {

  private val prefs = context.getSharedPreferences("play_history", Context.MODE_PRIVATE)

  private val _recent = MutableStateFlow(load())
  override val recent: StateFlow<List<Song>> = _recent.asStateFlow()

  /** 记录一次播放：同一首歌移到最前，最多保留 [MAX_SIZE] 首 */
  override fun record(song: Song) {
    val next = (listOf(song) + _recent.value.filterNot { it.id == song.id }).take(MAX_SIZE)
    _recent.value = next
    prefs.edit().putString(KEY, toJson(next)).apply()
  }

  private fun load(): List<Song> = runCatching {
    val raw = prefs.getString(KEY, null) ?: return emptyList()
    val array = JSONArray(raw)
    buildList {
      for (index in 0 until array.length()) {
        array.optJSONObject(index)?.let(::toSong)?.let { add(it) }
      }
    }
  }.getOrDefault(emptyList())

  private fun toJson(songs: List<Song>): String {
    val array = JSONArray()
    songs.forEach { song ->
      array.put(
        JSONObject().apply {
          put("id", song.id)
          put("title", song.title)
          put("artist", song.artist)
          put("album", song.album ?: "")
          put("albumArtUrl", song.albumArtUrl ?: "")
          put("durationMs", song.durationMs)
          put("source", song.source)
          put("sourceUrl", song.sourceUrl)
          put("lastModifiedAt", song.lastModifiedAt)
          put("mimeType", song.mimeType ?: "")
          put("fileName", song.fileName ?: "")
        }
      )
    }
    return array.toString()
  }

  private fun toSong(json: JSONObject): Song? {
    val id = json.optString("id")
    val title = json.optString("title")
    if (id.isEmpty() || title.isEmpty()) return null
    return Song(
      id = id,
      title = title,
      artist = json.optString("artist"),
      album = json.optString("album").takeIf { it.isNotEmpty() },
      albumArtUrl = json.optString("albumArtUrl").takeIf { it.isNotEmpty() },
      durationMs = json.optLong("durationMs"),
      source = json.optString("source"),
      sourceUrl = json.optString("sourceUrl"),
      lastModifiedAt = json.optLong("lastModifiedAt"),
      mimeType = json.optString("mimeType").takeIf { it.isNotEmpty() },
      fileName = json.optString("fileName").takeIf { it.isNotEmpty() }
    )
  }

  private companion object {
    const val KEY = "songs"
    const val MAX_SIZE = 20
  }
}
