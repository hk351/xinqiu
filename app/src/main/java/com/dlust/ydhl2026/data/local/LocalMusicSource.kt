package com.dlust.ydhl2026.data.local

import com.dlust.ydhl2026.data.model.Song

/**
 * 本地音乐数据源抽象。
 * ViewModel 只依赖这个接口；真实实现查 MediaStore，测试用内存假数据。
 */
interface LocalMusicSource {

  /**
   * 扫描本机音频文件，映射为 Song 列表。
   * 实现需过滤掉时长过短的提示音（铃声/通知），只保留真正的歌曲。
   */
  suspend fun scan(): List<Song>
}
