package com.dlust.ydhl2026.data.lyrics

/**
 * 搜索结果条目，字段与在线接口返回对齐。
 */
data class LyricCandidate(
  val id: Long,
  val name: String,
  val artist: String,
  val durationMs: Long
)

/**
 * 用「歌名 + 歌手」组成搜索词。
 * 歌手缺失或是 MediaStore 的占位值时不拼进去，避免搜索词太长反而搜不到。
 */
fun buildSearchKeyword(title: String, artist: String): String? {
  val cleanTitle = title.trim()
  if (cleanTitle.isEmpty()) return null
  val cleanArtist = artist.trim()
  val usableArtist = cleanArtist.isNotEmpty() &&
    cleanArtist != "未知歌手" &&
    !cleanArtist.equals("unknown", ignoreCase = true) &&
    cleanArtist != "<unknown>"
  return if (usableArtist) "$cleanTitle $cleanArtist" else cleanTitle
}

/**
 * 从候选里挑最可能的那首：
 * 先按歌名匹配程度分层（完全相同 > 互相包含），同名时选时长最接近的。
 * 不按时长硬过滤，否则翻唱、试听片段这类时长差很多的正确结果会被误杀。
 */
fun pickBestMatch(
  candidates: List<LyricCandidate>,
  title: String,
  durationMs: Long
): LyricCandidate? {
  val target = normalizeTitle(title)
  if (target.isEmpty()) return null
  return candidates
    .mapNotNull { candidate ->
      val name = normalizeTitle(candidate.name)
      val tier = when {
        name == target -> 2
        name.contains(target) || target.contains(name) -> 1
        else -> 0
      }
      if (tier == 0) null else Triple(candidate, tier, kotlin.math.abs(candidate.durationMs - durationMs))
    }
    // 分层优先，其次时长差最小
    .maxWithOrNull(compareBy({ it.second }, { -it.third }))
    ?.first
}

/** 去掉空格与标点，只留文字和数字，避开「晴天 (Live)」「晴天-周杰伦」这类差异 */
internal fun normalizeTitle(raw: String): String =
  raw.lowercase().filter { it.isLetterOrDigit() }
