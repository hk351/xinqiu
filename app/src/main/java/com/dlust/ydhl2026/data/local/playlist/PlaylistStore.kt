package com.dlust.ydhl2026.data.local.playlist

import androidx.room.withTransaction
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song

/**
 * 歌单的本地持久化。
 *
 * 榜单的歌单曲目来自网络，但用户删掉的歌和拖出来的顺序要跨重启保留：
 * 第一次打开榜单时把曲目灌进数据库，之后以数据库为准，网络只负责补充新上榜的歌。
 */
interface PlaylistStore {
  /** 加载榜单歌单：首次打开时落库，之后返回数据库中的可见顺序 */
  suspend fun loadChart(playlist: Playlist, songs: List<Song>): List<Song>

  /** 隐藏一首歌（撤销时再放出来），榜单刷新不会把它重新塞回来 */
  suspend fun removeSong(playlistId: Long, songId: String)

  /** 撤销删除并写回最新顺序 */
  suspend fun restoreSong(playlistId: Long, songId: String, order: List<String>)

  /** 拖拽结束后保存当前可见顺序 */
  suspend fun saveOrder(playlistId: Long, songIds: List<String>)
}

class RoomPlaylistStore(private val database: PlaylistDatabase) : PlaylistStore {

  private val dao = database.playlistDao()

  override suspend fun loadChart(playlist: Playlist, songs: List<Song>): List<Song> =
    database.withTransaction {
      dao.upsertPlaylist(playlist.toEntity(kind = "chart"))
      val existing = dao.entries(playlist.id)
      if (existing.isEmpty()) {
        // 第一次打开：把网络曲目原样落库，之后删除/排序都以库为准
        dao.upsertSongs(songs.map { it.toEntity() })
        dao.upsertEntries(
          songs.mapIndexed { index, song ->
            PlaylistSongEntity(playlist.id, song.id, position = index)
          }
        )
      } else {
        // 榜单每天变化：只把还没见过的歌追加到末尾，不打扰用户已有的顺序
        val known = existing.mapTo(HashSet()) { it.songId }
        val fresh = songs.filter { it.id !in known }
        if (fresh.isNotEmpty()) {
          var next = (dao.maxPosition(playlist.id) ?: -1) + 1
          dao.upsertSongs(fresh.map { it.toEntity() })
          dao.upsertEntries(
            fresh.map { PlaylistSongEntity(playlist.id, it.id, position = next++) }
          )
        }
      }
      dao.visibleSongs(playlist.id).map { it.toSong() }
    }

  override suspend fun removeSong(playlistId: Long, songId: String) {
    database.withTransaction { dao.setHidden(playlistId, songId, hidden = true) }
  }

  override suspend fun restoreSong(playlistId: Long, songId: String, order: List<String>) {
    database.withTransaction {
      dao.setHidden(playlistId, songId, hidden = false)
      writeOrder(playlistId, order)
    }
  }

  override suspend fun saveOrder(playlistId: Long, songIds: List<String>) {
    database.withTransaction { writeOrder(playlistId, songIds) }
  }

  private suspend fun writeOrder(playlistId: Long, songIds: List<String>) {
    songIds.forEachIndexed { index, songId -> dao.setPosition(playlistId, songId, index) }
  }
}
