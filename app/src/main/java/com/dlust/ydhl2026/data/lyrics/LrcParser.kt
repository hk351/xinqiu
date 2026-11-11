package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.LyricWord

/**
 * LRC 解析器，支持两种写法：
 *
 * 1. 普通 LRC：`[00:12.34]歌词文本`，一行可带多个时间标签，也支持 `[00:12]` 这种不带毫秒的；
 * 2. 增强 LRC：行内用 `<00:12.34>` 标记每个词的开始时间，用来做逐字高亮；
 * 3. 同一时间戳出现两行时，第二行当作译文合并进第一行。
 *
 * `[ti:]` `[ar:]` 这类元信息标签直接跳过，`[offset:]` 会作用到所有时间上。
 */
object LrcParser {

  /** 行首时间标签：分钟、秒、可选毫秒（用 . 或 : 分隔） */
  private val TimeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

  /** 行内逐字时间标签：<mm:ss.xx> */
  private val WordTag = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")

  /** 全局偏移，单位毫秒，正数表示歌词整体延后 */
  private val OffsetTag = Regex("""^\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

  fun parse(raw: String): List<LyricLine> {
    if (raw.isBlank()) return emptyList()

    var offsetMs = 0L
    val collected = mutableListOf<LyricLine>()

    raw.lineSequence().forEach { source ->
      val line = source.trim()
      if (line.isEmpty()) return@forEach

      OffsetTag.find(line)?.let {
        offsetMs = it.groupValues[1].toLong()
        return@forEach
      }

      // 逐个吃掉行首时间标签，其余内容才是歌词
      val times = mutableListOf<Long>()
      var cursor = 0
      while (true) {
        val match = TimeTag.matchAt(line, cursor) ?: break
        times += timestampMs(match)
        cursor = match.range.last + 1
      }
      if (times.isEmpty()) return@forEach

      val body = line.substring(cursor).trim()
      val parsed = parseBody(body)
      times.forEach { time ->
        collected += LyricLine(
          timeMs = time + offsetMs,
          text = parsed.text,
          words = parsed.words.map { word ->
            word.copy(startMs = word.startMs + offsetMs, endMs = word.endMs + offsetMs)
          }
        )
      }
    }

    if (collected.isEmpty()) return emptyList()
    val merged = mergeTranslations(collected.sortedBy { it.timeMs })
    return fillEndTimes(merged)
  }

  /** 解析行内逐字标签；没有逐字标签时 words 为空，文本原样返回 */
  private fun parseBody(body: String): ParsedBody {
    val matches = WordTag.findAll(body).toList()
    if (matches.isEmpty()) return ParsedBody(body, emptyList())

    val words = mutableListOf<LyricWord>()
    var text = ""
    matches.forEachIndexed { index, match ->
      val start = timestampMs(match)
      val contentStart = match.range.last + 1
      val contentEnd = matches.getOrNull(index + 1)?.range?.first ?: body.length
      val content = body.substring(contentStart, contentEnd)
      // 行尾残留的纯文本（没有对应时间标签）留给最后一个词
      val nextStart = matches.getOrNull(index + 1)?.let { timestampMs(it) } ?: start
      if (content.isNotEmpty() || index == matches.lastIndex) {
        words += LyricWord(startMs = start, endMs = maxOf(nextStart, start + 1), text = content)
        text += content
      }
    }
    return ParsedBody(text.trim(), words)
  }

  /** 同时间戳的第二行视为译文；重复的同一句则丢弃 */
  private fun mergeTranslations(lines: List<LyricLine>): List<LyricLine> {
    val result = mutableListOf<LyricLine>()
    lines.forEach { line ->
      val last = result.lastOrNull()
      when {
        last == null || line.timeMs != last.timeMs -> result += line
        last.translation == null && line.text.isNotBlank() && line.text != last.text ->
          result[result.lastIndex] = last.copy(translation = line.text)
        else -> Unit
      }
    }
    return result
  }

  /** 行结束时间 = 下一行开始时间；最后一行退回到逐字时间或固定兜底时长 */
  private fun fillEndTimes(lines: List<LyricLine>): List<LyricLine> = lines.mapIndexed { index, line ->
    val nextStart = lines.getOrNull(index + 1)?.timeMs
    val end = nextStart ?: maxOf(line.words.lastOrNull()?.endMs ?: 0L, line.timeMs + 4_000L)
    val safeEnd = maxOf(end, line.timeMs + 1)
    val words = line.words.mapIndexed { wordIndex, word ->
      if (wordIndex == line.words.lastIndex) word.copy(endMs = maxOf(safeEnd, word.startMs + 1)) else word
    }
    line.copy(endMs = safeEnd, words = words)
  }

  private fun timestampMs(match: MatchResult): Long {
    val minutes = match.groupValues[1].toLong()
    val seconds = match.groupValues[2].toLong()
    val fraction = match.groupValues[3]
    val millis = when (fraction.length) {
      0 -> 0L
      1 -> fraction.toLong() * 100
      2 -> fraction.toLong() * 10
      else -> fraction.take(3).toLong()
    }
    return minutes * 60_000 + seconds * 1_000 + millis
  }

  private data class ParsedBody(val text: String, val words: List<LyricWord>)
}
