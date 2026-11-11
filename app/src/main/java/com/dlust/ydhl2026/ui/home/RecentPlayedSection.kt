package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.pressScale
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/** 最近播放：横向滑动的一排歌曲卡，点击即播放 */
@Composable
fun RecentPlayedRow(
  songs: List<Song>,
  isDark: Boolean,
  onPlaySong: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(items = songs, key = { it.id }) { song ->
      val interactionSource = remember { MutableInteractionSource() }
      Column(
        modifier = Modifier
          .width(88.dp)
          .pressScale(interactionSource)
          .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
          ) { onPlaySong(song) }
      ) {
        CoverArt(
          seed = song.id,
          label = song.title.take(1),
          cornerRadius = 12.dp,
          labelSize = 22.sp,
          modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = song.title,
          color = if (isDark) EchoTextPrimaryDark else EchoTextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = song.artist,
          color = EchoTextSecondary,
          fontSize = 10.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
