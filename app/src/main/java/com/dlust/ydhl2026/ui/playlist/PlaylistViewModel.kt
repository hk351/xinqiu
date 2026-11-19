package com.dlust.ydhl2026.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.local.playlist.PlaylistStore
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 拖拽时每一步都写库太频繁，停手后再落一次最终顺序 */
private const val ORDER_SAVE_DELAY_MS = 250L

/** 歌单详情页的 UI 状态 */
data class PlaylistUiState(
  val playlist: Playlist? = null,
  val songs: List<Song> = emptyList(),
  val isLoading: Boolean = true
)

/**
 * 歌单详情页状态管理。
 *
 * 榜单曲目仍来自网易云，但用户的删除与拖拽顺序会写进本地 SQLite，
 * 重启后保持上次调整的结果；撤销删除也一并持久化。
 */
class PlaylistViewModel(
  private val playlistId: Long,
  private val repository: MusicRepository,
  private val store: PlaylistStore
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlaylistUiState())
  val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

  /** 最近一次删除的位置与歌曲，供 Snackbar 撤销 */
  private var lastRemoved: Pair<Int, Song>? = null
  private var orderJob: Job? = null

  init {
    viewModelScope.launch {
      val playlist = try {
        repository.charts().firstOrNull { it.id == playlistId }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        null
      }
      val tracks = try {
        repository.chartTracks(playlistId).map { it.song }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        emptyList()
      }
      // 数据库不可用时退回网络列表，至少保证页面能看
      val songs = if (playlist == null) {
        tracks
      } else {
        try {
          store.loadChart(playlist, tracks)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          tracks
        }
      }
      _uiState.update {
        it.copy(playlist = playlist, songs = songs, isLoading = false)
      }
    }
  }

  /** 删除一首歌，并记住它的原位置以便撤销 */
  fun removeSong(songId: String) {
    val current = _uiState.value.songs
    val index = current.indexOfFirst { it.id == songId }
    if (index < 0) return
    lastRemoved = index to current[index]
    orderJob?.cancel()
    _uiState.update { it.copy(songs = current.filterNot { song -> song.id == songId }) }
    persist { store.removeSong(playlistId, songId) }
  }

  /** 撤销上一次删除，放回原来的位置 */
  fun undoRemove() {
    val (index, song) = lastRemoved ?: return
    lastRemoved = null
    val current = _uiState.value.songs
    if (current.any { it.id == song.id }) return
    val restored = current.toMutableList().apply {
      add(index.coerceIn(0, size), song)
    }
    _uiState.update { it.copy(songs = restored) }
    orderJob?.cancel()
    persist { store.restoreSong(playlistId, song.id, restored.map { it.id }) }
  }

  /** 拖拽排序：把 songId 对应的歌往上/下移动 steps 行 */
  fun moveSongBy(songId: String, steps: Int) {
    val current = _uiState.value.songs
    if (steps == 0) return
    val from = current.indexOfFirst { it.id == songId }
    if (from < 0) return
    val to = (from + steps).coerceIn(0, current.lastIndex)
    if (to == from) return
    val reordered = current.toMutableList().apply { add(to, removeAt(from)) }
    _uiState.update { it.copy(songs = reordered) }
    scheduleOrderSave(reordered)
  }

  private fun scheduleOrderSave(songs: List<Song>) {
    orderJob?.cancel()
    orderJob = viewModelScope.launch {
      delay(ORDER_SAVE_DELAY_MS)
      try {
        store.saveOrder(playlistId, songs.map { it.id })
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // 本地写失败不影响继续操作，下次改动会再尝试
      }
    }
  }

  private fun persist(block: suspend () -> Unit) {
    viewModelScope.launch {
      try {
        block()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // 本地写失败不打断列表操作
      }
    }
  }
}
