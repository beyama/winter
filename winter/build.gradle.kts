plugins {
    id("kotlin-multiplatform-library-configuration")
    id("mvn-push-configuration")
}

kotlin {
    sourceSets {
        nativeMain.dependencies {
            implementation(libs.atomicfu)
        }
    }
}
