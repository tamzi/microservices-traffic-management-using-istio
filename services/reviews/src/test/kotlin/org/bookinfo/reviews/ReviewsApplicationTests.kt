package org.bookinfo.reviews

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@Tag("integration")
@SpringBootTest
class ReviewsApplicationTests {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Test
    fun contextLoads() {
        assertNotNull(applicationContext)
    }
}
