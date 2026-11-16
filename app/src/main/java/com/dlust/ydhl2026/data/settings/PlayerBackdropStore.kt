package com.dlust.ydhl2026.data.settings

import android.content.Context
import com.dlust.ydhl2026.ui.player.PlayerBackdrop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户 DIY 的播放背景。
 *
 * 状态与持久化拆开：本类只负责内存里的当前值并把变更交给 [persist]，
 * 这样单元测试不用碰 SharedPreferences，Android 侧的读写放在 [createPlayerBackdropStore]。
 */
class PlayerBackdropStore(
  initial: PlayerBackdrop = PlayerBackdrop.Default,
  private val persist: (PlayerBackdrop) -> Unit = {}
) {

  private val _backdrop = MutableStateFlow(initial.coerce())
  val backdrop: StateFlow<PlayerBackdrop> = _backdrop.asStateFlow()

  fun update(next: PlayerBackdrop) {
    val fixed = next.coerce()
    // 相同值不重复落盘，拖动滑杆时只有真正变化才写
    if (_backdrop.value == fixed) return
    _backdrop.value = fixed
    persist(fixed)
  }
}

/** 从本地读回上次的选择；没存过或存坏了都回落到默认预设 */
fun createPlayerBackdropStore(context: Context): PlayerBackdropStore {
  val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  val preset = PlayerBackdrop.presetFrom(prefs.getString(KEY_PRESET, null))
  val initial = PlayerBackdrop(
    preset = preset,
    blurDp = prefs.getFloat(KEY_BLUR, preset.defaultBlurDp),
    dim = prefs.getFloat(KEY_DIM, preset.defaultDim)
  )
  return PlayerBackdropStore(initial) { backdrop ->
    prefs.edit()
      .putString(KEY_PRESET, backdrop.preset.key)
      .putFloat(KEY_BLUR, backdrop.blurDp)
      .putFloat(KEY_DIM, backdrop.dim)
      .apply()
  }
}

private const val PREFS_NAME = "player_backdrop"
private const val KEY_PRESET = "preset"
private const val KEY_BLUR = "blur_dp"
private const val KEY_DIM = "dim"
