plugins {
    id("kotlin-library-configuration")
    id("junit5-configuration")
}

dependencies {
    api(project(":winter"))

    implementation(libs.kotlin.reflect)
}
