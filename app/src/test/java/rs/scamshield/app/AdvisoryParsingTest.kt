package rs.scamshield.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sample pages below are RECONSTRUCTED from the text of the live https://cert.rs/obavestenja.html
 * page (date line, heading link to /obavestenje/<id>-<slug>.html, "Detaljnije" link). They are not a
 * byte-for-byte copy of the site's HTML, so a green test shows the parser follows that structure, not
 * that the site's exact markup is unchanged. Re-save the real page and compare when the site changes.
 */
class AdvisoryParsingTest {
    private val cyrillic = """
        <nav><a href="https://cert.rs/obavestenja.html">Обавештења</a><a href="/preporuke.html">Препоруке</a></nav>
        <div class="item"><span class="date">14. Август 2026</span>
          <h3><a href="https://cert.rs/obavestenje/1586-Aktuelna-fi%C5%A1ing-kampanja.html">Актуелна фишинг кампања у којој се корисницима шаљу лажни имејлови</a></h3>
          <a href="https://cert.rs/obavestenje/1586-Aktuelna-fi%C5%A1ing-kampanja.html">Детаљније</a></div>
        <div class="item"><span class="date">22.&nbsp;Април&nbsp;2026</span>
          <h3><a href="/obavestenje/1545-Druga-kampanja.html">Актуелна фишинг кампања &amp; лажне поруке</a></h3>
          <a href="/obavestenje/1545-Druga-kampanja.html">Детаљније</a></div>
        <div class="item"><h3><a href="/obavestenje/1519-Bez-datuma.html">Obaveštenje bez datuma u listi</a></h3>
          <a href="/obavestenje/1519-Bez-datuma.html">Детаљније</a></div>
    """.trimIndent()

    private val numericDates = """
        <div><span class="date">14.08.2026.</span>
          <h3><a href="/obavestenje/1600-Numeric-date.html">Obaveštenje sa numeričkim datumom</a></h3></div>
    """.trimIndent()

    private val externalLink = """
        <div><span class="date">14.08.2026.</span>
          <h3><a href="https://evil.example/obavestenje/1601-Not-CERT.html">Spoljni link koji nije CERT objava</a></h3></div>
    """.trimIndent()

    private val latin = """
        <div><span>5. Decembar 2025</span><h3><a href="/rs/obavestenje/1481-Poziv.html">Poziv za mala i srednja preduzeća - besplatan vebinar</a></h3>
        <a href="/rs/obavestenje/1481-Poziv.html">Detaljnije</a></div>
        <div><span>26. Novembar 2025</span><h3><a href="/rs/obavestenje/1478-Prevare.html">Onlajn prevare tokom praznicnog perioda</a></h3></div>
    """.trimIndent()

    @Test fun parsesCyrillicListingWithDates() {
        val n = AdvisoryParsing.parseCertRsListing(cyrillic, "https://cert.rs/obavestenja.html")
        assertEquals(3, n.size)
        assertEquals("2026-08-14T00:00:00Z", AdvisoryParsing.toIso(n[0].dateMillis))
        assertEquals("2026-04-22T00:00:00Z", AdvisoryParsing.toIso(n[1].dateMillis))
        assertEquals("https://cert.rs/obavestenje/1545-Druga-kampanja.html", n[1].link)
        assertTrue(n[1].title.contains("& лажне поруке"))
    }

    @Test fun itemWithoutDateIsUndatedNotMisdated() {
        val n = AdvisoryParsing.parseCertRsListing(cyrillic, "https://cert.rs/obavestenja.html")
        assertEquals(0L, n[2].dateMillis)
    }

    @Test fun detaljnijeLinkDoesNotCreateDuplicates() {
        val n = AdvisoryParsing.parseCertRsListing(cyrillic, "https://cert.rs/obavestenja.html")
        assertEquals(n.map { it.link }.toSet().size, n.size)
        assertTrue(n.none { it.title == "Детаљније" })
    }

    @Test fun parsesLatinListingAndRelativeLinks() {
        val n = AdvisoryParsing.parseCertRsListing(latin, "https://www.cert.rs/rs/obavestenja.html")
        assertEquals(2, n.size)
        assertEquals("https://www.cert.rs/rs/obavestenje/1481-Poziv.html", n[0].link)
        assertEquals("2025-12-05T00:00:00Z", AdvisoryParsing.toIso(n[0].dateMillis))
        assertEquals("2025-11-26T00:00:00Z", AdvisoryParsing.toIso(n[1].dateMillis))
    }

    @Test fun parsesNumericDateFromListing() {
        val n = AdvisoryParsing.parseCertRsListing(numericDates, "https://cert.rs/obavestenja.html")
        assertEquals(1, n.size)
        assertEquals("2026-08-14T00:00:00Z", AdvisoryParsing.toIso(n.single().dateMillis))
    }

    @Test fun rejectsNoticeLinksOutsideOfficialCertHost() {
        assertTrue(AdvisoryParsing.parseCertRsListing(externalLink, "https://cert.rs/obavestenja.html").isEmpty())
    }

    @Test fun emptyOrUnrelatedPageGivesNoItems() {
        assertTrue(AdvisoryParsing.parseCertRsListing("<html><body>Maintenance</body></html>", "https://cert.rs/").isEmpty())
    }

    @Test fun parsesCommonFeedDates() {
        val expected = "2026-11-19T10:00:00Z"
        assertEquals(expected, AdvisoryParsing.toIso(AdvisoryParsing.parseDateMillis("Thu, 19 Nov 2026 10:00:00 +0000")!!))
        assertEquals(expected, AdvisoryParsing.toIso(AdvisoryParsing.parseDateMillis("2026-11-19T10:00:00Z")!!))
        assertEquals(expected, AdvisoryParsing.toIso(AdvisoryParsing.parseDateMillis("2026-11-19T12:00:00+02:00")!!))
        assertEquals(expected, AdvisoryParsing.toIso(AdvisoryParsing.parseDateMillis("2026-11-19T10:00:00.250Z")!!))
        assertEquals("2026-08-14T00:00:00Z", AdvisoryParsing.toIso(AdvisoryParsing.parseDateMillis("14.08.2026.")!!))
    }

    @Test fun garbageDatesAreNull() {
        assertNull(AdvisoryParsing.parseDateMillis(""))
        assertNull(AdvisoryParsing.parseDateMillis("soon"))
        assertNull(AdvisoryParsing.parseDateMillis("31. Foo 2026"))
        assertNull(AdvisoryParsing.parseDateMillis("2026-13-45"))
    }

    @Test fun sameNoticeThroughDifferentUrlsHasOneKey() {
        val a = AdvisoryParsing.urlKey("https://www.cert.rs/rs/obavestenje/1586-X.html?utm_source=a#top")
        val b = AdvisoryParsing.urlKey("https://cert.rs/obavestenje/1586-x.html")
        assertEquals(a, b)
    }

    @Test fun differentNoticesWithSameTitleStaySeparate() {
        assertNotEquals(
            AdvisoryParsing.urlKey("https://cert.rs/obavestenje/1-a.html"),
            AdvisoryParsing.urlKey("https://cert.rs/obavestenje/2-a.html")
        )
    }

    private data class E(val link: String, val title: String, val source: String, val rank: Int, val ts: Long)

    private fun order(list: List<E>) = AdvisoryParsing.dedupeAndSort(list, { it.link }, { it.title }, { it.source }, { it.rank }, { it.ts })

    @Test fun sortIsNumericNewestFirstAndUndatedLast() {
        val out = order(listOf(
            E("https://a/1", "old", "x", 0, 1_000L),
            E("https://a/2", "undated", "x", 0, 0L),
            E("https://a/3", "new", "x", 0, 5_000_000_000L),
            E("https://a/4", "admin", "x", 2, 9_000_000_000L)
        ))
        assertEquals(listOf("new", "old", "undated", "admin"), out.map { it.title })
    }

    @Test fun dedupeUsesUrlNotTitleAndKeepsNewestCopy() {
        val out = order(listOf(
            E("https://cert.rs/obavestenje/1-a.html", "Same title", "rs", 0, 100L),
            E("https://cert.rs/obavestenje/2-b.html", "Same title", "rs", 0, 200L),
            E("https://www.cert.rs/rs/obavestenje/1-a.html", "Same title", "rs", 0, 300L)
        ))
        assertEquals(2, out.size)
        assertEquals(300L, out.first { it.link.contains("1-a") }.ts)
    }
}
