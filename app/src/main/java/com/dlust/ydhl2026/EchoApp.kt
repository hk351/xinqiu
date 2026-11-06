package com.dlust.ydhl2026

import android.app.Application
import com.dlust.ydhl2026.data.playback.Media3PlaybackEngine
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.repository.MockMusicRepository
import com.dlust.ydhl2026.data.repository.MusicRepository

/**
 * 手动 DI 容器（本课程不使用 Hilt）：
 * Repository 与播放引擎在这里创建，ViewModel 通过构造函数接收，依赖关系一眼可见。
 */
class EchoApp : Application() {

  lateinit var musicRepository: MusicRepository
    private set

  /** 全应用共用一个 ExoPlayer：迷你条、全屏播放器操作的是同一份播放状态 */
  lateinit var playbackEngine: PlaybackEngine
    private set

  override fun onCreate() {
    super.onCreate()
    // 当前是 Mock 数据源；接入真实数据时只替换这一行
    musicRepository = MockMusicRepository()
    playbackEngine = Media3PlaybackEngine(this)
  }

  override fun onTerminate() {
    playbackEngine.release()
    super.onTerminate()
  }
}
