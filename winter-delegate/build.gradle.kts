plugins {
    id("kotlin-multiplatform-library-configuration")
    id("mvn-push-configuration")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The public API uses types of the core module.
            api(project(":winter"))
        }
    }
}
