package com.dlust.ydhl2026.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
 *
 * 真实的「出声」交给 [PlaybackEngine]（当前是 Media3/ExoPlayer）：
 * - 界面上的交互都先更新 uiState，再调用引擎，保证按钮点击即时响应；
 * - 进度和时长反过来由引擎的 state 流驱动，避免再维护一套定时器；
 * - 一首歌自然结束（引擎 completed）时按循环模式决定单曲重播、随机切歌或下一首。
 */
class PlayerViewModel(
  private val engine: PlaybackEngine
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      engine.state.collect { snapshot ->
        _uiState.update { state ->
          state.copy(
            isPlaying = if (state.currentSong == null) false else snapshot.isPlaying,
            positionMs = snapshot.positionMs,
            // 网络流刚起播时引擎还不知道总时长，先用歌曲自带的时长兜底
            durationMs = if (snapshot.durationMs > 0) snapshot.durationMs else state.durationMs
          )
        }
      }
    }
    viewModelScope.launch {
      engine.completed.collect { handleCompletion() }
    }
  }

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
    engine.play(song.sourceUrl)
  }

  fun onPlayPause() {
    val state = _uiState.value
    if (state.currentSong == null) return
    val playing = !state.isPlaying
    _uiState.update { it.copy(isPlaying = playing) }
    engine.setPlaying(playing)
  }

  fun onNext() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    switchTo(state.queue[(index + 1) % state.queue.size])
  }

  fun onPrevious() {
    val state = _uiState.value
    val index = state.queue.indexOfFirst { it.id == state.currentSong?.id }
    if (index < 0 || state.queue.isEmpty()) return
    switchTo(state.queue[(index - 1 + state.queue.size) % state.queue.size])
  }

  fun onRepeatModeChange() {
    _uiState.update { it.copy(repeatMode = (it.repeatMode + 1) % 3) }
  }

  /** 在播放队列面板里点某一首：直接切过去并从头播放 */
  fun onPlayFromQueue(index: Int) {
    val song = _uiState.value.queue.getOrNull(index) ?: return
    switchTo(song)
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
    val target = positionMs.coerceIn(0L, _uiState.value.durationMs)
    _uiState.update { it.copy(positionMs = target) }
    engine.seekTo(target)
  }

  /** 切到某首歌并从头播放 */
  private fun switchTo(song: Song) {
    _uiState.update {
      it.copy(currentSong = song, isPlaying = true, positionMs = 0L, durationMs = song.durationMs)
    }
    engine.play(song.sourceUrl)
  }

  /** 引擎播完一首歌：按循环模式决定接下来放什么 */
  private fun handleCompletion() {
    val state = _uiState.value
    val song = state.currentSong ?: return
    when (state.repeatMode) {
      1 -> restartCurrent()                                     // 单曲循环
      2 -> {                                                    // 随机
        val others = state.queue.filter { it.id != song.id }
        if (others.isEmpty()) restartCurrent() else switchTo(others.random())
      }

      else -> onNext()                                          // 顺序（到末尾回到第一首）
    }
  }

  private fun restartCurrent() {
    _uiState.update { it.copy(isPlaying = true, positionMs = 0L) }
    engine.seekTo(0L)
    engine.setPlaying(true)
  }
}
