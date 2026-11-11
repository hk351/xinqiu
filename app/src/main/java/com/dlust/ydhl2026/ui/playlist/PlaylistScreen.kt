package com.dlust.ydhl2026.ui.playlist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.PlaylistViewModelFactory
import com.dlust.ydhl2026.ui.components.ArrowBackIcon
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.DragHandleIcon
import com.dlust.ydhl2026.ui.components.MiniPlayerBar
import com.dlust.ydhl2026.ui.components.PlayIcon
import com.dlust.ydhl2026.ui.components.ShuffleIcon
import com.dlust.ydhl2026.ui.components.TrashIcon
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.musicRepository
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.player.playerPaletteFor
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary
import kotlinx.coroutines.launch

private val SongRowHeight = 72.dp
private val TopBarHeight = 56.dp
private val DarkTextPrimary = EchoTextPrimaryDark
private val DarkTextSecondary = Color(0xFF9AA3AD)
private val DangerRed = Color(0xFFE5484D)

/**
 * 歌单详情页。
 *
 * 设计要点：
 * - 封面 / 歌名 / 歌曲数固定在列表首项，随滚动缩小淡出，顶部栏同步浮现歌单名；
 * - 歌曲行支持左滑删除（带撤销）与手柄拖拽排序，两者都带即时动画反馈；
 * - 点按有轻微缩放 + 悬浮/播放中背景着色；当前播放行显示跳动的音量条；
 * - 空歌单使用自绘插画空态，宽屏（≥600dp）自动切换为「封面在左、信息在右」布局。
 *
 * 数据层仍是 Mock：删除与排序只改 ViewModel 内存，接 Room 后替换实现即可。
 */
@Composable
fun PlaylistScreen(
  playlistId: Long,
  playerViewModel: PlayerViewModel,
  onBack: () -> Unit,
  onOpenPlayer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val repository = remember { context.musicRepository() }
  val viewModel: PlaylistViewModel =
    viewModel(factory = PlaylistViewModelFactory(repository, playlistId))
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
  // 播放进度每 200ms 变化一次，这里只派生真正要用到的三个字段，
  // 避免整页（含 LazyColumn）被无谓地重组
  val currentSong by remember { derivedStateOf { playerState.currentSong } }
  val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }
  val isPlaying by remember { derivedStateOf { playerState.isPlaying } }

  val isDark = isSystemInDarkTheme()
  val background = if (isDark) EchoBackgroundDark else EchoBackground
  val textPrimary = if (isDark) DarkTextPrimary else EchoTextPrimary
  val textSecondary = if (isDark) DarkTextSecondary else EchoTextSecondary
  val accent = remember(playlistId) { playerPaletteFor("playlist-$playlistId").primary }

  val listState = rememberLazyListState()
  val density = LocalDensity.current
  val collapseThreshold = with(density) { 180.dp.toPx() }
  val headerCollapse by remember(collapseThreshold) {
    derivedStateOf {
      if (listState.firstVisibleItemIndex > 0) {
        1f
      } else {
        (listState.firstVisibleItemScrollOffset / collapseThreshold).coerceIn(0f, 1f)
      }
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  // 撤销恢复行时改变 key，丢弃该行残留的"已滑出"状态
  var rowKeyNonce by remember { mutableIntStateOf(0) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = background,
    bottomBar = {
      currentSong?.let { song ->
        MiniPlayerBar(
          song = song,
          isPlaying = isPlaying,
          progress = if (playerState.durationMs > 0L) {
            playerState.positionMs.toFloat() / playerState.durationMs
          } else {
            0f
          },
          onTogglePlay = playerViewModel::onPlayPause,
          onClick = onOpenPlayer
        )
      }
    }
  ) { innerPadding ->
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Snackbar 放在内容层最上方，避免被全屏列表拦截点击
    Box(modifier = Modifier.fillMaxSize()) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      // 平板/横屏时把内容收进 840dp 的居中列，避免列表被拉得过宽
      val sidePadding = if (maxWidth > 840.dp) (maxWidth - 840.dp) / 2 else 0.dp

      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 12.dp + sidePadding,
          end = 12.dp + sidePadding,
          top = statusBarTop + TopBarHeight + 8.dp,
          bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
      ) {
        item(key = "header") {
          PlaylistHeader(
            playlistName = state.playlist?.name ?: "歌单",
            songCount = state.songs.size,
            totalMinutes = (state.songs.sumOf { it.durationMs } / 60_000L).toInt(),
            coverSeed = "playlist-$playlistId",
            coverLabel = (state.playlist?.name ?: "歌").take(1),
            accent = accent,
            collapse = headerCollapse,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onPlayAll = {
              state.songs.firstOrNull()?.let { playerViewModel.onPlaySong(it, state.songs) }
            },
            onShuffle = {
              state.songs.randomOrNull()?.let { playerViewModel.onPlaySong(it, state.songs) }
            }
          )
        }

        when {
          state.isLoading -> item(key = "loading") {
            LoadingRow(accent = accent, textSecondary = textSecondary)
          }

          state.songs.isEmpty() -> item(key = "empty") {
            EmptyPlaylistState(
              playlistExists = state.playlist != null,
              accent = accent,
              textPrimary = textPrimary,
              textSecondary = textSecondary,
              onBack = onBack
            )
          }

          else -> itemsIndexed(
            items = state.songs,
            key = { _, song -> "${song.id}-$rowKeyNonce" }
          ) { index, song ->
            val isCurrent = song.id == currentSongId
            PlaylistSongRow(
              index = index,
              song = song,
              isCurrent = isCurrent,
              isPlaying = isCurrent && isPlaying,
              accent = accent,
              textPrimary = textPrimary,
              textSecondary = textSecondary,
              surface = background,
              onPlay = { playerViewModel.onPlaySong(song, state.songs) },
              onRemove = {
                viewModel.removeSong(song.id)
                scope.launch {
                  val result = snackbarHostState.showSnackbar(
                    message = "已移除「${song.title}」",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Short
                  )
                  if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoRemove()
                    rowKeyNonce++
                  }
                }
              },
              onMoveBy = { steps -> viewModel.moveSongBy(song.id, steps) },
              modifier = Modifier.animateItem()
            )
          }
        }
      }

      PlaylistTopBar(
        title = state.playlist?.name.orEmpty(),
        collapse = headerCollapse,
        background = background,
        textPrimary = textPrimary,
        onBack = onBack
      )
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 12.dp)
          .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp)
      )
    }
  }
}

/** 顶部固定栏：透明起底，列表滚动后逐渐浮现背景与歌单名 */
@Composable
private fun PlaylistTopBar(
  title: String,
  collapse: Float,
  background: Color,
  textPrimary: Color,
  onBack: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background.copy(alpha = collapse))
      .statusBarsPadding()
      .height(TopBarHeight)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .clickable(onClick = onBack),
      contentAlignment = Alignment.Center
    ) {
      ArrowBackIcon(modifier = Modifier.size(20.dp), color = textPrimary)
    }
    Text(
      text = title,
      color = textPrimary.copy(alpha = collapse),
      fontSize = 16.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(start = 4.dp)
    )
    Spacer(Modifier.width(44.dp))
  }
}

/** 列表首项：封面 + 歌单信息 + 播放操作 */
@Composable
private fun PlaylistHeader(
  playlistName: String,
  songCount: Int,
  totalMinutes: Int,
  coverSeed: String,
  coverLabel: String,
  accent: Color,
  collapse: Float,
  textPrimary: Color,
  textSecondary: Color,
  onPlayAll: () -> Unit,
  onShuffle: () -> Unit
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val wide = maxWidth >= 600.dp
    val coverSize = if (wide) 128.dp else minOf(maxWidth * 0.34f, 124.dp)
    val coverScale = 1f - collapse * 0.16f
    val coverAlpha = 1f - collapse * 0.35f
    val meta = if (songCount == 0) "暂无歌曲" else "$songCount 首 · 约 $totalMinutes 分钟"

    if (wide) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HeaderCover(
          seed = coverSeed,
          label = coverLabel,
          size = coverSize,
          accent = accent,
          scale = coverScale,
          contentAlpha = coverAlpha
        )
        Spacer(Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
          HeaderTexts(playlistName, meta, textPrimary, textSecondary)
          Spacer(Modifier.height(18.dp))
          HeaderActions(
            enabled = songCount > 0,
            accent = accent,
            onPlayAll = onPlayAll,
            onShuffle = onShuffle
          )
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        HeaderCover(
          seed = coverSeed,
          label = coverLabel,
          size = coverSize,
          accent = accent,
          scale = coverScale,
          contentAlpha = coverAlpha
        )
        Spacer(Modifier.height(16.dp))
        HeaderTexts(playlistName, meta, textPrimary, textSecondary, centered = true)
        Spacer(Modifier.height(18.dp))
        HeaderActions(
          enabled = songCount > 0,
          accent = accent,
          onPlayAll = onPlayAll,
          onShuffle = onShuffle
        )
      }
    }
  }
}

@Composable
private fun HeaderCover(
  seed: String,
  label: String,
  size: Dp,
  accent: Color,
  scale: Float,
  contentAlpha: Float
) {
  CoverArt(
    seed = seed,
    label = label,
    cornerRadius = 16.dp,
    labelSize = if (size >= 120.dp) 40.sp else 32.sp,
    modifier = Modifier
      .size(size)
      .shadow(
        elevation = 18.dp,
        shape = RoundedCornerShape(16.dp),
        ambientColor = accent,
        spotColor = accent
      )
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = contentAlpha
      }
  )
}

@Composable
private fun HeaderTexts(
  playlistName: String,
  meta: String,
  textPrimary: Color,
  textSecondary: Color,
  centered: Boolean = false
) {
  Text(
    text = playlistName,
    color = textPrimary,
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.3.sp,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    textAlign = if (centered) TextAlign.Center else TextAlign.Start
  )
  Spacer(Modifier.height(6.dp))
  Text(
    text = meta,
    color = textSecondary,
    fontSize = 12.sp,
    letterSpacing = 0.4.sp,
    textAlign = if (centered) TextAlign.Center else TextAlign.Start
  )
}

@Composable
private fun HeaderActions(
  enabled: Boolean,
  accent: Color,
  onPlayAll: () -> Unit,
  onShuffle: () -> Unit
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ActionPill(
      text = "播放全部",
      filled = true,
      enabled = enabled,
      accent = accent,
      onClick = onPlayAll,
      leading = { PlayIcon(modifier = Modifier.size(13.dp), color = Color.White) }
    )
    ActionPill(
      text = "随机",
      filled = false,
      enabled = enabled,
      accent = accent,
      onClick = onShuffle,
      leading = { ShuffleIcon(modifier = Modifier.size(13.dp), color = accent) }
    )
  }
}

/** 胶囊按钮：实心为主操作，描边为次操作 */
@Composable
private fun ActionPill(
  text: String,
  filled: Boolean,
  enabled: Boolean,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leading: (@Composable () -> Unit)? = null
) {
  Row(
    modifier = modifier
      .height(40.dp)
      .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
      .clip(CircleShape)
      .background(if (filled) accent else Color.Transparent)
      .border(
        width = 1.dp,
        color = if (filled) Color.Transparent else accent.copy(alpha = 0.45f),
        shape = CircleShape
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 18.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    leading?.invoke()
    if (leading != null) Spacer(Modifier.width(8.dp))
    Text(
      text = text,
      color = if (filled) Color.White else accent,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

/**
 * 歌曲行：左滑删除 + 手柄拖拽排序 + 点按缩放。
 * 高度固定为 [SongRowHeight]，拖拽时按行高换算位移，跨过一行就实时换位，
 * 配合 animateItem 的位移动画形成连续的拖动反馈。
 */
@Composable
private fun PlaylistSongRow(
  index: Int,
  song: Song,
  isCurrent: Boolean,
  isPlaying: Boolean,
  accent: Color,
  textPrimary: Color,
  textSecondary: Color,
  surface: Color,
  onPlay: () -> Unit,
  onRemove: () -> Unit,
  onMoveBy: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val dismissState = rememberSwipeToDismissBoxState(
    positionalThreshold = { distance -> distance * 0.32f }
  )

  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val hovered by interactionSource.collectIsHoveredAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.985f else 1f,
    animationSpec = tween(durationMillis = 90),
    label = "rowPressScale"
  )

  val rowHeightPx = with(LocalDensity.current) { SongRowHeight.toPx() }
  var dragging by remember { mutableStateOf(false) }
  var dragOffset by remember { mutableFloatStateOf(0f) }

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    onDismiss = { direction ->
      if (direction == SwipeToDismissBoxValue.EndToStart) onRemove()
    },
    modifier = modifier
      .height(SongRowHeight)
      .zIndex(if (dragging) 2f else 0f),
    backgroundContent = {
      Row(
        modifier = Modifier
          .fillMaxSize()
          // 红层向内缩，静止时被不透明的行内容完全盖住，滑动时才露出
          .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(
            Brush.horizontalGradient(
              listOf(Color(0xFFB02328), DangerRed)
            )
          )
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TrashIcon(modifier = Modifier.size(18.dp), color = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(text = "移除", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      }
    }
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 4.dp)
        .graphicsLayer {
          val scale = if (dragging) 1.02f else pressScale
          scaleX = scale
          scaleY = scale
          translationY = if (dragging) dragOffset else 0f
        }
        .clip(RoundedCornerShape(12.dp))
        // 先铺页面底色盖住左滑删除层，再叠加交互着色
        .background(surface)
        .background(
          when {
            dragging -> accent.copy(alpha = 0.16f)
            isCurrent -> accent.copy(alpha = 0.10f)
            hovered -> accent.copy(alpha = 0.05f)
            else -> Color.Transparent
          }
        )
        .hoverable(interactionSource)
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          onClick = onPlay
        )
        .padding(start = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
        if (isCurrent) {
          PlayingIndicator(isPlaying = isPlaying, color = accent)
        } else {
          Text(
            text = "${index + 1}",
            color = textSecondary,
            fontSize = 12.sp
          )
        }
      }

      CoverArt(
        seed = song.id,
        label = song.title.take(1),
        cornerRadius = 8.dp,
        labelSize = 16.sp,
        modifier = Modifier.size(44.dp)
      )
      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          color = if (isCurrent) accent else textPrimary,
          fontSize = 14.sp,
          fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = listOfNotNull(song.artist, song.album).joinToString(" · "),
          color = textSecondary,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(Modifier.width(8.dp))
      Text(
        text = formatDuration(song.durationMs),
        color = textSecondary,
        fontSize = 11.sp
      )

      Box(
        modifier = Modifier
          .width(40.dp)
          .fillMaxHeight()
          .pointerInput(song.id) {
            detectVerticalDragGestures(
              onDragStart = {
                dragging = true
                dragOffset = 0f
              },
              onDragEnd = {
                dragging = false
                dragOffset = 0f
              },
              onDragCancel = {
                dragging = false
                dragOffset = 0f
              },
              onVerticalDrag = { change, dragAmount ->
                change.consume()
                dragOffset += dragAmount
                val steps = (dragOffset / rowHeightPx).toInt()
                if (steps != 0) {
                  onMoveBy(steps)
                  dragOffset -= steps * rowHeightPx
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        DragHandleIcon(
          modifier = Modifier.size(16.dp),
          color = textSecondary.copy(alpha = if (dragging) 1f else 0.65f)
        )
      }
    }
  }
}

/** 当前播放行左侧的三根音量条：播放时上下跳动，暂停时静止 */
@Composable
private fun PlayingIndicator(
  isPlaying: Boolean,
  color: Color,
  modifier: Modifier = Modifier
) {
  if (!isPlaying) {
    Canvas(modifier = modifier.size(14.dp)) {
      val barWidth = size.width * 0.2f
      val gap = size.width * 0.18f
      listOf(0.45f, 0.8f, 0.6f).forEachIndexed { i, ratio ->
        val barHeight = size.height * ratio
        drawRoundRect(
          color = color,
          topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
      }
    }
    return
  }

  val transition = rememberInfiniteTransition(label = "playingBars")
  val bars = List(3) { index ->
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 520 + index * 130, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "bar$index"
    )
  }

  Canvas(modifier = modifier.size(14.dp)) {
    val barWidth = size.width * 0.2f
    val gap = size.width * 0.18f
    bars.forEachIndexed { i, animated ->
      val barHeight = size.height * animated.value
      drawRoundRect(
        color = color,
        topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
      )
    }
  }
}

@Composable
private fun LoadingRow(accent: Color, textSecondary: Color) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 32.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    CircularProgressIndicator(
      modifier = Modifier.size(18.dp),
      color = accent,
      strokeWidth = 2.dp
    )
    Spacer(Modifier.width(10.dp))
    Text(text = "正在加载歌单…", color = textSecondary, fontSize = 13.sp)
  }
}

/** 空歌单：自绘虚线圆 + 音符插画，配一句引导文案 */
@Composable
private fun EmptyPlaylistState(
  playlistExists: Boolean,
  accent: Color,
  textPrimary: Color,
  textSecondary: Color,
  onBack: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 36.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    EmptyPlaylistArt(accent = accent, modifier = Modifier.size(120.dp))
    Spacer(Modifier.height(20.dp))
    Text(
      text = if (playlistExists) "这个歌单还是空的" else "这个歌单找不到了",
      color = textPrimary,
      fontSize = 16.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = if (playlistExists) "去首页挑几首喜欢的歌，\n它们会出现在这里" else "它可能已经被删除了",
      color = textSecondary,
      fontSize = 12.sp,
      lineHeight = 19.sp,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(22.dp))
    ActionPill(
      text = "返回首页",
      filled = false,
      enabled = true,
      accent = accent,
      onClick = onBack
    )
  }
}

/** 空态插画：虚线圆 + 音符 + 两个星点，全部 Canvas 绘制，不引入图片 */
@Composable
private fun EmptyPlaylistArt(accent: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val radius = size.minDimension * 0.36f
    val center = Offset(size.width / 2, size.height / 2)
    val strokeWidth = size.minDimension * 0.018f

    drawCircle(
      color = accent.copy(alpha = 0.10f),
      radius = radius * 0.78f,
      center = center
    )
    drawCircle(
      color = accent.copy(alpha = 0.55f),
      radius = radius,
      center = center,
      style = Stroke(
        width = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 5, strokeWidth * 4))
      )
    )

    // 音符：竖杆 + 符头 + 符尾
    val noteX = center.x - radius * 0.18f
    val noteTop = center.y - radius * 0.44f
    drawLine(
      color = accent,
      start = Offset(noteX, center.y + radius * 0.12f),
      end = Offset(noteX, noteTop),
      strokeWidth = strokeWidth * 1.6f,
      cap = StrokeCap.Round
    )
    drawCircle(
      color = accent,
      radius = radius * 0.15f,
      center = Offset(noteX, center.y + radius * 0.2f)
    )
    val flag = Path().apply {
      moveTo(noteX, noteTop)
      quadraticTo(
        noteX + radius * 0.42f,
        noteTop + radius * 0.10f,
        noteX + radius * 0.32f,
        noteTop + radius * 0.46f
      )
    }
    drawPath(
      path = flag,
      color = accent,
      style = Stroke(width = strokeWidth * 1.2f, cap = StrokeCap.Round)
    )

    // 星点
    listOf(
      Offset(center.x + radius * 0.62f, center.y - radius * 0.52f),
      Offset(center.x - radius * 0.58f, center.y + radius * 0.5f)
    ).forEach { offset ->
      drawCircle(color = accent.copy(alpha = 0.5f), radius = strokeWidth * 1.8f, center = offset)
    }
  }
}
