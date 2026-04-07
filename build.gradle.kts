import org.gradle.api.tasks.testing.Test

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
            "services/web-bff/src",
            "services/mobile-bff/src",
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

    afterEvaluate {
        val hasSpringBoot = project.plugins.hasPlugin("org.springframework.boot")
        if (hasSpringBoot) {
            tasks.named<Test>("test") {
                useJUnitPlatform {
                    excludeTags("integration")
                }
                finalizedBy(tasks.named("jacocoTestReport"))
            }

            tasks.register<Test>("integrationTest") {
                group = "verification"
                description =
                    "Runs tests tagged with \"integration\" (slower; may require Docker for Testcontainers)"
                val unitTest = tasks.named<Test>("test")
                testClassesDirs = unitTest.get().testClassesDirs
                classpath = unitTest.get().classpath
                useJUnitPlatform {
                    includeTags("integration")
                }
                filter {
                    isFailOnNoMatchingTests = false
                }
                shouldRunAfter(unitTest)
            }

            tasks.named("check") {
                dependsOn(tasks.named("integrationTest"))
            }
        } else {
            tasks.withType<Test>().configureEach {
                finalizedBy(tasks.named("jacocoTestReport"))
            }
        }
    }
}
