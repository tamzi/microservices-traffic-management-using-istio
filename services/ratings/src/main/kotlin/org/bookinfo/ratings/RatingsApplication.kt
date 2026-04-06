package org.bookinfo.ratings

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RatingsApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<RatingsApplication>(*args)
}
