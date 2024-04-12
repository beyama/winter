plugins {
    id("android-library-configuration")
}

android {
    namespace = "io.jentz.winter.androidx.lifecycle"
}

dependencies {
    implementation(project(":winter"))

    implementation(libs.androidx.activity)

    testImplementation(libs.junit)
    testImplementation(libs.kotlintest.assertions)
    testImplementation(libs.kotlin.reflect)
}