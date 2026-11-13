package com.dlust.ydhl2026.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlust.ydhl2026.ui.theme.EchoInk

/**
 * 独立路由下的播放页。
 *
 * 首页用的是「卡片原位展开」的面板层；从歌单、本地音乐页进入播放器时没有可展开的卡片，
 * 这里直接铺满背景复用同一个面板层，保证两条入口的交互和视觉完全一致。
 */
@Composable
fun NowPlayingScreen(
  playerViewModel: PlayerViewModel,
  onCollapse: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by playerViewModel.uiState.collectAsStateWithLifecycle()

  Box(modifier = modifier.fillMaxSize().background(EchoInk)) {
    if (state.currentSong == null) {
      BackHandler(onBack = onCollapse)
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(text = "暂无正在播放的歌曲", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Text(
          text = "返回",
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCollapse)
            .padding(horizontal = 22.dp, vertical = 9.dp)
        )
      }
    } else {
      NowPlayingPanelLayer(
        playerViewModel = playerViewModel,
        onDismiss = onCollapse
      )
    }
  }
}
