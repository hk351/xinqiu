package com.dlust.ydhl2026.data.remote

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 榜单的每日磁盘缓存。
 *
 * 榜单一天只更新一次，没必要每次进首页都打接口：当天已拉过就直接用文件，
 * 拉取失败时退回最近一份（显示昨天的榜总比白屏好）。
 * 缓存的是接口原始 JSON，读出来再走同一套解析，避免再维护一份序列化。
 */
class ChartCache(
  context: Context,
  private val clock: () -> Long = System::currentTimeMillis
) {

  private val dir = File(context.filesDir, "charts")
  private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  /** 今天已经缓存过的榜单原文；没有则返回 null */
  fun loadToday(chartId: Long): String? {
    val today = today()
    return runCatching { file(chartId, today).readText() }.getOrNull()
  }

  /** 最近一次缓存（可能是昨天的），用于断网兜底 */
  fun loadLatest(chartId: Long): String? {
    val files = chartFiles(chartId)
    return files.lastOrNull()?.let { runCatching { it.readText() }.getOrNull() }
  }

  fun save(chartId: Long, rawJson: String) {
    runCatching {
      dir.mkdirs()
      val today = today()
      file(chartId, today).writeText(rawJson)
      // 只保留今天这一份，昨天之后的旧文件没有价值
      chartFiles(chartId).filter { it.name != fileName(chartId, today) }.forEach { it.delete() }
    }
  }

  private fun today(): String = dayFormat.format(Date(clock()))

  private fun file(chartId: Long, day: String) = File(dir, fileName(chartId, day))

  private fun fileName(chartId: Long, day: String) = "chart_${chartId}_$day.json"

  private fun chartFiles(chartId: Long): List<File> =
    runCatching {
      dir.listFiles { f -> f.name.startsWith("chart_${chartId}_") }
        ?.sortedBy { it.name }
        .orEmpty()
    }.getOrDefault(emptyList())
}
