package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.playback.FakePlaybackEngine
import com.dlust.ydhl2026.data.settings.PlayerBackdropStore
import com.dlust.ydhl2026.ui.player.BackdropPreset
import com.dlust.ydhl2026.ui.player.PlayerBackdrop
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 播放器逻辑测试：点歌切歌、拖拽排序，以及接入引擎后的指令与续播行为。
 * ViewModel 里的状态流依赖主线程调度，这里用测试调度器接管。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

  private lateinit var engine: FakePlaybackEngine

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    engine = FakePlaybackEngine()
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
    sourceUrl = "https://example.com/$id.mp3",
    lastModifiedAt = 0L
  )

  @Test
  fun `从队列点歌会切歌并重置进度`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
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
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onPlayFromQueue(9)
    viewModel.onPlayFromQueue(-1)

    assertEquals("s1", viewModel.uiState.value.currentSong?.id)
  }

  @Test
  fun `队列拖拽可以前后换位`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[1], queue)

    viewModel.onMoveQueueBy("s1", 2)
    assertEquals(listOf("s2", "s3", "s1"), viewModel.uiState.value.queue.map { it.id })

    viewModel.onMoveQueueBy("s1", -1)
    assertEquals(listOf("s2", "s1", "s3"), viewModel.uiState.value.queue.map { it.id })
  }

  @Test
  fun `拖拽越界时停在队列两端且不影响当前歌曲`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onMoveQueueBy("s1", -3)
    viewModel.onMoveQueueBy("s3", 5)

    assertEquals(listOf("s1", "s2", "s3"), viewModel.uiState.value.queue.map { it.id })
    assertEquals("s1", viewModel.uiState.value.currentSong?.id)
  }

  @Test
  fun `拖拽不存在的歌曲不会改变队列`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onMoveQueueBy("s99", 1)

    assertEquals(queue.map { it.id }, viewModel.uiState.value.queue.map { it.id })
  }

  @Test
  fun `点歌会把歌曲地址交给播放引擎`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()

    viewModel.onPlaySong(queue[0], queue)

    assertEquals(listOf("https://example.com/s1.mp3"), engine.playedUrls)
    assertTrue(engine.playing)
  }

  @Test
  fun `暂停与继续会同步给播放引擎`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onPlayPause()
    assertFalse(viewModel.uiState.value.isPlaying)
    assertFalse(engine.playing)

    viewModel.onPlayPause()
    assertTrue(viewModel.uiState.value.isPlaying)
    assertTrue(engine.playing)
  }

  @Test
  fun `拖动进度会同步给播放引擎并钳制在时长内`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    viewModel.onSeekTo(600L)
    assertEquals(600L, engine.lastSeekTo)

    viewModel.onSeekTo(99_999L)
    assertEquals(1000L, engine.lastSeekTo)
    assertEquals(1000L, viewModel.uiState.value.positionMs)
  }

  @Test
  fun `换背景会同步到界面状态并落盘`() {
    val saved = mutableListOf<PlayerBackdrop>()
    val viewModel = PlayerViewModel(
      engine,
      PlayerBackdropStore(PlayerBackdrop.Default) { saved += it }
    )
    val chosen = PlayerBackdrop.Default.withPreset(BackdropPreset.Dominant)

    viewModel.onBackdropChange(chosen)

    assertEquals(BackdropPreset.Dominant, viewModel.uiState.value.backdrop.preset)
    assertEquals(listOf(chosen), saved)
  }

  @Test
  fun `顺序模式下一首放完会自动播下一首`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)

    engine.simulateCompletion()

    val state = viewModel.uiState.value
    assertEquals("s2", state.currentSong?.id)
    assertEquals(0L, state.positionMs)
    assertTrue(state.isPlaying)
    assertEquals("https://example.com/s2.mp3", engine.playedUrls.last())
  }

  @Test
  fun `单曲循环模式下一首放完会回到开头继续播放`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)
    viewModel.onRepeatModeChange()
    viewModel.onSeekTo(900L)

    engine.simulateCompletion()

    val state = viewModel.uiState.value
    assertEquals(1, state.repeatMode)
    assertEquals("s1", state.currentSong?.id)
    assertEquals(0L, state.positionMs)
    assertTrue(state.isPlaying)
    assertEquals(0L, engine.lastSeekTo)
    // 单曲循环不重新选曲，只把同一首拉回开头
    assertEquals(1, engine.playedUrls.size)
  }

  @Test
  fun `随机模式下一首放完会切到队列里的其他歌曲`() {
    val viewModel = PlayerViewModel(engine, PlayerBackdropStore())
    val queue = queue()
    viewModel.onPlaySong(queue[0], queue)
    viewModel.onRepeatModeChange()
    viewModel.onRepeatModeChange()

    engine.simulateCompletion()

    val state = viewModel.uiState.value
    assertEquals(2, state.repeatMode)
    assertNotEquals("s1", state.currentSong?.id)
    assertEquals("https://example.com/${state.currentSong?.id}.mp3", engine.playedUrls.last())
  }
}
