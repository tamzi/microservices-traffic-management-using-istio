package org.bookinfo.mobilebff

import org.bookinfo.mobilebff.config.MobileBffConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [MobileBffConfiguration::class])
class MobileBffApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<MobileBffApplication>(*args)
}
