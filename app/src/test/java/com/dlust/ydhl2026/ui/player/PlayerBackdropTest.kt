package com.dlust.ydhl2026.ui.player

import com.dlust.ydhl2026.data.settings.PlayerBackdropStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 播放背景的纯逻辑：预设推荐值、滑杆夹紧、持久化读写约定 */
class PlayerBackdropTest {

  @Test
  fun `默认背景是原图且不带模糊`() {
    val backdrop = PlayerBackdrop.Default
    assertEquals(BackdropPreset.Artwork, backdrop.preset)
    assertEquals(0f, backdrop.blurDp, 0.001f)
    assertTrue(backdrop.dim > 0f)
  }

  @Test
  fun `切换预设会套用该预设的推荐值`() {
    val frosted = PlayerBackdrop.Default.withPreset(BackdropPreset.Frosted)

    assertEquals(BackdropPreset.Frosted, frosted.preset)
    assertEquals(BackdropPreset.Frosted.defaultBlurDp, frosted.blurDp, 0.001f)
    assertEquals(BackdropPreset.Frosted.defaultDim, frosted.dim, 0.001f)
  }

  @Test
  fun `滑杆越界会被夹紧`() {
    val backdrop = PlayerBackdrop.Default.withBlur(999f).withDim(-1f)

    assertEquals(PlayerBackdrop.MAX_BLUR_DP, backdrop.blurDp, 0.001f)
    assertEquals(PlayerBackdrop.MIN_DIM, backdrop.dim, 0.001f)
  }

  @Test
  fun `预设名可以往返解析且未知值回落到原图`() {
    BackdropPreset.entries.forEach { preset ->
      assertEquals(preset, PlayerBackdrop.presetFrom(preset.key))
    }

    assertEquals(BackdropPreset.Artwork, PlayerBackdrop.presetFrom("nope"))
    assertEquals(BackdropPreset.Artwork, PlayerBackdrop.presetFrom(null))
  }

  @Test
  fun `本地读回的脏数据会被修正`() {
    val fixed = PlayerBackdrop(BackdropPreset.Ink, 999f, -2f).coerce()

    assertEquals(PlayerBackdrop.MAX_BLUR_DP, fixed.blurDp, 0.001f)
    assertEquals(PlayerBackdrop.MIN_DIM, fixed.dim, 0.001f)
    // 修正数值不能顺手改掉用户选的预设
    assertEquals(BackdropPreset.Ink, fixed.preset)
  }

  @Test
  fun `存储只在值变化时落盘`() {
    val saved = mutableListOf<PlayerBackdrop>()
    val store = PlayerBackdropStore(PlayerBackdrop.Default) { saved += it }
    val frosted = PlayerBackdrop.Default.withPreset(BackdropPreset.Frosted)

    store.update(frosted)
    assertEquals(frosted, store.backdrop.value)
    assertEquals(listOf(frosted), saved)

    store.update(frosted)
    assertEquals(1, saved.size)
  }
}
