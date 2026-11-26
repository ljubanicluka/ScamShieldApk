package rs.scamshield.app

import java.net.URL
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pure parsing helpers for the official advisory sources. They use no Android classes, so they
 * are covered by JVM unit tests (app/src/test). Nothing here performs network access.
 */
object AdvisoryParsing {
    data class Notice(val title: String, val link: String, val dateMillis: Long)

    private val monthNames: Map<String, Int> = mapOf(
        "januar" to 1, "јануар" to 1, "january" to 1,
        "februar" to 2, "фебруар" to 2, "february" to 2,
        "mart" to 3, "март" to 3, "march" to 3,
        "april" to 4, "април" to 4,
        "maj" to 5, "мај" to 5, "may" to 5,
        "jun" to 6, "јун" to 6, "juni" to 6, "june" to 6,
        "jul" to 7, "јул" to 7, "juli" to 7, "july" to 7,
        "avgust" to 8, "август" to 8, "august" to 8,
        "septembar" to 9, "септембар" to 9, "september" to 9,
        "oktobar" to 10, "октобар" to 10, "october" to 10,
        "novembar" to 11, "новембар" to 11, "november" to 11,
        "decembar" to 12, "децембар" to 12, "december" to 12
    )

    private val textDate = Regex("(\\d{1,2})\\s*\\.\\s*([\\p{L}]+)\\.?\\s+(\\d{4})")
    private val numericDate = Regex("^(\\d{1,2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{4})\\.?$")
    private val numericDateInText = Regex("(?<!\\d)(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4}\\.?)(?!\\d)")
    private val notice = Regex(
        "(?is)<a\\b[^>]*?href\\s*=\\s*[\"']([^\"']*?/obavestenje/\\d+-[^\"']*)[\"'][^>]*>(.*?)</a>"
    )

    private const val MIN_TITLE_CHARS = 8
    private const val MAX_DATE_DISTANCE_CHARS = 600

    private fun utc() = TimeZone.getTimeZone("UTC")

    private fun utcMillis(year: Int, month: Int, day: Int): Long? {
        if (year !in 2000..2100 || month !in 1..12 || day !in 1..31) return null
        val f = SimpleDateFormat("yyyy-M-d", Locale.US).apply { timeZone = utc(); isLenient = false }
        return try { f.parse("$year-$month-$day")?.time } catch (_: Exception) { null }
    }

    /** "14. Avgust 2026", "14. Август 2026" or "14.08.2026." (Serbian page dates) to UTC millis. */
    fun parseSerbianDate(text: String): Long? {
        val v = text.trim()
        numericDate.matchEntire(v)?.let {
            return utcMillis(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())
        }
        val m = textDate.find(v) ?: return null
        val month = monthNames[m.groupValues[2].lowercase(Locale.ROOT)] ?: return null
        return utcMillis(m.groupValues[3].toInt(), month, m.groupValues[1].toInt())
    }

    private val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss zzz",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"
    )

    /** RSS/Atom/ISO date text to UTC millis, or null if it is not a recognised format. */
    fun parseDateMillis(raw: String): Long? {
        val v = raw.trim()
        if (v.isEmpty()) return null
        for (pattern in patterns) {
            try {
                val f = SimpleDateFormat(pattern, Locale.US).apply { timeZone = utc(); isLenient = false }
                val pos = ParsePosition(0)
                val parsed = f.parse(v, pos)
                if (parsed != null && pos.index == v.length) return parsed.time
            } catch (_: Exception) {
                // try the next pattern
            }
        }
        return parseSerbianDate(v)
    }

    fun toIso(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = utc() }.format(java.util.Date(millis))

    /**
     * Parses the National CERT notice list (cert.rs/obavestenja.html, Latin, Cyrillic or English).
     * A notice is an anchor whose href contains /obavestenje/<number>-; its date is the nearest date
     * text just before it. Items without a recognisable date keep dateMillis = 0 (shown as undated).
     */
    fun parseCertRsListing(html: String, pageUrl: String, limit: Int = 30): List<Notice> {
        val page = html.replace("&nbsp;", " ").replace('\u00a0', ' ')
        // The listing uses both month-name dates (e.g. 14. Август 2026) and numeric
        // dates (e.g. 14.08.2026.). Collect both; the previous parser silently ignored the latter.
        val dates = (textDate.findAll(page).map { it.range.first to it.value } +
            numericDateInText.findAll(page).map { it.range.first to it.value })
            .mapNotNull { (position, value) -> parseSerbianDate(value)?.let { position to it } }
            .sortedBy { it.first }
        val result = mutableListOf<Notice>()
        val seen = HashSet<String>()
        var previousAnchor = -1
        for (m in notice.findAll(page)) {
            val start = m.range.first
            val link = absolute(m.groupValues[1], pageUrl)
            val title = plainText(m.groupValues[2])
            if (title.length >= MIN_TITLE_CHARS && isOfficialCertLink(link) && seen.add(urlKey(link))) {
                val date = dates.lastOrNull {
                    it.first < start && it.first > previousAnchor && start - it.first <= MAX_DATE_DISTANCE_CHARS
                }?.second ?: 0L
                result.add(Notice(title.take(240), link, date))
                if (result.size >= limit) break
            }
            previousAnchor = start
        }
        return result
    }

    private fun absolute(href: String, pageUrl: String): String = try {
        URL(URL(pageUrl), href.trim()).toString()
    } catch (_: Exception) {
        ""
    }

    private fun isOfficialCertLink(link: String): Boolean = try {
        val parsed = URL(link)
        parsed.protocol.equals("https", ignoreCase = true) &&
            parsed.host.lowercase(Locale.ROOT).removePrefix("www.") == "cert.rs"
    } catch (_: Exception) {
        false
    }

    private val entities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "laquo" to "«", "raquo" to "»",
        "bdquo" to "„", "ldquo" to "“", "rdquo" to "”", "lsquo" to "‘", "rsquo" to "’"
    )

    /** Strips tags, decodes common entities, collapses whitespace. */
    fun plainText(html: String): String {
        var s = html.replace(Regex("(?is)<(script|style)\\b.*?</\\1>"), " ").replace(Regex("<[^>]*>"), " ")
        s = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);").replace(s) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                body.startsWith("#") ->
                    body.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                else -> entities[body.lowercase(Locale.ROOT)] ?: m.value
            }
        }
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Identity of an advisory for de-duplication: host without "www.", path without a language
     * segment (rs, sr, en), no fragment, no utm_ parameters, lower case. Two different notices with
     * the same title therefore stay separate; the same notice reached through two URLs merges.
     */
    fun urlKey(url: String): String {
        var s = url.trim().substringBefore('#')
        s = s.replace(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://"), "")
        val host = s.substringBefore('/').lowercase(Locale.ROOT).removePrefix("www.")
        val rest = if (s.contains('/')) "/" + s.substringAfter('/') else ""
        val path = rest.substringBefore('?').replace(Regex("^/(rs|sr|en)(?=/)"), "").trimEnd('/').lowercase(Locale.ROOT)
        val query = rest.substringAfter('?', "").split('&')
            .filter { it.isNotBlank() && !it.startsWith("utm_", ignoreCase = true) }
            .joinToString("&").lowercase(Locale.ROOT)
        return host + path + if (query.isEmpty()) "" else "?$query"
    }

    /** Fallback identity when an item has no link: source id plus the normalised title. */
    fun titleKey(sourceId: String, title: String): String =
        sourceId + "|" + title.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    /**
     * Keeps one copy per advisory (identified by URL, else source+title; the newest copy wins), then
     * orders by audience rank (lower first), then publication time descending (undated items last in
     * their group), then title. Dates are compared as numbers, never as text.
     */
    fun <T> dedupeAndSort(
        items: List<T>,
        link: (T) -> String,
        title: (T) -> String,
        source: (T) -> String,
        rank: (T) -> Int,
        ts: (T) -> Long
    ): List<T> =
        items.sortedByDescending(ts)
            .distinctBy { if (link(it).isNotBlank()) urlKey(link(it)) else titleKey(source(it), title(it)) }
            .sortedWith(compareBy<T> { rank(it) }.thenByDescending { ts(it) }.thenBy { title(it) })
}
