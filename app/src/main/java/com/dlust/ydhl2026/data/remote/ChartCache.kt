package com.dlust.ydhl2026.data.remote

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 榜单的每日磁盘缓存。
 *
 * 榜单一天只更新一次，没必要每次进首页都打接口：当天已拉过就直接用文件，
 * 拉取失败时退回最近一份（显示昨天的榜总比白屏好）。
 * 缓存的是接口原始 JSON，读出来再走同一套解析，避免再维护一份序列化。
 *
 * 读写都放到 IO 线程：这些方法会被 ViewModel 在协程里直接调用，
 * 如果留在主线程，首屏进推荐页会多出一次同步磁盘读。
 */
class ChartCache(
  context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = System::currentTimeMillis
) {

  private val dir = File(context.filesDir, "charts")

  /** 今天已经缓存过的榜单原文；没有则返回 null */
  suspend fun loadToday(chartId: Long): String? = withContext(ioDispatcher) {
    val today = today()
    runCatching { file(chartId, today).readText() }.getOrNull()
  }

  /** 最近一次缓存（可能是昨天的），用于断网兜底 */
  suspend fun loadLatest(chartId: Long): String? = withContext(ioDispatcher) {
    val files = chartFiles(chartId)
    files.lastOrNull()?.let { runCatching { it.readText() }.getOrNull() }
  }

  suspend fun save(chartId: Long, rawJson: String) = withContext(ioDispatcher) {
    runCatching {
      dir.mkdirs()
      val today = today()
      file(chartId, today).writeText(rawJson)
      // 只保留今天这一份，昨天之后的旧文件没有价值
      chartFiles(chartId).filter { it.name != fileName(chartId, today) }.forEach { it.delete() }
    }
    Unit
  }

  /** SimpleDateFormat 不是线程安全的，按调用新建一份，一天也就格式化几次 */
  private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clock()))

  private fun file(chartId: Long, day: String) = File(dir, fileName(chartId, day))

  private fun fileName(chartId: Long, day: String) = "chart_${chartId}_$day.json"

  private fun chartFiles(chartId: Long): List<File> =
    runCatching {
      dir.listFiles { f -> f.name.startsWith("chart_${chartId}_") }
        ?.sortedBy { it.name }
        .orEmpty()
    }.getOrDefault(emptyList())
}
