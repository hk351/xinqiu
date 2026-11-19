package com.dlust.ydhl2026.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlust.ydhl2026.data.local.LocalMusicStore
import com.dlust.ydhl2026.data.model.ChartTrack
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.data.repository.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 搜索防抖时长：太短等于没防抖，太长输入手感会发涩 */
private const val SEARCH_DEBOUNCE_MS = 300L

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
)

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
          // 输入停顿 300ms 再查：边打边发既费流量，也容易让旧结果盖住新结果
          delay(SEARCH_DEBOUNCE_MS)
          val results = try {
            repository.search(action.query)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            emptyList()
          }
          // 结果回来时关键词可能已经变了，旧结果直接丢弃
          if (_uiState.value.searchQuery != action.query) return@launch
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
      // 取消是正常流程：runCatching 会把它一起吞掉，导致旧任务把新任务的状态覆盖掉
      val charts = try {
        repository.charts()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        emptyList()
      }
      val recommended = try {
        repository.recommended()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        emptyList()
      }
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
