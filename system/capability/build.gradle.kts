plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin: no Android dependencies, so capability *logic* is unit-testable
// on a plain JVM. Hardware probing lives in :app behind AndroidHardwareSource.
// Target 17 to match the Android module so the two can share types.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
