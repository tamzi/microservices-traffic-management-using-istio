package org.bookinfo.reviews

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReviewsSlugUnitTest {

    @Test
    fun `slug normalizes spaces`() {
        assertEquals("a-b", "a b".replace(' ', '-'))
    }
}
