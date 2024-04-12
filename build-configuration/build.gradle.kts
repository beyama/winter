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
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(gradleApi())
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)

}