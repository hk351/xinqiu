package com.dlust.ydhl2026.data.lyrics

import android.content.Context
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.LyricWord
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 歌词缓存抽象。
 * 网络歌取过一次词后写入缓存，之后重播、断网都不必再请求；测试用内存实现替换。
 */
interface LyricsCache {

  /** 没有可用缓存时返回 null（空列表不入缓存，避免把「这次没找到」永久钉住） */
  suspend fun get(songId: String): List<LyricLine>?

  /** 只缓存非空歌词；写盘失败不影响播放 */
  suspend fun put(songId: String, lines: List<LyricLine>)
}

/**
 * 按歌曲 id 存成 filesDir/lyrics/<id>.json。
 * 不塞 SharedPreferences：带逐字时间轴的歌词可能几十 KB，会把偏好文件撑大。
 */
class FileLyricsCache(
  context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LyricsCache {

  private val dir = File(context.filesDir, "lyrics")

  override suspend fun get(songId: String): List<LyricLine>? = withContext(ioDispatcher) {
    val file = File(dir, fileName(songId))
    if (!file.exists()) return@withContext null
    runCatching { decode(JSONArray(file.readText())) }
      .getOrNull()
      ?.takeIf { it.isNotEmpty() }
  }

  override suspend fun put(songId: String, lines: List<LyricLine>) {
    if (lines.isEmpty()) return
    withContext(ioDispatcher) {
      runCatching {
        dir.mkdirs()
        File(dir, fileName(songId)).writeText(encode(lines).toString())
      }
    }
  }

  /** 歌曲 id 里只保留文件名安全字符，避免 ncm-1/2 这类意外值写到目录外 */
  private fun fileName(songId: String): String =
    songId.filter { it.isLetterOrDigit() || it == '-' || it == '_' } + ".json"

  private fun encode(lines: List<LyricLine>): JSONArray = JSONArray().apply {
    lines.forEach { line ->
      put(JSONObject().apply {
        put("t", line.timeMs)
        put("e", line.endMs)
        put("x", line.text)
        line.translation?.let { put("tr", it) }
        if (line.words.isNotEmpty()) {
          put("w", JSONArray().apply {
            line.words.forEach { word ->
              put(JSONArray().put(word.startMs).put(word.endMs).put(word.text))
            }
          })
        }
      })
    }
  }

  private fun decode(array: JSONArray): List<LyricLine> = buildList {
    for (index in 0 until array.length()) {
      val obj = array.optJSONObject(index) ?: continue
      val words = obj.optJSONArray("w").toWords()
      add(
        LyricLine(
          timeMs = obj.optLong("t"),
          endMs = obj.optLong("e"),
          text = obj.optString("x"),
          words = words,
          translation = obj.optString("tr").takeIf { it.isNotEmpty() }
        )
      )
    }
  }

  private fun JSONArray?.toWords(): List<LyricWord> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val item = optJSONArray(index) ?: continue
        add(LyricWord(item.optLong(0), item.optLong(1), item.optString(2)))
      }
    }
  }
}
