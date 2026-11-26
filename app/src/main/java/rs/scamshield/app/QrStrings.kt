package rs.scamshield.app

/**
 * Text for the native QR screens. The language switch lives in the web page, so the page sends
 * its choice ("en", "sr-lat" or "sr-cyr") through ScamShieldNative.setLanguage() before it opens
 * a native screen. Serbian Cyrillic is produced by transliterating the Latin text.
 */
object QrStrings {
    const val LANG_EN = "en"
    const val LANG_SR_LAT = "sr-lat"
    const val LANG_SR_CYR = "sr-cyr"

    enum class Msg(val sr: String, val en: String) {
        POINT_CAMERA(
            "Usmerite kameru ka QR kodu. Kod se čita na ovom telefonu.",
            "Point the camera at a QR code. It is read on this device."
        ),
        STARTING("Pokrećem kameru…", "Starting camera…"),
        HOLD_STEADY(
            "Cela QR slika treba da bude u kadru. Dodirnite ekran za fokus.",
            "Keep the whole QR code in view. Tap the screen to focus."
        ),
        SLOW_HINT(
            "Ne uspeva? Približite se, dodajte svetla, izbegnite odsjaj ili izaberite sliku iz galerije.",
            "Not working? Move closer, add light, avoid glare, or choose an image from the gallery."
        ),
        FOUND("QR kod je pronađen.", "QR code found."),
        FLASH("Lampa", "Flashlight"),
        FLASH_ON("Lampa: uključena", "Flashlight: on"),
        FLASH_OFF("Lampa: isključena", "Flashlight: off"),
        NO_FLASH("Nema lampe", "No flashlight"),
        CANCEL("Otkaži", "Cancel"),
        RETRY("Pokušaj ponovo", "Try again"),

        ERR_CAMERA_DENIED(
            "Dozvola za kameru nije data. Uključite je u podešavanjima aplikacije ili izaberite QR sliku iz galerije.",
            "Camera permission was not granted. Turn it on in the app settings or choose a QR image from the gallery."
        ),
        ERR_NO_CAMERA(
            "Ovaj uređaj nema dostupnu kameru. Izaberite QR sliku iz galerije.",
            "No camera is available on this device. Choose a QR image from the gallery."
        ),
        ERR_CAMERA_START(
            "Kamera nije mogla da se pokrene. Zatvorite druge aplikacije koje koriste kameru i pokušajte ponovo.",
            "The camera could not start. Close other apps that use the camera and try again."
        ),
        ERR_EMPTY(
            "Skener je vratio prazan rezultat. Pokušajte ponovo.",
            "The scanner returned an empty result. Try again."
        ),
        ERR_SCANNER_OPEN(
            "Nije moguće otvoriti skener kamere. Izaberite sliku iz galerije.",
            "Could not open the camera scanner. Choose an image from the gallery."
        ),
        ERR_PICKER_OPEN(
            "Nije moguće otvoriti izbor slike.",
            "Could not open the image picker."
        ),
        ERR_NO_QR(
            "U ovoj slici nije pronađen QR kod. Probajte oštriju sliku na kojoj se vidi ceo kod.",
            "No QR code was found in this image. Try a sharper image that shows the whole code."
        ),
        ERR_UNREADABLE(
            "Slika se ne može otvoriti. Možda je oštećena ili je u formatu koji ovaj telefon ne podržava. Probajte JPEG ili PNG.",
            "The image could not be opened. It may be damaged or in a format this phone does not support. Try JPEG or PNG."
        ),
        ERR_TOO_LARGE(
            "Slika je prevelika za obradu na ovom telefonu. Probajte manju sliku ili snimak ekrana.",
            "The image is too large to process on this phone. Try a smaller image or a screenshot."
        ),
        ERR_BLOCKED_PAGE(
            "Ova radnja je dozvoljena samo iz ScamShield ekrana.",
            "This action is only allowed from the ScamShield screen."
        ),

        OPEN_TITLE("Otvoriti ovaj link?", "Open this link?"),
        OPEN_BODY(
            "Link će se otvoriti u pregledaču. ScamShield nije potvrdio da je bezbedan.\n\n{0}",
            "The link will open in your browser. ScamShield has not confirmed that it is safe.\n\n{0}"
        ),
        OPEN_YES("Otvori", "Open"),
        OPEN_NO("Ne otvaraj", "Do not open"),
        OPEN_FAILED(
            "Nijedna aplikacija ne može da otvori ovaj link.",
            "No app can open this link."
        )
    }

    /** Maps the error codes used on the wire to their message. */
    fun errorMsg(code: String): Msg? = when (code) {
        "camera_denied" -> Msg.ERR_CAMERA_DENIED
        "no_camera" -> Msg.ERR_NO_CAMERA
        "camera_start" -> Msg.ERR_CAMERA_START
        "empty" -> Msg.ERR_EMPTY
        "scanner_open" -> Msg.ERR_SCANNER_OPEN
        "picker_open" -> Msg.ERR_PICKER_OPEN
        "no_qr" -> Msg.ERR_NO_QR
        "unreadable" -> Msg.ERR_UNREADABLE
        "too_large" -> Msg.ERR_TOO_LARGE
        "blocked_page" -> Msg.ERR_BLOCKED_PAGE
        else -> null
    }

    fun normalizeLang(raw: String?): String = when (raw?.trim()?.lowercase()) {
        LANG_EN -> LANG_EN
        LANG_SR_CYR -> LANG_SR_CYR
        else -> LANG_SR_LAT
    }

    /** Placeholders are written as {0}, {1}... and are filled after transliteration. */
    fun text(msg: Msg, lang: String, vararg args: String): String {
        val normalized = normalizeLang(lang)
        var out = when (normalized) {
            LANG_EN -> msg.en
            LANG_SR_CYR -> toCyrillic(msg.sr)
            else -> msg.sr
        }
        args.forEachIndexed { index, value -> out = out.replace("{$index}", value) }
        return out
    }

    private val protectedWords = Regex("(ScamShield|JPEG|PNG|HEIC|QR|Wi-Fi|\\{\\d+\\})")

    /** Serbian Latin to Cyrillic. Brand names, file formats and placeholders are left alone. */
    fun toCyrillic(latin: String): String {
        val out = StringBuilder()
        var last = 0
        for (match in protectedWords.findAll(latin)) {
            out.append(transliterate(latin.substring(last, match.range.first)))
            out.append(match.value)
            last = match.range.last + 1
        }
        out.append(transliterate(latin.substring(last)))
        return out.toString()
    }

    private val single = mapOf(
        'a' to "а", 'b' to "б", 'c' to "ц", 'č' to "ч", 'ć' to "ћ", 'd' to "д", 'đ' to "ђ",
        'e' to "е", 'f' to "ф", 'g' to "г", 'h' to "х", 'i' to "и", 'j' to "ј", 'k' to "к",
        'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п", 'r' to "р", 's' to "с",
        'š' to "ш", 't' to "т", 'u' to "у", 'v' to "в", 'z' to "з", 'ž' to "ж"
    )

    private fun transliterate(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val next = if (i + 1 < text.length) text[i + 1] else '\u0000'
            val lower = ch.lowercaseChar()
            val nextLower = next.lowercaseChar()
            val digraph = when {
                lower == 'l' && nextLower == 'j' -> "љ"
                lower == 'n' && nextLower == 'j' -> "њ"
                lower == 'd' && nextLower == 'ž' -> "џ"
                else -> null
            }
            if (digraph != null) {
                out.append(if (ch.isUpperCase()) digraph.uppercase() else digraph)
                i += 2
                continue
            }
            val mapped = single[lower]
            if (mapped == null) {
                out.append(ch)
            } else {
                out.append(if (ch.isUpperCase()) mapped.uppercase() else mapped)
            }
            i++
        }
        return out.toString()
    }
}
