package com.dlust.ydhl2026.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.HomeViewModelFactory
import com.dlust.ydhl2026.ui.components.ArrowBackIcon
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.EmptyState
import com.dlust.ydhl2026.ui.components.MiniPlayerBar
import com.dlust.ydhl2026.ui.components.MusicNoteIcon
import com.dlust.ydhl2026.ui.components.SearchIcon
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.home.mosaic.MosaicCanvas
import com.dlust.ydhl2026.ui.home.mosaic.MosaicEntry
import com.dlust.ydhl2026.ui.home.mosaic.MosaicScrollState
import com.dlust.ydhl2026.ui.localMusicStore
import com.dlust.ydhl2026.ui.musicRepository
import com.dlust.ydhl2026.ui.player.NowPlayingPanelLayer
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/**
 * 首页（Folia 风格海报墙）。
 *
 * 页面只有一层滚动容器：两列瀑布流。歌单、歌曲混在一起按封面铺满，不再有卡片底色和分区大标题。
 * 点歌曲会先起播，再把播放面板从原位放大展开；点歌单进入歌单详情。
 */
@Composable
fun HomeScreen(
  playerViewModel: PlayerViewModel,
  onPlaylistClick: (Playlist) -> Unit = {},
  onOpenLocalMusic: () -> Unit = {},
  onOpenProfile: () -> Unit = {},
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

  var panelOpen by rememberSaveable { mutableStateOf(false) }
  var showSearch by rememberSaveable { mutableStateOf(false) }

  val background = if (isDark) EchoBackgroundDark else EchoBackground

  // 每个 Tab 各记一份画布位置，切回来还停在原处
  val scrollStates = remember { HomeTab.entries.associateWith { MosaicScrollState() } }
  var canvasDragging by remember { mutableStateOf(false) }
  // 拖动时头部整体淡出，避免挡住正在看的内容；停手后自动恢复
  val headerAlpha by animateFloatAsState(
    targetValue = if (canvasDragging) 0.28f else 1f,
    animationSpec = tween(220),
    label = "homeHeaderAlpha"
  )

  val recommendedEntries = remember(state.recentSongs, state.playlists) {
    recommendEntries(state.recentSongs, state.playlists)
  }
  val recentEntries = remember(state.recentSongs) {
    state.recentSongs.map { MosaicEntry.Track(it, state.recentSongs) }
  }
  val playlistEntries = remember(state.playlists) {
    state.playlists.map { MosaicEntry.Collection(it) }
  }
  val localEntries = remember(state.localSongs) {
    state.localSongs.map { MosaicEntry.Track(it, state.localSongs) }
  }

  Box(modifier = modifier.fillMaxSize().background(background)) {
    // 画布铺满整屏（含状态栏与导航栏区域），头部只是浮在上面的一层
    when (state.selectedTab) {
      HomeTab.RECOMMEND -> MosaicCanvas(
        entries = recommendedEntries,
        currentSongId = playerState.currentSong?.id,
        isPlaying = playerState.isPlaying,
        isDark = isDark,
        scrollState = scrollStates.getValue(HomeTab.RECOMMEND),
        onTrackClick = { song, queue ->
          playerViewModel.onPlaySong(song, queue)
          panelOpen = true
        },
        onPlaylistClick = onPlaylistClick,
        onDragStateChange = { canvasDragging = it },
        modifier = Modifier.fillMaxSize()
      )

      HomeTab.RECENT -> MosaicCanvas(
        entries = recentEntries,
        currentSongId = playerState.currentSong?.id,
        isPlaying = playerState.isPlaying,
        isDark = isDark,
        scrollState = scrollStates.getValue(HomeTab.RECENT),
        onTrackClick = { song, queue ->
          playerViewModel.onPlaySong(song, queue)
          panelOpen = true
        },
        onPlaylistClick = onPlaylistClick,
        onDragStateChange = { canvasDragging = it },
        modifier = Modifier.fillMaxSize()
      )

      HomeTab.PLAYLISTS -> MosaicCanvas(
        entries = playlistEntries,
        currentSongId = playerState.currentSong?.id,
        isPlaying = playerState.isPlaying,
        isDark = isDark,
        scrollState = scrollStates.getValue(HomeTab.PLAYLISTS),
        onTrackClick = { song, queue ->
          playerViewModel.onPlaySong(song, queue)
          panelOpen = true
        },
        onPlaylistClick = onPlaylistClick,
        onDragStateChange = { canvasDragging = it },
        modifier = Modifier.fillMaxSize()
      )

      HomeTab.LOCAL -> if (state.localSongs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 150.dp, bottom = 96.dp)) {
          EmptyState(
            title = "还没有本地歌曲",
            description = "扫描手机里已下载的音频文件，\n它们会直接出现在这面海报墙上。",
            isDark = isDark,
            icon = {
              MusicNoteIcon(
                modifier = Modifier.size(24.dp),
                color = EchoAccent.copy(alpha = 0.7f)
              )
            },
            actionLabel = "去扫描",
            onAction = onOpenLocalMusic
          )
        }
      } else {
        MosaicCanvas(
          entries = localEntries,
          currentSongId = playerState.currentSong?.id,
          isPlaying = playerState.isPlaying,
          isDark = isDark,
          scrollState = scrollStates.getValue(HomeTab.LOCAL),
          onTrackClick = { song, queue ->
            playerViewModel.onPlaySong(song, queue)
            panelOpen = true
          },
          onPlaylistClick = onPlaylistClick,
          onDragStateChange = { canvasDragging = it },
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    // 浮层头部：上下渐变铺一层底色，保证标题压在封面之上仍然可读
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .graphicsLayer { alpha = headerAlpha }
        .background(
          Brush.verticalGradient(
            0f to background.copy(alpha = 0.95f),
            0.7f to background.copy(alpha = 0.72f),
            1f to Color.Transparent
          )
        )
        .statusBarsPadding()
    ) {
      HomeTopBar(isDark = isDark, onSearch = { showSearch = true }, onProfile = onOpenProfile)
      HomeTabRow(
        selected = state.selectedTab,
        isDark = isDark,
        onSelect = { homeViewModel.onAction(HomeAction.SelectTab(it)) }
      )
      if (state.selectedTab == HomeTab.LOCAL && state.localSongs.isNotEmpty()) {
        LocalHeaderRow(
          count = state.localSongs.size,
          isDark = isDark,
          onRescan = onOpenLocalMusic
        )
      }
    }

    // 迷你条悬浮在墙底，细进度线 + 一行文字，不遮挡封面主体
    val currentSong = playerState.currentSong
    if (currentSong != null) {
      // 外层先铺页面底色再让出导航栏高度，保证三键导航下渐变不会被截断
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(background)
          .navigationBarsPadding()
      ) {
        MiniPlayerBar(
          song = currentSong,
          isPlaying = playerState.isPlaying,
          progress = if (playerState.durationMs > 0L) {
            playerState.positionMs.toFloat() / playerState.durationMs
          } else {
            0f
          },
          onTogglePlay = playerViewModel::onPlayPause,
          onClick = { panelOpen = true },
          isDark = isDark
        )
      }
    }

    if (showSearch) {
      SearchOverlay(
        state = state,
        isDark = isDark,
        onQueryChange = { homeViewModel.onAction(HomeAction.SearchQueryChange(it)) },
        onClose = {
          homeViewModel.onAction(HomeAction.SearchQueryChange(""))
          showSearch = false
        },
        onPlay = { song ->
          playerViewModel.onPlaySong(song, state.searchResults)
          homeViewModel.onAction(HomeAction.SearchQueryChange(""))
          showSearch = false
          panelOpen = true
        }
      )
    }

    if (panelOpen) {
      NowPlayingPanelLayer(
        playerViewModel = playerViewModel,
        onDismiss = { panelOpen = false }
      )
    }
  }
}

/** 推荐页把歌单插进歌曲之间，避免整面墙全是同一种内容 */
private fun recommendEntries(
  songs: List<Song>,
  playlists: List<Playlist>
): List<MosaicEntry> {
  val result = mutableListOf<MosaicEntry>()
  val rounds = maxOf(songs.size, playlists.size)
  for (index in 0 until rounds) {
    songs.getOrNull(index)?.let { result += MosaicEntry.Track(it, songs) }
    playlists.getOrNull(index)?.let { result += MosaicEntry.Collection(it) }
  }
  return result
}

/** 顶部：品牌字 + 搜索入口 + 头像 */
@Composable
private fun HomeTopBar(
  isDark: Boolean,
  onSearch: () -> Unit,
  onProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)) {
          append("EC")
        }
        withStyle(
          SpanStyle(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = EchoAccent
          )
        ) { append("H") }
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)) {
          append("O")
        }
      },
      color = if (isDark) EchoTextPrimaryDark else EchoTextPrimary,
      fontSize = 17.sp
    )
    Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .clickable(onClick = onSearch),
      contentAlignment = Alignment.Center
    ) {
      SearchIcon(
        modifier = Modifier.size(17.dp),
        color = if (isDark) EchoTextPrimaryDark.copy(alpha = 0.62f) else EchoTextSecondary
      )
    }
    Spacer(Modifier.width(8.dp))
    Box(
      modifier = Modifier
        .size(26.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFF9A825)))
        )
        .clickable(onClick = onProfile),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "H", color = Color(0xFF0B1020), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

/** 文字 Tab：只有选中态的字重和一条 14dp 下划线，没有任何胶囊底色 */
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
      .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(22.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HomeTab.entries.forEach { tab ->
      val active = tab == selected
      val color by animateColorAsState(
        targetValue = when {
          active -> if (isDark) EchoTextPrimaryDark else EchoTextPrimary
          isDark -> EchoTextPrimaryDark.copy(alpha = 0.34f)
          else -> EchoTextSecondary
        },
        animationSpec = tween(200),
        label = "homeTabColor"
      )
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { onSelect(tab) }
      ) {
        Text(
          text = tab.label,
          color = color,
          fontSize = 13.5.sp,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(5.dp))
        Box(
          modifier = Modifier
            .width(14.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(if (active) EchoAccent else Color.Transparent)
        )
      }
    }
  }
}

/** 本地页浮在画布上的一行小信息：数量 + 重新扫描 */
@Composable
private fun LocalHeaderRow(
  count: Int,
  isDark: Boolean,
  onRescan: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "本地 · $count 首",
      color = if (isDark) EchoTextPrimaryDark.copy(alpha = 0.6f) else EchoTextSecondary,
      fontSize = 11.sp
    )
    Spacer(Modifier.weight(1f))
    Text(
      text = "重新扫描",
      color = EchoAccent,
      fontSize = 11.sp,
      modifier = Modifier.clickable(onClick = onRescan)
    )
  }
}

/** 全屏搜索：沿用海报墙的深/浅底色，结果用最朴素的行列表 */
@Composable
private fun SearchOverlay(
  state: HomeUiState,
  isDark: Boolean,
  onQueryChange: (String) -> Unit,
  onClose: () -> Unit,
  onPlay: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  val background = if (isDark) EchoBackgroundDark else EchoBackground
  val primary = if (isDark) EchoTextPrimaryDark else EchoTextPrimary

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(background)
      .statusBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
      ) {
        ArrowBackIcon(
          modifier = Modifier.size(18.dp),
          color = if (isDark) EchoTextPrimaryDark.copy(alpha = 0.8f) else EchoTextPrimary
        )
      }
      Spacer(Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f))
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        BasicTextField(
          value = state.searchQuery,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(color = primary, fontSize = 14.sp),
          cursorBrush = SolidColor(EchoAccent),
          modifier = Modifier.fillMaxWidth(),
          decorationBox = { innerTextField ->
            if (state.searchQuery.isEmpty()) {
              Text(text = "搜索歌名、歌手或专辑", color = EchoTextSecondary, fontSize = 13.sp)
            }
            innerTextField()
          }
        )
      }
    }

    if (state.searchQuery.isBlank()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "输入关键词开始搜索", color = EchoTextSecondary, fontSize = 12.5.sp)
      }
    } else if (state.searchResults.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "没有找到相关歌曲", color = EchoTextSecondary, fontSize = 12.5.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
      ) {
        items(items = state.searchResults, key = { it.id }) { song ->
          SearchResultRow(song = song, isDark = isDark, onClick = { onPlay(song) })
        }
      }
    }
  }
}

@Composable
private fun SearchResultRow(
  song: Song,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CoverArt(
      seed = song.id,
      // 与本地/歌单列表的缩略图保持一致：没有真实封面时显示歌名首字
      label = song.title.take(1),
      labelSize = 17.sp,
      cornerRadius = 3.dp,
      modifier = Modifier.size(44.dp),
      artUrl = song.albumArtUrl
    )
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        color = if (isDark) EchoTextPrimaryDark else EchoTextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = song.artist,
        color = EchoTextSecondary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Text(text = formatDuration(song.durationMs), color = EchoTextSecondary, fontSize = 11.sp)
  }
}
