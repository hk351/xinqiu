package com.dlust.ydhl2026.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.theme.EchoBlue
import com.dlust.ydhl2026.ui.theme.EchoBorder
import com.dlust.ydhl2026.ui.theme.EchoBorderDark
import com.dlust.ydhl2026.ui.theme.EchoCardDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

/**
 * 搜索 Tab：输入框 + 结果列表。
 * 过滤逻辑在 Repository / ViewModel 里，这里只负责显示与回调。
 */
@Composable
fun SearchTabContent(
  query: String,
  results: List<Song>,
  isDark: Boolean,
  onQueryChange: (String) -> Unit,
  onPlaySong: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  val textPrimary = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary

  Column(modifier = modifier.fillMaxSize()) {
    SearchField(
      query = query,
      isDark = isDark,
      onQueryChange = onQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    )
    when {
      query.isBlank() -> HintText("输入歌名、歌手或专辑开始搜索", isDark)
      results.isEmpty() -> HintText("没有找到「$query」相关的内容", isDark)
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(items = results, key = { it.id }) { song ->
          SongRow(
            song = song,
            isDark = isDark,
            onClick = { onPlaySong(song) }
          )
        }
      }
    }
  }
}

/** 圆角搜索框，用 BasicTextField 以便完全控制深浅色表现 */
@Composable
private fun SearchField(
  query: String,
  isDark: Boolean,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(22.dp)
  Box(
    modifier = modifier
      .height(44.dp)
      .clip(shape)
      .background(if (isDark) EchoCardDark else Color.White)
      .border(1.dp, if (isDark) EchoBorderDark else EchoBorder, shape)
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = "🔎", fontSize = 13.sp)
      Spacer(Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = "搜索歌曲 / 歌手 / 专辑",
            color = EchoTextSecondary,
            fontSize = 14.sp
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(
            color = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary,
            fontSize = 14.sp
          ),
          cursorBrush = SolidColor(EchoBlue),
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

/** 搜索结果行：封面 + 歌名歌手 + 时长 */
@Composable
private fun SongRow(
  song: Song,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CoverArt(
      seed = song.id,
      label = song.title.take(1),
      cornerRadius = 8.dp,
      labelSize = 16.sp,
      modifier = Modifier.size(44.dp)
    )
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        color = if (isDark) Color(0xFFE8EAED) else EchoTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = listOfNotNull(song.artist, song.album).joinToString(" · "),
        color = EchoTextSecondary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(Modifier.width(8.dp))
    Text(
      text = formatDuration(song.durationMs),
      color = EchoTextSecondary,
      fontSize = 11.sp
    )
  }
}

@Composable
private fun HintText(text: String, isDark: Boolean) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    Text(
      text = text,
      color = if (isDark) Color(0xFF6B7280) else EchoTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center
    )
  }
}

/** 毫秒转 m:ss，如 223000 → 3:43 */
private fun formatDuration(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
