plugins {
    id("kotlin-library-configuration")
    id("junit5-configuration")
}

dependencies {
    api(libs.javax.inject)
    id("mvn-push-configuration")
}