package com.dlust.ydhl2026.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.ui.components.SeekBar
import com.dlust.ydhl2026.ui.components.pressScale
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoInkSurface
import kotlin.math.roundToInt

/**
 * 播放背景 DIY 面板。
 *
 * 六套预设决定底图，两个滑杆在预设推荐值上继续微调；
 * 所有改动即时生效并落盘，关掉应用再进来还是这套背景。
 */
@Composable
fun BackdropSheet(
  backdrop: PlayerBackdrop,
  seed: String,
  artUrl: String?,
  onBackdropChange: (PlayerBackdrop) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {
    // 点空白处收起，遮罩比播放面板的浅一点，能看见改完的效果
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.42f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss
        )
    )

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        .background(EchoInkSurface.copy(alpha = 0.98f))
        // 卡片自己吃掉点击，只有点遮罩才收起；否则点到文字或缝隙会误关
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        )
        .navigationBarsPadding()
        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp)
    ) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .size(width = 34.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color.White.copy(alpha = 0.18f))
      )
      Spacer(Modifier.height(14.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "播放背景",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "预设 + 滑杆，随你调",
          color = Color.White.copy(alpha = 0.35f),
          fontSize = 10.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = "恢复默认",
          color = Color.White.copy(alpha = 0.45f),
          fontSize = 11.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onBackdropChange(PlayerBackdrop.Default) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
          text = "完成",
          color = EchoAccent,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Spacer(Modifier.height(14.dp))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BackdropPreset.entries.chunked(3).forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { preset ->
              PresetChip(
                preset = preset,
                selected = preset == backdrop.preset,
                seed = seed,
                artUrl = artUrl,
                onClick = { onBackdropChange(backdrop.withPreset(preset)) },
                modifier = Modifier.weight(1f)
              )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
          }
        }
      }

      Spacer(Modifier.height(18.dp))

      BackdropSlider(
        label = "模糊",
        valueText = "${backdrop.blurDp.roundToInt()} dp",
        fraction = backdrop.blurDp / PlayerBackdrop.MAX_BLUR_DP,
        onFraction = { fraction ->
          onBackdropChange(backdrop.withBlur(fraction * PlayerBackdrop.MAX_BLUR_DP))
        }
      )
      Spacer(Modifier.height(12.dp))
      BackdropSlider(
        label = "压暗",
        valueText = "${(backdrop.dim * 100).roundToInt()}%",
        fraction = backdrop.dim / PlayerBackdrop.MAX_DIM,
        onFraction = { fraction ->
          onBackdropChange(backdrop.withDim(fraction * PlayerBackdrop.MAX_DIM))
        }
      )
    }
  }
}

/** 预设小卡：预览图用的是该预设的推荐值，不跟随滑杆，方便比较各套默认效果 */
@Composable
private fun PresetChip(
  preset: BackdropPreset,
  selected: Boolean,
  seed: String,
  artUrl: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(10.dp)
  Column(
    modifier = modifier
      .pressScale(interactionSource, pressedScale = 0.96f)
      .clip(RoundedCornerShape(12.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(shape)
    ) {
      PlayerBackdropLayer(
        backdrop = PlayerBackdrop.of(preset),
        seed = seed,
        artUrl = artUrl,
        modifier = Modifier.fillMaxSize()
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .border(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) EchoAccent else Color.White.copy(alpha = 0.10f),
            shape = shape
          )
      )
    }
    Spacer(Modifier.height(5.dp))
    Text(
      text = preset.label,
      color = if (selected) EchoAccent else Color.White.copy(alpha = 0.55f),
      fontSize = 10.sp,
      maxLines = 1
    )
  }
}

@Composable
private fun BackdropSlider(
  label: String,
  valueText: String,
  fraction: Float,
  onFraction: (Float) -> Unit
) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = label, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp)
      Spacer(Modifier.weight(1f))
      Text(text = valueText, color = Color.White.copy(alpha = 0.38f), fontSize = 10.sp)
    }
    Spacer(Modifier.height(4.dp))
    SeekBar(
      progress = fraction,
      onSeek = onFraction,
      trackColor = Color.White.copy(alpha = 0.12f),
      progressBrush = SolidColor(EchoAccent),
      trackHeight = 2.dp,
      touchHeight = 26.dp
    )
  }
}
