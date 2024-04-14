plugins {
    id("kotlin-library-configuration")
    id("junit5-configuration")
    id("mvn-push-configuration")
}

dependencies {
    api(project(":winter"))

    implementation(libs.kotlin.reflect)
}
