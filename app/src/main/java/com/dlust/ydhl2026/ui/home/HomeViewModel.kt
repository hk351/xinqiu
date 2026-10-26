package com.dlust.ydhl2026.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页 UI 状态 */
data class HomeUiState(
  val selectedTab: HomeTab = HomeTab.PLAYLISTS,
  val playlists: List<Playlist> = emptyList(),
  val recentSongs: List<Song> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<Song> = emptyList(),
  val isLoading: Boolean = true
)

/** 界面能发出的操作（UDF：事件单向流入 ViewModel） */
sealed interface HomeAction {
  data class SelectTab(val tab: HomeTab) : HomeAction
  data class SearchQueryChange(val query: String) : HomeAction
}

/**
 * 首页状态管理。
 * 页面只负责把 HomeAction 丢进来，其余全部由这里改状态。
 */
class HomeViewModel(private val repository: MusicRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null

  init {
    viewModelScope.launch {
      combine(repository.playlists(), repository.recentSongs()) { playlists, recent ->
        playlists to recent
      }.collect { (playlists, recent) ->
        _uiState.update {
          it.copy(playlists = playlists, recentSongs = recent, isLoading = false)
        }
      }
    }
  }

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.SelectTab ->
        _uiState.update { it.copy(selectedTab = action.tab) }

      is HomeAction.SearchQueryChange -> {
        _uiState.update { it.copy(searchQuery = action.query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
          val results = repository.search(action.query)
          _uiState.update { it.copy(searchResults = results) }
        }
      }
    }
  }
}
