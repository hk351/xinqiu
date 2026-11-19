package com.dlust.ydhl2026.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页 UI 状态 */
data class HomeUiState(
  val selectedTab: HomeTab = HomeTab.RECOMMEND,
  /** 歌单 Tab：三个官方榜单 */
  val charts: List<Playlist> = emptyList(),
  /** 推荐页：热歌榜按名次排好的曲目 */
  val recommended: List<ChartTrack> = emptyList(),
  val recentSongs: List<Song> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<Song> = emptyList(),
  val localSongs: List<Song> = emptyList(),
  val isLoading: Boolean = true,
  /** 榜单拉取失败时的提示；有缓存兜底时为 null */
  val errorMessage: String? = null
) {
  val localSongCount: Int get() = localSongs.size
}

/** 界面能发出的操作（UDF：事件单向流入 ViewModel） */
sealed interface HomeAction {
  data class SelectTab(val tab: HomeTab) : HomeAction
  data class SearchQueryChange(val query: String) : HomeAction
  /** 榜单拉取失败后手动重试 */
  data object Retry : HomeAction
}

/**
 * 首页状态管理。
 * 页面只负责把 HomeAction 丢进来，其余全部由这里改状态。
 */
class HomeViewModel(
  private val repository: MusicRepository,
  private val localMusicStore: LocalMusicStore
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null
  private var chartJob: Job? = null

  init {
    // 本地歌与最近播放是本地流，先订阅着；榜单要走网络，单独加载
    viewModelScope.launch {
      localMusicStore.songs.collect { localSongs ->
        _uiState.update { it.copy(localSongs = localSongs) }
      }
    }
    viewModelScope.launch {
      repository.recentSongs().collect { recent ->
        _uiState.update { it.copy(recentSongs = recent) }
      }
    }
    loadCharts()
  }

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.SelectTab ->
        _uiState.update { it.copy(selectedTab = action.tab) }

      is HomeAction.Retry -> loadCharts()

      is HomeAction.SearchQueryChange -> {
        _uiState.update { it.copy(searchQuery = action.query) }
        searchJob?.cancel()
        if (action.query.isBlank()) {
          _uiState.update { it.copy(searchResults = emptyList()) }
          return
        }
        searchJob = viewModelScope.launch {
          val results = runCatching { repository.search(action.query) }.getOrDefault(emptyList())
          _uiState.update { it.copy(searchResults = results) }
        }
      }
    }
  }

  /** 榜单与推荐页共用一次加载：推荐页就是热歌榜的前若干首 */
  private fun loadCharts() {
    chartJob?.cancel()
    chartJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val charts = runCatching { repository.charts() }.getOrDefault(emptyList())
      val recommended = runCatching { repository.recommended() }.getOrDefault(emptyList())
      _uiState.update {
        it.copy(
          charts = charts,
          recommended = recommended,
          isLoading = false,
          // 接口失败且没有任何缓存时才会是空列表，此时给用户一个重试入口
          errorMessage = if (recommended.isEmpty()) "榜单加载失败，请检查网络后重试" else null
        )
      }
    }
  }
}
