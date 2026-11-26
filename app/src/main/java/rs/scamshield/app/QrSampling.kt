package rs.scamshield.app

/**
 * Pure helpers shared by the camera and gallery QR paths. Nothing in this file touches Android
 * classes, so it is covered by plain JVM unit tests (app/src/test).
 */
object QrSampling {
    /** A QR code holds at most about 3 KB, so anything longer is not a genuine payload. */
    const val MAX_PAYLOAD_CHARS = 4096

    /** Upper bound on distinct codes returned from one image or frame. */
    const val MAX_PAYLOADS = 10

    /**
     * Smallest power-of-two sample size that brings the longer image side to [maxSide] or less.
     * Returns 1 when the image already fits or when a dimension is unknown.
     */
    fun sampleSize(longSide: Int, maxSide: Int): Int {
        if (longSide <= 0 || maxSide <= 0) return 1
        var sample = 1
        while (longSide / sample > maxSide) {
            sample *= 2
            if (sample >= 1 shl 20) break
        }
        return sample
    }

    /**
     * Drops null, blank and oversized values, removes duplicates while keeping order, and caps
     * the list. The raw text is otherwise returned unchanged: no URL is opened or rewritten.
     */
    fun cleanPayloads(raw: List<String?>): List<String> {
        val seen = LinkedHashSet<String>()
        for (value in raw) {
            if (value.isNullOrBlank()) continue
            if (value.length > MAX_PAYLOAD_CHARS) continue
            seen.add(value)
            if (seen.size >= MAX_PAYLOADS) break
        }
        return seen.toList()
    }
}
