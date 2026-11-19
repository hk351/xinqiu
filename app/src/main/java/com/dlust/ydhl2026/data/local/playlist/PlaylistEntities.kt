package com.dlust.ydhl2026.data.local.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dlust.ydhl2026.data.model.Playlist
import com.dlust.ydhl2026.data.model.Song

/** 歌单表：榜单、收藏、自建歌单共用一行，kind 区分来源 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey val id: Long,
  val name: String,
  /** "chart" / "favorite" / "custom"，为后续收藏与自建歌单留位 */
  val kind: String,
  val coverUrl: String?,
  val createdAt: Long,
  val lastModifiedAt: Long
)

/** 歌曲快照：网络歌只存元数据，播放地址仍在点击时实时解析 */
@Entity(tableName = "songs")
data class SongEntity(
  @PrimaryKey val id: String,
  val title: String,
  val artist: String,
  val album: String?,
  val albumArtUrl: String?,
  val durationMs: Long,
  val source: String,
  val sourceUrl: String,
  val lastModifiedAt: Long,
  val mimeType: String?,
  val fileName: String?
)

/**
 * 歌单与歌曲的关联。
 *
 * position 决定拖动排序后的展示顺序；hidden 让「删除」可以撤销——
 * 榜单每天会重新下发曲目，如果直接从表里删掉，下次合并又会被当成新歌塞回来。
 */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
  val playlistId: Long,
  val songId: String,
  val position: Int,
  val hidden: Boolean = false
)

fun Playlist.toEntity(kind: String): PlaylistEntity = PlaylistEntity(
  id = id,
  name = name,
  kind = kind,
  coverUrl = coverUrl,
  createdAt = createdAt,
  lastModifiedAt = lastModifiedAt
)

fun Song.toEntity(): SongEntity = SongEntity(
  id = id,
  title = title,
  artist = artist,
  album = album,
  albumArtUrl = albumArtUrl,
  durationMs = durationMs,
  source = source,
  sourceUrl = sourceUrl,
  lastModifiedAt = lastModifiedAt,
  mimeType = mimeType,
  fileName = fileName
)

/** 歌词不进库：网络歌播放时按 id 重新取词并走磁盘缓存 */
fun SongEntity.toSong(): Song = Song(
  id = id,
  title = title,
  artist = artist,
  album = album,
  albumArtUrl = albumArtUrl,
  durationMs = durationMs,
  source = source,
  sourceUrl = sourceUrl,
  lastModifiedAt = lastModifiedAt,
  mimeType = mimeType,
  fileName = fileName
)
