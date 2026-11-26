package rs.scamshield.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrSamplingTest {
    @Test fun sampleSizeKeepsSmallImages() {
        assertEquals(1, QrSampling.sampleSize(1000, 2048))
        assertEquals(1, QrSampling.sampleSize(2048, 2048))
    }

    @Test fun sampleSizeIsPowerOfTwoAndFits() {
        assertEquals(2, QrSampling.sampleSize(2049, 2048))
        assertEquals(2, QrSampling.sampleSize(4000, 2048))
        assertEquals(4, QrSampling.sampleSize(8000, 2048))
        for (side in listOf(3000, 6000, 12000, 20000)) {
            val s = QrSampling.sampleSize(side, 2048)
            assertTrue(s and (s - 1) == 0)
            assertTrue(side / s <= 2048)
        }
    }

    @Test fun sampleSizeHandlesUnknownDimensions() {
        assertEquals(1, QrSampling.sampleSize(0, 2048))
        assertEquals(1, QrSampling.sampleSize(-5, 2048))
        assertEquals(1, QrSampling.sampleSize(5000, 0))
    }

    @Test fun cleanPayloadsDropsBlankNullAndDuplicates() {
        assertEquals(listOf("a", "b"), QrSampling.cleanPayloads(listOf(null, "", "  ", "a", "a", "b")))
    }

    @Test fun cleanPayloadsKeepsTextUnchanged() {
        val raw = "  https://example.com/?x=1  "
        assertEquals(listOf(raw), QrSampling.cleanPayloads(listOf(raw)))
    }

    @Test fun cleanPayloadsRejectsOversizedAndCapsCount() {
        val huge = "x".repeat(QrSampling.MAX_PAYLOAD_CHARS + 1)
        assertTrue(QrSampling.cleanPayloads(listOf(huge)).isEmpty())
        val many = (1..50).map { "code$it" }
        assertEquals(QrSampling.MAX_PAYLOADS, QrSampling.cleanPayloads(many).size)
    }
}
