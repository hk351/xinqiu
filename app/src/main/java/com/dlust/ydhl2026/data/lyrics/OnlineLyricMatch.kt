package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import kotlin.math.abs

/**
 * 搜索结果条目，字段与在线接口返回对齐。
 */
data class LyricCandidate(
  val id: Long,
  val name: String,
  val artist: String,
  val durationMs: Long
)

/** 包含匹配时，多出来的部分至少要有 2 个字，避免 demo-a 这种文件名靠一个字母配到不相干的歌 */
private const val MIN_CONTAIN_EXTRA_CHARS = 2

/** 时长容许误差的下限；候选本身越长，允许差异也按比例放大 */
private const val MIN_DURATION_TOLERANCE_MS = 60_000L

/** 制作人员信息行，不是歌词本身 */
private val CREDIT_LINE_PREFIX = Regex(
  "^\\s*(作词|作曲|编曲|制作人|监制|出品|发行|混音|母带|录音|吉他|贝斯|鼓|键盘|和声|统筹|企划|词|曲)\\s*[:：]"
)

/**
 * 用「歌名 + 歌手」组成搜索词。
 * 歌手缺失或是 MediaStore 的占位值时不拼进去，避免搜索词太长反而搜不到。
 */
fun buildSearchKeyword(title: String, artist: String): String? {
  val cleanTitle = title.trim()
  if (cleanTitle.isEmpty()) return null
  val cleanArtist = normalizeArtist(artist)
  return if (cleanArtist.isNotEmpty()) "$cleanTitle ${artist.trim()}" else cleanTitle
}

/**
 * 从候选里挑最可能的那首：
 * 先按歌名匹配程度分层（完全相同 > 互相包含），同名时选时长最接近的。
 *
 * 包含匹配要求「多出来的部分至少 2 个字」，并把歌手作为约束；
 * 时长差超过容忍值的候选直接排除，避免 40 秒的片段配上整首歌的歌词。
 * 任一方时长未知（0）时不做时长排除，宁可多匹配也不误杀。
 */
fun pickBestMatch(
  candidates: List<LyricCandidate>,
  title: String,
  artist: String,
  durationMs: Long
): LyricCandidate? {
  val target = normalizeTitle(title)
  if (target.isEmpty()) return null
  val targetArtist = normalizeArtist(artist)
  return candidates
    .mapNotNull { candidate ->
      val name = normalizeTitle(candidate.name)
      val tier = when {
        name == target -> 3
        isContainedMatch(name, target, targetArtist, normalizeArtist(candidate.artist)) -> 2
        else -> 0
      }
      if (tier == 0 || isDurationMismatch(candidate.durationMs, durationMs)) {
        null
      } else {
        Triple(candidate, tier, abs(candidate.durationMs - durationMs))
      }
    }
    // 分层优先，其次时长差最小
    .maxWithOrNull(compareBy({ it.second }, { -it.third }))
    ?.first
}

/** 去掉制作人员信息行（作词：xxx、编曲：xxx 这类），它们不该出现在歌词舞台上 */
fun dropCreditLines(lines: List<LyricLine>): List<LyricLine> = lines.filterNot { isCreditLine(it.text) }

fun isCreditLine(text: String): Boolean = CREDIT_LINE_PREFIX.containsMatchIn(text)

/** 标题只是包含关系时，只有歌手也能对得上才认，否则会把名字相近的别的歌算进来 */
private fun isContainedMatch(
  name: String,
  target: String,
  targetArtist: String,
  candidateArtist: String
): Boolean {
  if (!name.contains(target) && !target.contains(name)) return false
  if (minOf(name.length, target.length) < 2) return false
  if (maxOf(name.length, target.length) - minOf(name.length, target.length) < MIN_CONTAIN_EXTRA_CHARS) {
    return false
  }
  if (targetArtist.isEmpty()) return true
  return candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist)
}

/** 时长差超过容忍值时视为不是同一首；容忍值取 60 秒与候选时长 25% 中的较大者 */
private fun isDurationMismatch(candidateMs: Long, targetMs: Long): Boolean {
  if (candidateMs <= 0L || targetMs <= 0L) return false
  return abs(candidateMs - targetMs) > maxOf(MIN_DURATION_TOLERANCE_MS, candidateMs / 4)
}

/** 去掉空格与标点，只留文字和数字，避开「晴天 (Live)」「晴天-周杰伦」这类差异 */
internal fun normalizeTitle(raw: String): String =
  raw.lowercase().filter { it.isLetterOrDigit() }

/** 歌手归一化：MediaStore 的占位歌手视为未知，返回空串表示「不作约束」 */
internal fun normalizeArtist(raw: String): String {
  val clean = raw.trim()
  if (
    clean.isEmpty() ||
    clean == "未知歌手" ||
    clean == "<unknown>" ||
    clean.equals("unknown", ignoreCase = true)
  ) {
    return ""
  }
  return normalizeTitle(clean)
}
