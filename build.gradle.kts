import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

// Plugins and shared configuration live in the precompiled script plugins of build-configuration.
// Loading them once in the root project makes all subprojects share the same Kotlin/Android
// Gradle plugin classes.
plugins {
    id("kotlin-library-configuration") apply false
    id("kotlin-multiplatform-library-configuration") apply false
    id("android-library-configuration") apply false
    id("api-docs-configuration")
}

dependencies {
    dokka(project(":winter"))
    dokka(project(":winter-delegate"))
    dokka(project(":winter-androidx"))
    dokka(project(":winter-junit4"))
    dokka(project(":winter-junit5"))
}

// Node.js and Yarn are downloaded from the repositories declared in settings.gradle.kts, the build
// doesn't allow project repositories. The projects' Node.js setup is configured in
// kotlin-multiplatform-library-configuration.
plugins.withType<NodeJsRootPlugin> {
    the<NodeJsEnvSpec>().downloadBaseUrl = null
}
plugins.withType<WasmNodeJsRootPlugin> {
    the<WasmNodeJsEnvSpec>().downloadBaseUrl = null
}
plugins.withType<YarnPlugin> {
    the<YarnRootEnvSpec>().downloadBaseUrl = null
}
plugins.withType<WasmYarnPlugin> {
    the<WasmYarnRootEnvSpec>().downloadBaseUrl = null
}
