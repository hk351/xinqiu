package com.dlust.ydhl2026.ui.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlust.ydhl2026.data.model.LyricLine
import com.dlust.ydhl2026.ui.components.ChevronDownIcon
import com.dlust.ydhl2026.ui.components.PauseIcon
import com.dlust.ydhl2026.ui.components.PlayIcon
import com.dlust.ydhl2026.ui.components.QueueListIcon
import com.dlust.ydhl2026.ui.components.SkipNextIcon
import com.dlust.ydhl2026.ui.components.SkipPreviousIcon
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark

/**
 * 全屏播放器（原型图3 的展开态）。
 *
 * 视觉规则来自已定稿的 HTML Demo：
 * - 每首歌一套主题色，整屏背景 / 控件 / 进度条 / 歌词高亮一起换；
 * - 圆形封面播放时慢速旋转，暂停即停；
 * - 大封面 + 厚留白，控件紧凑贴在下方。
 *
 * 本轮仍为假播放：进度由 PlayerViewModel 的定时器推进，不发声。
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

  // 封面旋转：只在播放时累加角度，暂停后角度保持不动
  var angle by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(state.isPlaying) {
    if (!state.isPlaying) return@LaunchedEffect
    var lastNanos = 0L
    while (true) {
      withFrameNanos { now ->
        if (lastNanos != 0L) {
          // 45°/秒 ≈ 8 秒转一圈，与 Demo 一致
          angle = (angle + (now - lastNanos) / 1_000_000_000f * 45f) % 360f
        }
        lastNanos = now
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(palette.bgTop, palette.bgBottom)))
  ) {
    val coverSize = minOf(maxWidth * 0.62f, maxHeight * 0.32f)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 顶部：收起 + 状态文案
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
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
        Text(
          text = "正在播放",
          color = palette.textSecondary,
          fontSize = 12.sp,
          letterSpacing = 1.sp
        )
        Spacer(Modifier.width(40.dp))
      }

      Spacer(Modifier.height(28.dp))

      // 大封面：主题色光盘，播放时旋转
      Box(contentAlignment = Alignment.Center) {
        Box(
          modifier = Modifier
            .size(coverSize)
            .shadow(
              elevation = 24.dp,
              shape = CircleShape,
              ambientColor = palette.primary,
              spotColor = palette.primary
            )
            .clip(CircleShape)
            .background(
              Brush.sweepGradient(
                listOf(
                  palette.cover3,
                  palette.cover2,
                  palette.cover1,
                  palette.cover2,
                  palette.cover3
                )
              )
            )
            .border(2.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .graphicsLayer { rotationZ = angle }
        )
        // 唱片中心孔
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(palette.bgBottom)
            .border(3.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        )
      }

      Spacer(Modifier.height(32.dp))

      // 歌名 / 歌手
      Text(
        text = song.title,
        color = palette.text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        maxLines = 1
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = song.album?.let { "${song.artist} · $it" } ?: song.artist,
        color = palette.textSecondary,
        fontSize = 13.sp,
        maxLines = 1
      )

      Spacer(Modifier.height(26.dp))

      // 进度条 + 时间
      SeekBar(
        progress = if (state.durationMs > 0) {
          state.positionMs.toFloat() / state.durationMs
        } else {
          0f
        },
        trackColor = palette.track,
        fillColor = palette.primary,
        onSeek = { fraction ->
          playerViewModel.onSeekTo((fraction * state.durationMs).toLong())
        }
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
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

      Spacer(Modifier.height(22.dp))

      // 控制区：模式 / 上一首 / 播放 / 下一首 / 队列
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape)
            .clickable { playerViewModel.onRepeatModeChange() },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = when (state.repeatMode) {
              1 -> "🔂"
              2 -> "🔀"
              else -> "🔁"
            },
            fontSize = 17.sp
          )
        }

        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape)
            .clickable { playerViewModel.onPrevious() },
          contentAlignment = Alignment.Center
        ) {
          SkipPreviousIcon(
            modifier = Modifier.size(28.dp),
            color = palette.text.copy(alpha = 0.85f)
          )
        }

        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(palette.primary)
            .clickable { playerViewModel.onPlayPause() },
          contentAlignment = Alignment.Center
        ) {
          if (state.isPlaying) {
            PauseIcon(modifier = Modifier.size(26.dp), color = palette.bgTop)
          } else {
            PlayIcon(modifier = Modifier.size(26.dp), color = palette.bgTop)
          }
        }

        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape)
            .clickable { playerViewModel.onNext() },
          contentAlignment = Alignment.Center
        ) {
          SkipNextIcon(
            modifier = Modifier.size(28.dp),
            color = palette.text.copy(alpha = 0.85f)
          )
        }

        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape).clickable {
            Toast.makeText(context, "播放队列下一轮实现", Toast.LENGTH_SHORT).show()
          },
          contentAlignment = Alignment.Center
        ) {
          QueueListIcon(
            modifier = Modifier.size(20.dp),
            color = palette.text.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(Modifier.weight(1f))

      // 歌词：当前行高亮，上下各露出一行
      LyricsSection(
        lyrics = song.lyrics,
        positionMs = state.positionMs,
        palette = palette
      )

      Spacer(Modifier.height(12.dp))
    }
  }
}

/** 细进度条：支持点击与横向拖动跳转 */
@Composable
private fun SeekBar(
  progress: Float,
  trackColor: Color,
  fillColor: Color,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(22.dp)
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onDragStart = { offset ->
            onSeek((offset.x / size.width).coerceIn(0f, 1f))
          },
          onHorizontalDrag = { change, _ ->
            onSeek((change.position.x / size.width).coerceIn(0f, 1f))
          }
        )
      }
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          onSeek((offset.x / size.width).coerceIn(0f, 1f))
        }
      },
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(CircleShape)
        .background(trackColor)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(progress.coerceIn(0f, 1f))
          .height(4.dp)
          .clip(CircleShape)
          .background(fillColor)
      )
    }
  }
}

/** 三行歌词：上一行 / 当前行 / 下一行 */
@Composable
private fun LyricsSection(
  lyrics: List<LyricLine>?,
  positionMs: Long,
  palette: PlayerPalette,
  modifier: Modifier = Modifier
) {
  if (lyrics.isNullOrEmpty()) {
    Text(
      text = "暂无歌词",
      color = palette.textSecondary,
      fontSize = 12.sp,
      modifier = modifier
    )
    return
  }
  val active = lyrics.indexOfLast { it.timeMs <= positionMs }
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(9.dp)
  ) {
    for (offset in -1..1) {
      val index = active + offset
      if (index !in lyrics.indices) {
        Box(Modifier.height(18.dp))
      } else {
        val isActive = offset == 0
        Text(
          text = lyrics[index].text,
          color = if (isActive) palette.text else palette.textSecondary,
          fontSize = if (isActive) 15.sp else 13.sp,
          fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
          textAlign = TextAlign.Center,
          maxLines = 1
        )
      }
    }
  }
}

