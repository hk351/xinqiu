package com.dlust.ydhl2026

import android.app.Application
import com.dlust.ydhl2026.data.local.LocalLyricsSource
import com.dlust.ydhl2026.data.local.LocalMusicSource
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.local.LyricsFolderStore
import com.dlust.ydhl2026.data.local.MediaStoreLocalMusicSource
import com.dlust.ydhl2026.data.local.SafLrcLyricsSource
import com.dlust.ydhl2026.data.lyrics.NeteaseLyricsSource
import com.dlust.ydhl2026.data.lyrics.OnlineLyricsSource
import com.dlust.ydhl2026.data.playback.Media3PlaybackEngine
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.repository.MockMusicRepository
import com.dlust.ydhl2026.data.repository.MusicRepository

/**
 * 手动 DI 容器（本课程不使用 Hilt）：
 * Repository、播放引擎、本地音乐扫描都在这里创建，ViewModel 通过构造函数接收，依赖关系一眼可见。
 */
class EchoApp : Application() {

  lateinit var musicRepository: MusicRepository
    private set

  /** 全应用共用一个 ExoPlayer：迷你条、全屏播放器操作的是同一份播放状态 */
  lateinit var playbackEngine: PlaybackEngine
    private set

  lateinit var localMusicSource: LocalMusicSource
    private set

  lateinit var localLyricsSource: LocalLyricsSource
    private set

  lateinit var onlineLyricsSource: OnlineLyricsSource
    private set

  /** 用户选过的歌词文件夹（SAF 树 URI），本地音乐页读写 */
  val lyricsFolderStore by lazy { LyricsFolderStore(this) }

  /** 本地音乐扫描结果的内存缓存，首页入口卡与本地音乐页共享 */
  val localMusicStore = LocalMusicStore()

  override fun onCreate() {
    super.onCreate()
    // 当前是 Mock 数据源；接入真实数据时只替换这一行
    musicRepository = MockMusicRepository()
    playbackEngine = Media3PlaybackEngine(this)
    localMusicSource = MediaStoreLocalMusicSource(this)
    localLyricsSource = SafLrcLyricsSource(this, lyricsFolderStore)
    onlineLyricsSource = NeteaseLyricsSource()
  }

  override fun onTerminate() {
    playbackEngine.release()
    super.onTerminate()
  }
}
