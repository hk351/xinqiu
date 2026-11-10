package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.HomeViewModelFactory
import com.dlust.ydhl2026.ui.components.MiniPlayerBar
import com.dlust.ydhl2026.ui.localMusicStore
import com.dlust.ydhl2026.ui.musicRepository
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.EchoBlue
import com.dlust.ydhl2026.ui.theme.EchoBlueLight
import com.dlust.ydhl2026.ui.theme.EchoBorder
import com.dlust.ydhl2026.ui.theme.EchoBorderDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary

private val DarkTextPrimary = Color(0xFFE8EAED)

/**
 * 首页（原型图2 的歌单页）。
 *
 * 组装方式遵循计划：
 * - 迷你播放器挂在 Scaffold.bottomBar，属于全局层，切 Tab 不会消失；
 * - 顶部三个胶囊 Tab 切换主体内容；
 * - 主体只有一个滚动容器（LazyVerticalGrid），「最近播放」的横向列表作为网格里的整行 item。
 *
 * 页面只做两件事：把 HomeAction 丢给 ViewModel、把状态画出来（UDF）。
 */
@Composable
fun HomeScreen(
  playerViewModel: PlayerViewModel,
  onOpenPlayer: () -> Unit = {},
  onPlaylistClick: (Playlist) -> Unit = {},
  onOpenLocalMusic: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val repository = remember { context.musicRepository() }
  val localMusicStore = remember { context.localMusicStore() }
  val homeViewModel: HomeViewModel =
    viewModel(factory = HomeViewModelFactory(repository, localMusicStore))
  val state by homeViewModel.uiState.collectAsStateWithLifecycle()
  val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
  val isDark = isSystemInDarkTheme()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = if (isDark) EchoBackgroundDark else EchoBackground,
    bottomBar = {
      playerState.currentSong?.let { song ->
        MiniPlayerBar(
          song = song,
          isPlaying = playerState.isPlaying,
          onTogglePlay = playerViewModel::onPlayPause,
          onClick = onOpenPlayer
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      HomeTabRow(
        selected = state.selectedTab,
        isDark = isDark,
        onSelect = { homeViewModel.onAction(HomeAction.SelectTab(it)) }
      )
      when (state.selectedTab) {
        HomeTab.PLAYLISTS -> PlaylistsTab(
          state = state,
          isDark = isDark,
          onPlaylistClick = onPlaylistClick,
          onOpenLocalMusic = onOpenLocalMusic,
          onPlaySong = { song -> playerViewModel.onPlaySong(song, state.recentSongs) }
        )

        HomeTab.SEARCH -> SearchTabContent(
          query = state.searchQuery,
          results = state.searchResults,
          isDark = isDark,
          onQueryChange = { homeViewModel.onAction(HomeAction.SearchQueryChange(it)) },
          onPlaySong = { song -> playerViewModel.onPlaySong(song, state.searchResults) }
        )

        HomeTab.FAVORITES -> FavoritesTabContent(isDark = isDark)
      }
    }
  }
}

/** 顶部胶囊 Tab 条：选中填充品牌蓝，未选中只留描边 */
@Composable
private fun HomeTabRow(
  selected: HomeTab,
  isDark: Boolean,
  onSelect: (HomeTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HomeTab.entries.forEach { tab ->
      val active = tab == selected
      val shape = RoundedCornerShape(17.dp)
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(shape)
          .background(if (active) EchoBlue else Color.Transparent)
          .border(
            width = 1.dp,
            color = if (active) Color.Transparent else if (isDark) EchoBorderDark else EchoBorder,
            shape = shape
          )
          .clickable { onSelect(tab) }
          .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = tab.emoji, fontSize = 12.sp)
          Spacer(Modifier.width(4.dp))
          Text(
            text = tab.label,
            color = if (active) Color.White else if (isDark) DarkTextPrimary else EchoTextPrimary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
          )
        }
      }
    }
  }
}

/**
 * 歌单 Tab：一个 LazyVerticalGrid 承载全部内容。
 * 标题和「最近播放」用整行 span item 插入，保证只有一个纵向滚动容器。
 */
@Composable
private fun PlaylistsTab(
  state: HomeUiState,
  isDark: Boolean,
  onPlaylistClick: (Playlist) -> Unit,
  onOpenLocalMusic: () -> Unit,
  onPlaySong: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      LocalMusicEntryCard(
        songCount = state.localSongCount,
        isDark = isDark,
        onClick = onOpenLocalMusic
      )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      SectionHeader(title = "我的歌单", isDark = isDark)
    }
    items(items = state.playlists, key = { it.id }) { playlist ->
      PlaylistCoverCard(
        playlist = playlist,
        isDark = isDark,
        onClick = { onPlaylistClick(playlist) }
      )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        SectionHeader(title = "最近播放", isDark = isDark)
        Spacer(Modifier.height(12.dp))
        RecentPlayedRow(
          songs = state.recentSongs,
          isDark = isDark,
          onPlaySong = onPlaySong
        )
      }
    }
  }
}

/**
 * 本地音乐入口卡：放在「我的歌单」上方的整行渐变卡。
 * 扫描过就显示歌曲数，否则引导「读取手机里的歌曲」。
 */
@Composable
private fun LocalMusicEntryCard(
  songCount: Int,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(18.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        Brush.horizontalGradient(listOf(EchoBlue, EchoBlueLight))
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(13.dp))
        .background(Color.White.copy(alpha = 0.17f)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "♪", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.width(13.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "本地音乐",
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(Modifier.height(3.dp))
      Text(
        text = if (songCount > 0) "已扫描到 $songCount 首歌曲" else "扫描手机里的歌曲",
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 11.5.sp
      )
    }
    Text(text = "›", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
  }
}
