@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    versionCatalogs {
        create("libs") {
            from(files("./gradle/libs.toml"))
        }
    }
}

pluginManagement {
    resolutionStrategy {
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

rootProject.name = "ViMusic"

include(":app")
include(":core:data")
include(":core:ui")
include(":compose-routing")
include(":compose-reordering")
include(":compose-persist")
include(":ktor-client-brotli")
include(":providers:common")
include(":providers:innertube")
include(":providers:kugou")
include(":providers:lrclib")
include(":providers:piped")
