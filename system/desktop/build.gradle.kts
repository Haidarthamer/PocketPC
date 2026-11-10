plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Desktop shell model: window manager, app registry, taskbar/start-menu state.
// Android-free by design so the interaction rules are unit-testable on a plain JVM.
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
    api(project(":system:capability"))
    testImplementation(libs.junit)
}
