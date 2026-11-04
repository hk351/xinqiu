package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.repository.MockMusicRepository
import com.dlust.ydhl2026.ui.playlist.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 加载、拖拽排序、滑动删除与撤销都不依赖 Android 框架。
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

  @Test
  fun `加载歌单时同时拿到歌单名与歌曲列表`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = MockMusicRepository())
    val state = viewModel.uiState.value

    assertEquals("跑步歌单", state.playlist?.name)
    assertEquals(
      listOf("mock-04", "mock-07", "mock-08", "mock-09"),
      state.songs.map { it.id }
    )
    assertFalse(state.isLoading)
  }

  @Test
  fun `拖拽排序把歌曲向后移动两行`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = MockMusicRepository())

    viewModel.moveSongBy("mock-07", 2)

    assertEquals(
      listOf("mock-04", "mock-08", "mock-09", "mock-07"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `拖拽越界时钳制在列表两端`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = MockMusicRepository())

    viewModel.moveSongBy("mock-07", -3)
    viewModel.moveSongBy("mock-08", 9)

    assertEquals(
      listOf("mock-07", "mock-04", "mock-09", "mock-08"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `滑动删除后可以从原位置撤销`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = MockMusicRepository())

    viewModel.removeSong("mock-04")
    assertEquals(
      listOf("mock-07", "mock-08", "mock-09"),
      viewModel.uiState.value.songs.map { it.id }
    )

    viewModel.undoRemove()
    assertEquals(
      listOf("mock-04", "mock-07", "mock-08", "mock-09"),
      viewModel.uiState.value.songs.map { it.id }
    )
  }

  @Test
  fun `删除不存在的歌曲不会改变列表`() {
    val viewModel = PlaylistViewModel(playlistId = 1L, repository = MockMusicRepository())

    viewModel.removeSong("mock-99")

    assertEquals(4, viewModel.uiState.value.songs.size)
  }
}
