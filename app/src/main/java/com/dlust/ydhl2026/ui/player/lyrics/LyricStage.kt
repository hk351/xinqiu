package com.dlust.ydhl2026.ui.player.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.ui.player.PlayerPalette

/**
 * 歌词样式。
 *
 * 对应 Folia 里可切换的 visualizer：渲染流程共用一条时间轴，只有排版和动效不同。
 * 目前两种样式已经够用，等第三种出现再抽注册表，不做提前抽象。
 */
enum class LyricStyle(val label: String) {
  STILL("静帧"),
  CLASSIC("逐字")
}

/**
 * 歌词舞台：统一换算时间轴，再按样式交给具体实现绘制。
 *
 * @param positionMs 播放器回传的原始进度，样式层内部会插值成逐帧时间轴
 */
@Composable
fun LyricStage(
  lines: List<LyricLine>,
  positionMs: Long,
  isPlaying: Boolean,
  palette: PlayerPalette,
  style: LyricStyle,
  modifier: Modifier = Modifier
) {
  val smoothPosition = rememberLyricPosition(positionMs = positionMs, isPlaying = isPlaying)

  when (style) {
    LyricStyle.STILL -> StillLyrics(
      lines = lines,
      positionMs = smoothPosition,
      palette = palette,
      modifier = modifier
    )

    LyricStyle.CLASSIC -> ClassicLyrics(
      lines = lines,
      positionMs = smoothPosition,
      palette = palette,
      modifier = modifier
    )
  }
}
