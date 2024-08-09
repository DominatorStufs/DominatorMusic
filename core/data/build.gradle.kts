plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.vitune.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    implementation(libs.core.ktx)

    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}
