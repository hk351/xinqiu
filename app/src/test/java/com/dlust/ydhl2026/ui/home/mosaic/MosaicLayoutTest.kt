package com.dlust.ydhl2026.ui.home.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 拼贴排版的单元测试：只验证几何契约，不依赖 Compose 运行时 */
class MosaicLayoutTest {

  private val viewportWidthPx = 1080
  private val planeWidthPx = MosaicLayout.planeWidthPx(viewportWidthPx)

  @Test
  fun `平面比屏幕宽很多 横向有足够拖动余量`() {
    assertTrue(planeWidthPx >= viewportWidthPx * 2)
  }

  @Test
  fun `同一输入排版完全确定`() {
    assertEquals(
      MosaicLayout.tiles(viewportWidthPx, 6000),
      MosaicLayout.tiles(viewportWidthPx, 6000)
    )
  }

  @Test
  fun `卡片之间互不重叠`() {
    val tiles = MosaicLayout.tiles(viewportWidthPx, 6000)
    assertTrue(tiles.size > 20)
    for (i in tiles.indices) {
      for (j in i + 1 until tiles.size) {
        val a = tiles[i]
        val b = tiles[j]
        val overlapX = a.x < b.x + b.width && b.x < a.x + a.width
        val overlapY = a.y < b.y + b.height && b.y < a.y + a.height
        assertTrue("卡片 ${a.key} 与 ${b.key} 重叠", !(overlapX && overlapY))
      }
    }
  }

  @Test
  fun `卡片不越出平面左右边界且尺寸为正`() {
    MosaicLayout.tiles(viewportWidthPx, 6000).forEach { tile ->
      assertTrue(tile.width > 0 && tile.height > 0)
      assertTrue("${tile.key} 左边越界", tile.x >= 0)
      assertTrue("${tile.key} 右边越界", tile.x + tile.width <= planeWidthPx)
    }
  }

  @Test
  fun `卡片足够大 最窄的卡也不低于四分之一屏`() {
    val tiles = MosaicLayout.tiles(viewportWidthPx, 3000)
    val narrowest = tiles.filter { it.span == 1 }.minOf { it.width }
    assertTrue("最窄的卡只有 ${narrowest}px", narrowest >= viewportWidthPx * 0.26f)
  }

  @Test
  fun `每一列自己向下连续 没有空洞`() {
    // 按卡片中心线往下找下一张：列缝只有几像素，按像素条带采样会踩进缝里误报
    val tiles = MosaicLayout.tiles(viewportWidthPx, 6000)
    tiles.forEach { tile ->
      val centerX = tile.x + tile.width / 2
      val next = tiles
        .filter { centerX >= it.x && centerX < it.x + it.width && it.y >= tile.y + tile.height }
        .minByOrNull { it.y }
        ?: return@forEach
      val space = next.y - (tile.y + tile.height)
      assertTrue("${tile.key} 下方空了 ${space}px", space <= 12)
    }
  }

  @Test
  fun `各列顶部起点参差不齐 且最前面的卡不会被顶出平面`() {
    val tiles = MosaicLayout.tiles(viewportWidthPx, 3000)
    // 顶部半屏内应出现多个不同的起点，而不是一条横平竖直的楼线
    val tops = tiles.map { it.y }.filter { it < viewportWidthPx / 2 }.distinct()
    assertTrue("顶部起点只有 ${tops.size} 种", tops.size >= 3)
    // 视口向上滑不动，排在前面的卡必须在平面内，否则推荐页会从中间名次开始
    assertEquals(0, tiles.first().y)
    assertTrue("有卡片被排到了平面上方", tiles.all { it.y >= 0 })
  }

  @Test
  fun `序号从零开始依次递增`() {
    val tiles = MosaicLayout.tiles(viewportWidthPx, 3000)
    assertEquals(tiles.indices.map { it }, tiles.map { it.globalIndex })
  }

  @Test
  fun `偶尔出现跨两列的宽卡 其余都是单列`() {
    val tiles = MosaicLayout.tiles(viewportWidthPx, 12000)
    assertTrue(tiles.any { it.span == 2 })
    assertTrue(tiles.all { it.span == 1 || it.span == 2 })
  }

  @Test
  fun `播放中的歌曲永远用强调样式`() {
    assertEquals(
      MosaicStyle.Accent,
      MosaicLayout.styleFor(span = 1, isCollection = true, playing = true, globalIndex = 0)
    )
  }

  @Test
  fun `宽卡用横排样式 窄卡按序号在文字与海报间轮换`() {
    assertEquals(
      MosaicStyle.Wide,
      MosaicLayout.styleFor(span = 2, isCollection = true, playing = false, globalIndex = 0)
    )
    assertEquals(
      MosaicStyle.TextBlock,
      MosaicLayout.styleFor(span = 1, isCollection = true, playing = false, globalIndex = 0)
    )
    assertEquals(
      MosaicStyle.Poster,
      MosaicLayout.styleFor(span = 1, isCollection = false, playing = false, globalIndex = 0)
    )
  }

  @Test
  fun `视口越往下 需要生成的卡越多 且前面的卡保持不变`() {
    val shallow = MosaicLayout.tilesForViewport(viewportWidthPx, 0f, 2400)
    val deep = MosaicLayout.tilesForViewport(viewportWidthPx, 8000f, 2400)
    assertTrue(deep.size > shallow.size)
    assertEquals(shallow.first(), deep.first())
  }
}
