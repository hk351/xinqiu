package com.dlust.ydhl2026.ui.home.mosaic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.coverGradientPair
import com.dlust.ydhl2026.ui.components.pressScale
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoInk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * 画布条目：歌曲自带播放队列，点哪一张就用所在那一面墙的数据起播；
 * 歌单点击后进详情页，不参与播放队列。
 */
sealed interface MosaicEntry {
  /**
   * [rank] 是榜单名次：推荐页直接显示「TOP 3」。
   * 本地歌曲和播放历史没有名次，传 null 时退回「单曲 · 序号」。
   */
  data class Track(val song: Song, val queue: List<Song>, val rank: Int? = null) : MosaicEntry
  data class Collection(val playlist: Playlist) : MosaicEntry
}

/**
 * 一面墙的滚动状态。
 * 页面按 Tab 各持一份，切换 Tab 再回来时停在原处；画布只读写这里，不再自己存一份。
 * 偏移是负值（内容相对视口向左上移动），与 Compose 的滚动约定一致。
 */
@Stable
class MosaicScrollState(initial: Offset = Offset.Zero) {
  var current by mutableStateOf(initial)
    private set

  // 非 State 镜像：只在初始化排版时取一次快照，避免在组合期订阅每帧变化的偏移
  private var mirror = initial

  internal fun set(value: Offset) {
    current = value
    mirror = value
  }

  internal fun peek(): Offset = mirror
}

/**
 * 二维拼贴画布。
 *
 * 平面上所有卡片一次性绝对定位，整面墙靠 [graphicsLayer] 平移，拖动时只更新图层，不重组每一张卡。
 * 竖向无限向下补充新的块，横向被限制在"平面宽 - 视口宽"的范围内，保证拖不出空白。
 */
@Composable
fun MosaicCanvas(
  entries: List<MosaicEntry>,
  currentSongId: String?,
  isPlaying: Boolean,
  isDark: Boolean,
  scrollState: MosaicScrollState,
  onTrackClick: (Song, List<Song>) -> Unit,
  onPlaylistClick: (Playlist) -> Unit,
  onDragStateChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  if (entries.isEmpty()) return

  BoxWithConstraints(modifier = modifier.clipToBounds()) {
    val density = LocalDensity.current
    val viewportWidthPx = constraints.maxWidth
    val viewportHeightPx = constraints.maxHeight
    val planeWidthPx = remember(viewportWidthPx) { MosaicLayout.planeWidthPx(viewportWidthPx) }

    // 首屏按记住的位置一次铺够，切回某个 Tab 时不会先闪一帧顶部的排版
    var tiles by remember(planeWidthPx, viewportHeightPx, scrollState) {
      mutableStateOf(
        MosaicLayout.tilesForViewport(
          viewportWidthPx = viewportWidthPx,
          scrollYPx = -scrollState.peek().y,
          viewportHeightPx = viewportHeightPx
        )
      )
    }

    // 每跨过约 0.7 屏才重排一次：拖动过程中不会每帧重算整面墙
    LaunchedEffect(planeWidthPx, viewportHeightPx, scrollState) {
      val step = (viewportHeightPx * 0.7f).coerceAtLeast(1f)
      snapshotFlow { (scrollState.current.y / step).toInt() }
        .distinctUntilChanged()
        .collect {
          tiles = MosaicLayout.tilesForViewport(
            viewportWidthPx = viewportWidthPx,
            scrollYPx = -scrollState.current.y,
            viewportHeightPx = viewportHeightPx
          )
        }
    }

    val scope = rememberCoroutineScope()
    val inertiaJob = remember(scrollState) { arrayOfNulls<Job>(1) }
    // 换了一面墙就停掉上一面墙的惯性，避免它继续改另一个 Tab 的偏移
    LaunchedEffect(scrollState) { inertiaJob[0]?.cancel() }

    val dragState by rememberUpdatedState(onDragStateChange)
    val dragModifier = Modifier.pointerInput(
      planeWidthPx,
      viewportWidthPx,
      viewportHeightPx,
      scrollState
    ) {
      val minX = -(planeWidthPx - viewportWidthPx).toFloat()
      val touchSlop = viewConfiguration.touchSlop
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var x = scrollState.current.x
        var y = scrollState.current.y
        while (true) {
          val event = awaitPointerEvent()
          val change = event.changes.firstOrNull { it.id == down.id } ?: break
          if (!change.pressed) break
          if (!dragging && (change.position - down.position).getDistance() > touchSlop) {
            dragging = true
            inertiaJob[0]?.cancel()
            dragState(true)
          }
          if (dragging) {
            val delta = change.positionChange()
            x = (x + delta.x).coerceIn(minX, 0f)
            y = (y + delta.y).coerceAtMost(0f)
            scrollState.set(Offset(x, y))
            tracker.addPosition(change.uptimeMillis, change.position)
            change.consume()
          }
        }
        if (dragging) {
          dragState(false)
          val velocity = tracker.calculateVelocity()
          inertiaJob[0] = scope.launch {
            runInertia(scrollState = scrollState, startVx = velocity.x, startVy = velocity.y, minX = minX)
          }
        }
      }
    }

    val planeWidthDp = with(density) { planeWidthPx.toDp() }
    val planeHeightDp = with(density) {
      (tiles.maxOfOrNull { it.y + it.height } ?: 0).toDp()
    }

    Box(modifier = Modifier.fillMaxSize().then(dragModifier)) {
      Box(
        modifier = Modifier
          .size(width = planeWidthDp, height = planeHeightDp)
          .graphicsLayer {
            translationX = scrollState.current.x
            translationY = scrollState.current.y
          }
      ) {
        tiles.forEach { slot ->
          val entry = entries[slot.globalIndex % entries.size]
          val playing = entry is MosaicEntry.Track && entry.song.id == currentSongId
          val style = MosaicLayout.styleFor(
            span = slot.span,
            isCollection = entry is MosaicEntry.Collection,
            playing = playing,
            globalIndex = slot.globalIndex
          )
          MosaicCard(
            entry = entry,
            style = style,
            isPlaying = isPlaying,
            isDark = isDark,
            // 卡片上的「单曲 · 序号」要按歌单里的位置算，不能用画布上的第几块，
            // 否则同一个条目循环补屏时会出现 20、23 这种对不上的编号
            ordinal = slot.globalIndex % entries.size,
            // 字号跟着卡片实际宽度走，宽卡才有资格用大字，窄卡也不会溢出
            titleSp = (slot.width / density.density * 0.098f).coerceIn(13f, 26f),
            onClick = {
              when (entry) {
                is MosaicEntry.Track -> onTrackClick(entry.song, entry.queue)
                is MosaicEntry.Collection -> onPlaylistClick(entry.playlist)
              }
            },
            modifier = Modifier
              .offset { IntOffset(slot.x, slot.y) }
              .size(
                width = with(density) { slot.width.toDp() },
                height = with(density) { slot.height.toDp() }
              )
          )
        }
      }
    }
  }
}

/**
 * 松手后的惯性滑行：速度按帧指数衰减，撞到平面边界就把对应的轴停住。
 * 用 withFrameNanos 手写而不是 Animatable，是为了能在衰减过程中同步钳制边界。
 */
private suspend fun CoroutineScope.runInertia(
  scrollState: MosaicScrollState,
  startVx: Float,
  startVy: Float,
  minX: Float
) {
  var vx = startVx
  var vy = startVy
  var x = scrollState.current.x
  var y = scrollState.current.y
  var lastFrame = 0L
  while (isActive && (abs(vx) > 40f || abs(vy) > 40f)) {
    withFrameNanos { now ->
      if (lastFrame != 0L) {
        val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.032f)
        val targetX = x + vx * dt
        val targetY = y + vy * dt
        val nextX = targetX.coerceIn(minX, 0f)
        val nextY = targetY.coerceAtMost(0f)
        if (nextX != targetX) vx = 0f
        if (nextY != targetY) vy = 0f
        x = nextX
        y = nextY
        scrollState.set(Offset(x, y))
        val decay = 0.9f.pow(dt * 60f)
        vx *= decay
        vy *= decay
      }
      lastFrame = now
    }
  }
}

/** 一张拼贴卡：尺寸完全由槽位决定，内部所有文字都压在封面里，没有额外留白 */
@Composable
private fun MosaicCard(
  entry: MosaicEntry,
  style: MosaicStyle,
  isPlaying: Boolean,
  isDark: Boolean,
  ordinal: Int,
  titleSp: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val seed = when (entry) {
    is MosaicEntry.Track -> entry.song.id
    is MosaicEntry.Collection -> "playlist-${entry.playlist.id}"
  }
  val artUrl = (entry as? MosaicEntry.Track)?.song?.albumArtUrl
  val chipText = when (entry) {
    is MosaicEntry.Track -> entry.rank?.let { "TOP $it" }
      ?: "单曲 · ${(ordinal + 1).toString().padStart(2, '0')}"
    is MosaicEntry.Collection -> "歌单 · ${entry.playlist.songCount}"
  }
  val title = when (entry) {
    is MosaicEntry.Track -> entry.song.title
    is MosaicEntry.Collection -> entry.playlist.name
  }
  val subtitle = when (entry) {
    is MosaicEntry.Track ->
      entry.song.album?.takeIf { it.isNotBlank() }?.let { "${entry.song.artist} · $it" }
        ?: entry.song.artist
    is MosaicEntry.Collection -> "共 ${entry.playlist.songCount} 首"
  }

  Box(
    modifier = modifier
      .pressScale(interactionSource, pressedScale = 0.985f)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
  ) {
    if (style == MosaicStyle.TextBlock) {
      TextBackdrop(seed = seed)
    } else {
      CoverArt(
        seed = seed,
        label = "",
        labelSize = (titleSp * 2.2f).sp,
        cornerRadius = 0.dp,
        modifier = Modifier.fillMaxSize(),
        artUrl = artUrl
      )
      PosterScrim(
        strength = when (style) {
          MosaicStyle.Hero -> 0.90f
          MosaicStyle.Accent -> 0.78f
          MosaicStyle.Wide -> 0.70f
          else -> 0.66f
        }
      )
      // 深色模式下再整体压一层，让画布和近黑的页面底色连成一体，不显得突兀
      if (isDark) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.14f)))
      }
    }

    CardChip(
      text = chipText,
      modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
    )

    if (style == MosaicStyle.Accent) {
      Box(modifier = Modifier.fillMaxSize().border(1.5.dp, EchoAccent))
      PlayingBars(
        isPlaying = isPlaying,
        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
      )
    }

    CardCaption(
      title = title,
      subtitle = subtitle,
      titleSp = when (style) {
        MosaicStyle.Hero -> titleSp * 1.32f
        MosaicStyle.Wide -> titleSp * 1.12f
        MosaicStyle.TextBlock -> titleSp * 1.1f
        else -> titleSp
      },
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(horizontal = 9.dp, vertical = 9.dp)
    )
  }
}

/** 纯文字卡的底：把封面渐变压到近黑的墨色，只留一点色相，避免和真封面抢注意力 */
@Composable
private fun TextBackdrop(seed: String) {
  val pair = remember(seed) { coverGradientPair(seed) }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.linearGradient(
          listOf(
            lerp(EchoInk, pair.first, 0.30f),
            lerp(EchoInk, pair.second, 0.18f)
          )
        )
      )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            listOf(
              Color.White.copy(alpha = 0.06f),
              Color.Transparent,
              Color.Black.copy(alpha = 0.22f)
            )
          )
        )
    )
  }
}

/** 左上角小标签：黑底半透明，字号很小，只交代"这是什么"，不抢封面 */
@Composable
private fun CardChip(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    color = Color.White.copy(alpha = 0.78f),
    fontSize = 8.sp,
    letterSpacing = 0.3.sp,
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.42f))
      .padding(horizontal = 4.dp, vertical = 2.dp)
  )
}

/** 压底文案：粗体标题 + 更小更淡的副标题，全部贴左下角 */
@Composable
private fun CardCaption(
  title: String,
  subtitle: String,
  titleSp: Float,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      color = Color.White,
      fontSize = titleSp.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = subtitle,
      color = Color.White.copy(alpha = 0.66f),
      fontSize = (titleSp * 0.68f).coerceAtLeast(9f).sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/** 封面底部压暗：明亮渐变上也能压住白色标题 */
@Composable
private fun PosterScrim(strength: Float) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = strength)
          )
        )
      )
  )
}

/** 正在播放的三根跳动音量条 */
@Composable
private fun PlayingBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
  if (!isPlaying) {
    BarsCanvas(heights = listOf(0.45f, 0.9f, 0.6f), modifier = modifier)
    return
  }
  val transition = rememberInfiniteTransition(label = "mosaicPlayingBars")
  val bars = List(3) { index ->
    transition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 620 + index * 140, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "mosaicBar$index"
    )
  }
  BarsCanvas(heights = bars.map { it.value }, modifier = modifier)
}

@Composable
private fun BarsCanvas(heights: List<Float>, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(width = 11.dp, height = 12.dp)) {
    val barWidth = size.width * 0.2f
    val gap = size.width * 0.2f
    heights.forEachIndexed { index, ratio ->
      val barHeight = size.height * ratio
      drawRoundRect(
        color = EchoAccent,
        topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
      )
    }
  }
}
