package rs.scamshield.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrStringsTest {
    @Test fun languageNormalisation() {
        assertEquals(QrStrings.LANG_SR_LAT, QrStrings.normalizeLang(null))
        assertEquals(QrStrings.LANG_SR_LAT, QrStrings.normalizeLang("de"))
        assertEquals(QrStrings.LANG_EN, QrStrings.normalizeLang(" EN "))
        assertEquals(QrStrings.LANG_SR_CYR, QrStrings.normalizeLang("sr-cyr"))
    }

    @Test fun transliterationDigraphsAndCase() {
        assertEquals("Покушај поново", QrStrings.toCyrillic("Pokušaj ponovo"))
        assertEquals("Подешавања", QrStrings.toCyrillic("Podešavanja"))
        assertEquals("Љубљана", QrStrings.toCyrillic("Ljubljana"))
        assertEquals("Њива", QrStrings.toCyrillic("Njiva"))
        assertEquals("Џем", QrStrings.toCyrillic("Džem"))
    }

    @Test fun brandNamesAndFormatsAreNotTransliterated() {
        assertEquals("ScamShield QR JPEG PNG", QrStrings.toCyrillic("ScamShield QR JPEG PNG"))
    }

    @Test fun placeholdersAreFilledAfterTransliteration() {
        val out = QrStrings.text(QrStrings.Msg.OPEN_BODY, QrStrings.LANG_SR_CYR, "https://example.com/a")
        assertTrue(out.contains("https://example.com/a"))
        assertFalse(out.contains("{0}"))
    }

    @Test fun everyMessageExistsInEveryLanguage() {
        val latinLeft = Regex("[A-Za-zčćžšđČĆŽŠĐ]")
        val protectedWords = Regex("(ScamShield|JPEG|PNG|HEIC|QR|Wi-Fi|\\{\\d+\\})")
        for (msg in QrStrings.Msg.values()) {
            assertTrue(msg.name, msg.sr.isNotBlank() && msg.en.isNotBlank())
            val cyr = QrStrings.text(msg, QrStrings.LANG_SR_CYR)
            assertFalse(msg.name + " still has Latin letters: " + cyr, latinLeft.containsMatchIn(protectedWords.replace(cyr, "")))
        }
    }

    @Test fun everyWireErrorCodeHasText() {
        for (code in listOf("camera_denied", "no_camera", "camera_start", "empty", "scanner_open",
            "picker_open", "no_qr", "unreadable", "too_large", "blocked_page")) {
            assertNotNull(code, QrStrings.errorMsg(code))
        }
        assertEquals(null, QrStrings.errorMsg("cancelled"))
    }
}
