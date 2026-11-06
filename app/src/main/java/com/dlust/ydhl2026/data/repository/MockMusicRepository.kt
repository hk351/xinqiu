package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock 数据源：内置 10 首歌曲 + 4 个歌单，用于阶段二/三的 UI 与架构验证。
 * 后续接入本地 MP3 或网络接口时，只替换这个实现，上层代码不动。
 */
class MockMusicRepository : MusicRepository {

  /** 5 首 demo 歌曲的示例歌词（阶段五接 LRC 后由真实文件替换） */
  private val lyricMap: Map<String, List<LyricLine>> = mapOf(
    "mock-01" to listOf(
      LyricLine(0, "对这个世界如果你有太多的抱怨"),
      LyricLine(4_000, "跌倒了就不敢继续往前走"),
      LyricLine(8_500, "为什么人要这么的脆弱 堕落"),
      LyricLine(13_000, "请你打开电视看看"),
      LyricLine(17_000, "多少人为生命在努力勇敢的走下去"),
      LyricLine(22_000, "我们是不是该知足"),
      LyricLine(25_500, "珍惜一切就算没有拥有"),
      LyricLine(30_000, "还记得你说家是唯一的城堡")
    ),
    "mock-03" to listOf(
      LyricLine(0, "有人问我我就会讲"),
      LyricLine(3_500, "但是无人来"),
      LyricLine(6_500, "我期待到无奈"),
      LyricLine(9_500, "有话要讲得不到装载"),
      LyricLine(13_000, "我的心情犹像樽盖"),
      LyricLine(16_500, "等被揭开"),
      LyricLine(19_500, "嘴巴却在养青苔"),
      LyricLine(23_000, "人潮内愈文静")
    ),
    "mock-04" to listOf(
      LyricLine(0, "今天我 寒夜里看雪飘过"),
      LyricLine(4_500, "怀着冷却了的心窝漂远方"),
      LyricLine(9_000, "风雨里追赶 雾里分不清影踪"),
      LyricLine(14_000, "天空海阔你与我"),
      LyricLine(17_500, "可会变（谁没在变）"),
      LyricLine(22_000, "多少次 迎着冷眼与嘲笑"),
      LyricLine(26_500, "从没有放弃过心中的理想"),
      LyricLine(31_000, "一刹那恍惚 若有所失的感觉")
    ),
    "mock-07" to listOf(
      LyricLine(0, "徘徊着的 在路上的"),
      LyricLine(3_500, "你要走吗 via via"),
      LyricLine(7_000, "易碎的 骄傲着"),
      LyricLine(10_500, "那也曾是我的模样"),
      LyricLine(14_000, "沸腾着的 不安着的"),
      LyricLine(17_500, "你要去哪 via via"),
      LyricLine(21_000, "谜一样的 沉默着的"),
      LyricLine(24_500, "故事你真的在听吗")
    ),
    "mock-08" to listOf(
      LyricLine(0, "一群嗜血的蚂蚁 被腐肉所吸引"),
      LyricLine(5_000, "我面无表情 看孤独的风景"),
      LyricLine(10_000, "失去你 爱恨开始分明"),
      LyricLine(14_500, "失去你 还有什么事好关心"),
      LyricLine(19_000, "当鸽子不再象征和平"),
      LyricLine(23_500, "我终于被提醒 广场上喂食的是秃鹰"),
      LyricLine(29_000, "我用漂亮的押韵"),
      LyricLine(32_500, "形容被掠夺一空的爱情")
    )
  )

  /** 10 首 Mock 歌曲（对应课程文档“10 首 Mock”要求） */
  private val songs: List<Song> = listOf(
    song("mock-01", "稻香", "周杰伦", "魔杰座", 223_000),
    song("mock-02", "晴天", "周杰伦", "叶惠美", 269_000),
    song("mock-03", "浮夸", "陈奕迅", "U87", 282_000),
    song("mock-04", "海阔天空", "Beyond", "乐与怒", 323_000),
    song("mock-05", "童年", "罗大佑", "童年", 192_000),
    song("mock-06", "红玫瑰", "陈奕迅", "认了吧", 241_000),
    song("mock-07", "平凡之路", "朴树", "猎户星座", 276_000),
    song("mock-08", "夜曲", "周杰伦", "十一月的萧邦", 226_000),
    song("mock-09", "光辉岁月", "Beyond", "命运派对", 298_000),
    song("mock-10", "十年", "陈奕迅", "黑白灰", 205_000)
  )

  /** 歌单 id -> 歌曲列表 */
  private val playlistSongs: Map<Long, List<Song>> = mapOf(
    1L to songs.filter { it.id in listOf("mock-07", "mock-09", "mock-04", "mock-08") },
    2L to songs.filter { it.id in listOf("mock-01", "mock-02", "mock-10", "mock-06") },
    3L to songs.filter { it.id in listOf("mock-09", "mock-03", "mock-07") },
    4L to songs.filter { it.id in listOf("mock-05", "mock-04", "mock-10", "mock-06", "mock-02") }
  )

  /** 4 个歌单，songCount 与上面的真实歌曲数保持一致 */
  private val playlists: List<Playlist> = listOf(
    playlist(1L, "跑步歌单"),
    playlist(2L, "深夜电台"),
    playlist(3L, "运动必备"),
    playlist(4L, "怀旧金曲")
  )

  override fun playlists(): Flow<List<Playlist>> = flowOf(playlists)

  override fun recentSongs(): Flow<List<Song>> = flowOf(songs.take(6))

  override fun songsOf(playlistId: Long): Flow<List<Song>> =
    flowOf(playlistSongs[playlistId].orEmpty())

  override suspend fun search(query: String): List<Song> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return emptyList()
    return songs.filter {
      it.title.contains(keyword, ignoreCase = true) ||
        it.artist.contains(keyword, ignoreCase = true) ||
        it.album?.contains(keyword, ignoreCase = true) == true
    }
  }

  private fun song(
    id: String,
    title: String,
    artist: String,
    album: String,
    durationMs: Long
  ) = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumArtUrl = null,           // 无图片素材，封面由 CoverArt 组件按 id 绘制
    durationMs = durationMs,
    source = "mock",
    // 工程里没有本地音频资源，这里用 SoundHelix 的公开示例 MP3 占位，
    // 让 Media3 能真实出声；拿到真实音频后只需把这一行换成实际地址
    sourceUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" +
      "${id.substringAfter('-').toInt()}.mp3",
    lyrics = lyricMap[id].orEmpty(),
    lastModifiedAt = MOCK_TIMESTAMP
  )

  private fun playlist(id: Long, name: String) = Playlist(
    id = id,
    name = name,
    coverUrl = null,
    songCount = playlistSongs[id].orEmpty().size,
    createdAt = MOCK_TIMESTAMP,
    lastModifiedAt = MOCK_TIMESTAMP
  )

  private companion object {
    const val MOCK_TIMESTAMP = 1_760_000_000_000L
  }
}
