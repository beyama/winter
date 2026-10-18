import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// AGP 9 compiles Kotlin itself (built-in Kotlin), so the Kotlin Android plugin isn't applied.
plugins {
    id("com.android.library")
}

internal val Project.libs: VersionCatalog get() =
    project.extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }

    // Keep published artifacts consumable by projects on older Kotlin versions.
    coreLibrariesVersion = libs.findVersion("kotlinConsumerStdlib").get().requiredVersion
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Test")) return@configureEach
    val consumerApi = KotlinVersion.fromVersion(libs.findVersion("kotlinConsumerApi").get().requiredVersion)
    compilerOptions {
        languageVersion = consumerApi
        apiVersion = consumerApi
        // The old language version is deliberate, see kotlinConsumerApi in libs.versions.toml.
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}
