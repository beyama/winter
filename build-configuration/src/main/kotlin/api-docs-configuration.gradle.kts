// Aggregates the Dokka API docs of all modules that are added as `dokka(project(...))` dependency.
// Generate with ./gradlew :dokkaGenerate (output in build/dokka/html).
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    moduleName = "Winter"
}
