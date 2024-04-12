plugins {
    id("android-library-configuration")
}

android {
    namespace = "io.jentz.winter.androidx.fragment"
}

dependencies {
    implementation(project(":winter"))
    implementation(project(":winter-androidx"))

    implementation(libs.androidx.fragment)

    testImplementation(libs.junit)
    testImplementation(libs.kotlintest.assertions)
    testImplementation(libs.kotlin.reflect)
}