plugins {
    id("android-library-configuration")
    id("mvn-push-configuration")
}

android {
    namespace = "io.jentz.winter.androidx"
}

dependencies {
    api(project(":winter"))
    api(project(":winter-delegate"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.process)

    androidTestImplementation(project(":winter-junit4"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.assertk)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.lifecycle.runtime.testing)
}