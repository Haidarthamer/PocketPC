plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin: no Android dependencies, so capability *logic* is unit-testable
// on a plain JVM. Hardware probing lives in :app behind CapabilityProbe.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
