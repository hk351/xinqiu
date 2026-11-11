package com.dlust.ydhl2026.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import com.dlust.ydhl2026.ui.theme.EchoBlue
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/**
 * 统一空态：圆形图标底 + 标题 + 说明 + 可选主按钮。
 * 收藏、搜索无结果等空白页共用，避免每个页面各写一套排版。
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
        .size(76.dp)
        .clip(CircleShape)
        .background(EchoBlue.copy(alpha = if (isDark) 0.20f else 0.07f)),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
    Spacer(Modifier.height(18.dp))
    Text(
      text = title,
      color = if (isDark) EchoTextPrimaryDark else EchoTextPrimary,
      fontSize = 15.sp,
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
      Spacer(Modifier.height(24.dp))
      val interactionSource = remember { MutableInteractionSource() }
      Box(
        modifier = Modifier
          .height(40.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(EchoBlue)
          .pressScale(interactionSource, pressedScale = 0.95f)
          .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onAction
          )
          .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = actionLabel,
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
