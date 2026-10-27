package com.dlust.ydhl2026.ui.player

import androidx.compose.ui.graphics.Color

/**
 * 全屏播放器的一套配色。
 * 对应 Demo 里「每首歌换一个主题色」的做法：切歌时整屏背景、控件、进度条、歌词高亮一起换。
 */
data class PlayerPalette(
  val bgTop: Color,
  val bgBottom: Color,
  val primary: Color,
  val text: Color,
  val textSecondary: Color,
  val track: Color,
  val cover1: Color,
  val cover2: Color,
  val cover3: Color
)

/** 五套主题：暖金 / 电光紫粉 / 深海蓝青 / 林间新绿 / 深夜紫靛 */
private val Palettes = listOf(
  PlayerPalette(
    bgTop = Color(0xFF1A1409),
    bgBottom = Color(0xFF2A1F0A),
    primary = Color(0xFFE2A735),
    text = Color(0xFFFFF8E7),
    textSecondary = Color(0xFFFFF8E7).copy(alpha = 0.55f),
    track = Color(0xFFE2A735).copy(alpha = 0.22f),
    cover1 = Color(0xFFF6C563),
    cover2 = Color(0xFFC88A2A),
    cover3 = Color(0xFF7A5214)
  ),
  PlayerPalette(
    bgTop = Color(0xFF16081A),
    bgBottom = Color(0xFF280D2E),
    primary = Color(0xFFE564D8),
    text = Color(0xFFFFEAF9),
    textSecondary = Color(0xFFFFEAF9).copy(alpha = 0.55f),
    track = Color(0xFFE564D8).copy(alpha = 0.22f),
    cover1 = Color(0xFFF07DE8),
    cover2 = Color(0xFFB53BB0),
    cover3 = Color(0xFF5A1760)
  ),
  PlayerPalette(
    bgTop = Color(0xFF07131A),
    bgBottom = Color(0xFF0D2330),
    primary = Color(0xFF4FC4D8),
    text = Color(0xFFE6F7FB),
    textSecondary = Color(0xFFE6F7FB).copy(alpha = 0.55f),
    track = Color(0xFF4FC4D8).copy(alpha = 0.22f),
    cover1 = Color(0xFF5FD4E8),
    cover2 = Color(0xFF2D8EA0),
    cover3 = Color(0xFF124654)
  ),
  PlayerPalette(
    bgTop = Color(0xFF0D160D),
    bgBottom = Color(0xFF142516),
    primary = Color(0xFF78D282),
    text = Color(0xFFEAFBEE),
    textSecondary = Color(0xFFEAFBEE).copy(alpha = 0.55f),
    track = Color(0xFF78D282).copy(alpha = 0.22f),
    cover1 = Color(0xFF8CE095),
    cover2 = Color(0xFF4A9A55),
    cover3 = Color(0xFF1E4A24)
  ),
  PlayerPalette(
    bgTop = Color(0xFF0B0A1A),
    bgBottom = Color(0xFF14112A),
    primary = Color(0xFF9B82E6),
    text = Color(0xFFEFEAFF),
    textSecondary = Color(0xFFEFEAFF).copy(alpha = 0.55f),
    track = Color(0xFF9B82E6).copy(alpha = 0.22f),
    cover1 = Color(0xFFB6A3F2),
    cover2 = Color(0xFF6E55C2),
    cover3 = Color(0xFF2E2064)
  )
)

/** 同一首歌永远拿到同一套配色，保证切歌不闪烁 */
fun playerPaletteFor(seed: String): PlayerPalette =
  Palettes[(seed.hashCode() and 0x7FFFFFFF) % Palettes.size]
