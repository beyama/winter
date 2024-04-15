plugins {
    id("android-library-configuration")
    id("mvn-push-configuration")
}

android {
    namespace = "io.jentz.winter.androidx.lifecycle"
}

dependencies {
    implementation(project(":winter"))
    implementation(libs.androidx.activity)

    androidTestImplementation(project(":winter-junit4"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.assertk)
    androidTestImplementation(libs.androidx.espresso.core)
}