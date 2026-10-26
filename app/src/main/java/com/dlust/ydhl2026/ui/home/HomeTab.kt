package com.dlust.ydhl2026.ui.home

/** 首页顶部的三个 Tab（与原型图2 一致：歌单 / 搜索 / 收藏） */
enum class HomeTab(val label: String, val emoji: String) {
  PLAYLISTS("歌单", "🎵"),
  SEARCH("搜索", "🔎"),
  FAVORITES("收藏", "💚")
}
