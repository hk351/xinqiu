package com.dlust.ydhl2026.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 主题模式与系统深浅色的合成规则 */
class ThemeModeTest {

  @Test
  fun `跟随系统时直接沿用系统深浅色`() {
    assertTrue(ThemeMode.SYSTEM.resolveDark(systemDark = true))
    assertFalse(ThemeMode.SYSTEM.resolveDark(systemDark = false))
  }

  @Test
  fun `强制浅色时忽略系统设置`() {
    assertFalse(ThemeMode.LIGHT.resolveDark(systemDark = true))
    assertFalse(ThemeMode.LIGHT.resolveDark(systemDark = false))
  }

  @Test
  fun `强制深色时忽略系统设置`() {
    assertTrue(ThemeMode.DARK.resolveDark(systemDark = false))
    assertTrue(ThemeMode.DARK.resolveDark(systemDark = true))
  }
}
