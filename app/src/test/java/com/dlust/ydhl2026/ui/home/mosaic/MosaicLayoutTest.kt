package com.dlust.ydhl2026.ui.home.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 拼贴排版的单元测试：只验证几何契约，不依赖 Compose 运行时 */
class MosaicLayoutTest {

  private val viewportWidthPx = 1080
  private val planeWidthPx = MosaicLayout.planeWidthPx(viewportWidthPx)

  @Test
  fun `平面比屏幕宽 横向拖动才有余量`() {
    assertTrue(planeWidthPx > viewportWidthPx)
  }

  @Test
  fun `同一输入排版完全确定`() {
    assertEquals(
      MosaicLayout.tiles(planeWidthPx, 6000),
      MosaicLayout.tiles(planeWidthPx, 6000)
    )
  }

  @Test
  fun `卡片之间互不重叠`() {
    val tiles = MosaicLayout.tiles(planeWidthPx, 6000)
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
    MosaicLayout.tiles(planeWidthPx, 6000).forEach { tile ->
      assertTrue(tile.width > 0 && tile.height > 0)
      assertTrue("${tile.key} 左边越界", tile.x >= 0)
      assertTrue("${tile.key} 右边越界", tile.x + tile.width <= planeWidthPx)
    }
  }

  @Test
  fun `整体向下连续 没有成片的空洞`() {
    val tiles = MosaicLayout.tiles(planeWidthPx, 6000)
    val sorted = tiles.sortedBy { it.y }
    var coveredUntil = sorted.first().y
    sorted.forEach { tile ->
      // 单个列内两张卡之间只有一条缝，再大就说明排版漏了格子
      assertTrue("在 y=${tile.y} 处出现空洞", tile.y <= coveredUntil + 8)
      coveredUntil = maxOf(coveredUntil, tile.y + tile.height)
    }
    assertEquals(sorted.maxOf { it.y + it.height }, coveredUntil)
  }

  @Test
  fun `序号从零开始依次递增`() {
    val tiles = MosaicLayout.tiles(planeWidthPx, 3000)
    assertEquals(tiles.indices.map { it }, tiles.map { it.globalIndex })
  }

  @Test
  fun `偶尔出现跨两列的宽卡 其余都是单列`() {
    val tiles = MosaicLayout.tiles(planeWidthPx, 12000)
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
    val shallow = MosaicLayout.tilesForViewport(planeWidthPx, 0f, 2400)
    val deep = MosaicLayout.tilesForViewport(planeWidthPx, 8000f, 2400)
    assertTrue(deep.size > shallow.size)
    assertEquals(shallow.first(), deep.first())
  }
}
