plugins {
    base
    alias(libs.plugins.detekt)
}

group = "org.bookinfo"
version = "1.0.0-SNAPSHOT"

detekt {
    buildUponDefaultConfig = true
    parallel = true
    source.setFrom(
        files(
            "services/productpage/src",
            "services/details/src",
            "services/reviews/src",
            "services/ratings/src",
        ),
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}

tasks.named("check") {
    dependsOn(tasks.detekt)
}

subprojects {
    apply(plugin = "jacoco")

    tasks.withType<Test>().configureEach {
        finalizedBy(tasks.named("jacocoTestReport"))
    }
}
