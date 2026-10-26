package com.dlust.ydhl2026.data.repository

import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock 数据源：内置 10 首歌曲 + 4 个歌单，用于阶段二/三的 UI 与架构验证。
 * 后续接入本地 MP3 或网络接口时，只替换这个实现，上层代码不动。
 */
class MockMusicRepository : MusicRepository {

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
    sourceUrl = "mock://$id",
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
