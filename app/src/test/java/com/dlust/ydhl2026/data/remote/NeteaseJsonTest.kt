package com.dlust.ydhl2026.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 接口 JSON 解析的单元测试。
 * 样本取自网易云网页端接口的真实结构（截断到两条），验证字段映射与容错。
 */
class NeteaseJsonTest {

  /** 现在的接口把歌单字段直接平铺在 result 上，样本照此裁两条 */
  private val chartJson = """
    {"result":{"name":"云音乐热歌榜","updateTime":1795048917148,"trackCount":2,
    "tracks":[
      {"id":3460493895,"name":"晴天","duration":269000,
       "artists":[{"name":"周杰伦"}],
       "album":{"name":"叶惠美","picUrl":"http://p2.music.126.net/cover1.jpg"}},
      {"id":1234567,"name":"","duration":1000,"artists":[],"album":null}
    ]}}
  """.trimIndent()

  /** 老版接口多包一层 playlist，解析仍要兼容 */
  private val legacyChartJson = """
    {"result":{"playlist":{"name":"云音乐新歌榜","updateTime":1,
    "tracks":[{"id":1,"name":"旧结构","duration":1000,"artists":[],"album":null}]}}}
  """.trimIndent()

  private val searchJson = """
    {"result":{"songs":[
      {"id":8888,"name":"浮夸","duration":282000,
       "artists":[{"name":"陈奕迅"}],
       "album":{"name":"U87","picUrl":"http://p3.music.126.net/cover2.jpg"}}
    ]}}
  """.trimIndent()

  @Test
  fun `榜单解析出名次且跳过没有歌名的脏数据`() {
    val snapshot = NeteaseJson.parseChart(chartJson)

    assertEquals("云音乐热歌榜", snapshot?.name)
    assertEquals(1, snapshot?.tracks?.size)
    assertEquals(1, snapshot?.tracks?.first()?.rank)
  }

  @Test
  fun `榜单曲目映射成网络歌 带封面与歌手`() {
    val song = NeteaseJson.parseChart(chartJson)!!.tracks.first().song

    assertEquals("ncm-3460493895", song.id)
    assertEquals("晴天", song.title)
    assertEquals("周杰伦", song.artist)
    assertEquals("叶惠美", song.album)
    assertEquals("http://p2.music.126.net/cover1.jpg", song.albumArtUrl)
    assertEquals(269_000L, song.durationMs)
    assertEquals("netease", song.source)
    // 播放地址要等点播时再解析
    assertTrue(song.sourceUrl.isEmpty())
  }

  @Test
  fun `接口返回空结构时当作没有榜单`() {
    assertNull(NeteaseJson.parseChart("""{"code":200}"""))
  }

  @Test
  fun `老版嵌套在 playlist 里的榜单也能解析`() {
    val snapshot = NeteaseJson.parseChart(legacyChartJson)

    assertEquals("云音乐新歌榜", snapshot?.name)
    assertEquals("旧结构", snapshot?.tracks?.first()?.song?.title)
  }

  @Test
  fun `搜索解析出歌名歌手与专辑`() {
    val songs = NeteaseJson.parseSearch(searchJson)

    assertEquals(1, songs.size)
    assertEquals("浮夸", songs.first().title)
    assertEquals("陈奕迅", songs.first().artist)
    assertEquals("U87", songs.first().album)
  }

  @Test
  fun `没有版权时播放地址为空`() {
    assertNull(NeteaseJson.parsePlayUrl("""{"data":[{"id":1,"url":null}]}"""))
    assertNull(NeteaseJson.parsePlayUrl("""{"data":[{"id":1,"url":""}]}"""))
  }

  @Test
  fun `有版权时取到可播放链接`() {
    val url = NeteaseJson.parsePlayUrl(
      """{"data":[{"id":1,"url":"http://m701.music.126.net/a.mp3"}]}"""
    )
    assertEquals("http://m701.music.126.net/a.mp3", url)
  }
}
