package com.dlust.ydhl2026.ui.local

/**
 * 推断列表右侧展示的音频格式标签。
 * 本地歌曲的播放地址是 content:// URI，不带文件后缀，所以优先看 MediaStore 给出的 MIME；
 * 拿不到 MIME 时再退回按 URL 后缀猜，最后给个兜底值。纯函数，方便单测。
 */
fun audioFormatLabel(mimeType: String?, sourceUrl: String): String {
  val mime = mimeType?.trim()?.lowercase()
  when (mime) {
    "audio/mpeg", "audio/mp3", "audio/x-mp3" -> return "MP3"
    "audio/flac", "audio/x-flac" -> return "FLAC"
    "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac", "audio/x-aac" -> return "M4A"
    "audio/ogg", "audio/x-ogg", "audio/opus", "application/ogg" -> return "OGG"
    "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> return "WAV"
  }

  val path = sourceUrl.substringBefore('?').lowercase()
  return when {
    path.endsWith(".mp3") -> "MP3"
    path.endsWith(".flac") -> "FLAC"
    path.endsWith(".m4a") || path.endsWith(".mp4") || path.endsWith(".aac") -> "M4A"
    path.endsWith(".ogg") || path.endsWith(".oga") || path.endsWith(".opus") -> "OGG"
    path.endsWith(".wav") -> "WAV"
    else -> "AUDIO"
  }
}
