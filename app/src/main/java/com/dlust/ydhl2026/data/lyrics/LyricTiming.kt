package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.LyricWord

/** 解析器没拿到下一行时，给最后一行一个默认时长 */
private const val TAIL_MS = 5_000L

/** 当前播放到第几行；早于第一行时返回 -1 */
fun List<LyricLine>.activeLineIndex(positionMs: Long): Int =
  indexOfLast { it.timeMs <= positionMs }

/**
 * 第 [index] 行的结束时间。
 * 解析器通常会填好 endMs；手工构造的数据则回退到下一行开始时间。
 */
fun List<LyricLine>.lineEndMs(index: Int): Long {
  val line = getOrNull(index) ?: return 0L
  if (line.endMs > line.timeMs) return line.endMs
  return getOrNull(index + 1)?.timeMs ?: (line.timeMs + TAIL_MS)
}

/**
 * 把整行文本拆成逐字时间轴。
 *
 * 有真实逐字数据时直接用；否则按字数均分整个行时长，
 * 保证没有增强 LRC 的歌也能有逐字动画（节奏均匀但不跑调）。
 */
fun LyricLine.wordsOrSpread(lineEndMs: Long): List<LyricWord> {
  if (words.isNotEmpty()) return words
  val tokens = splitTokens(text)
  if (tokens.isEmpty()) return emptyList()
  val start = timeMs
  val end = maxOf(lineEndMs, start + 1)
  val total = (end - start).toFloat()
  val weights = tokens.map { tokenWeight(it) }
  val weightSum = weights.sum().takeIf { it > 0f } ?: 1f

  var cursor = start
  return tokens.mapIndexed { index, token ->
    val span = if (index == tokens.lastIndex) {
      end - cursor
    } else {
      ((end - start) * (weights[index] / weightSum)).toLong()
    }
    val wordEnd = (cursor + span).coerceAtMost(end)
    LyricWord(startMs = cursor, endMs = maxOf(wordEnd, cursor + 1), text = token)
      .also { cursor = it.endMs }
  }
}

/**
 * 切词：中文按单字，连续的西文字母/数字算一个词，标点和空格附着在前一个词后。
 * 保证拼回去与原文完全一致，否则逐字渲染会丢字。
 */
internal fun splitTokens(text: String): List<String> {
  if (text.isEmpty()) return emptyList()
  val tokens = mutableListOf<String>()
  val buffer = StringBuilder()

  fun flush() {
    if (buffer.isNotEmpty()) {
      tokens += buffer.toString()
      buffer.clear()
    }
  }

  for (char in text) {
    when {
      isCjk(char) -> {
        flush()
        tokens += char.toString()
      }
      char.isLetterOrDigit() || char == '\'' || char == '’' -> buffer.append(char)
      else -> {
        // 标点、空格粘到前一个词，避免单独一个空格占一段时间
        if (buffer.isNotEmpty()) {
          buffer.append(char)
          flush()
        } else if (tokens.isNotEmpty()) {
          tokens[tokens.lastIndex] = tokens.last() + char
        } else {
          buffer.append(char)
          flush()
        }
      }
    }
  }
  flush()
  return tokens
}

/** 中文一个字占一拍，西文按 0.6 拍折算，和实际咬字速度更接近 */
private fun tokenWeight(token: String): Float =
  token.sumOf { if (isCjk(it)) 1.0 else 0.6 }.toFloat()

private fun isCjk(char: Char): Boolean {
  val code = char.code
  return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0xFF01..0xFF60
}
