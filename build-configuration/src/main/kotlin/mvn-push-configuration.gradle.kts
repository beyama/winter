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
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        issueManagement {
            system = providers.gradleProperty("POM_ISSUE_MANAGEMENT_SYSTEM")
            url = providers.gradleProperty("POM_ISSUE_MANAGEMENT_URL")
        }
    }
}
