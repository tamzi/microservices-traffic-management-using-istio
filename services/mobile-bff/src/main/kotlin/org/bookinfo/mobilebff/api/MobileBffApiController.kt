package org.bookinfo.mobilebff.api

import org.bookinfo.mobilebff.api.dto.HealthResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/mobile")
@Tag(name = "Mobile BFF", description = "Aggregated operations for mobile clients")
class MobileBffApiController {

    @GetMapping("/health", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Health check",
        description = "Returns service availability for mobile clients and gateways.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is up",
        content = [Content(schema = Schema(implementation = HealthResponseDto::class))],
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    fun health(): ResponseEntity<HealthResponseDto> =
        ResponseEntity.ok(HealthResponseDto(status = "UP", service = "mobile-bff"))
}
