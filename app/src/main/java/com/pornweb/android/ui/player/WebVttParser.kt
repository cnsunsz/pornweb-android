package com.pornweb.android.ui.player

/**
 * Minimal WebVTT cue parser: timestamps + plain text.
 * Ignores STYLE/NOTE/REGION and advanced cue settings/styling.
 */
data class WebVttCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object WebVttParser {
    private val arrowSplit = Regex("""\s*-->\s*""")
    private val tagStrip = Regex("""</?[^>]+>""")
    private val cueSettingTrim = Regex("""\s+.*$""")

    fun parse(content: String): List<WebVttCue> {
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.lineSequence().toList()
        val cues = ArrayList<WebVttCue>(64)
        var i = 0
        while (i < lines.size) {
            var line = lines[i].trim()
            i++
            if (line.isEmpty()) continue
            if (line.startsWith("WEBVTT", ignoreCase = true)) continue
            if (line.startsWith("NOTE", ignoreCase = true) ||
                line.startsWith("STYLE", ignoreCase = true) ||
                line.startsWith("REGION", ignoreCase = true)
            ) {
                while (i < lines.size && lines[i].trim().isNotEmpty()) i++
                continue
            }
            // Optional cue identifier
            if (!line.contains("-->")) {
                if (i >= lines.size) break
                val next = lines[i].trim()
                if (!next.contains("-->")) continue
                line = next
                i++
            }
            val parts = arrowSplit.split(line, limit = 2)
            if (parts.size != 2) continue
            val startMs = parseTimestamp(parts[0].trim()) ?: continue
            val endRaw = parts[1].trim().replace(cueSettingTrim, "")
            val endMs = parseTimestamp(endRaw) ?: continue
            val textLines = ArrayList<String>(2)
            while (i < lines.size) {
                val body = lines[i]
                i++
                if (body.trim().isEmpty()) break
                textLines += stripTags(body)
            }
            val text = textLines.joinToString("\n").trim()
            if (text.isEmpty() || endMs <= startMs) continue
            cues += WebVttCue(startMs, endMs, text)
        }
        return cues
    }

    fun activeText(cues: List<WebVttCue>, positionMs: Long): String? {
        if (cues.isEmpty()) return null
        // Prefer the last matching cue if overlapping
        var found: String? = null
        for (cue in cues) {
            if (positionMs >= cue.startMs && positionMs < cue.endMs) {
                found = cue.text
            } else if (found != null && positionMs < cue.startMs) {
                break
            }
        }
        return found
    }

    private fun stripTags(raw: String): String =
        raw.replace(tagStrip, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()

    /** Accepts HH:MM:SS.mmm, MM:SS.mmm; also tolerates comma millis. */
    private fun parseTimestamp(raw: String): Long? {
        val ts = raw.trim().replace(',', '.')
        if (ts.isEmpty()) return null
        val segments = ts.split(':')
        return try {
            when (segments.size) {
                3 -> {
                    val h = segments[0].toLong()
                    val m = segments[1].toLong()
                    val (s, ms) = splitSecMs(segments[2])
                    ((h * 3600L) + (m * 60L) + s) * 1000L + ms
                }
                2 -> {
                    val m = segments[0].toLong()
                    val (s, ms) = splitSecMs(segments[1])
                    ((m * 60L) + s) * 1000L + ms
                }
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun splitSecMs(secPart: String): Pair<Long, Long> {
        val bits = secPart.split('.', limit = 2)
        val s = bits[0].toLong()
        val ms = bits.getOrNull(1)
            ?.padEnd(3, '0')
            ?.take(3)
            ?.toLongOrNull()
            ?: 0L
        return s to ms
    }
}
