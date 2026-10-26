package com.dlust.ydhl2026.data.model

/**
 * 播放器状态（用于持久化到 DataStore，阶段五使用）。
 * 本轮只记录“上次播到哪”，不落盘。
 */
data class PlaybackState(
  val currentSongId: String?,
  val currentPositionMs: Long,
  val isPlaying: Boolean,
  val repeatMode: Int,        // 0=顺序 1=单曲 2=随机
  val lastModifiedAt: Long
)
