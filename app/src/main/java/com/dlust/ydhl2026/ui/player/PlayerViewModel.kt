package com.dlust.ydhl2026.ui.player

import androidx.lifecycle.ViewModel
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 播放器 UI 状态：UDF 里的唯一数据源，界面只读不改 */
data class PlayerUiState(
  val currentSong: Song? = null,
  val isPlaying: Boolean = false,
  val repeatMode: Int = 0,        // 0=顺序 1=单曲 2=随机
  val queue: List<Song> = emptyList()
)

/**
 * 播放状态持有者。
 * 本轮播放是「假」的：只切换状态，不发声、不推进进度。
 * 阶段四接入 Media3 时，把这里的实现换成 MediaController 调用即可，界面不用改。
 */
class PlayerViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  /** 播放某首歌，并把整份播放队列一起带进来（用于上一首/下一首） */
  fun onPlaySong(song: Song, queue: List<Song>) {
    _uiState.update { it.copy(currentSong = song, isPlaying = true, queue = queue) }
  }

  fun onPlayPause() {
    if (_uiState.value.currentSong == null) return
    _uiState.update { it.copy(isPlaying = !it.isPlaying) }
  }

  fun onNext() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    val next = state.queue[(index + 1) % state.queue.size]
    _uiState.update { it.copy(currentSong = next, isPlaying = true) }
  }

  fun onPrevious() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    val previous = state.queue[(index - 1 + state.queue.size) % state.queue.size]
    _uiState.update { it.copy(currentSong = previous, isPlaying = true) }
  }

  fun onRepeatModeChange() {
    _uiState.update { it.copy(repeatMode = (it.repeatMode + 1) % 3) }
  }
}
