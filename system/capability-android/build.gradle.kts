plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// Android bindings for the capability contract.
//
// Split from :system:capability on purpose: the core decision logic must stay
// JVM-testable with no Android dependency, while the probes that touch EGL,
// PackageManager and PowerManager necessarily need the Android SDK.
android {
    namespace = "dev.pocketpc.system.capability.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":system:capability"))
}
