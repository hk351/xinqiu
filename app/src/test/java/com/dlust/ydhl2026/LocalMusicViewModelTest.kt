package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.local.FakeLocalLyricsSource
import com.dlust.ydhl2026.data.local.FakeLocalMusicSource
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.lyrics.FakeOnlineLyricsSource
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.local.LocalMusicPhase
import com.dlust.ydhl2026.ui.local.LocalMusicSort
import com.dlust.ydhl2026.ui.local.LocalMusicViewModel
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
 * 本地音乐页逻辑测试：扫描状态流转、结果写入共享 Store、搜索与排序。
 * 用测试调度器接管主线程，扫描源使用内存假实现。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicViewModelTest {

  private lateinit var store: LocalMusicStore

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    store = LocalMusicStore()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun song(id: String, title: String, artist: String, durationMs: Long, added: Long) =
    Song(
      id = id,
      title = title,
      artist = artist,
      album = "专辑",
      durationMs = durationMs,
      source = "local",
      sourceUrl = "content://media/audio/$id",
      lastModifiedAt = added
    )

  private fun sampleSongs() = listOf(
    song("l1", "十年", "陈奕迅", 205_000L, 100L),
    song("l2", "海阔天空", "Beyond", 323_000L, 300L),
    song("l3", "晴天", "周杰伦", 269_000L, 200L)
  )

  @Test
  fun `未授权时停在引导阶段且不扫描`() {
    val source = FakeLocalMusicSource(sampleSongs())
    val viewModel = LocalMusicViewModel(source, store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    viewModel.onPermissionResult(false)

    assertEquals(LocalMusicPhase.PERMISSION, viewModel.uiState.value.phase)
    assertTrue(store.songs.value.isEmpty())
  }

  @Test
  fun `授权后扫描结果写入 Store 并进入就绪`() {
    val songs = sampleSongs()
    val viewModel = LocalMusicViewModel(FakeLocalMusicSource(songs), store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    viewModel.onPermissionResult(true)

    assertEquals(LocalMusicPhase.READY, viewModel.uiState.value.phase)
    assertEquals(songs, store.songs.value)
    assertEquals(3, viewModel.uiState.value.songs.size)
    assertEquals(3, viewModel.uiState.value.visible.size)
  }

  @Test
  fun `扫描结果为空时进入空态`() {
    val viewModel = LocalMusicViewModel(FakeLocalMusicSource(emptyList()), store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    viewModel.onPermissionResult(true)

    assertEquals(LocalMusicPhase.EMPTY, viewModel.uiState.value.phase)
    assertTrue(store.songs.value.isEmpty())
  }

  @Test
  fun `扫描时权限被收回会退回权限引导并清空缓存`() {
    store.replace(sampleSongs())
    val source = FakeLocalMusicSource(emptyList(), SecurityException("no permission"))
    val viewModel = LocalMusicViewModel(source, store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    viewModel.startScan()

    assertEquals(LocalMusicPhase.PERMISSION, viewModel.uiState.value.phase)
    assertTrue(viewModel.uiState.value.songs.isEmpty())
    assertTrue(store.songs.value.isEmpty())
  }

  @Test
  fun `其他扫描异常仍按空态处理`() {
    val source = FakeLocalMusicSource(emptyList(), IllegalStateException("cursor closed"))
    val viewModel = LocalMusicViewModel(source, store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    viewModel.onPermissionResult(true)

    assertEquals(LocalMusicPhase.EMPTY, viewModel.uiState.value.phase)
  }

  @Test
  fun `上次扫描的缓存会在进入页面时直接展示`() {
    store.replace(sampleSongs())
    val viewModel = LocalMusicViewModel(FakeLocalMusicSource(emptyList()), store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())

    assertEquals(LocalMusicPhase.READY, viewModel.uiState.value.phase)
    assertEquals(3, viewModel.uiState.value.visible.size)
  }

  @Test
  fun `扫描后按文件名挂上同名歌词`() {
    val lyricLines = listOf(LyricLine(timeMs = 1_000L, text = "如果那两个字没有颤抖"))
    val songs = listOf(
      song("l1", "十年", "陈奕迅", 205_000L, 100L).copy(fileName = "ShiNian.mp3")
    )
    val viewModel = LocalMusicViewModel(
      FakeLocalMusicSource(songs),
      store,
      FakeLocalLyricsSource(mapOf("shinian" to lyricLines)),
      FakeOnlineLyricsSource()
    )

    viewModel.onPermissionResult(true)

    assertEquals(lyricLines, viewModel.uiState.value.songs.single().lyrics)
    assertEquals(lyricLines, store.songs.value.single().lyrics)
  }

  @Test
  fun `读取歌词索引失败不影响歌曲列表`() {
    val viewModel = LocalMusicViewModel(
      FakeLocalMusicSource(sampleSongs()),
      store,
      FakeLocalLyricsSource(error = IllegalStateException("saf error")),
      FakeOnlineLyricsSource()
    )

    viewModel.onPermissionResult(true)

    assertEquals(LocalMusicPhase.READY, viewModel.uiState.value.phase)
    assertEquals(3, viewModel.uiState.value.songs.size)
    assertTrue(viewModel.uiState.value.songs.all { it.lyrics.isEmpty() })
  }

  @Test
  fun `扫描后自动补全在线歌词并写回缓存`() {
    val onlineLines = listOf(LyricLine(timeMs = 2_000L, text = "故事的小黄花"))
    val online = FakeOnlineLyricsSource(mapOf("晴天" to onlineLines))
    val viewModel = LocalMusicViewModel(
      FakeLocalMusicSource(sampleSongs()),
      store,
      FakeLocalLyricsSource(),
      online
    )

    viewModel.onPermissionResult(true)

    val sunny = viewModel.uiState.value.songs.first { it.id == "l3" }
    assertEquals(onlineLines, sunny.lyrics)
    assertEquals(onlineLines, store.songs.value.first { it.id == "l3" }.lyrics)
    // 另外两首搜不到，保持无歌词
    assertTrue(viewModel.uiState.value.songs.filter { it.id != "l3" }.all { it.lyrics.isEmpty() })
  }

  @Test
  fun `已有本地歌词的歌不再请求在线`() {
    val localLines = listOf(LyricLine(timeMs = 1_000L, text = "如果那两个字没有颤抖"))
    val online = FakeOnlineLyricsSource()
    val songs = listOf(
      song("l1", "十年", "陈奕迅", 205_000L, 100L).copy(fileName = "shinian.mp3"),
      song("l3", "晴天", "周杰伦", 269_000L, 200L)
    )
    val viewModel = LocalMusicViewModel(
      FakeLocalMusicSource(songs),
      store,
      FakeLocalLyricsSource(mapOf("shinian" to localLines)),
      online
    )

    viewModel.onPermissionResult(true)

    assertEquals(listOf("晴天"), online.lookedUpTitles)
    assertEquals(localLines, viewModel.uiState.value.songs.first { it.id == "l1" }.lyrics)
  }

  @Test
  fun `在线歌词请求失败不影响歌曲列表`() {
    val viewModel = LocalMusicViewModel(
      FakeLocalMusicSource(sampleSongs()),
      store,
      FakeLocalLyricsSource(),
      FakeOnlineLyricsSource(error = IllegalStateException("timeout"))
    )

    viewModel.onPermissionResult(true)

    assertEquals(LocalMusicPhase.READY, viewModel.uiState.value.phase)
    assertEquals(3, viewModel.uiState.value.songs.size)
    assertTrue(viewModel.uiState.value.songs.all { it.lyrics.isEmpty() })
  }

  @Test
  fun `搜索按歌名歌手专辑过滤`() {
    val viewModel = LocalMusicViewModel(FakeLocalMusicSource(sampleSongs()), store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())
    viewModel.onPermissionResult(true)

    viewModel.onQueryChange("周杰伦")
    assertEquals(listOf("l3"), viewModel.uiState.value.visible.map { it.id })

    viewModel.onQueryChange("Beyond")
    assertEquals(listOf("l2"), viewModel.uiState.value.visible.map { it.id })

    viewModel.onQueryChange("  ")
    assertEquals(3, viewModel.uiState.value.visible.size)
  }

  @Test
  fun `排序按钮在歌名时长最近添加间循环`() {
    val viewModel = LocalMusicViewModel(FakeLocalMusicSource(sampleSongs()), store, FakeLocalLyricsSource(), FakeOnlineLyricsSource())
    viewModel.onPermissionResult(true)

    // 时长降序：海阔天空 323s > 晴天 269s > 十年 205s
    viewModel.onSortChange()
    assertEquals(LocalMusicSort.DURATION, viewModel.uiState.value.sort)
    assertEquals(listOf("l2", "l3", "l1"), viewModel.uiState.value.visible.map { it.id })

    // 最近添加降序：added 300 > 200 > 100
    viewModel.onSortChange()
    assertEquals(LocalMusicSort.ADDED, viewModel.uiState.value.sort)
    assertEquals(listOf("l2", "l3", "l1"), viewModel.uiState.value.visible.map { it.id })

    // 回到歌名排序（Unicode 序：十年 < 晴天 < 海阔天空）
    viewModel.onSortChange()
    assertEquals(LocalMusicSort.TITLE, viewModel.uiState.value.sort)
    assertEquals(listOf("l1", "l3", "l2"), viewModel.uiState.value.visible.map { it.id })
  }
}
