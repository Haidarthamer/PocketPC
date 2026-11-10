plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// GPU verification engine. Lives in an Android library because it needs EGL/GLES20;
// the *verdict* logic (GpuVerifier / SoftwareRendererDetector) is pure Kotlin and is
// unit tested on the JVM without a GPU.
android {
    namespace = "dev.pocketpc.graphics"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        // The engine executes GLES20 calls; declare it so the APK advertises the need
        // at install time without making it mandatory for the rest of the app.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":system:capability"))
    testImplementation(libs.junit)
}
