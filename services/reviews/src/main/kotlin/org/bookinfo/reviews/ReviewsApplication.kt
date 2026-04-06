package org.bookinfo.reviews

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReviewsApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<ReviewsApplication>(*args)
}
