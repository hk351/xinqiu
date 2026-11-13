package com.dlust.ydhl2026.ui.local

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 格式标签推断测试：优先用 MediaStore 的 MIME，其次退回 URL 后缀，最后兜底 AUDIO。
 */
class AudioFormatTest {

  @Test
  fun `优先使用 MIME 类型`() {
    assertEquals("MP3", audioFormatLabel("audio/mpeg", "content://media/external/audio/media/1"))
    assertEquals("FLAC", audioFormatLabel("audio/x-flac", "content://media/external/audio/media/2"))
    assertEquals("M4A", audioFormatLabel("audio/mp4", "content://media/external/audio/media/3"))
    assertEquals("OGG", audioFormatLabel("audio/ogg", "content://media/external/audio/media/4"))
    assertEquals("WAV", audioFormatLabel("audio/x-wav", "content://media/external/audio/media/5"))
  }

  @Test
  fun `MIME 大小写和空格不影响识别`() {
    assertEquals("MP3", audioFormatLabel(" Audio/MPEG ", "content://media/external/audio/media/1"))
  }

  @Test
  fun `没有 MIME 时按 URL 后缀猜`() {
    assertEquals("MP3", audioFormatLabel(null, "https://example.com/a/b.mp3"))
    assertEquals("FLAC", audioFormatLabel(null, "https://example.com/b.FLAC?token=1"))
    assertEquals("M4A", audioFormatLabel(null, "file:///sdcard/Music/c.m4a"))
    assertEquals("OGG", audioFormatLabel(null, "file:///sdcard/Music/d.opus"))
    assertEquals("WAV", audioFormatLabel(null, "file:///sdcard/Music/e.wav"))
  }

  @Test
  fun `都识别不出时兜底 AUDIO`() {
    assertEquals("AUDIO", audioFormatLabel(null, "content://media/external/audio/media/9"))
    assertEquals("AUDIO", audioFormatLabel("audio/unknown", "content://media/external/audio/media/9"))
  }
}
