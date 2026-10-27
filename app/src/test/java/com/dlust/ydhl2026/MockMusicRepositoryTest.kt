package com.dlust.ydhl2026

import com.dlust.ydhl2026.data.repository.MockMusicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mock 数据源的单元测试：歌单、搜索、歌词三条主链路 */
class MockMusicRepositoryTest {

  private val repository = MockMusicRepository()

  @Test
  fun `歌单数量为 4`() = runBlocking {
    assertEquals(4, repository.playlists().first().size)
  }

  @Test
  fun `按歌名搜索能命中`() = runBlocking {
    val result = repository.search("稻香")
    assertEquals(1, result.size)
    assertEquals("稻香", result.first().title)
  }

  @Test
  fun `按歌手搜索能命中多首`() = runBlocking {
    val result = repository.search("周杰伦")
    assertEquals(3, result.size)
  }

  @Test
  fun `空关键词返回空列表`() = runBlocking {
    assertTrue(repository.search("   ").isEmpty())
  }

  @Test
  fun `稻香带有示例歌词`() = runBlocking {
    val rice = repository.recentSongs().first().first { it.title == "稻香" }
    assertTrue(rice.lyrics.isNotEmpty())
  }

  @Test
  fun `没有歌词的歌曲返回空列表`() = runBlocking {
    val sunny = repository.recentSongs().first().first { it.title == "晴天" }
    assertTrue(sunny.lyrics.isEmpty())
  }
}
