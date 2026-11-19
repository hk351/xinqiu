package com.dlust.ydhl2026.data.model

/**
 * 榜单里的一个条目：名次 + 歌曲。
 *
 * 公开接口拿不到单曲播放量，但榜单本身就是官方按播放热度排出来的，
 * 所以用名次表达"热到什么程度"，比编造一个播放数更诚实。
 */
data class ChartTrack(
  val rank: Int,
  val song: Song
)
