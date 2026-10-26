package com.dlust.ydhl2026.data.model

/**
 * 一个歌单。songCount 直接给 UI 展示“xx 首”，避免卡片再去查一次列表。
 */
data class Playlist(
  val id: Long,
  val name: String,
  val coverUrl: String? = null,
  val songCount: Int = 0,
  val createdAt: Long,
  val lastModifiedAt: Long
)
