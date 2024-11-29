import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java-library")
}

internal val Project.libs: VersionCatalog get() =
    project.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findLibrary("junit.jupiter.api").get())
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    testRuntimeOnly(libs.findLibrary("junit.vintage.engine").get())
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.test {
    dependsOn("cleanTest")

    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_ERROR,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT
        )

        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor?) = Unit
        override fun beforeTest(testDescriptor: TestDescriptor?) = Unit
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) = Unit
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println("\nTest result: ${result.resultType}")
                println("Test summary: ${result.testCount} tests, " +
                        "${result.successfulTestCount} succeeded, " +
                        "${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped")
            }
        }
    })
}