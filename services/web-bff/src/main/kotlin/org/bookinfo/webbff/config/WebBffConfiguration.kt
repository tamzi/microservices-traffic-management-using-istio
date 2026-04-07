package org.bookinfo.webbff.config

import org.bookinfo.webbff.api.WebBffApiController
import org.bookinfo.webbff.exception.WebBffGlobalExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class WebBffConfiguration {

    @Bean
    fun webBffApiController(): WebBffApiController = WebBffApiController()

    @Bean
    fun webBffGlobalExceptionHandler(): WebBffGlobalExceptionHandler = WebBffGlobalExceptionHandler()
}
