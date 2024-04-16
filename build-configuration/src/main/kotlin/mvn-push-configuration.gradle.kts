plugins {
    id("maven-publish")
    id("signing")
}

fun stringProperty(propertyName: String) =
    checkNotNull(project.property(propertyName)) { "Property `$propertyName` not found" }.toString()

version = checkNotNull(project.property("VERSION_NAME"))
group = checkNotNull(project.property("GROUP"))

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name.toString()
            version = project.version.toString()

            if (project.name.contains("android", true)) {
                afterEvaluate { from(components["release"]) }
            } else {
                from(components["java"])
            }

            pom {
                name = stringProperty("POM_NAME")

                description = stringProperty("POM_DESCRIPTION")
                url = stringProperty("POM_URL")
                packaging = stringProperty("POM_PACKAGING")

                scm {
                    url = stringProperty("POM_SCM_URL")
                    connection = stringProperty("POM_SCM_CONNECTION")
                    developerConnection = stringProperty("POM_SCM_DEV_CONNECTION")
                }

                licenses {
                    license {
                        name = stringProperty("POM_LICENCE_NAME")
                        url = stringProperty("POM_LICENCE_URL")
                        distribution = stringProperty("POM_LICENCE_DIST")
                    }
                }

                developers {
                    developer {
                        id = stringProperty("POM_DEVELOPER_ID")
                        name = stringProperty("POM_DEVELOPER_NAME")
                    }
                }

                issueManagement {
                    system = stringProperty("POM_ISSUE_MANAGEMENT_SYSTEM")
                    url = stringProperty("POM_ISSUE_MANAGEMENT_URL")
                }
            }

            repositories {
                maven {
                    val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")

                    name = "OSSRH"
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotRepo else releaseRepo

                    credentials {
                        username = stringProperty("SONATYPE_NEXUS_USERNAME")
                        password = stringProperty("SONATYPE_NEXUS_PASSWORD")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

if (project.name.contains("android", true)) {
    tasks.withType<Sign> {
        dependsOn("bundleReleaseAar")
    }
}

//
//    tasks.withType(Sign).tap {
//        configureEach {
//            onlyIf { isReleaseVersion }
//        }
//    }
//
//}