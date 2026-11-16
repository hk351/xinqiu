package com.dlust.ydhl2026.ui.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.cachedArtworkColor
import com.dlust.ydhl2026.ui.components.coverGradientPair
import com.dlust.ydhl2026.ui.components.loadArtworkColor
import com.dlust.ydhl2026.ui.theme.EchoInk

/**
 * 播放面板与歌词舞台共用的背景层。
 *
 * 结构固定为「底图 + 压暗」，预设只决定底图怎么来；模糊和压暗由用户滑杆继续微调。
 * 面板底部那层竖向渐变压暗不在本层里，那里还要保证标题可读。
 */
@Composable
fun PlayerBackdropLayer(
  backdrop: PlayerBackdrop,
  seed: String,
  artUrl: String?,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(EchoInk)
      // 模糊会溢出边界，统一在层内裁掉，避免糊到相邻区域
      .clipToBounds()
  ) {
    when (backdrop.preset.source) {
      BackdropSource.Ink -> Unit
      BackdropSource.Artwork -> CoverBackdrop(seed, artUrl, backdrop.blurDp)
      BackdropSource.Gradient ->
        GradientBackdrop(seed, artUrl, backdrop.preset.coverAlpha, backdrop.blurDp)
      BackdropSource.Dominant -> DominantBackdrop(seed, artUrl, backdrop.blurDp)
    }

    if (backdrop.dim > 0f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = backdrop.dim))
      )
    }
  }
}

@Composable
private fun CoverBackdrop(seed: String, artUrl: String?, blurDp: Float) {
  CoverArt(
    seed = seed,
    label = "",
    cornerRadius = 0.dp,
    artUrl = artUrl,
    modifier = Modifier
      .fillMaxSize()
      .backdropBlur(blurDp)
  )
}

/** 按歌曲派生的同色系渐变；[coverAlpha] 大于 0 时再半透明叠一层封面，做出混合感 */
@Composable
private fun GradientBackdrop(seed: String, artUrl: String?, coverAlpha: Float, blurDp: Float) {
  val pair = remember(seed) { coverGradientPair(seed) }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .backdropBlur(blurDp)
      .background(Brush.linearGradient(listOf(pair.first, pair.second)))
  ) {
    // 对角高光：纯渐变也能有体积感，不显得像一块死色
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            listOf(
              Color.White.copy(alpha = 0.10f),
              Color.Transparent,
              Color.Black.copy(alpha = 0.18f)
            )
          )
        )
    )
    if (artUrl != null && coverAlpha > 0f) {
      CoverArt(
        seed = seed,
        label = "",
        cornerRadius = 0.dp,
        artUrl = artUrl,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = coverAlpha }
      )
    }
  }
}

/** 取封面主色做径向渐变；取不到图或颜色太黑时回落到 seed 渐变色 */
@Composable
private fun DominantBackdrop(seed: String, artUrl: String?, blurDp: Float) {
  val fallback = remember(seed) { coverGradientPair(seed) }
  val context = LocalContext.current
  val dominant by produceState(
    initialValue = artUrl?.let(::cachedArtworkColor),
    artUrl
  ) {
    value = artUrl?.let { loadArtworkColor(context, it) }
  }

  // 全黑的封面取不出有意义的颜色，用 seed 渐变兜底
  val base = dominant?.takeIf { it.luminance() > 0.04f }
  val colors = if (base == null) {
    listOf(fallback.first, fallback.second)
  } else {
    listOf(
      lerp(base, Color.Black, 0.62f),
      lerp(base, Color.Black, 0.26f),
      lerp(base, Color.White, 0.10f)
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .backdropBlur(blurDp)
      .background(Brush.radialGradient(colors))
  )
}

/**
 * 背景模糊。
 *
 * API 31 以下没有实时模糊，只能把底图放大，靠模糊预设附带的压暗弱化细节；
 * 31+ 用无边界模糊，再放大一点盖住四周可能被吃掉的边缘。
 */
private fun Modifier.backdropBlur(blurDp: Float): Modifier = when {
  blurDp <= 0f -> this
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> this
    .graphicsLayer {
      scaleX = 1.12f
      scaleY = 1.12f
    }
    .blur(blurDp.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)

  else -> this.graphicsLayer {
    scaleX = 1.18f
    scaleY = 1.18f
  }
}
