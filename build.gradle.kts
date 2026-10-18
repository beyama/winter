// Plugins and shared configuration live in the precompiled script plugins of build-configuration.
// Loading them once in the root project makes all subprojects share the same Kotlin/Android
// Gradle plugin classes.
plugins {
    id("kotlin-library-configuration") apply false
    id("android-library-configuration") apply false
}
