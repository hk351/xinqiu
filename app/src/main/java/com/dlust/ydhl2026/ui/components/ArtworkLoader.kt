package com.dlust.ydhl2026.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地音频的内嵌封面加载。
 *
 * 本地歌曲的 cover 地址就是音频自身的 content:// URI：
 * - Android 10+ 走 ContentResolver.loadThumbnail，由 MediaProvider 解内嵌图，带系统级缓存；
 * - 更低版本没有该 API，退回 MediaMetadataRetriever.embeddedPicture。
 * 解析不出封面时返回 null，调用方继续用渐变占位，不会报错。
 */

/** 按 URL 缓存解码结果，避免列表滚动时反复解同一张图 */
private val artworkCache = object : LruCache<String, ImageBitmap>(32) {}

fun cachedArtwork(url: String): ImageBitmap? = artworkCache.get(url)

suspend fun loadArtwork(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
  cachedArtwork(url)?.let { return@withContext it }
  val bitmap = runCatching { decodeArtwork(context, url) }.getOrNull()
  val image = bitmap?.asImageBitmap()
  if (image != null) artworkCache.put(url, image)
  image
}

/** 封面主色缓存：与图片同生命周期，切歌时不用重新缩图 */
private val artworkColorCache = object : LruCache<String, Int>(32) {}

/** 已经有缓存时同步取主色，背景层首帧就是正确颜色，不会先闪一下默认渐变 */
fun cachedArtworkColor(url: String): Color? = artworkColorCache.get(url)?.let(::Color)

suspend fun loadArtworkColor(context: Context, url: String): Color? = withContext(Dispatchers.IO) {
  cachedArtworkColor(url)?.let { return@withContext it }
  val bitmap = runCatching { decodeArtwork(context, url) }.getOrNull() ?: return@withContext null
  // 缩到 1x1 让系统做平均，比逐像素统计快得多；背景只需要一个大致的主色
  val scaled = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
  val pixel = scaled.getPixel(0, 0)
  if (scaled !== bitmap) scaled.recycle()
  artworkColorCache.put(url, pixel)
  Color(pixel)
}

private fun decodeArtwork(context: Context, url: String): Bitmap? {
  val uri = Uri.parse(url)
  // 网络封面留给后续 Subsonic 接入，这里只处理本地 content://
  if (uri.scheme != "content") return null
  val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    context.contentResolver.loadThumbnail(uri, Size(THUMB_SIZE, THUMB_SIZE), null)
  } else {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    } finally {
      retriever.release()
    }
  }
  return bitmap
}

private const val THUMB_SIZE = 256
