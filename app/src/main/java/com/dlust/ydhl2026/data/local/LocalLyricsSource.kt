package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.LyricLine

/**
 * 本地歌词来源抽象。
 * 实现按「文件名（不含扩展名，小写）」建索引，扫描时给同名歌曲挂上字幕；
 * 测试用内存假实现，不触碰 SAF。
 */
interface LocalLyricsSource {

  /** 返回 {\"歌名文件\": 歌词行} 的索引；没绑定文件夹时返回空 Map */
  suspend fun loadIndex(): Map<String, List<LyricLine>>
}
