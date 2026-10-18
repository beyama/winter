// Publishing to Maven Central (Central Portal) via the vanniktech plugin.
//
// Coordinates and POM data are read from the gradle.properties files (GROUP, VERSION_NAME, POM_*),
// Maven Central publishing and signing are enabled there as well.
//
// Snapshots and releases are uploaded with `./gradlew publishToMavenCentral`. Releases then need to
// be published on https://central.sonatype.com/publishing/deployments.
//
// Credentials (Central Portal user token) and the signing key are expected in
// ~/.gradle/gradle.properties: mavenCentralUsername, mavenCentralPassword and signing.*
//
// Dokka generates the API docs for the website and the published javadoc jars.
plugins {
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

dokka {
    // POM_NAME is defined in the module's gradle.properties, which providers.gradleProperty ignores.
    moduleName = findProperty("POM_NAME")?.toString() ?: project.name

    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory = layout.projectDirectory.dir("src/main")
            remoteUrl("https://github.com/beyama/winter/tree/master/${project.name}/src/main")
            remoteLineSuffix = "#L"
        }
    }
}

mavenPublishing {
    pom {
        issueManagement {
            system = providers.gradleProperty("POM_ISSUE_MANAGEMENT_SYSTEM")
            url = providers.gradleProperty("POM_ISSUE_MANAGEMENT_URL")
        }
    }
}
