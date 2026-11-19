package com.dlust.ydhl2026.data.local.playlist

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlaylistDao {

  @Query("SELECT * FROM playlists WHERE id = :playlistId")
  suspend fun playlist(playlistId: Long): PlaylistEntity?

  @Upsert
  suspend fun upsertPlaylist(playlist: PlaylistEntity)

  @Upsert
  suspend fun upsertSongs(songs: List<SongEntity>)

  @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
  suspend fun entries(playlistId: Long): List<PlaylistSongEntity>

  @Upsert
  suspend fun upsertEntries(entries: List<PlaylistSongEntity>)

  /** 按 position 取可见曲目，删除的歌（hidden）不返回 */
  @Query(
    """
    SELECT s.* FROM songs s
    INNER JOIN playlist_songs ps ON s.id = ps.songId
    WHERE ps.playlistId = :playlistId AND ps.hidden = 0
    ORDER BY ps.position ASC
    """
  )
  suspend fun visibleSongs(playlistId: Long): List<SongEntity>

  @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
  suspend fun maxPosition(playlistId: Long): Int?

  @Query(
    "UPDATE playlist_songs SET hidden = :hidden WHERE playlistId = :playlistId AND songId = :songId"
  )
  suspend fun setHidden(playlistId: Long, songId: String, hidden: Boolean)

  @Query(
    "UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId"
  )
  suspend fun setPosition(playlistId: Long, songId: String, position: Int)
}
