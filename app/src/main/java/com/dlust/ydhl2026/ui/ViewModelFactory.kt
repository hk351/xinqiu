package com.dlust.ydhl2026.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dlust.ydhl2026.EchoApp
import com.dlust.ydhl2026.data.local.LocalLyricsSource
import com.dlust.ydhl2026.data.local.LocalMusicSource
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.local.LyricsFolderStore
import com.dlust.ydhl2026.data.lyrics.OnlineLyricsSource
import com.dlust.ydhl2026.data.playback.PlaybackEngine
import com.dlust.ydhl2026.data.repository.MusicRepository
import com.dlust.ydhl2026.data.settings.PlayerBackdropStore
import com.dlust.ydhl2026.ui.home.HomeViewModel
import com.dlust.ydhl2026.ui.local.LocalMusicViewModel
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

fun Context.localMusicSource(): LocalMusicSource =
  (applicationContext as EchoApp).localMusicSource

fun Context.localMusicStore(): LocalMusicStore =
  (applicationContext as EchoApp).localMusicStore

fun Context.localLyricsSource(): LocalLyricsSource =
  (applicationContext as EchoApp).localLyricsSource

fun Context.onlineLyricsSource(): OnlineLyricsSource =
  (applicationContext as EchoApp).onlineLyricsSource

fun Context.lyricsFolderStore(): LyricsFolderStore =
  (applicationContext as EchoApp).lyricsFolderStore

fun Context.playerBackdropStore(): PlayerBackdropStore =
  (applicationContext as EchoApp).playerBackdropStore

class HomeViewModelFactory(
  private val repository: MusicRepository,
  private val localMusicStore: LocalMusicStore
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    HomeViewModel(repository, localMusicStore) as T
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
  private val engine: PlaybackEngine,
  private val backdropStore: PlayerBackdropStore
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    PlayerViewModel(engine, backdropStore) as T
}

class LocalMusicViewModelFactory(
  private val source: LocalMusicSource,
  private val store: LocalMusicStore,
  private val lyricsSource: LocalLyricsSource,
  private val onlineLyricsSource: OnlineLyricsSource
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    LocalMusicViewModel(source, store, lyricsSource, onlineLyricsSource) as T
}
