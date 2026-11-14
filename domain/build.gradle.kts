import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    maxHeapSize = "2g"
    testLogging {
        events("failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// `./gradlew testDebugUnitTest` (perintah di Definition of Done) juga menjalankan test domain.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias untuk :domain:test agar ikut terpanggil oleh testDebugUnitTest."
    dependsOn(tasks.test)
}

// Benchmark solver lengkap (hasilnya dicatat di README): ./gradlew :domain:benchmark
tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Menjalankan benchmark solver 3x3, 4x4, 5x5."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("id.rosdie.puzzle.domain.SolverBenchmarkKt")
    maxHeapSize = "2g"
}
