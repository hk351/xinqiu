package com.dlust.ydhl2026.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dlust.ydhl2026.EchoApp
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.ui.home.HomeViewModel

/**
 * 手动 DI 的落地方式：从 Application 里取 Repository，再用工厂把依赖传给 ViewModel。
 * 不使用 Hilt，依赖关系全部显式可见。
 */
fun Context.musicRepository(): MusicRepository =
  (applicationContext as EchoApp).musicRepository

class HomeViewModelFactory(
  private val repository: MusicRepository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    HomeViewModel(repository) as T
}
