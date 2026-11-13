package com.dlust.ydhl2026.ui.local

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dlust.ydhl2026.data.model.Song
import com.dlust.ydhl2026.ui.components.ArrowBackIcon
import com.dlust.ydhl2026.ui.components.CoverArt
import com.dlust.ydhl2026.ui.components.MiniPlayerBar
import com.dlust.ydhl2026.ui.formatDuration
import com.dlust.ydhl2026.ui.localMusicSource
import com.dlust.ydhl2026.ui.localMusicStore
import com.dlust.ydhl2026.ui.localLyricsSource
import com.dlust.ydhl2026.ui.lyricsFolderStore
import com.dlust.ydhl2026.ui.LocalMusicViewModelFactory
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.EchoBlue
import com.dlust.ydhl2026.ui.theme.EchoBlueLight
import com.dlust.ydhl2026.ui.theme.EchoBorder
import com.dlust.ydhl2026.ui.theme.EchoBorderDark
import com.dlust.ydhl2026.ui.theme.EchoCardDark
import com.dlust.ydhl2026.ui.theme.EchoTextPrimary
import com.dlust.ydhl2026.ui.theme.EchoTextPrimaryDark
import com.dlust.ydhl2026.ui.theme.EchoTextSecondary

private val DarkTextPrimary = EchoTextPrimaryDark

/**
 * 本地音乐页：四种状态 —— 未授权 / 扫描中 / 就绪列表 / 空态。
 * 权限走 ActivityResult，扫描查 MediaStore，点歌直接复用全局 PlayerViewModel。
 */
@Composable
fun LocalMusicScreen(
  playerViewModel: PlayerViewModel,
  onBack: () -> Unit,
  onOpenPlayer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val store = remember { context.localMusicStore() }
  val source = remember { context.localMusicSource() }
  val lyricsSource = remember { context.localLyricsSource() }
  val lyricsFolder = remember { context.lyricsFolderStore() }
  var lyricsBound by remember { mutableStateOf(lyricsFolder.treeUri != null) }
  val viewModel: LocalMusicViewModel =
    viewModel(factory = LocalMusicViewModelFactory(source, store, lyricsSource))

  // 选歌词文件夹：SAF 授权后持久化读权限，重新扫描时按同名 .lrc 挂歌词
  val lyricsFolderPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      lyricsFolder.save(uri)
      lyricsBound = true
      viewModel.startScan()
    }
  }

  val permission = if (Build.VERSION.SDK_INT >= 33) {
    Manifest.permission.READ_MEDIA_AUDIO
  } else {
    Manifest.permission.READ_EXTERNAL_STORAGE
  }
  val activity = LocalActivity.current
  var hasPermission by remember { mutableStateOf(context.hasPermission(permission)) }
  // 只有真正请求过一次之后，rationale=false 才代表用户选了「不再询问 / 永久拒绝」
  var requested by rememberSaveable { mutableStateOf(false) }
  val permanentlyDenied = requested &&
    !hasPermission &&
    activity?.shouldShowRequestPermissionRationale(permission) == false

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    requested = true
    hasPermission = granted
    viewModel.onPermissionResult(granted)
  }

  // 回页面（含从系统设置返回）时重新读权限：刚开启就自动补扫
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    hasPermission = context.hasPermission(permission)
    if (hasPermission && viewModel.uiState.value.phase == LocalMusicPhase.PERMISSION) {
      viewModel.onPermissionResult(true)
    }
  }

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
  val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }
  val isPlaying by remember { derivedStateOf { playerState.isPlaying } }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = if (isDark) EchoBackgroundDark else EchoBackground,
    bottomBar = {
      playerState.currentSong?.let { song ->
        MiniPlayerBar(
          song = song,
          isPlaying = playerState.isPlaying,
          progress = if (playerState.durationMs > 0L) {
            playerState.positionMs.toFloat() / playerState.durationMs
          } else {
            0f
          },
          onTogglePlay = playerViewModel::onPlayPause,
          onClick = onOpenPlayer,
          isDark = isDark
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .systemBarsPadding()
    ) {
      NavBar(
        count = if (state.phase == LocalMusicPhase.READY) state.songs.size else 0,
        isDark = isDark,
        lyricsBound = lyricsBound,
        onBack = onBack,
        onPickLyricsFolder = { lyricsFolderPicker.launch(null) }
      )
      when (state.phase) {
        LocalMusicPhase.PERMISSION -> PermissionView(
          isDark = isDark,
          permanentlyDenied = permanentlyDenied,
          onGrant = { permissionLauncher.launch(permission) },
          onOpenSettings = {
            context.startActivity(
              Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
              )
            )
          },
          onSkip = onBack
        )

        LocalMusicPhase.SCANNING -> ScanningView(isDark = isDark)

        LocalMusicPhase.EMPTY -> EmptyView(isDark = isDark, onRescan = viewModel::startScan)

        LocalMusicPhase.READY -> ReadyView(
          state = state,
          isDark = isDark,
          currentSongId = currentSongId,
          isPlaying = isPlaying,
          onQueryChange = viewModel::onQueryChange,
          onSortChange = viewModel::onSortChange,
          onPlaySong = { song -> playerViewModel.onPlaySong(song, state.visible) }
        )
      }
    }
  }
}

/** 顶部导航：返回 + 标题 + 歌词目录入口 + 歌曲数 */
@Composable
private fun NavBar(
  count: Int,
  isDark: Boolean,
  lyricsBound: Boolean,
  onBack: () -> Unit,
  onPickLyricsFolder: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .clickable(onClick = onBack),
      contentAlignment = Alignment.Center
    ) {
      ArrowBackIcon(
        modifier = Modifier.size(20.dp),
        color = if (isDark) DarkTextPrimary else EchoTextPrimary
      )
    }
    Spacer(Modifier.width(10.dp))
    Text(
      text = "本地音乐",
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 17.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.weight(1f))
    Text(
      text = if (lyricsBound) "歌词目录已绑定" else "绑定歌词目录",
      color = if (lyricsBound) EchoBlue else EchoTextSecondary,
      fontSize = 11.5.sp,
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onPickLyricsFolder)
        .padding(horizontal = 8.dp, vertical = 5.dp)
    )
    if (count > 0) {
      Spacer(Modifier.width(4.dp))
      Text(
        text = "$count 首",
        color = EchoTextSecondary,
        fontSize = 12.sp
      )
    }
  }
}

/** 未授权：说明用途 + 一键申请；永久拒绝后改为引导去系统设置 */
@Composable
private fun PermissionView(
  isDark: Boolean,
  permanentlyDenied: Boolean,
  onGrant: () -> Unit,
  onOpenSettings: () -> Unit,
  onSkip: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 34.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(112.dp)
        .clip(RoundedCornerShape(34.dp))
        .background(
          Brush.linearGradient(
            listOf(
              if (isDark) EchoBlue.copy(alpha = 0.35f) else Color(0xFFE9EEFF),
              if (isDark) EchoBlueLight.copy(alpha = 0.25f) else Color(0xFFD8E1FF)
            )
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      CoverArt(
        seed = "local-music",
        label = "♪",
        cornerRadius = 34.dp,
        labelSize = 44.sp,
        modifier = Modifier.fillMaxSize()
      )
    }
    Spacer(Modifier.height(20.dp))
    Text(
      text = "把手机里的歌接进来",
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "Echo 需要读取本机音频文件的权限，\n扫描完成后会建立本地音乐库。",
      color = EchoTextSecondary,
      fontSize = 12.5.sp,
      lineHeight = 20.sp,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    PermissionItem("01", "只读取音频文件，不访问照片和通讯录", isDark)
    PermissionItem("02", "文件不会上传，全部留在本机", isDark)
    PermissionItem("03", "随时可以在系统设置里收回权限", isDark)
    Spacer(Modifier.height(if (permanentlyDenied) 16.dp else 28.dp))
    if (permanentlyDenied) {
      Text(
        text = "权限已被永久拒绝，需要到系统设置里手动开启",
        color = EchoTextSecondary,
        fontSize = 11.5.sp,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(12.dp))
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(EchoBlue)
        .clickable(onClick = if (permanentlyDenied) onOpenSettings else onGrant),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (permanentlyDenied) "去系统设置开启" else "允许读取本地音乐",
        color = Color.White,
        fontSize = 14.5.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
    Spacer(Modifier.height(10.dp))
    Text(
      text = "暂时不用，返回首页",
      color = EchoTextSecondary,
      fontSize = 12.sp,
      modifier = Modifier
        .clickable(onClick = onSkip)
        .padding(8.dp)
    )
  }
}

@Composable
private fun PermissionItem(num: String, text: String, isDark: Boolean, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = num, color = EchoBlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.width(10.dp))
    Text(
      text = text,
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 12.sp
    )
  }
}

/** 扫描中：涟漪动画 + 提示文案（MediaStore 查询很快，不展示假进度条） */
@Composable
private fun ScanningView(
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "ripple")
  val rippleAlpha by transition.animateFloat(
    initialValue = 0.55f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart),
    label = "rippleAlpha"
  )
  val rippleScale by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
    label = "rippleScale"
  )
  val coreScale by transition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    label = "coreScale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(bottom = 60.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
      // 两圈错峰扩散的涟漪
      Box(
        modifier = Modifier
          .size(120.dp)
          .scale(rippleScale)
          .alpha(rippleAlpha)
          .border(1.5.dp, EchoBlue, CircleShape)
      )
      Box(
        modifier = Modifier
          .size(120.dp)
          .scale((rippleScale + 1f) / 2f)
          .alpha(rippleAlpha * 0.8f)
          .border(1.dp, EchoBlueLight, CircleShape)
      )
      Box(
        modifier = Modifier
          .size(46.dp)
          .scale(coreScale)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(EchoBlueLight, EchoBlue))),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "♪", color = Color.White, fontSize = 20.sp)
      }
    }
    Spacer(Modifier.height(26.dp))
    Text(
      text = "正在扫描本地音乐…",
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(6.dp))
    Text(text = "读取系统媒体库中的音频文件", color = EchoTextSecondary, fontSize = 11.5.sp)
  }
}

/** 空态：引导放文件后重新扫描 */
@Composable
private fun EmptyView(
  isDark: Boolean,
  onRescan: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 44.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    CoverArt(
      seed = "local-empty",
      label = "📂",
      cornerRadius = 28.dp,
      labelSize = 34.sp,
      modifier = Modifier.size(88.dp)
    )
    Spacer(Modifier.height(18.dp))
    Text(
      text = "没有找到本地音乐",
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "手机上还没有可识别的音频文件。\n把 MP3 / FLAC / M4A 放进「音乐」文件夹后再扫描一次。",
      color = EchoTextSecondary,
      fontSize = 12.sp,
      lineHeight = 19.sp,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .clip(RoundedCornerShape(23.dp))
        .background(EchoBlue)
        .clickable(onClick = onRescan),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "重新扫描", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

/** 就绪：搜索 + 排序工具条与歌曲列表 */
@Composable
private fun ReadyView(
  state: LocalMusicUiState,
  isDark: Boolean,
  currentSongId: String?,
  isPlaying: Boolean,
  onQueryChange: (String) -> Unit,
  onSortChange: () -> Unit,
  onPlaySong: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      SearchField(
        query = state.query,
        isDark = isDark,
        onQueryChange = onQueryChange,
        modifier = Modifier.weight(1f)
      )
      SortButton(label = "⇅ ${state.sort.label}", isDark = isDark, onClick = onSortChange)
    }
    val summary = if (state.query.trim().isEmpty()) {
      "共 ${state.songs.size} 首 · 合计 ${formatDuration(state.songs.sumOf { it.durationMs })}"
    } else {
      "找到 ${state.visible.size} 首匹配「${state.query.trim()}」的歌曲"
    }
    Text(
      text = summary,
      color = EchoTextSecondary,
      fontSize = 11.5.sp,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 12.dp, end = 12.dp, top = 2.dp, bottom = 16.dp
      )
    ) {
      items(items = state.visible, key = { it.id }) { song ->
        val active = song.id == currentSongId
        LocalSongRow(
          song = song,
          active = active,
          playing = active && isPlaying,
          isDark = isDark,
          onClick = { onPlaySong(song) }
        )
      }
      if (state.visible.isEmpty()) {
        item {
          Text(
            text = "没有匹配的歌曲，换个关键词试试",
            color = EchoTextSecondary,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 48.dp)
          )
        }
      }
    }
  }
}

/** 圆角搜索框，风格与首页搜索 Tab 一致 */
@Composable
private fun SearchField(
  query: String,
  isDark: Boolean,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(19.dp)
  Box(
    modifier = modifier
      .height(38.dp)
      .clip(shape)
      .background(if (isDark) EchoCardDark else Color.White)
      .border(1.dp, if (isDark) EchoBorderDark else EchoBorder, shape)
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = "🔎", fontSize = 12.sp)
      Spacer(Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(text = "搜索本地歌曲、歌手或专辑", color = EchoTextSecondary, fontSize = 13.sp)
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(
            color = if (isDark) DarkTextPrimary else EchoTextPrimary,
            fontSize = 13.sp
          ),
          cursorBrush = Brush.verticalGradient(listOf(EchoBlue, EchoBlue)),
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun SortButton(
  label: String,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(19.dp)
  Box(
    modifier = modifier
      .height(38.dp)
      .clip(shape)
      .border(1.dp, if (isDark) EchoBorderDark else EchoBorder, shape)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (isDark) DarkTextPrimary else EchoTextPrimary,
      fontSize = 12.sp
    )
  }
}

/** 本地歌曲行：封面 + 歌名/歌手专辑 + 时长/格式，播放中显示跳动音量条 */
@Composable
private fun LocalSongRow(
  song: Song,
  active: Boolean,
  playing: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(14.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        when {
          active && isDark -> EchoBlue.copy(alpha = 0.22f)
          active -> EchoBlue.copy(alpha = 0.09f)
          else -> Color.Transparent
        }
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CoverArt(
      seed = song.id,
      label = song.title.take(1),
      cornerRadius = 12.dp,
      labelSize = 17.sp,
      modifier = Modifier.size(46.dp)
    )
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        color = if (active) EchoBlue else if (isDark) DarkTextPrimary else EchoTextPrimary,
        fontSize = 14.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(3.dp))
      Text(
        text = listOfNotNull(song.artist, song.album).joinToString(" · "),
        color = EchoTextSecondary,
        fontSize = 11.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Spacer(Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (playing) {
        VolumeBars()
      } else {
        Text(text = formatDuration(song.durationMs), color = EchoTextSecondary, fontSize = 11.5.sp)
      }
      FormatTag(format = audioFormatLabel(song.mimeType, song.sourceUrl), isDark = isDark)
    }
  }
}

/** 运行时权限检查：13+ 读音频权限、12 及以下读存储权限 */
private fun Context.hasPermission(permission: String): Boolean =
  ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun FormatTag(format: String, isDark: Boolean, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(4.dp)
  Text(
    text = format,
    color = EchoTextSecondary,
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.6.sp,
    modifier = modifier
      .border(1.dp, if (isDark) EchoBorderDark else EchoBorder, shape)
      .padding(horizontal = 4.dp)
  )
}

/** 播放中的三根跳动音量条（与歌单页同款视觉） */
@Composable
private fun VolumeBars(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "volume")
  val scale by transition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
    label = "scale"
  )
  Row(
    modifier = modifier.height(12.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    listOf(5f, 11f, 7f).forEachIndexed { index, height ->
      Box(
        modifier = Modifier
          .width(2.5.dp)
          .height(height.dp)
          .graphicsLayer(scaleY = if (index == 1) scale else 1f - scale * 0.4f)
          .background(EchoBlue, RoundedCornerShape(2.dp))
      )
    }
  }
}
