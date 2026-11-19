package com.dlust.ydhl2026.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.local.PlayHistory
import com.dlust.ydhl2026.data.lyrics.LyricsCache
import com.dlust.ydhl2026.data.lyrics.OnlineLyricsSource
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.data.settings.PlayerBackdropStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 网易云来源标记，与 NeteaseApi 里写入 Song.source 的值保持一致 */
private const val NET_EASE_SOURCE = "netease"

/** 播放器 UI 状态：UDF 里的唯一数据源，界面只读不改 */
data class PlayerUiState(
  val currentSong: Song? = null,
  val isPlaying: Boolean = false,
  val repeatMode: Int = 0,        // 0=顺序 1=单曲 2=随机
  val queue: List<Song> = emptyList(),
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  /** 用户 DIY 的播放背景，面板与歌词舞台共用 */
  val backdrop: PlayerBackdrop = PlayerBackdrop.Default,
  /** 一次性提示（比如网络歌没有版权），界面展示后回调清空 */
  val notice: String? = null,
  /** 在线歌词正在取，舞台展示「正在加载歌词…」而不是「没有歌词」 */
  val lyricsLoading: Boolean = false
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
  private val engine: PlaybackEngine,
  private val backdropStore: PlayerBackdropStore,
  private val repository: MusicRepository,
  private val history: PlayHistory,
  private val lyricsSource: OnlineLyricsSource,
  private val lyricsCache: LyricsCache
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    // 背景是本地偏好，首帧就用读回来的值，避免面板先闪一下默认样式
    PlayerUiState(backdrop = backdropStore.backdrop.value)
  )
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  /** 正在进行的地址解析；用户快速切歌时取消上一次，避免旧请求把新歌覆盖掉 */
  private var playJob: Job? = null

  /** 正在进行的歌词请求，和地址解析同理，切歌就取消 */
  private var lyricsJob: Job? = null

  init {
    viewModelScope.launch {
      backdropStore.backdrop.collect { saved ->
        _uiState.update { it.copy(backdrop = saved) }
      }
    }
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
    viewModelScope.launch {
      engine.errors.collect {
        // 引擎已经停下来了，这里同步状态并提示；没有当前歌时（启动阶段误报）忽略
        val title = _uiState.value.currentSong?.title ?: return@collect
        _uiState.update {
          it.copy(isPlaying = false, notice = "「$title」播放失败，请检查网络后重试")
        }
      }
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
    history.record(song)
    resolveAndPlay(song)
    loadLyrics(song)
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

  /** 换预设或拖滑杆：先更新 UI，再交给 store 落盘 */
  fun onBackdropChange(backdrop: PlayerBackdrop) {
    val fixed = backdrop.coerce()
    _uiState.update { it.copy(backdrop = fixed) }
    backdropStore.update(fixed)
  }

  /** 切到某首歌并从头播放 */
  private fun switchTo(song: Song) {
    _uiState.update {
      it.copy(currentSong = song, isPlaying = true, positionMs = 0L, durationMs = song.durationMs)
    }
    history.record(song)
    resolveAndPlay(song)
    loadLyrics(song)
  }

  /**
   * 网络歌的歌词在首页/搜索里拿不到，点播时按 id 补取：
   * 先看磁盘缓存，没有再请求接口；回来后核对当前歌没被切走才写进状态。
   * 本地歌的歌词在扫描阶段就配好了，这里不再重复请求。
   */
  private fun loadLyrics(song: Song) {
    lyricsJob?.cancel()
    if (song.source != NET_EASE_SOURCE || song.lyrics.isNotEmpty()) {
      _uiState.update { it.copy(lyricsLoading = false) }
      return
    }
    _uiState.update { it.copy(lyricsLoading = true) }
    lyricsJob = viewModelScope.launch {
      val cached = lyricsCache.get(song.id)
      // 取消是切歌的正常流程，不能当成「请求失败」继续往下走，否则会把空结果写进状态
      val fetched = cached ?: try {
        lyricsSource.find(song)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        emptyList()
      }
      val lines = cached ?: fetched.also { if (it.isNotEmpty()) lyricsCache.put(song.id, it) }
      if (_uiState.value.currentSong?.id != song.id) return@launch
      _uiState.update { state ->
        state.copy(
          currentSong = state.currentSong?.copy(lyrics = lines),
          lyricsLoading = false
        )
      }
    }
  }

  /**
   * 网络歌的播放地址带时效，点播时才解析；本地歌直接用自己的路径。
   * 解析有耗时，期间用户可能又点了别的歌，所以返回后要核对当前歌是不是同一首。
   */
  private fun resolveAndPlay(song: Song) {
    playJob?.cancel()
    playJob = viewModelScope.launch {
      // 同理：取消必须透传，否则旧任务会把「取消」当成解析失败，
      // 在重新播放同一首歌时误弹「暂时没有可播放的音频」
      val url = try {
        repository.playUrl(song)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        null
      }
      if (_uiState.value.currentSong?.id != song.id) return@launch
      if (url.isNullOrBlank()) {
        _uiState.update {
          it.copy(isPlaying = false, notice = "「${song.title}」暂时没有可播放的音频")
        }
        return@launch
      }
      engine.play(url)
    }
  }

  /** 提示展示完，界面回调清空，避免重组时反复弹 */
  fun onNoticeShown() {
    _uiState.update { it.copy(notice = null) }
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
