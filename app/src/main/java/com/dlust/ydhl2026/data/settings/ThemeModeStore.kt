package com.dlust.ydhl2026.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用外观模式。
 *
 * 登录页右上角的黑白按钮会显式切到 [LIGHT] / [DARK]；
 * 首次安装默认 [SYSTEM]，跟随手机系统深浅色。
 */
enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK;

  /** 结合系统当前深浅色，算出实际是否使用深色主题 */
  fun resolveDark(systemDark: Boolean): Boolean = when (this) {
    SYSTEM -> systemDark
    LIGHT -> false
    DARK -> true
  }
}

/**
 * 主题模式的持久化。
 *
 * 抽成接口是为了让页面只依赖「读当前模式 / 写新模式」，不必关心存储方式。
 */
interface ThemeModeStore {
  val mode: StateFlow<ThemeMode>

  fun setMode(mode: ThemeMode)
}

fun createThemeModeStore(context: Context): ThemeModeStore = SharedPrefsThemeModeStore(context)

private class SharedPrefsThemeModeStore(context: Context) : ThemeModeStore {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _mode = MutableStateFlow(read())
  override val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

  override fun setMode(mode: ThemeMode) {
    if (_mode.value == mode) return
    _mode.value = mode
    // 改完立刻生效，磁盘写入放后台，不阻塞点击
    prefs.edit().putString(KEY_MODE, mode.name).apply()
  }

  private fun read(): ThemeMode {
    val saved = prefs.getString(KEY_MODE, null) ?: return ThemeMode.SYSTEM
    return runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
  }

  private companion object {
    const val PREFS_NAME = "appearance"
    const val KEY_MODE = "theme_mode"
  }
}
