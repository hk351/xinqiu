package com.dlust.ydhl2026.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 歌单详情页的 UI 状态 */
data class PlaylistUiState(
  val playlist: Playlist? = null,
  val songs: List<Song> = emptyList(),
  val isLoading: Boolean = true
)

/**
 * 歌单详情页状态管理。
 * 现接入的是网易云官方榜（热歌/飙升/新歌），榜单曲目即歌单曲目；
 * 滑动删除与拖拽排序只改内存里的列表，不写回服务端。
 */
class PlaylistViewModel(
  private val playlistId: Long,
  private val repository: MusicRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlaylistUiState())
  val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

  /** 最近一次删除的位置与歌曲，供 Snackbar 撤销 */
  private var lastRemoved: Pair<Int, Song>? = null

  init {
    viewModelScope.launch {
      val playlist = runCatching { repository.charts() }.getOrDefault(emptyList())
        .firstOrNull { it.id == playlistId }
      val songs = runCatching { repository.chartTracks(playlistId) }.getOrDefault(emptyList())
        .map { it.song }
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
    _uiState.update { it.copy(songs = current.filterNot { song -> song.id == songId }) }
  }

  /** 撤销上一次删除，放回原来的位置 */
  fun undoRemove() {
    val (index, song) = lastRemoved ?: return
    lastRemoved = null
    _uiState.update { state ->
      if (state.songs.any { it.id == song.id }) {
        state
      } else {
        val restored = state.songs.toMutableList().apply {
          add(index.coerceIn(0, size), song)
        }
        state.copy(songs = restored)
      }
    }
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
  }
}
