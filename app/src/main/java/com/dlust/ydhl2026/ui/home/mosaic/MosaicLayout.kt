package com.dlust.ydhl2026.ui.home.mosaic

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 卡片在二维平面上的视觉类型。
 * 排版只决定几何，具体用哪种样式由内容类型、是否在播放和序号一起决定。
 */
enum class MosaicStyle {
  /** 封面海报：封面铺满，底部压歌名与歌手 */
  Poster,

  /** 沉浸大字：标题明显放大，作为画布里的视觉重音 */
  Hero,

  /** 文字纹理：没有封面的深色渐变卡，避免整面墙全是图片 */
  TextBlock,

  /** 跨列横幅：横向占两格，用来打断竖卡的节奏 */
  Wide,

  /** 正在播放：在海报基础上加品牌色描边与跳动音量条 */
  Accent
}

/** 一个卡片槽位：坐标与尺寸以平面左上角为原点，单位 px，可直接用于绝对定位 */
data class TileSlot(
  val key: String,
  val globalIndex: Int,
  val span: Int,
  val x: Int,
  val y: Int,
  val width: Int,
  val height: Int
)

/**
 * 五条宽度不等的列。
 * 每张卡直接落在某一条列里，偶尔跨两列；各列独立向下排，横缝和竖缝都不会贯通，
 * 这是拼贴感和普通网格最大的区别。
 */
private val LaneWidths = floatArrayOf(0.24f, 0.20f, 0.22f, 0.18f, 0.16f)

/** 卡片高度 = 宽度 × 这组比例，循环取用，让高矮有节奏地变化 */
private val HeightRatios = floatArrayOf(1.34f, 1.05f, 1.20f, 0.95f, 1.10f, 1.28f)

/**
 * 拼贴平面的几何排版。
 *
 * 纯 Kotlin，不引 Android 依赖：同一组参数永远产出同一份结果，卡片之间不重叠、不越界，
 * 单元测试可以直接验证。画面只负责把 [TileSlot] 绝对定位到平面上。
 */
object MosaicLayout {

  /** 平面比屏幕宽，横向拖动才有内容可看；1.25 倍大约多露出四分之一画面 */
  const val PlaneWidthFactor = 1.25f

  /** 竖向兜底：防止极端拖动把整面墙无限算进内存 */
  private const val MaxTiles = 6000

  fun planeWidthPx(viewportWidthPx: Int): Int =
    (viewportWidthPx * PlaneWidthFactor).roundToInt().coerceAtLeast(viewportWidthPx)

  /**
   * 该用哪种卡片样式。
   * 规则保持稳定，同一张卡每次重绘样式不变；久滑只会看到内容循环，不会看到样式乱跳。
   */
  fun styleFor(
    span: Int,
    isCollection: Boolean,
    playing: Boolean,
    globalIndex: Int
  ): MosaicStyle = when {
    playing -> MosaicStyle.Accent
    span >= 2 -> MosaicStyle.Wide
    isCollection && globalIndex % 3 != 1 -> MosaicStyle.TextBlock
    globalIndex % 5 == 2 -> MosaicStyle.Hero
    else -> MosaicStyle.Poster
  }

  /**
   * 从平面顶部开始砌，直到最矮的一列也超过 [minHeightPx]。
   * 已生成的卡片坐标只依赖序号，所以滚动后再生成时前面的结果完全一致，画面不会跳。
   */
  fun tiles(planeWidthPx: Int, minHeightPx: Int): List<TileSlot> {
    if (planeWidthPx <= 0 || minHeightPx <= 0) return emptyList()
    // 细缝按平面宽度取：屏幕上大约 1~2dp，让纯色块之间既有边界感又保持"一整面"
    val gap = max(2, (planeWidthPx * 0.0021f).roundToInt())
    val half = gap / 2
    val laneCount = LaneWidths.size

    val laneStarts = FloatArray(laneCount)
    var acc = 0f
    for (i in 0 until laneCount) {
      laneStarts[i] = acc
      acc += LaneWidths[i]
    }

    val cursor = IntArray(laneCount)
    val result = ArrayList<TileSlot>(256)
    var index = 0
    while (cursor.min() < minHeightPx && result.size < MaxTiles) {
      // 每次填当前最矮的一列；平局时按序号轮换，避免总是从最左列开始
      var lane = index % laneCount
      for (i in 0 until laneCount) {
        if (cursor[i] < cursor[lane]) lane = i
      }

      // 每隔几张尝试跨两列：只有两列底部齐平、且合起来不会宽到占满视野时才跨
      var span = 1
      if (
        index % 8 == 3 &&
        lane + 1 < laneCount &&
        cursor[lane] == cursor[lane + 1] &&
        LaneWidths[lane] + LaneWidths[lane + 1] <= 0.42f
      ) {
        span = 2
      }

      val xRatio = laneStarts[lane]
      var widthRatio = 0f
      repeat(span) { widthRatio += LaneWidths[lane + it] }

      val left = (xRatio * planeWidthPx).roundToInt() + half
      val right = ((xRatio + widthRatio) * planeWidthPx).roundToInt() - (gap - half)
      val width = right - left
      if (width <= 0) {
        index++
        continue
      }

      val top = cursor[lane]
      var height = (width * HeightRatios[index % HeightRatios.size]).roundToInt()

      // 和相邻列底部接近时，把高度微调过去对齐，下一张就有机会跨列
      for (neighbor in intArrayOf(lane - 1, lane + 1)) {
        if (neighbor < 0 || neighbor >= laneCount) continue
        val target = cursor[neighbor]
        val bottom = top + height
        if (target > bottom && target - bottom <= (width * 0.35f).roundToInt()) {
          height = target - top
          break
        }
      }

      result += TileSlot(
        key = "tile-$index",
        globalIndex = index,
        span = span,
        x = left,
        y = top,
        width = width,
        height = height
      )
      val bottom = top + height + gap
      for (i in lane until lane + span) cursor[i] = bottom
      index++
    }
    return result
  }

  /**
   * 生成覆盖当前视口所需的卡片，并多留一屏余量。
   * 视口向下移动时只需追加后面的卡，前面的几何不变，所以画面不会跳动。
   */
  fun tilesForViewport(
    planeWidthPx: Int,
    scrollYPx: Float,
    viewportHeightPx: Int
  ): List<TileSlot> {
    val targetBottom = (scrollYPx + viewportHeightPx * 2f).roundToInt()
    return tiles(planeWidthPx, targetBottom)
  }
}
