package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.ui.theme.EchoAccent
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/**
 * 统一空态：细描边圆底 + 标题 + 说明 + 可选文字按钮。
 * 收藏、搜索无结果等空白页共用，避免每个页面各写一套排版。
 *
 * 视觉上刻意做轻：不铺大色块，避免空态比内容还抢眼。
 */
@Composable
fun EmptyState(
  title: String,
  description: String,
  isDark: Boolean,
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit = {},
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .border(
          width = 1.dp,
          color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      color = if (isDark) EchoTextPrimaryDark else EchoTextPrimary,
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(9.dp))
    Text(
      text = description,
      color = EchoTextSecondary,
      fontSize = 12.5.sp,
      lineHeight = 19.sp,
      textAlign = TextAlign.Center
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(20.dp))
      val interactionSource = remember { MutableInteractionSource() }
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(17.dp))
          .background(EchoAccent.copy(alpha = if (isDark) 0.12f else 0.08f))
          .border(1.dp, EchoAccent.copy(alpha = 0.45f), RoundedCornerShape(17.dp))
          .pressScale(interactionSource, pressedScale = 0.95f)
          .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onAction
          )
          .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = actionLabel,
          color = EchoAccent,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
