package com.dlust.ydhl2026.ui.home.mosaic

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
 * 六条宽度不等的列，单位是「屏幕宽的倍数」。
 *
 * 用屏幕宽而不是平面宽做基准，是为了让卡片大小和平面宽度解耦：
 * 想把平面拉长（横向能滑更远）时只要加列，卡片不会被一起缩小。
 */
private val LaneWidths = floatArrayOf(
  0.42f, 0.30f, 0.38f, 0.28f, 0.36f, 0.32f, 0.34f, 0.30f, 0.40f, 0.28f
)

/**
 * 各列从不同的高度起砌（屏幕宽倍数）。
 * 参差主要来自这里：如果所有列都从 0 开始，再随机的高度比例也会在第一屏排出横向对齐的"楼层线"。
 */
private val LaneStarts = floatArrayOf(
  0f, 0.30f, 0.10f, 0.44f, 0.18f, 0.36f, 0.06f, 0.40f, 0.14f, 0.28f
)

/** 卡片高度 = 宽度 × 这组比例，跨度拉到 0.72~1.85，横条和超长竖条交替出现 */
private val HeightRatios =
  floatArrayOf(1.85f, 0.72f, 1.42f, 1.06f, 1.68f, 0.88f, 1.24f, 1.52f, 0.78f, 1.34f)

/** 跨列卡的合宽上限：超过一屏就装不下，宁可不跨 */
private const val MaxSpanWidth = 0.96f

/**
 * 拼贴平面的几何排版。
 *
 * 纯 Kotlin，不引 Android 依赖：同一组参数永远产出同一份结果，卡片之间不重叠、不越界，
 * 单元测试可以直接验证。画面只负责把 [TileSlot] 绝对定位到平面上。
 */
object MosaicLayout {

  /** 竖向兜底：防止极端拖动把整面墙无限算进内存 */
  private const val MaxTiles = 6000

  /** 平面总宽 = 各列宽之和，约 2.76 屏，横向能滑近两屏远 */
  fun planeWidthPx(viewportWidthPx: Int): Int =
    (viewportWidthPx * LaneWidths.sum()).roundToInt()

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
  fun tiles(viewportWidthPx: Int, minHeightPx: Int): List<TileSlot> {
    if (viewportWidthPx <= 0 || minHeightPx <= 0) return emptyList()
    // 细缝按屏幕宽取：大约 1dp，色块之间有边界感又仍像"一整面"
    val gap = max(3, (viewportWidthPx * 0.0028f).roundToInt())
    val half = gap / 2
    val laneCount = LaneWidths.size

    val laneStarts = FloatArray(laneCount)
    var acc = 0f
    for (i in 0 until laneCount) {
      laneStarts[i] = acc
      acc += LaneWidths[i]
    }

    // 起始高度按列往上错开：卡片从平面上边缘之上伸进来，顶部既没有横平竖直的起点，
    // 也不会在浅色模式下露出一块空白的页面底色
    val cursor = IntArray(laneCount) { -(LaneStarts[it] * viewportWidthPx).roundToInt() }
    val result = ArrayList<TileSlot>(256)
    var index = 0
    var lastSpan = -10
    while (cursor.min() < minHeightPx && result.size < MaxTiles) {
      // 每次填当前最矮的一列；平局时按序号轮换，避免总是从最左列开始
      var lane = index % laneCount
      for (i in 0 until laneCount) {
        if (cursor[i] < cursor[lane]) lane = i
      }

      // 相邻两列底部齐平时跨两列，但至少隔两张才跨一次，免得宽卡连成横条
      var startLane = lane
      var span = 1
      if (index - lastSpan >= 2) {
        // 平局时最矮列可能是左边那列也可能是右边那列，两种拼法都要试
        for (start in intArrayOf(lane, lane - 1)) {
          if (start < 0 || start + 1 >= laneCount) continue
          if (
            cursor[start] == cursor[start + 1] &&
            LaneWidths[start] + LaneWidths[start + 1] <= MaxSpanWidth
          ) {
            startLane = start
            span = 2
            lastSpan = index
            break
          }
        }
      }

      val xRatio = laneStarts[startLane]
      var widthRatio = 0f
      repeat(span) { widthRatio += LaneWidths[startLane + it] }

      val left = (xRatio * viewportWidthPx).roundToInt() + half
      val right = ((xRatio + widthRatio) * viewportWidthPx).roundToInt() - (gap - half)
      val width = right - left
      if (width <= 0) {
        index++
        continue
      }

      val top = cursor[startLane]
      var height = (width * HeightRatios[(index + startLane * 3) % HeightRatios.size]).roundToInt()

      // 和相邻列底部接近时，把高度拉长过去对齐（减掉缝宽，让下一张的起点正好落在同一高度），
      // 下一张就有机会跨列
      for (neighbor in intArrayOf(startLane - 1, startLane + span)) {
        if (neighbor < 0 || neighbor >= laneCount) continue
        val target = cursor[neighbor]
        val bottom = top + height
        if (target > bottom && target - bottom <= (width * 0.55f).roundToInt()) {
          height = target - top - gap
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
      for (i in startLane until startLane + span) cursor[i] = bottom
      index++
    }
    return result
  }

  /**
   * 生成覆盖当前视口所需的卡片，并多留一屏余量。
   * 视口向下移动时只需追加后面的卡，前面的几何不变，所以画面不会跳动。
   */
  fun tilesForViewport(
    viewportWidthPx: Int,
    scrollYPx: Float,
    viewportHeightPx: Int
  ): List<TileSlot> {
    val targetBottom = (scrollYPx + viewportHeightPx * 2f).roundToInt()
    return tiles(viewportWidthPx, targetBottom)
  }
}
