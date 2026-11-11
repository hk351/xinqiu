package com.dlust.ydhl2026.ui.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.ChevronDownIcon
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.DragHandleIcon
import com.dlust.ydhl2026.ui.components.HeartIcon
import com.dlust.ydhl2026.ui.components.MoreIcon
import com.dlust.ydhl2026.ui.components.PauseIcon
import com.dlust.ydhl2026.ui.components.PlayIcon
import com.dlust.ydhl2026.ui.components.QueueListIcon
import com.dlust.ydhl2026.ui.components.RepeatIcon
import com.dlust.ydhl2026.ui.components.RepeatOneIcon
import com.dlust.ydhl2026.ui.components.ShuffleIcon
import com.dlust.ydhl2026.ui.components.SkipNextIcon
import com.dlust.ydhl2026.ui.components.SkipPreviousIcon
import com.dlust.ydhl2026.ui.components.pressScale
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.player.lyrics.LyricStage
import com.dlust.ydhl2026.ui.player.lyrics.LyricStyle
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark

/**
 * 全屏播放器（沉浸海报风）。
 *
 * 设计要点：
 * - 背景沿用每首歌一套主题色，在封面上方打一层同色径向光晕，形成「光从封面透出来」的层次；
 * - 封面是大圆角卡片而不是圆形唱片，播放时做缓慢呼吸缩放，暂停即停；
 * - 歌名左对齐大字号，右侧收藏按钮；进度条是细线 + 圆点，可直接点击/拖动跳转；
 * - 控制区全部使用 Canvas 图标，不再出现 emoji；
 * - 底部三行歌词，点击后展开全屏歌词面板，可下滑或返回键收起。
 *
 * 已接入 Media3：播放、暂停、拖动进度与切歌都作用在 ExoPlayer 上，进度由播放器回调推进。
 */
@Composable
fun FullPlayerScreen(
  playerViewModel: PlayerViewModel,
  onCollapse: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by playerViewModel.uiState.collectAsStateWithLifecycle()
  val song = state.currentSong
  val context = LocalContext.current

  if (song == null) {
    Box(modifier = modifier.fillMaxSize().background(EchoBackgroundDark))
    return
  }

  val palette = remember(song.id) { playerPaletteFor(song.id) }
  var showLyrics by remember { mutableStateOf(false) }
  var showQueue by remember { mutableStateOf(false) }
  var liked by remember(song.id) { mutableStateOf(false) }
  // 歌词样式跟着页面记住，收起面板再打开还是上一次的选择
  var lyricStyle by rememberSaveable { mutableStateOf(LyricStyle.CLASSIC) }

  // 面板展开时，返回键先收起面板而不是直接退出播放页
  BackHandler(enabled = showLyrics || showQueue) {
    if (showQueue) showQueue = false else showLyrics = false
  }

  // 歌词面板淡入上滑：0 → 1，关闭时反向播放完才移除
  val panelProgress by animateFloatAsState(
    targetValue = if (showLyrics) 1f else 0f,
    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
  )
  // 播放队列底部弹层：从底部滑入
  val queueProgress by animateFloatAsState(
    targetValue = if (showQueue) 1f else 0f,
    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
  )

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val coverSize = minOf(maxWidth * 0.72f, maxHeight * 0.32f)

    PlayerBackground(palette = palette)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
      PlayerTopBar(
        palette = palette,
        onCollapse = onCollapse,
        onMore = {
          Toast.makeText(context, "更多操作下一轮实现", Toast.LENGTH_SHORT).show()
        }
      )

      Spacer(Modifier.height(10.dp))

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CoverCard(
          song = song,
          palette = palette,
          sizeDp = coverSize,
          isPlaying = state.isPlaying
        )
      }

      Spacer(Modifier.height(18.dp))

      TitleRow(
        song = song,
        palette = palette,
        liked = liked,
        onToggleLike = { liked = !liked }
      )

      Spacer(Modifier.weight(1f))

      LyricSnippet(
        lyrics = song.lyrics,
        positionMs = state.positionMs,
        palette = palette,
        onClick = { showLyrics = true }
      )

      Spacer(Modifier.weight(1f))

      PlayerSlider(
        progress = if (state.durationMs > 0) {
          state.positionMs.toFloat() / state.durationMs
        } else {
          0f
        },
        palette = palette,
        onSeek = { fraction ->
          playerViewModel.onSeekTo((fraction * state.durationMs).toLong())
        }
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = formatDuration(state.positionMs),
          color = palette.textSecondary,
          fontSize = 11.sp
        )
        Text(
          text = formatDuration(state.durationMs),
          color = palette.textSecondary,
          fontSize = 11.sp
        )
      }

      Spacer(Modifier.height(14.dp))

      ControlRow(
        palette = palette,
        isPlaying = state.isPlaying,
        repeatMode = state.repeatMode,
        onRepeat = { playerViewModel.onRepeatModeChange() },
        onPrevious = { playerViewModel.onPrevious() },
        onPlayPause = { playerViewModel.onPlayPause() },
        onNext = { playerViewModel.onNext() },
        onQueue = { showQueue = true }
      )

      Spacer(Modifier.height(10.dp))
    }

    if (showLyrics || panelProgress > 0.001f) {
      LyricsPanel(
        lyrics = song.lyrics,
        positionMs = state.positionMs,
        isPlaying = state.isPlaying,
        palette = palette,
        style = lyricStyle,
        onStyleChange = { lyricStyle = it },
        onClose = { showLyrics = false },
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            alpha = panelProgress
            translationY = (1f - panelProgress) * size.height * 0.08f
          }
      )
    }

    if (showQueue || queueProgress > 0.001f) {
      QueueSheet(
        queue = state.queue,
        currentSongId = state.currentSong?.id,
        isPlaying = state.isPlaying,
        palette = palette,
        onPlay = { playerViewModel.onPlayFromQueue(it) },
        onMoveBy = { id, steps -> playerViewModel.onMoveQueueBy(id, steps) },
        onClose = { showQueue = false },
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            alpha = queueProgress
            translationY = (1f - queueProgress) * size.height * 0.4f
          }
      )
    }
  }
}

/** 整屏背景：主题色纵向渐变 + 封面位置的径向光晕 + 底部压暗 */
@Composable
private fun PlayerBackground(palette: PlayerPalette) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(palette.bgTop, palette.bgBottom)))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(palette.primary.copy(alpha = 0.30f), Color.Transparent),
          center = Offset(size.width * 0.5f, size.height * 0.28f),
          radius = size.width * 1.05f
        )
      )
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, palette.bgTop.copy(alpha = 0.75f)),
          startY = size.height * 0.55f,
          endY = size.height
        )
      )
    }
  }
}

/** 顶部：收起 / 状态 / 更多 */
@Composable
private fun PlayerTopBar(
  palette: PlayerPalette,
  onCollapse: () -> Unit,
  onMore: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onCollapse),
      contentAlignment = Alignment.Center
    ) {
      ChevronDownIcon(
        modifier = Modifier.size(20.dp),
        color = palette.text.copy(alpha = 0.85f)
      )
    }
    Spacer(Modifier.weight(1f))
    Text(
      text = "正在播放",
      color = palette.textSecondary,
      fontSize = 11.sp,
      letterSpacing = 2.sp
    )
    Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onMore),
      contentAlignment = Alignment.Center
    ) {
      MoreIcon(
        modifier = Modifier.size(18.dp),
        color = palette.text.copy(alpha = 0.7f)
      )
    }
  }
}

/** 大圆角封面卡片：渐变底色 + 光斑 + 细环 + 首字，播放时呼吸缩放 */
@Composable
private fun CoverCard(
  song: Song,
  palette: PlayerPalette,
  sizeDp: Dp,
  isPlaying: Boolean
) {
  val transition = rememberInfiniteTransition(label = "coverBreath")
  val breath by transition.animateFloat(
    initialValue = 1f,
    targetValue = 1.035f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "coverBreathScale"
  )
  val scale = if (isPlaying) breath else 1f

  Box(
    modifier = Modifier
      .size(sizeDp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .shadow(
        elevation = 26.dp,
        shape = RoundedCornerShape(26.dp),
        ambientColor = palette.primary,
        spotColor = palette.primary
      )
      .clip(RoundedCornerShape(26.dp))
      .background(Brush.linearGradient(listOf(palette.cover1, palette.cover2, palette.cover3)))
      .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // 左上角高光
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
          center = Offset(size.width * 0.22f, size.height * 0.14f),
          radius = size.width * 0.75f
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.22f, size.height * 0.14f)
      )
      // 两圈唱片细纹
      val ring = 1.dp.toPx()
      drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = size.width * 0.34f,
        style = Stroke(width = ring)
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.07f),
        radius = size.width * 0.44f,
        style = Stroke(width = ring)
      )
      // 底部压深，让卡片有海报的体积感
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, palette.cover3.copy(alpha = 0.55f)),
          startY = size.height * 0.45f,
          endY = size.height
        )
      )
    }
    Text(
      text = song.title.take(1),
      color = Color.White.copy(alpha = 0.95f),
      fontSize = 46.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.align(Alignment.Center)
    )
    Text(
      text = "E C H O",
      color = Color.White.copy(alpha = 0.55f),
      fontSize = 9.sp,
      letterSpacing = 1.sp,
      modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp)
    )
  }
}

/** 歌名 + 歌手（左对齐） + 收藏 */
@Composable
private fun TitleRow(
  song: Song,
  palette: PlayerPalette,
  liked: Boolean,
  onToggleLike: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        color = palette.text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = song.album?.let { "${song.artist} · $it" } ?: song.artist,
        color = palette.textSecondary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(Modifier.width(12.dp))
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.06f))
        .clickable(onClick = onToggleLike),
      contentAlignment = Alignment.Center
    ) {
      HeartIcon(
        modifier = Modifier.size(18.dp),
        color = if (liked) palette.primary else palette.text.copy(alpha = 0.75f),
        filled = liked
      )
    }
  }
}

/** 细线进度条：点击 / 横向拖动都能跳转，圆点带一圈光晕 */
@Composable
private fun PlayerSlider(
  progress: Float,
  palette: PlayerPalette,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(26.dp)
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onDragStart = { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) },
          onHorizontalDrag = { change, _ ->
            onSeek((change.position.x / size.width).coerceIn(0f, 1f))
          }
        )
      }
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          onSeek((offset.x / size.width).coerceIn(0f, 1f))
        }
      }
  ) {
    val value = progress.coerceIn(0f, 1f)
    val centerY = size.height / 2f
    val strokeWidth = 1.5.dp.toPx()
    val thumbRadius = 4.5.dp.toPx()
    val thumbX = (size.width * value).coerceIn(thumbRadius, size.width - thumbRadius)

    drawLine(
      color = palette.track,
      start = Offset(thumbRadius, centerY),
      end = Offset(size.width - thumbRadius, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    drawLine(
      color = palette.primary,
      start = Offset(thumbRadius, centerY),
      end = Offset(thumbX, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    drawCircle(
      color = palette.primary.copy(alpha = 0.28f),
      radius = thumbRadius * 2.2f,
      center = Offset(thumbX, centerY)
    )
    drawCircle(color = palette.primary, radius = thumbRadius, center = Offset(thumbX, centerY))
  }
}

/** 五个控制按钮：循环/随机 · 上一首 · 播放 · 下一首 · 队列 */
@Composable
private fun ControlRow(
  palette: PlayerPalette,
  isPlaying: Boolean,
  repeatMode: Int,
  onRepeat: () -> Unit,
  onPrevious: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onQueue: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val modeColor = if (repeatMode == 0) palette.text.copy(alpha = 0.5f) else palette.primary
    Box(
      modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onRepeat),
      contentAlignment = Alignment.Center
    ) {
      when (repeatMode) {
        1 -> RepeatOneIcon(modifier = Modifier.size(21.dp), color = modeColor)
        2 -> ShuffleIcon(modifier = Modifier.size(21.dp), color = modeColor)
        else -> RepeatIcon(modifier = Modifier.size(21.dp), color = modeColor)
      }
    }

    Box(
      modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onPrevious),
      contentAlignment = Alignment.Center
    ) {
      SkipPreviousIcon(
        modifier = Modifier.size(26.dp),
        color = palette.text.copy(alpha = 0.9f)
      )
    }

    Box(
      modifier = Modifier
        .size(64.dp)
        .shadow(
          elevation = 16.dp,
          shape = CircleShape,
          ambientColor = palette.primary,
          spotColor = palette.primary
        )
        .clip(CircleShape)
        .background(palette.primary)
        .clickable(onClick = onPlayPause),
      contentAlignment = Alignment.Center
    ) {
      if (isPlaying) {
        PauseIcon(modifier = Modifier.size(24.dp), color = palette.bgTop)
      } else {
        PlayIcon(modifier = Modifier.size(24.dp), color = palette.bgTop)
      }
    }

    Box(
      modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onNext),
      contentAlignment = Alignment.Center
    ) {
      SkipNextIcon(
        modifier = Modifier.size(26.dp),
        color = palette.text.copy(alpha = 0.9f)
      )
    }

    Box(
      modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onQueue),
      contentAlignment = Alignment.Center
    ) {
      QueueListIcon(
        modifier = Modifier.size(19.dp),
        color = palette.text.copy(alpha = 0.65f)
      )
    }
  }
}

/** 三行歌词：上一行淡、当前行高亮、下一行淡；整块可点击展开全屏歌词 */
@Composable
private fun LyricSnippet(
  lyrics: List<LyricLine>?,
  positionMs: Long,
  palette: PlayerPalette,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (lyrics.isNullOrEmpty()) {
      Text(
        text = "暂无歌词",
        color = palette.textSecondary,
        fontSize = 12.sp
      )
      return@Column
    }
    val active = lyrics.indexOfLast { it.timeMs <= positionMs }
    for (offset in -1..1) {
      val index = active + offset
      Box(modifier = Modifier.fillMaxWidth().height(if (offset == 0) 26.dp else 22.dp), contentAlignment = Alignment.Center) {
        if (index in lyrics.indices) {
          Text(
            text = lyrics[index].text,
            color = if (offset == 0) palette.text else palette.textSecondary.copy(alpha = 0.45f),
            fontSize = if (offset == 0) 16.sp else 12.sp,
            fontWeight = if (offset == 0) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

/** 全屏歌词面板：整屏交给歌词样式绘制，可滑动收起，右上角切换样式 */
@Composable
private fun LyricsPanel(
  lyrics: List<LyricLine>?,
  positionMs: Long,
  isPlaying: Boolean,
  palette: PlayerPalette,
  style: LyricStyle,
  onStyleChange: (LyricStyle) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(Brush.verticalGradient(listOf(palette.bgTop, palette.bgBottom)))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(palette.primary.copy(alpha = 0.20f), Color.Transparent),
          center = Offset(size.width * 0.5f, size.height * 0.22f),
          radius = size.width * 1.05f
        )
      )
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
      LyricsPanelHeader(
        palette = palette,
        style = style,
        onStyleChange = onStyleChange,
        onClose = onClose
      )

      if (lyrics.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = "暂无歌词", color = palette.textSecondary, fontSize = 14.sp)
        }
      } else {
        LyricStage(
          lines = lyrics,
          positionMs = positionMs,
          isPlaying = isPlaying,
          palette = palette,
          style = style,
          modifier = Modifier.fillMaxWidth().weight(1f)
        )
      }
    }
  }
}

/** 歌词面板顶部：把手 + 关闭箭头 + 标题 + 样式切换，向下滑动可收起 */
@Composable
private fun LyricsPanelHeader(
  palette: PlayerPalette,
  style: LyricStyle,
  onStyleChange: (LyricStyle) -> Unit,
  onClose: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, bottom = 4.dp)
        .pointerInput(Unit) {
          var dragged = 0f
          detectVerticalDragGestures(
            onDragEnd = {
              if (dragged > 40.dp.toPx()) onClose()
              dragged = 0f
            },
            onVerticalDrag = { _, delta -> dragged += delta }
          )
        },
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .width(34.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(palette.text.copy(alpha = 0.22f))
      )
    }
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .size(40.dp)
          .clip(CircleShape)
          .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
      ) {
        ChevronDownIcon(
          modifier = Modifier.size(20.dp),
          color = palette.text.copy(alpha = 0.85f)
        )
      }
      Text(
        text = "歌词",
        color = palette.textSecondary,
        fontSize = 12.sp,
        letterSpacing = 3.sp
      )
      LyricStyleChip(
        style = style,
        palette = palette,
        onNext = {
          val styles = LyricStyle.entries
          onStyleChange(styles[(style.ordinal + 1) % styles.size])
        },
        modifier = Modifier.align(Alignment.CenterEnd)
      )
    }
  }
}

/** 样式切换胶囊：点一下切到下一种歌词动画 */
@Composable
private fun LyricStyleChip(
  style: LyricStyle,
  palette: PlayerPalette,
  onNext: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier = modifier
      .pressScale(interactionSource)
      .clip(RoundedCornerShape(50))
      .border(1.dp, palette.text.copy(alpha = 0.18f), RoundedCornerShape(50))
      .clickable(interactionSource = interactionSource, indication = null, onClick = onNext)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = style.label,
      color = palette.text.copy(alpha = 0.8f),
      fontSize = 11.sp
    )
  }
}

/** 队列行高：拖拽时用它把像素位移换算成行数 */
private val QueueRowHeight = 60.dp

/** 播放队列底部弹层：当前歌高亮、点击切歌、拖动把手排序 */
@Composable
private fun QueueSheet(
  queue: List<Song>,
  currentSongId: String?,
  isPlaying: Boolean,
  palette: PlayerPalette,
  onPlay: (Int) -> Unit,
  onMoveBy: (String, Int) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {
    // 遮罩：点击空白处收起
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
        .clickable(onClick = onClose)
    )

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .fillMaxHeight(0.64f)
        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        .background(Brush.verticalGradient(listOf(palette.bgBottom, palette.bgTop)))
        .border(
          width = 1.dp,
          color = Color.White.copy(alpha = 0.08f),
          shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        )
        .navigationBarsPadding()
    ) {
      QueueHandle(palette = palette, onClose = onClose)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp)
          .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "播放队列",
          color = palette.text,
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = "共 ${queue.size} 首",
          color = palette.textSecondary,
          fontSize = 11.sp
        )
      }

      if (queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = "队列还是空的", color = palette.textSecondary, fontSize = 13.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
        ) {
          itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
            QueueRow(
              index = index,
              song = item,
              isCurrent = item.id == currentSongId,
              isPlaying = isPlaying,
              palette = palette,
              onPlay = { onPlay(index) },
              onMoveBy = { steps -> onMoveBy(item.id, steps) }
            )
          }
        }
      }
    }
  }
}

/** 弹层顶部的小横条：按住下滑可收起 */
@Composable
private fun QueueHandle(
  palette: PlayerPalette,
  onClose: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(24.dp)
      .pointerInput(Unit) {
        var dragged = 0f
        detectVerticalDragGestures(
          onDragEnd = {
            if (dragged > 40.dp.toPx()) onClose()
            dragged = 0f
          },
          onVerticalDrag = { _, delta -> dragged += delta }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .width(34.dp)
        .height(3.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(palette.text.copy(alpha = 0.22f))
    )
  }
}

/** 队列中的一行：序号/波形、封面、歌名、时长、拖拽把手 */
@Composable
private fun QueueRow(
  index: Int,
  song: Song,
  isCurrent: Boolean,
  isPlaying: Boolean,
  palette: PlayerPalette,
  onPlay: () -> Unit,
  onMoveBy: (Int) -> Unit
) {
  val rowHeightPx = with(LocalDensity.current) { QueueRowHeight.toPx() }
  var dragging by remember { mutableStateOf(false) }
  var dragOffset by remember { mutableFloatStateOf(0f) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(QueueRowHeight)
      .padding(horizontal = 12.dp)
      .graphicsLayer {
        val scale = if (dragging) 1.02f else 1f
        scaleX = scale
        scaleY = scale
        translationY = if (dragging) dragOffset else 0f
      }
      .clip(RoundedCornerShape(12.dp))
      .background(if (isCurrent) palette.primary.copy(alpha = 0.12f) else Color.Transparent)
      .clickable(onClick = onPlay)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
      if (isCurrent) {
        PlayingBars(isPlaying = isPlaying, color = palette.primary)
      } else {
        Text(text = "${index + 1}", color = palette.textSecondary, fontSize = 11.sp)
      }
    }

    CoverArt(
      seed = song.id,
      modifier = Modifier.size(36.dp),
      cornerRadius = 8.dp,
      label = song.title.take(1),
      labelSize = 14.sp
    )
    Spacer(Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        color = if (isCurrent) palette.primary else palette.text,
        fontSize = 13.sp,
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = song.artist,
        color = palette.textSecondary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(Modifier.width(6.dp))
    Text(
      text = formatDuration(song.durationMs),
      color = palette.textSecondary,
      fontSize = 11.sp
    )

    Box(
      modifier = Modifier
        .width(32.dp)
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
        modifier = Modifier.size(15.dp),
        color = palette.textSecondary.copy(alpha = if (dragging) 1f else 0.55f)
      )
    }
  }
}

/** 当前播放的三根音量条：播放时跳动，暂停时静止 */
@Composable
private fun PlayingBars(
  isPlaying: Boolean,
  color: Color,
  modifier: Modifier = Modifier
) {
  if (!isPlaying) {
    Canvas(modifier = modifier.size(13.dp)) {
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

  val transition = rememberInfiniteTransition(label = "queuePlayingBars")
  val bars = List(3) { index ->
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 520 + index * 130, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "queueBar$index"
    )
  }

  Canvas(modifier = modifier.size(13.dp)) {
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
