package com.dlust.ydhl2026.data.playback

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放引擎对外暴露的瞬时状态。
 * 只保留界面真正需要的三个字段，避免把 ExoPlayer 的细节泄漏到 UI 层。
 */
data class PlaybackSnapshot(
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L
)

/**
 * 播放器抽象。
 * 上层（PlayerViewModel）只依赖这个接口，播放实现可以随时替换
 * （当前是 Media3/ExoPlayer，换成 MediaSession 或自研解码器都不用改 UI）。
 */
interface PlaybackEngine {

  /** 播放位置 / 播放中 / 总时长，播放器内部按固定间隔刷新 */
  val state: StateFlow<PlaybackSnapshot>

  /** 一首歌自然播放结束时发一次，用于自动续播 */
  val completed: SharedFlow<Unit>

  /** 换歌并从头发送播放 */
  fun play(url: String)

  /** 暂停或继续当前歌曲 */
  fun setPlaying(playing: Boolean)

  /** 跳转到指定毫秒 */
  fun seekTo(positionMs: Long)

  /** 释放底层资源 */
  fun release()
}
