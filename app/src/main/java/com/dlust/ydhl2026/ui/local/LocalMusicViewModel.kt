package com.dlust.ydhl2026.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.local.LocalMusicSource
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.local.LocalLyricsSource
import com.dlust.ydhl2026.data.local.attachLyrics
import com.dlust.ydhl2026.data.lyrics.OnlineLyricsSource
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** 页面阶段：未授权 → 扫描中 → 就绪 / 空态 */
enum class LocalMusicPhase { PERMISSION, SCANNING, READY, EMPTY }

/** 列表排序方式，点击排序按钮循环切换 */
enum class LocalMusicSort(val label: String) {
  TITLE("歌名"),
  DURATION("时长"),
  ADDED("最近添加")
}

/** 本地音乐页 UI 状态：songs 是扫描原始结果，visible 是搜索+排序后的展示列表 */
data class LocalMusicUiState(
  val phase: LocalMusicPhase = LocalMusicPhase.PERMISSION,
  val songs: List<Song> = emptyList(),
  val query: String = "",
  val sort: LocalMusicSort = LocalMusicSort.TITLE,
  val visible: List<Song> = emptyList()
)

/**
 * 本地音乐页状态管理。
 * 权限由界面（ActivityResult）申请，结果回调进来；扫描交给 LocalMusicSource，
 * 结果写入 LocalMusicStore 供首页入口卡显示歌曲数。
 */
class LocalMusicViewModel(
  private val source: LocalMusicSource,
  private val store: LocalMusicStore,
  private val lyricsSource: LocalLyricsSource,
  private val onlineLyricsSource: OnlineLyricsSource
) : ViewModel() {

  private val _uiState = MutableStateFlow(LocalMusicUiState())
  val uiState: StateFlow<LocalMusicUiState> = _uiState.asStateFlow()

  init {
    // 之前扫过就直接展示缓存，避免每次进页面都等扫描动画
    val cached = store.songs.value
    if (cached.isNotEmpty()) {
      _uiState.update {
        it.copy(phase = LocalMusicPhase.READY, songs = cached, visible = filterSort(cached, "", it.sort))
      }
    }
  }

  /** 界面拿到权限结果后回调 */
  fun onPermissionResult(granted: Boolean) {
    if (granted) startScan()
  }

  fun startScan() {
    _uiState.update { it.copy(phase = LocalMusicPhase.SCANNING) }
    viewModelScope.launch {
      val result = runCatching { source.scan() }
      // 权限在扫描途中被收回时 MediaStore 会抛 SecurityException，
      // 此时要退回权限引导页，而不是伪装成「没有本地音乐」
      if (result.isFailure && result.exceptionOrNull() is SecurityException) {
        store.replace(emptyList())
        _uiState.update {
          it.copy(
            phase = LocalMusicPhase.PERMISSION,
            songs = emptyList(),
            visible = emptyList(),
            query = ""
          )
        }
        return@launch
      }
      val songs = attachLyrics(result.getOrDefault(emptyList()), loadLyricsIndex())
      store.replace(songs)
      _uiState.update {
        if (songs.isEmpty()) {
          it.copy(phase = LocalMusicPhase.EMPTY, songs = emptyList(), visible = emptyList(), query = "")
        } else {
          it.copy(
            phase = LocalMusicPhase.READY,
            songs = songs,
            visible = filterSort(songs, it.query, it.sort)
          )
        }
      }
      if (songs.isNotEmpty()) enrichOnlineLyrics()
    }
  }

  /**
   * 列表先出来，再补在线歌词：只处理还没有歌词的歌，限并发、限量，
   * 拿到一批后一次性写回状态和缓存，避免逐首刷新列表造成抖动。
   */
  private fun enrichOnlineLyrics() {
    viewModelScope.launch {
      val pending = _uiState.value.songs
        .filter { it.lyrics.isEmpty() }
        .take(MAX_ONLINE_LOOKUPS)
      if (pending.isEmpty()) return@launch

      val semaphore = Semaphore(ONLINE_CONCURRENCY)
      val found = coroutineScope {
        pending.map { song ->
          async {
            val lines = semaphore.withPermit {
              runCatching { onlineLyricsSource.find(song) }.getOrDefault(emptyList())
            }
            song.id to lines
          }
        }.awaitAll()
      }.filter { it.second.isNotEmpty() }.toMap()
      if (found.isEmpty()) return@launch

      val merged = _uiState.value.songs.map { song ->
        found[song.id]?.let { song.copy(lyrics = it) } ?: song
      }
      store.replace(merged)
      _uiState.update {
        it.copy(songs = merged, visible = filterSort(merged, it.query, it.sort))
      }
    }
  }

  /** 读取 .lrc 索引；没绑定歌词文件夹或读取失败都返回空，不影响歌曲列表本身 */
  private suspend fun loadLyricsIndex(): Map<String, List<LyricLine>> =
    runCatching { lyricsSource.loadIndex() }.getOrDefault(emptyMap())

  fun onQueryChange(query: String) {
    _uiState.update {
      it.copy(query = query, visible = filterSort(it.songs, query, it.sort))
    }
  }

  /** 排序按钮：歌名 → 时长 → 最近添加 → 歌名 */
  fun onSortChange() {
    _uiState.update {
      val next = LocalMusicSort.entries[(it.sort.ordinal + 1) % LocalMusicSort.entries.size]
      it.copy(sort = next, visible = filterSort(it.songs, it.query, next))
    }
  }

  companion object {

    /** 在线补词的同时请求数，别把免费接口打限流 */
    private const val ONLINE_CONCURRENCY = 4

    /** 单次扫描最多补多少首，超大曲库留给后续手动触发 */
    private const val MAX_ONLINE_LOOKUPS = 80

    /** 搜索过滤 + 排序的纯函数，单独提出方便单测 */
    fun filterSort(songs: List<Song>, query: String, sort: LocalMusicSort): List<Song> {
      val keyword = query.trim()
      val filtered = if (keyword.isEmpty()) songs else songs.filter {
        it.title.contains(keyword, ignoreCase = true) ||
          it.artist.contains(keyword, ignoreCase = true) ||
          (it.album?.contains(keyword, ignoreCase = true) == true)
      }
      return when (sort) {
        LocalMusicSort.TITLE -> filtered.sortedBy { it.title }
        LocalMusicSort.DURATION -> filtered.sortedByDescending { it.durationMs }
        LocalMusicSort.ADDED -> filtered.sortedByDescending { it.lastModifiedAt }
      }
    }
  }
}
