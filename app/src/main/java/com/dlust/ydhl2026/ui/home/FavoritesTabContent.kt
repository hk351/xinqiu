package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/** 收藏 Tab：本轮还没有收藏数据，先给一个空态占位 */
@Composable
fun FavoritesTabContent(
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(text = "💚", fontSize = 40.sp)
      Spacer(Modifier.height(14.dp))
      Text(
        text = "还没有收藏的歌曲",
        color = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "在歌单或搜索里点开歌曲，\n把喜欢的加进来吧",
        color = EchoTextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}
