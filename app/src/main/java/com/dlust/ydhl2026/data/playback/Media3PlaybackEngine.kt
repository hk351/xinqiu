package com.dlust.ydhl2026.data.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 基于 Media3 ExoPlayer 的真实播放实现。
 *
 * 关键点：
 * - AudioAttributes 指定 USAGE_MEDIA 并打开音频焦点，来电/其他应用播放时自动避让；
 * - Player.Listener 收到 STATE_ENDED 时发出 completed，由 ViewModel 决定下一首还是单曲循环；
 * - 播放位置没有现成回调，这里在主线程每 250ms 采样一次推到 StateFlow，供进度条使用。
 */
class Media3PlaybackEngine(context: Context) : PlaybackEngine {

  private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
    setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build(),
      /* handleAudioFocus = */ true
    )
    playWhenReady = false
  }

  private val _state = MutableStateFlow(PlaybackSnapshot())
  override val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

  private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  override val completed: SharedFlow<Unit> = _completed.asSharedFlow()

  private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
  private var tickJob: Job? = null

  init {
    player.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) _completed.tryEmit(Unit)
        publish()
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        publish()
        // 只有真正在走时长时才需要采样，暂停后停掉定时器省电
        if (isPlaying) startTicker() else stopTicker()
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.w(TAG, "播放失败：${error.errorCodeName}", error)
        publish()
      }
    })
  }

  override fun play(url: String) {
    player.setMediaItem(MediaItem.fromUri(url))
    player.prepare()
    player.playWhenReady = true
    publish()
  }

  override fun setPlaying(playing: Boolean) {
    player.playWhenReady = playing
    publish()
  }

  override fun seekTo(positionMs: Long) {
    player.seekTo(positionMs.coerceAtLeast(0L))
    publish()
  }

  override fun release() {
    stopTicker()
    scope.cancel()
    player.release()
  }

  private fun startTicker() {
    if (tickJob?.isActive == true) return
    tickJob = scope.launch {
      while (isActive) {
        publish()
        delay(TICK_MS)
      }
    }
  }

  private fun stopTicker() {
    tickJob?.cancel()
    tickJob = null
  }

  private fun publish() {
    _state.value = PlaybackSnapshot(
      isPlaying = player.playWhenReady,
      positionMs = player.currentPosition.coerceAtLeast(0L),
      // 未准备好时 Media3 返回 C.TIME_UNSET，这里统一折算成 0 交给上层兜底
      durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
    )
  }

  private companion object {
    const val TAG = "Media3PlaybackEngine"
    const val TICK_MS = 250L
  }
}
