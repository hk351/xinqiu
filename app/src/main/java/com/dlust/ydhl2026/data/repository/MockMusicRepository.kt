package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.lyrics.LrcParser
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

  /**
   * 5 首 demo 歌曲的示例歌词，直接写成 LRC 原文再交给解析器，
   * 这样解析、行时序、逐字动画在 Mock 数据上就能跑通（稻香带增强 LRC 逐字标签）。
   */
  private val lyricMap: Map<String, List<LyricLine>> = mapOf(
    "mock-01" to LrcParser.parse(
      """
      [00:00.00]<00:00.00>对<00:00.50>这个<00:01.10>世界<00:01.70>如果<00:02.30>你<00:02.70>有<00:03.10>太多<00:03.60>的<00:03.80>抱怨
      [00:04.00]<00:04.00>跌<00:04.50>倒了<00:05.00>就<00:05.40>不敢<00:06.00>继续<00:06.70>往前<00:07.40>走
      [00:08.50]<00:08.50>为什么<00:09.60>人<00:10.10>要<00:10.60>这么<00:11.30>的<00:11.70>脆弱<00:12.90>堕落
      [00:13.00]<00:13.00>请你<00:13.80>打开<00:14.60>电视<00:15.50>看看
      [00:17.00]多少人为生命在努力勇敢的走下去
      [00:22.00]我们是不是该知足
      [00:25.50]珍惜一切就算没有拥有
      [00:30.00]还记得你说家是唯一的城堡
      """.trimIndent()
    ),
    "mock-03" to LrcParser.parse(
      """
      [00:00.00]有人问我我就会讲
      [00:03.50]但是无人来
      [00:06.50]我期待到无奈
      [00:09.50]有话要讲得不到装载
      [00:13.00]我的心情犹像樽盖
      [00:16.50]等被揭开
      [00:19.50]嘴巴却在养青苔
      [00:23.00]人潮内愈文静
      """.trimIndent()
    ),
    "mock-04" to LrcParser.parse(
      """
      [00:00.00]今天我 寒夜里看雪飘过
      [00:04.50]怀着冷却了的心窝漂远方
      [00:09.00]风雨里追赶 雾里分不清影踪
      [00:14.00]天空海阔你与我
      [00:17.50]可会变（谁没在变）
      [00:22.00]多少次 迎着冷眼与嘲笑
      [00:26.50]从没有放弃过心中的理想
      [00:31.00]一刹那恍惚 若有所失的感觉
      """.trimIndent()
    ),
    "mock-07" to LrcParser.parse(
      """
      [00:00.00]徘徊着的 在路上的
      [00:03.50]你要走吗 via via
      [00:07.00]易碎的 骄傲着
      [00:10.50]那也曾是我的模样
      [00:14.00]沸腾着的 不安着的
      [00:17.50]你要去哪 via via
      [00:21.00]谜一样的 沉默着的
      [00:24.50]故事你真的在听吗
      """.trimIndent()
    ),
    "mock-08" to LrcParser.parse(
      """
      [00:00.00]一群嗜血的蚂蚁 被腐肉所吸引
      [00:05.00]我面无表情 看孤独的风景
      [00:10.00]失去你 爱恨开始分明
      [00:14.50]失去你 还有什么事好关心
      [00:19.00]当鸽子不再象征和平
      [00:23.50]我终于被提醒 广场上喂食的是秃鹰
      [00:29.00]我用漂亮的押韵
      [00:32.50]形容被掠夺一空的爱情
      """.trimIndent()
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
