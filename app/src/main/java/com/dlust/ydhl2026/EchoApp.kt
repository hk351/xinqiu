package com.dlust.ydhl2026

import android.app.Application
import com.dlust.ydhl2026.data.local.LocalLyricsSource
import com.dlust.ydhl2026.data.local.LocalMusicSource
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.local.LyricsFolderStore
import com.dlust.ydhl2026.data.local.MediaStoreLocalMusicSource
import com.dlust.ydhl2026.data.local.PlayHistoryStore
import com.dlust.ydhl2026.data.local.SafLrcLyricsSource
import com.dlust.ydhl2026.data.lyrics.FileLyricsCache
import com.dlust.ydhl2026.data.lyrics.NeteaseLyricsSource
import com.dlust.ydhl2026.data.lyrics.OnlineLyricsSource
import com.dlust.ydhl2026.data.playback.Media3PlaybackEngine
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.remote.ChartCache
import com.dlust.ydhl2026.data.remote.NeteaseApi
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.data.repository.NeteaseMusicRepository
import com.dlust.ydhl2026.data.settings.ThemeModeStore
import com.dlust.ydhl2026.data.settings.createPlayerBackdropStore
import com.dlust.ydhl2026.data.settings.createThemeModeStore

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

  /** 播放背景的用户偏好，面板里改完即持久化 */
  val playerBackdropStore by lazy { createPlayerBackdropStore(this) }

  /** 深/浅色主题，登录页右上角切换后全应用共用 */
  val themeModeStore: ThemeModeStore by lazy { createThemeModeStore(this) }

  /** 网络歌歌词的磁盘缓存，播放器取词后写入，重播不再请求 */
  val lyricsCache by lazy { FileLyricsCache(this) }

  /** 本地音乐扫描结果的内存缓存，首页入口卡与本地音乐页共享 */
  val localMusicStore = LocalMusicStore()

  /** 最近播放记录，首页「最近」Tab 与播放器共同读写 */
  val playHistoryStore by lazy { PlayHistoryStore(this) }

  override fun onCreate() {
    super.onCreate()
    // 榜单数据源：热歌榜/飙升榜/新歌榜 + 每日缓存 + 播放历史
    musicRepository = NeteaseMusicRepository(
      api = NeteaseApi(),
      cache = ChartCache(this),
      history = playHistoryStore
    )
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
