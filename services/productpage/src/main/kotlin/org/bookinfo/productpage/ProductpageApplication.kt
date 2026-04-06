package org.bookinfo.productpage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProductpageApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<ProductpageApplication>(*args)
}
