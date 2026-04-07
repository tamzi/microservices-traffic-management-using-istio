package org.bookinfo.mobilebff.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MobileBffOpenApiConfiguration {

    @Bean
    fun mobileBffOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("BookInfo Mobile BFF API")
                .version("1.0.0")
                .description("Public HTTP API for native mobile applications."),
        )
}
