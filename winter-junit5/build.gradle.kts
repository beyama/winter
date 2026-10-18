plugins {
    id("kotlin-library-configuration")
    id("junit5-configuration")
    id("mvn-push-configuration")
}

dependencies {
    api(project(":winter"))

    api(libs.junit.jupiter.api)

    testImplementation(libs.assertk)
}