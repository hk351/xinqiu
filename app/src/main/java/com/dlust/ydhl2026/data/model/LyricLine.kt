package com.dlust.ydhl2026.data.model

/**
 * 一个词（字）的起止时间。
 * 来源有两种：增强 LRC 的 `<mm:ss.xx>` 标记，或由整行时间均分推算（见 LyricTiming.spreadWords）。
 */
data class LyricWord(
  val startMs: Long,
  val endMs: Long,
  val text: String
)

/**
 * LRC 歌词的一行。
 *
 * - [endMs] 等于下一行开始时间（解析时统一补齐），逐字动画靠它算整行时长；
 * - [words] 为空表示这行只有整行时间，渲染时按字数均分兜底；
 * - [translation] 是同时间戳的第二行文本（双语歌词）。
 */
data class LyricLine(
  val timeMs: Long,
  val text: String,
  val endMs: Long = timeMs,
  val words: List<LyricWord> = emptyList(),
  val translation: String? = null
)
