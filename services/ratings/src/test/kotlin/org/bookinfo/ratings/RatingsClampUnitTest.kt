package org.bookinfo.ratings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.min

class RatingsClampUnitTest {

    @Test
    fun `clamp keeps rating in range`() {
        fun clamp(r: Int) = min(5, r.coerceAtLeast(1))
        assertEquals(1, clamp(0))
        assertEquals(5, clamp(99))
        assertEquals(3, clamp(3))
    }
}
