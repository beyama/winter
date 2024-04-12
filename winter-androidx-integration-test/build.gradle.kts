plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.jentz.winter.androidx.integration.test"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":winter"))
    implementation(project(":winter-androidx"))
    implementation(project(":winter-androidx-fragment"))

    implementation(libs.androidx.fragment)

    androidTestImplementation(project(":winter-junit4"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlintest.assertions)
    androidTestImplementation(libs.androidx.espresso.core)
}
