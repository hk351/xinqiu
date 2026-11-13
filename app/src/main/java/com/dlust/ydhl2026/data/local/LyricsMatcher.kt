package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.lyrics.LrcParser
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song

/**
 * 把 .lrc 索引挂到歌曲上：按文件名（不含扩展名、忽略大小写）配对。
 * 匹配不上就保留歌曲原有歌词，纯函数方便单测。
 */
fun attachLyrics(songs: List<Song>, index: Map<String, List<LyricLine>>): List<Song> {
  if (index.isEmpty()) return songs
  return songs.map { song ->
    val key = songFileNameKey(song.fileName)
    val lines = key?.let(index::get)
    if (lines.isNullOrEmpty()) song else song.copy(lyrics = lines)
  }
}

/** song.mp3 → song；没有文件名时返回 null */
internal fun songFileNameKey(fileName: String?): String? =
  fileName?.substringBeforeLast('.')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

/** 供扫描实现复用：.lrc 文件解析成歌词行，解析失败返回空列表 */
internal fun parseLrcText(raw: String): List<LyricLine> =
  runCatching { LrcParser.parse(raw) }.getOrDefault(emptyList())
