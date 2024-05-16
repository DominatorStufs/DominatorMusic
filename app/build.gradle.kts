plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    val appId = "${project.group}.android"

    namespace = appId
    compileSdk = 34

    defaultConfig {
        applicationId = appId

        minSdk = 21
        targetSdk = 34

        versionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 5
        versionName = project.version.toString()

        multiDexEnabled = true
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("ci") {
            storeFile = System.getenv("ANDROID_NIGHTLY_KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_NIGHTLY_KEYSTORE_ALIAS")
            keyPassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            manifestPlaceholders["appName"] = "ViTune Debug"
        }

        release {
            versionNameSuffix = "-RELEASE"
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "ViTune"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("nightly") {
            initWith(getByName("release"))
            matchingFallbacks += "release"

            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-NIGHTLY"
            manifestPlaceholders["appName"] = "ViTune Nightly"
            signingConfig = signingConfigs.findByName("ci")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        freeCompilerArgs += listOf("-Xcontext-receivers")
    }

    packaging {
        resources.excludes.add("META-INF/**/*")
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.fonts)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)
    implementation(libs.compose.lottie)
    implementation(libs.compose.material3)

    implementation(libs.palette)
    implementation(libs.monet)

    implementation(libs.exoplayer)
    implementation(libs.exoplayer.workmanager)
    implementation(libs.workmanager)
    implementation(libs.workmanager.ktx)

    implementation(libs.core.ktx)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.datetime)
    api(libs.kotlin.datetime)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)

    implementation(libs.ktor.http)
    implementation(libs.ktor.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)

    implementation(libs.ktor.client.okhttp)
    api(libs.ktor.http)

    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}
