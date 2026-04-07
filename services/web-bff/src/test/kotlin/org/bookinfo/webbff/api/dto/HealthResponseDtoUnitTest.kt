package org.bookinfo.webbff.api.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HealthResponseDtoUnitTest {

    @Test
    fun `should expose UP status when health payload is built for web-bff`() {
        val dto = HealthResponseDto(status = "UP", service = "web-bff")
        assertThat(dto.status).isEqualTo("UP")
        assertThat(dto.service).isEqualTo("web-bff")
    }
}
