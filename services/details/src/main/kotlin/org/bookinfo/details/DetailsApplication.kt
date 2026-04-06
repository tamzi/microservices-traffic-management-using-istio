package org.bookinfo.details

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DetailsApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<DetailsApplication>(*args)
}
