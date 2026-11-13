package com.dlust.ydhl2026.data.local

import android.content.Context
import android.net.Uri

/**
 * 用户通过 SAF 选定的「歌词文件夹」。
 * 只存树 URI（App 私有 SharedPreferences），重启后仍能读回；文件本身不复制，避免占空间。
 */
class LyricsFolderStore(context: Context) {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  val treeUri: Uri?
    get() = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

  fun save(uri: Uri) {
    prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
  }

  fun clear() {
    prefs.edit().remove(KEY_TREE_URI).apply()
  }

  private companion object {
    const val PREFS_NAME = "local_lyrics"
    const val KEY_TREE_URI = "tree_uri"
  }
}
