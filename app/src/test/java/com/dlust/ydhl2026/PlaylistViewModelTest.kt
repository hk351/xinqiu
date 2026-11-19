package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.ui.playlist.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * 歌单详情页 ViewModel 的纯逻辑测试：
 * 加载、拖拽排序、滑动删除与撤销都不依赖 Android 框架与网络。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private val repository = FakeRepository()

  @Test
  fun `加载歌单时同时拿到歌单名与歌曲列表`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = repository)
    val state = viewModel.uiState.value

    assertEquals("热歌榜", state.playlist?.name)
    assertEquals(listOf("ncm-4", "ncm-7", "ncm-8", "ncm-9"), state.songs.map { it.id })
    assertFalse(state.isLoading)
  }

  @Test
  fun `拖拽排序把歌曲向后移动两行`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = repository)

    viewModel.moveSongBy("ncm-7", 2)

    assertEquals(
      listOf("ncm-4", "ncm-8", "ncm-9", "ncm-7"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `拖拽越界时钳制在列表两端`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = repository)

    viewModel.moveSongBy("ncm-7", -3)
    viewModel.moveSongBy("ncm-8", 9)

    assertEquals(
      listOf("ncm-7", "ncm-4", "ncm-9", "ncm-8"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `滑动删除后可以从原位置撤销`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = repository)

    viewModel.removeSong("ncm-4")
    assertEquals(
      listOf("ncm-7", "ncm-8", "ncm-9"),
      viewModel.uiState.value.songs.map { it.id }
    )

    viewModel.undoRemove()
    assertEquals(
      listOf("ncm-4", "ncm-7", "ncm-8", "ncm-9"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `删除不存在的歌曲不会改变列表`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = repository)

    viewModel.removeSong("ncm-99")

    assertEquals(4, viewModel.uiState.value.songs.size)
  }

  /** 假的仓库：固定一份榜单，不触网 */
  private class FakeRepository : MusicRepository {
    private val tracks = listOf("ncm-4", "ncm-7", "ncm-8", "ncm-9").mapIndexed { index, id ->
      ChartTrack(
        rank = index + 1,
        song = Song(
          id = id,
          title = "歌 $index",
          artist = "歌手",
          durationMs = 200_000L,
          source = "netease",
          sourceUrl = "",
          lastModifiedAt = 0L
        )
      )
    }

    override suspend fun charts(): List<Playlist> =
      listOf(
        Playlist(
          id = 1L,
          name = "热歌榜",
          coverUrl = null,
          songCount = tracks.size,
          createdAt = 0L,
          lastModifiedAt = 0L
        )
      )

    override suspend fun chartTracks(chartId: Long): List<ChartTrack> =
      if (chartId == 1L) tracks else emptyList()

    override suspend fun recommended(): List<ChartTrack> = tracks

    override fun recentSongs(): Flow<List<Song>> = flowOf(emptyList())

    override suspend fun search(query: String): List<Song> = emptyList()

    override suspend fun playUrl(song: Song): String? = song.sourceUrl
  }
}
