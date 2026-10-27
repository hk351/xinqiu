package com.dlust.ydhl2026.data.model

/**
 * 一首歌。
 * 字段与课程原型文档的阶段一数据模型保持一致，方便后续接入 Room / 播放器。
 */
data class Song(
  val id: String,             // 本地路径 hash 或 URL
  val title: String,
  val artist: String,
  val album: String? = null,
  val albumArtUrl: String? = null,
  val durationMs: Long,       // 毫秒，便于进度条计算
  val source: String,         // "local" / "http" / "youtube"
  val sourceUrl: String,      // 本地路径或 URL，ExoPlayer 播放的就是它
  val lyrics: List<LyricLine> = emptyList(),  // 阶段五接入 LRC 解析前，先由 Mock 数据提供
  val lastModifiedAt: Long
)
