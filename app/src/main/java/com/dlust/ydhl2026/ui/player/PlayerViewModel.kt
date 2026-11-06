package com.dlust.ydhl2026.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 播放器 UI 状态：UDF 里的唯一数据源，界面只读不改 */
data class PlayerUiState(
  val currentSong: Song? = null,
  val isPlaying: Boolean = false,
  val repeatMode: Int = 0,        // 0=顺序 1=单曲 2=随机
  val queue: List<Song> = emptyList(),
  val positionMs: Long = 0L,
  val durationMs: Long = 0L
)

/**
 * 播放状态持有者。
 * 本轮播放仍是「假」的：不发声，但进度由定时器推进，UI 能完整演示。
 * 阶段四接入 Media3 时，把 advance() 换成 MediaController 的位置回调即可，界面不用改。
 */
class PlayerViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  private var tickJob: Job? = null

  /** 播放某首歌，并把整份播放队列一起带进来（用于上一首/下一首） */
  fun onPlaySong(song: Song, queue: List<Song>) {
    _uiState.update {
      it.copy(
        currentSong = song,
        isPlaying = true,
        queue = queue,
        positionMs = 0L,
        durationMs = song.durationMs
      )
    }
    startTicker()
  }

  fun onPlayPause() {
    val state = _uiState.value
    if (state.currentSong == null) return
    val playing = !state.isPlaying
    _uiState.update { it.copy(isPlaying = playing) }
    if (playing) startTicker() else stopTicker()
  }

  fun onNext() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    val next = state.queue[(index + 1) % state.queue.size]
    _uiState.update {
      it.copy(currentSong = next, isPlaying = true, positionMs = 0L, durationMs = next.durationMs)
    }
    startTicker()
  }

  fun onPrevious() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    val previous = state.queue[(index - 1 + state.queue.size) % state.queue.size]
    _uiState.update {
      it.copy(
        currentSong = previous,
        isPlaying = true,
        positionMs = 0L,
        durationMs = previous.durationMs
      )
    }
    startTicker()
  }

  fun onRepeatModeChange() {
    _uiState.update { it.copy(repeatMode = (it.repeatMode + 1) % 3) }
  }

  /** 在播放队列面板里点某一首：直接切过去并从头播放 */
  fun onPlayFromQueue(index: Int) {
    val state = _uiState.value
    val song = state.queue.getOrNull(index) ?: return
    _uiState.update {
      it.copy(
        currentSong = song,
        isPlaying = true,
        positionMs = 0L,
        durationMs = song.durationMs
      )
    }
    startTicker()
  }

  /** 播放队列拖拽排序：把 songId 对应的歌曲前后移动 steps 行（越界即停在两端） */
  fun onMoveQueueBy(songId: String, steps: Int) {
    val state = _uiState.value
    val from = state.queue.indexOfFirst { it.id == songId }
    if (from < 0) return
    val to = (from + steps).coerceIn(0, state.queue.lastIndex)
    if (to == from) return
    val moved = state.queue.toMutableList().apply { add(to, removeAt(from)) }
    _uiState.update { it.copy(queue = moved) }
  }

  /** 拖动进度条：把位置直接跳到指定毫秒 */
  fun onSeekTo(positionMs: Long) {
    val duration = _uiState.value.durationMs
    _uiState.update { it.copy(positionMs = positionMs.coerceIn(0L, duration)) }
  }

  private fun startTicker() {
    stopTicker()
    tickJob = viewModelScope.launch {
      var last = System.currentTimeMillis()
      while (isActive) {
        delay(200)
        val now = System.currentTimeMillis()
        val delta = now - last
        last = now
        advance(delta)
      }
    }
  }

  private fun stopTicker() {
    tickJob?.cancel()
    tickJob = null
  }

  private fun advance(deltaMs: Long) {
    val state = _uiState.value
    val song = state.currentSong ?: return
    val nextPosition = state.positionMs + deltaMs
    if (nextPosition < song.durationMs) {
      _uiState.update { it.copy(positionMs = nextPosition) }
      return
    }
    when (state.repeatMode) {
      1 -> _uiState.update { it.copy(positionMs = 0L) }          // 单曲循环
      2 -> {                                                     // 随机
        val others = state.queue.filter { it.id != song.id }
        if (others.isEmpty()) {
          _uiState.update { it.copy(positionMs = 0L) }
        } else {
          val pick = others.random()
          _uiState.update {
            it.copy(currentSong = pick, positionMs = 0L, durationMs = pick.durationMs)
          }
        }
      }

      else -> onNext()                                           // 顺序（到末尾回到第一首）
    }
  }
}
