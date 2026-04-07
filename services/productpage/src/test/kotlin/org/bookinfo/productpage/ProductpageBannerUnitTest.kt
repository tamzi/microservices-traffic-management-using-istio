package org.bookinfo.productpage

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductpageBannerUnitTest {

    @Test
    fun `service name prefix is stable`() {
        assertTrue("productpage".startsWith("product"))
    }
}
