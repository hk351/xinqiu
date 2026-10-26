package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/** 区块标题（我的歌单 / 最近播放） */
@Composable
fun SectionHeader(title: String, isDark: Boolean, modifier: Modifier = Modifier) {
  Text(
    text = title,
    color = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary,
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    modifier = modifier
  )
}

/** 歌单卡片：封面 + 名称 + 歌曲数 */
@Composable
fun PlaylistCoverCard(
  playlist: Playlist,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.clickable(onClick = onClick)) {
    CoverArt(
      seed = "playlist-${playlist.id}",
      label = playlist.name.take(1),
      cornerRadius = 12.dp,
      labelSize = 30.sp,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = playlist.name,
      color = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = "${playlist.songCount} 首",
      color = EchoTextSecondary,
      fontSize = 11.sp
    )
  }
}
