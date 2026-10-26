package com.dlust.ydhl2026

import android.app.Application
import com.dlust.ydhl2026.data.repository.MockMusicRepository
import com.dlust.ydhl2026.data.repository.MusicRepository

/**
 * 手动 DI 容器（本课程不使用 Hilt）：
 * Repository 在这里创建，ViewModel 通过构造函数接收，依赖关系一眼可见。
 */
class EchoApp : Application() {

  lateinit var musicRepository: MusicRepository
    private set

  override fun onCreate() {
    super.onCreate()
    // 当前是 Mock 数据源；接入真实数据时只替换这一行
    musicRepository = MockMusicRepository()
  }
}
