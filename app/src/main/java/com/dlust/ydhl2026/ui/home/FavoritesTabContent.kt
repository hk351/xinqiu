package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlust.ydhl2026.ui.components.EmptyState
import com.dlust.ydhl2026.ui.components.HeartIcon
import com.dlust.ydhl2026.ui.theme.EchoBlue

/** 收藏 Tab：还没有收藏数据，给出统一空态和一个「去听歌」的出口 */
@Composable
fun FavoritesTabContent(
  isDark: Boolean,
  onBrowse: () -> Unit,
  modifier: Modifier = Modifier
) {
  EmptyState(
    title = "还没有收藏的歌曲",
    description = "在歌单或搜索里打开一首歌，\n点右上角的心形就能收进来",
    isDark = isDark,
    modifier = modifier,
    icon = { HeartIcon(modifier = Modifier.size(30.dp), color = EchoBlue, filled = true) },
    actionLabel = "去听歌",
    onAction = onBrowse
  )
}
