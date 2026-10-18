import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

internal val Project.libs: VersionCatalog get() =
    project.extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }

    // Keep published artifacts consumable by projects on older Kotlin versions.
    coreLibrariesVersion = libs.findVersion("kotlinConsumerStdlib").get().requiredVersion
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    val consumerApi = KotlinVersion.fromVersion(libs.findVersion("kotlinConsumerApi").get().requiredVersion)
    compilerOptions {
        languageVersion = consumerApi
        apiVersion = consumerApi
        // The old language version is deliberate, see kotlinConsumerApi in libs.versions.toml.
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}
