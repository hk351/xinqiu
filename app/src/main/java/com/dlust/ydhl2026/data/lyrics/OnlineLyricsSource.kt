package com.dlust.ydhl2026.data.lyrics

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song

/**
 * 在线歌词来源抽象。
 * 实现按「歌名 + 歌手 + 时长」搜索并返回 LRC 行；测试用内存假实现，不联网。
 */
interface OnlineLyricsSource {

  /** 匹配不到或网络失败时返回空列表，不抛异常 */
  suspend fun find(song: Song): List<LyricLine>
}
