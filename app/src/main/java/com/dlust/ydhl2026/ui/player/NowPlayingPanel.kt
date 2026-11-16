package com.dlust.ydhl2026.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.HeartIcon
import com.dlust.ydhl2026.ui.components.PauseIcon
import com.dlust.ydhl2026.ui.components.PlayIcon
import com.dlust.ydhl2026.ui.components.QueueListIcon
import com.dlust.ydhl2026.ui.components.ShareIcon
import com.dlust.ydhl2026.ui.components.SeekBar
import com.dlust.ydhl2026.ui.components.SkipNextIcon
import com.dlust.ydhl2026.ui.components.SparkleIcon
import com.dlust.ydhl2026.ui.components.pressScale
import com.dlust.ydhl2026.ui.components.progressToMs
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.theme.EchoAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放面板层：海报墙上方压暗，面板从原卡片位置放大展开。
 *
 * 面板里再点封面会进入全屏散字歌词舞台；从舞台返回时面板保持展开，
 * 这样「卡片 → 面板 → 舞台」是一条连续路径，而不是三个互不相干的页面。
 * 面板与舞台共用同一个 PlayerViewModel 和播放引擎，不新建播放器。
 */
@Composable
fun NowPlayingPanelLayer(
  playerViewModel: PlayerViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by playerViewModel.uiState.collectAsStateWithLifecycle()
  val song = state.currentSong ?: return
  val scope = rememberCoroutineScope()

  var entered by remember { mutableStateOf(false) }
  var showStage by remember { mutableStateOf(false) }
  var showBackdrop by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { entered = true }

  val scrimAlpha by animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "panelScrim"
  )

  // 层级从上到下：背景弹层 > 歌词舞台 > 面板，返回键按这个顺序逐层收起
  val dismissLayer: () -> Unit = {
    when {
      showBackdrop -> showBackdrop = false
      showStage -> showStage = false
      else -> {
        entered = false
        scope.launch {
          delay(220)
          onDismiss()
        }
      }
    }
  }
  BackHandler { dismissLayer() }

  Box(modifier = modifier.fillMaxSize()) {
    // 遮罩压暗海报墙，但仍能看见后面的封面轮廓
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = scrimAlpha }
        .background(Color.Black.copy(alpha = 0.58f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = dismissLayer
        )
    )

    AnimatedVisibility(
      visible = entered,
      enter = fadeIn(tween(180)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
      exit = fadeOut(tween(160)) +
        scaleOut(targetScale = 0.94f, animationSpec = tween(200)),
      modifier = Modifier.align(Alignment.TopCenter)
    ) {
      PlayerPanel(
        song = song,
        isPlaying = state.isPlaying,
        positionMs = state.positionMs,
        durationMs = if (state.durationMs > 0L) state.durationMs else song.durationMs,
        queueSize = state.queue.size,
        backdrop = state.backdrop,
        onPlayPause = playerViewModel::onPlayPause,
        onNext = playerViewModel::onNext,
        onSeek = playerViewModel::onSeekTo,
        onOpenStage = { showStage = true },
        onOpenBackdrop = { showBackdrop = true }
      )
    }

    AnimatedVisibility(
      visible = showStage,
      enter = fadeIn(tween(260)),
      exit = fadeOut(tween(200)),
      modifier = Modifier.fillMaxSize()
    ) {
      ScatterLyricStage(
        lines = song.lyrics,
        positionMs = state.positionMs,
        durationMs = if (state.durationMs > 0L) state.durationMs else song.durationMs,
        isPlaying = state.isPlaying,
        backdrop = state.backdrop,
        seed = song.id,
        artUrl = song.albumArtUrl,
        onBack = { showStage = false },
        onSeek = playerViewModel::onSeekTo
      )
    }

    // 背景弹层盖在最上面：改完立刻能看到面板效果，不用来回切
    AnimatedVisibility(
      visible = showBackdrop,
      enter = fadeIn(tween(180)),
      exit = fadeOut(tween(160)),
      modifier = Modifier.fillMaxSize()
    ) {
      BackdropSheet(
        backdrop = state.backdrop,
        seed = song.id,
        artUrl = song.albumArtUrl,
        onBackdropChange = playerViewModel::onBackdropChange,
        onDismiss = { showBackdrop = false }
      )
    }
  }
}

/** 面板本体：整块封面出血，底部压一层渐变承载标题和控制 */
@Composable
private fun PlayerPanel(
  song: Song,
  isPlaying: Boolean,
  positionMs: Long,
  durationMs: Long,
  queueSize: Int,
  backdrop: PlayerBackdrop,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onSeek: (Long) -> Unit,
  onOpenStage: () -> Unit,
  onOpenBackdrop: () -> Unit,
  modifier: Modifier = Modifier
) {
  var liked by remember(song.id) { mutableStateOf(false) }
  val progress by animateFloatAsState(
    targetValue = if (durationMs > 0L) {
      (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
      0f
    },
    animationSpec = tween(durationMillis = 260, easing = LinearEasing),
    label = "panelProgress"
  )

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // 与原型一致：面板略靠上，下面仍露出海报墙的一条缝隙
    val topPadding = maxHeight * 0.13f
    val panelHeight = maxHeight * 0.68f

    Box(
      modifier = Modifier
        .padding(top = topPadding, start = 14.dp, end = 14.dp)
        .fillMaxWidth()
        .height(panelHeight)
        .clip(RoundedCornerShape(4.dp))
    ) {
      // 背景由用户 DIY：预设决定底图，滑杆在预设推荐值上继续微调
      PlayerBackdropLayer(
        backdrop = backdrop,
        seed = song.id,
        artUrl = song.albumArtUrl,
        modifier = Modifier.fillMaxSize()
      )

      // 底部压暗，标题与进度线在明亮封面上也能看清
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.72f),
                Color.Black.copy(alpha = 0.9f)
              ),
              startY = 0f
            )
          )
      )

      // 封面区域点击进入歌词舞台；底部控制区在它之后绘制，会优先接收点击
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onOpenStage
          )
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ECHO",
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 9.sp,
          letterSpacing = 0.6.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 5.dp, vertical = 3.dp)
        )
        Spacer(Modifier.weight(1f))
        PanelIconButton(onClick = onOpenBackdrop) {
          SparkleIcon(modifier = Modifier.size(14.dp), color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.width(8.dp))
        PanelIconButton(onClick = {}) {
          ShareIcon(modifier = Modifier.size(14.dp), color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.width(8.dp))
        PanelIconButton(onClick = { liked = !liked }) {
          HeartIcon(
            modifier = Modifier.size(14.dp),
            color = if (liked) EchoAccent else Color.White.copy(alpha = 0.85f),
            filled = liked
          )
        }
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
      ) {
        Text(
          text = song.title,
          color = Color.White,
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        Text(
          text = buildString {
            append(song.artist)
            song.album?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            append(" · ").append(formatDuration(durationMs))
          },
          color = Color.White.copy(alpha = 0.62f),
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          PanelPlayButton(isPlaying = isPlaying, onClick = onPlayPause)
          Spacer(Modifier.width(16.dp))
          QueueListIcon(modifier = Modifier.size(13.dp), color = Color.White.copy(alpha = 0.62f))
          Spacer(Modifier.width(6.dp))
          Text(
            text = if (queueSize > 0) "已在队列 · $queueSize 首" else "已在队列",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 11.sp
          )
          Spacer(Modifier.weight(1f))
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
          ) {
            SkipNextIcon(modifier = Modifier.size(17.dp), color = Color.White.copy(alpha = 0.85f))
          }
        }

        Spacer(Modifier.height(6.dp))
        SeekBar(
          progress = progress,
          onSeek = { fraction -> onSeek(progressToMs(fraction, durationMs)) },
          trackColor = Color.White.copy(alpha = 0.16f),
          progressBrush = SolidColor(EchoAccent),
          trackHeight = 2.dp,
          touchHeight = 24.dp
        )
      }
    }
  }
}

@Composable
private fun PanelIconButton(
  onClick: () -> Unit,
  content: @Composable () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .size(30.dp)
      .pressScale(interactionSource, pressedScale = 0.92f)
      .clip(RoundedCornerShape(7.dp))
      .background(Color.Black.copy(alpha = 0.35f))
      .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

@Composable
private fun PanelPlayButton(
  isPlaying: Boolean,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .size(36.dp)
      .pressScale(interactionSource, pressedScale = 0.92f)
      .clip(CircleShape)
      .background(Color.White.copy(alpha = 0.06f))
      .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    if (isPlaying) {
      PauseIcon(modifier = Modifier.size(15.dp), color = Color.White)
    } else {
      PlayIcon(modifier = Modifier.size(15.dp), color = Color.White)
    }
  }
}
