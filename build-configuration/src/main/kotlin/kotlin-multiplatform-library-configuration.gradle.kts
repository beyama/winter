import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// Kotlin Multiplatform library.
//
// The JVM target is used by JVM and Android consumers, there is no separate Android target since
// the libraries don't contain any Android specific code.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

internal val Project.libs: VersionCatalog get() =
    project.extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(17)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64()

    js {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        // Graph locks are expect classes, see Locks.kt.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Keep published artifacts consumable by projects on older Kotlin versions.
    coreLibrariesVersion = libs.findVersion("kotlinConsumerStdlib").get().requiredVersion

    sourceSets {
        commonTest.dependencies {
            implementation(libs.findLibrary("kotlin.test").get())
            implementation(libs.findLibrary("kotest.assertions").get())
            implementation(libs.findLibrary("assertk").get())
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("junit.bom").get()))
            implementation(libs.findLibrary("kotlin.reflect").get())
            implementation(libs.findLibrary("mockito.core").get())
            implementation(libs.findLibrary("mockito.kotlin").get())
            runtimeOnly(libs.findLibrary("junit.platform.launcher").get())
        }
    }
}

// Node.js is downloaded from the repository declared in settings.gradle.kts, the build doesn't
// allow project repositories.
plugins.withType<NodeJsPlugin> {
    the<NodeJsEnvSpec>().downloadBaseUrl = null
}
plugins.withType<WasmNodeJsPlugin> {
    the<WasmNodeJsEnvSpec>().downloadBaseUrl = null
}

// Kotlin/JS and Kotlin/Wasm only accept the standard library of the compiler. The published
// dependency stays kotlinConsumerStdlib, consumers resolve their own (newer) version anyway.
configurations.matching { it.name.startsWith("js") || it.name.startsWith("wasm") || it.name.startsWith("web") }
    .configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && (requested.name.startsWith("kotlin-stdlib") || requested.name == "kotlin-dom-api-compat")) {
                useVersion(libs.findVersion("kotlin").get().requiredVersion)
            }
        }
    }

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name.contains("Test")) return@configureEach
    val consumerApi = KotlinVersion.fromVersion(libs.findVersion("kotlinConsumerApi").get().requiredVersion)
    compilerOptions {
        languageVersion = consumerApi
        apiVersion = consumerApi
        // The old language version is deliberate, see kotlinConsumerApi in libs.versions.toml.
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

tasks.withType<AbstractTestTask>().configureEach {
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_ERROR,
            TestLogEvent.SKIPPED
        )

        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
