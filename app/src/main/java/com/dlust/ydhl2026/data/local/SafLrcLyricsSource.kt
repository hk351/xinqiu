package com.dlust.ydhl2026.data.local

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.dlust.ydhl2026.data.model.LyricLine
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从用户用 SAF 授权的文件夹里，递归收集 .lrc 并解析成歌词索引。
 *
 * 为什么走 SAF：Android 13+ 的 READ_MEDIA_AUDIO 只放行音频文件，
 * App 无法直接按路径读同目录的 .lrc；SAF 授权后再通过 ContentResolver 读才合法，
 * 而且不需要申请「所有文件访问」这种高风险权限。
 */
class SafLrcLyricsSource(
  private val context: Context,
  private val folder: LyricsFolderStore,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalLyricsSource {

  override suspend fun loadIndex(): Map<String, List<LyricLine>> = withContext(ioDispatcher) {
    val treeUri = folder.treeUri ?: return@withContext emptyMap()
    val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
      ?: return@withContext emptyMap()
    val index = mutableMapOf<String, List<LyricLine>>()
    collect(root, index, depth = 0)
    index
  }

  private fun collect(dir: DocumentFile, index: MutableMap<String, List<LyricLine>>, depth: Int) {
    if (depth > MAX_DEPTH) return
    val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
    children.forEach { file ->
      val name = file.name ?: return@forEach
      when {
        file.isDirectory -> collect(file, index, depth + 1)
        name.endsWith(".lrc", ignoreCase = true) -> {
          val key = songFileNameKey(name) ?: return@forEach
          val lines = parseLrcText(readText(file))
          if (lines.isNotEmpty()) index[key] = lines
        }
      }
    }
  }

  private fun readText(file: DocumentFile): String {
    val bytes = runCatching {
      context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
    }.getOrNull() ?: return ""
    return decode(bytes)
  }

  /**
   * LRC 常见 UTF-8 和 GBK 两种编码：先严格按 UTF-8 解码，失败再退回 GBK，
   * 避免中文歌词变乱码。同时去掉文件头可能存在的 BOM。
   */
  private fun decode(bytes: ByteArray): String {
    val decoder = Charsets.UTF_8.newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
      .getOrElse { String(bytes, Charset.forName("GBK")) }
    return text.removePrefix("\uFEFF")
  }

  private companion object {
    /** 按「歌手/专辑/歌曲.lrc」这类常见两级目录估算，留一层余量 */
    const val MAX_DEPTH = 4
  }
}
