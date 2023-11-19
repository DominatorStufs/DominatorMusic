plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(libs.kotlin.coroutines)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}