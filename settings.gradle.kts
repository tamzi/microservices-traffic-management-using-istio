rootProject.name = "bookinfo"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    "services:productpage",
    "services:web-bff",
    "services:mobile-bff",
    "services:details",
    "services:reviews",
    "services:ratings",
)
