package com.dlust.ydhl2026.ui.player

/** 底图来源：预设之间的差异只在于「用哪张底图 + 推荐多糊、多暗」 */
enum class BackdropSource {
  /** 专辑封面铺满 */
  Artwork,

  /** 按歌曲 seed 派生的同色系渐变 */
  Gradient,

  /** 从封面里提炼主色 */
  Dominant,

  /** 近黑纯色，专注歌词 */
  Ink
}

/**
 * 播放背景预设。
 *
 * 每个预设自带一档推荐模糊与压暗，点一下就是完整的一套效果；
 * 滑杆在推荐值的基础上继续微调，供用户 DIY。
 */
enum class BackdropPreset(
  val key: String,
  val label: String,
  val source: BackdropSource,
  val defaultBlurDp: Float,
  val defaultDim: Float,
  /** 大于 0 时把封面半透明叠在渐变底图上，做出「封面掺进色块」的混合感 */
  val coverAlpha: Float = 0f
) {
  Artwork("artwork", "原图", BackdropSource.Artwork, 0f, 0.30f),
  Frosted("frosted", "毛玻璃", BackdropSource.Artwork, 26f, 0.34f),
  Gradient("gradient", "渐层", BackdropSource.Gradient, 0f, 0.34f),
  Duotone("duotone", "封面渐层", BackdropSource.Gradient, 10f, 0.38f, coverAlpha = 0.32f),
  Dominant("dominant", "取色", BackdropSource.Dominant, 22f, 0.34f),
  Ink("ink", "墨黑", BackdropSource.Ink, 0f, 0.18f)
}

/**
 * 当前生效的背景：预设 + 两个可调参数。
 *
 * 数值一律夹在合法区间内，避免滑杆或历史脏数据把界面调成一片死黑。
 */
data class PlayerBackdrop(
  val preset: BackdropPreset,
  val blurDp: Float,
  val dim: Float
) {

  /** 换预设时回到该预设的推荐值，一键就是完整效果 */
  fun withPreset(next: BackdropPreset) = of(next)

  fun withBlur(value: Float) = copy(blurDp = value.coerceIn(MIN_BLUR_DP, MAX_BLUR_DP))

  fun withDim(value: Float) = copy(dim = value.coerceIn(MIN_DIM, MAX_DIM))

  /** 修正从本地读回来的脏值，不改用户选的预设 */
  fun coerce(): PlayerBackdrop = copy(
    blurDp = blurDp.coerceIn(MIN_BLUR_DP, MAX_BLUR_DP),
    dim = dim.coerceIn(MIN_DIM, MAX_DIM)
  )

  companion object {
    const val MIN_BLUR_DP = 0f
    const val MAX_BLUR_DP = 40f
    const val MIN_DIM = 0f
    const val MAX_DIM = 0.80f

    /** 按预设的推荐值生成背景，避免调用方漏填模糊或压暗 */
    fun of(preset: BackdropPreset) = PlayerBackdrop(
      preset = preset,
      blurDp = preset.defaultBlurDp,
      dim = preset.defaultDim
    )

    /** 首次安装、点「恢复默认」时用的一档 */
    val Default: PlayerBackdrop = of(BackdropPreset.Artwork)

    /** 持久化里可能存着旧版本或拼错的预设名，认不出就回落到默认 */
    fun presetFrom(key: String?): BackdropPreset =
      BackdropPreset.entries.firstOrNull { it.key == key } ?: BackdropPreset.Artwork
  }
}
