package com.dlust.ydhl2026.data.local.playlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 本地 SQLite 数据库（Room 在首次读写时自动创建 databases/echo.db）。
 *
 * version 从 1 起；以后改表结构必须升版本并补 migration，不能直接删库重建。
 */
@Database(
  entities = [PlaylistEntity::class, SongEntity::class, PlaylistSongEntity::class],
  version = 1,
  exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {

  abstract fun playlistDao(): PlaylistDao

  companion object {
    fun build(context: Context): PlaylistDatabase =
      Room.databaseBuilder(context.applicationContext, PlaylistDatabase::class.java, "echo.db")
        .build()
  }
}
