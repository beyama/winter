plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories{
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies{
    compileOnly(libs.kotlinJvmLib)

    implementation(gradleApi())
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}