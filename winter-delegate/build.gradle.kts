plugins {
    id("kotlin-library-configuration")
    id("junit5-configuration")
    id("mvn-push-configuration")
}

dependencies {
    implementation(project(":winter"))
}