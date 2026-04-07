package org.bookinfo.webbff

import org.bookinfo.webbff.config.WebBffConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [WebBffConfiguration::class])
class WebBffApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<WebBffApplication>(*args)
}
