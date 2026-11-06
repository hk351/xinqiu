package com.dlust.ydhl2026.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dlust.ydhl2026.EchoApp
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.ui.home.HomeViewModel
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.playlist.PlaylistViewModel

/**
 * 手动 DI 的落地方式：从 Application 里取 Repository，再用工厂把依赖传给 ViewModel。
 * 不使用 Hilt，依赖关系全部显式可见。
 */
fun Context.musicRepository(): MusicRepository =
  (applicationContext as EchoApp).musicRepository

fun Context.playbackEngine(): PlaybackEngine =
  (applicationContext as EchoApp).playbackEngine

class HomeViewModelFactory(
  private val repository: MusicRepository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    HomeViewModel(repository) as T
}

class PlaylistViewModelFactory(
  private val repository: MusicRepository,
  private val playlistId: Long
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    PlaylistViewModel(playlistId, repository) as T
}

class PlayerViewModelFactory(
  private val engine: PlaybackEngine
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    PlayerViewModel(engine) as T
}
