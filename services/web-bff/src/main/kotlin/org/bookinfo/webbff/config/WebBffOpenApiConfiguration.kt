package org.bookinfo.webbff.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class WebBffOpenApiConfiguration {

    @Bean
    fun webBffOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("BookInfo Web BFF API")
                .version("1.0.0")
                .description("Public HTTP API for the BookInfo web dashboard and browser clients."),
        )
}
