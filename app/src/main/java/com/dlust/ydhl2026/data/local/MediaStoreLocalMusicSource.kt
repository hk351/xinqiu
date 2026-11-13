package com.dlust.ydhl2026.data.local

import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import com.dlust.ydhl2026.data.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 MediaStore 的本地音频扫描实现。
 *
 * 关键点：
 * - 查询 MediaStore.Audio.Media，系统已建好索引，无需自己遍历文件系统；
 * - 过滤时长 < 30s 的音频（多为铃声/通知音），只保留歌曲；
 * - 播放地址直接用 content:// URI，Media3 的 DefaultDataSource 能原生处理；
 * - DATE_ADDED（秒）映射进 lastModifiedAt，供「最近添加」排序使用。
 */
class MediaStoreLocalMusicSource(
  private val context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalMusicSource {

  override suspend fun scan(): List<Song> = withContext(ioDispatcher) {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.DATE_ADDED,
      MediaStore.Audio.Media.MIME_TYPE,
      MediaStore.Audio.Media.DISPLAY_NAME
    )
    // 只要真正的歌曲：有时长、时长达标；标题缺失的也跳过
    val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
    val selectionArgs = arrayOf(MIN_DURATION_MS.toString())

    val songs = mutableListOf<Song>()
    context.contentResolver.query(
      collection,
      projection,
      selection,
      selectionArgs,
      "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
      val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
      val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
      val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
      val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
      while (cursor.moveToNext()) {
        val title = cursor.getString(titleCol) ?: continue
        val mediaId = cursor.getLong(idCol)
        // MediaStore 对缺失的艺术家/专辑会填 "<unknown>"，统一换成中文占位
        val artist = cursor.getString(artistCol)?.takeIf { it != UNKNOWN_MARKER } ?: UNKNOWN_ARTIST
        val album = cursor.getString(albumCol)?.takeIf { it != UNKNOWN_MARKER }
        val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()
        songs += Song(
          id = "local-$mediaId",
          title = title,
          artist = artist,
          album = album,
          // 本地曲目没有独立的封面 URI，直接用音频 URI，播放时可从中解出内嵌专辑图
          albumArtUrl = contentUri,
          durationMs = cursor.getLong(durationCol),
          source = "local",
          sourceUrl = contentUri,
          lyrics = emptyList(),
          lastModifiedAt = cursor.getLong(addedCol) * 1000L,
          mimeType = cursor.getString(mimeCol),
          fileName = cursor.getString(nameCol)
        )
      }
    }
    songs
  }

  private companion object {
    /** 短于 30 秒的音频通常是铃声/提示音，不当作歌曲 */
    const val MIN_DURATION_MS = 30_000L

    /** MediaStore 对缺失元数据的默认占位 */
    const val UNKNOWN_MARKER = "<unknown>"
    const val UNKNOWN_ARTIST = "未知歌手"
  }
}
