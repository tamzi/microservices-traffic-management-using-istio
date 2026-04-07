package org.bookinfo.details

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

interface TickingClock {
    fun millis(): Long
}

class DetailsClockMockkUnitTest {

    @Test
    fun `mockk can stub simple collaborators`() {
        val clock = mockk<TickingClock>()
        every { clock.millis() } returns 42L
        assertEquals(42L, clock.millis())
    }
}
