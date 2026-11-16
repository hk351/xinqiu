package com.dlust.ydhl2026

import com.dlust.ydhl2026.ui.components.progressToMs
import org.junit.Assert.assertEquals
import org.junit.Test

/** 进度条拖动比例的换算测试 */
class SeekBarTest {

  @Test
  fun `按比例换算毫秒`() {
    assertEquals(0L, progressToMs(0f, 200_000L))
    assertEquals(100_000L, progressToMs(0.5f, 200_000L))
    assertEquals(200_000L, progressToMs(1f, 200_000L))
  }

  @Test
  fun `比例越界会被夹住`() {
    assertEquals(0L, progressToMs(-0.4f, 200_000L))
    assertEquals(200_000L, progressToMs(1.8f, 200_000L))
  }

  @Test
  fun `时长未知时返回零`() {
    assertEquals(0L, progressToMs(0.6f, 0L))
    assertEquals(0L, progressToMs(0.6f, -1L))
  }
}
