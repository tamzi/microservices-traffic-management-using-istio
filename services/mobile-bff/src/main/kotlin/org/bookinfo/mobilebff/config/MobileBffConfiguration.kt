package org.bookinfo.mobilebff.config

import org.bookinfo.mobilebff.api.MobileBffApiController
import org.bookinfo.mobilebff.exception.MobileBffGlobalExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MobileBffConfiguration {

    @Bean
    fun mobileBffApiController(): MobileBffApiController = MobileBffApiController()

    @Bean
    fun mobileBffGlobalExceptionHandler(): MobileBffGlobalExceptionHandler =
        MobileBffGlobalExceptionHandler()
}
