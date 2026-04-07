package org.bookinfo.mobilebff.api.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HealthResponseDtoUnitTest {

    @Test
    fun `should expose UP status when health payload is built for mobile-bff`() {
        val dto = HealthResponseDto(status = "UP", service = "mobile-bff")
        assertThat(dto.status).isEqualTo("UP")
        assertThat(dto.service).isEqualTo("mobile-bff")
    }
}
