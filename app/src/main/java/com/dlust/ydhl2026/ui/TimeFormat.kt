package com.dlust.ydhl2026.ui

/**
 * 统一的时长格式化：毫秒 → m:ss，如 223000 → 3:43。
 * 搜索结果、全屏播放器、歌单详情都需要，放在一处避免重复实现。
 */
fun formatDuration(ms: Long): String {
  val totalSeconds = (ms / 1000).coerceAtLeast(0)
  return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
