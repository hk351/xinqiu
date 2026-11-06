package com.dlust.ydhl2026.data.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 测试替身：记录 ViewModel 下发的播放指令，并允许手动模拟「这一首放完了」。
 * 不依赖 Android 框架，因此能跑在 JVM 单测里。
 */
class FakePlaybackEngine : PlaybackEngine {

  /** 每一次 play() 收到的地址，按顺序记录 */
  val playedUrls = mutableListOf<String>()

  /** 当前是否处于「用户希望播放」的状态 */
  var playing = false
    private set

  var lastSeekTo: Long? = null
    private set

  private val _state = MutableStateFlow(PlaybackSnapshot())
  override val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

  private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
  override val completed: SharedFlow<Unit> = _completed.asSharedFlow()

  override fun play(url: String) {
    playedUrls += url
    playing = true
    _state.value = _state.value.copy(isPlaying = true, positionMs = 0L)
  }

  override fun setPlaying(playing: Boolean) {
    this.playing = playing
    _state.value = _state.value.copy(isPlaying = playing)
  }

  override fun seekTo(positionMs: Long) {
    lastSeekTo = positionMs
    _state.value = _state.value.copy(positionMs = positionMs)
  }

  override fun release() = Unit

  /** 模拟当前歌曲自然播放结束，触发 ViewModel 的续播逻辑 */
  fun simulateCompletion() {
    _completed.tryEmit(Unit)
  }
}
