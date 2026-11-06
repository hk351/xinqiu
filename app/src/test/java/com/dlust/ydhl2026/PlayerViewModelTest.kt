package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 播放队列的纯逻辑测试：点歌切歌、拖拽排序与越界钳制。
 * 假播放的定时器依赖主线程调度，这里用测试调度器接管。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** 三首歌的队列，时长依次为 1s / 2s / 3s，方便断言 */
  private fun queue(): List<Song> = listOf(
    song("s1", 1000L),
    song("s2", 2000L),
    song("s3", 3000L)
  )

  private fun song(id: String, durationMs: Long) = Song(
    id = id,
    title = "歌曲$id",
    artist = "歌手",
    album = null,
    durationMs = durationMs,
    source = "mock",
    sourceUrl = "mock://$id",
    lastModifiedAt = 0L
  )

  @Test
  fun `从队列点歌会切歌并重置进度`() {
    val viewModel = PlayerViewModel()
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)
    viewModel.onSeekTo(800L)

    viewModel.onPlayFromQueue(2)

    val state = viewModel.uiState.value
    assertEquals("s3", state.currentSong?.id)
    assertEquals(0L, state.positionMs)
    assertEquals(3000L, state.durationMs)
    assertTrue(state.isPlaying)
  }

  @Test
  fun `点击越界索引不会改变当前歌曲`() {
    val viewModel = PlayerViewModel()
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onPlayFromQueue(9)
    viewModel.onPlayFromQueue(-1)

    assertEquals("s1", viewModel.uiState.value.currentSong?.id)
  }

  @Test
  fun `队列拖拽可以前后换位`() {
    val viewModel = PlayerViewModel()
    val queue = queue()
    viewModel.onPlaySong(queue[1], queue)

    viewModel.onMoveQueueBy("s1", 2)
    assertEquals(listOf("s2", "s3", "s1"), viewModel.uiState.value.queue.map { it.id })

    viewModel.onMoveQueueBy("s1", -1)
    assertEquals(listOf("s2", "s1", "s3"), viewModel.uiState.value.queue.map { it.id })
  }

  @Test
  fun `拖拽越界时停在队列两端且不影响当前歌曲`() {
    val viewModel = PlayerViewModel()
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onMoveQueueBy("s1", -3)
    viewModel.onMoveQueueBy("s3", 5)

    assertEquals(listOf("s1", "s2", "s3"), viewModel.uiState.value.queue.map { it.id })
    assertEquals("s1", viewModel.uiState.value.currentSong?.id)
  }

  @Test
  fun `拖拽不存在的歌曲不会改变队列`() {
    val viewModel = PlayerViewModel()
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onMoveQueueBy("s99", 1)

    assertEquals(queue.map { it.id }, viewModel.uiState.value.queue.map { it.id })
  }
}
