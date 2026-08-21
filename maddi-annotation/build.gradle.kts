/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Split out of maddi-support at 0.9.1: the annotations are what a user's own code compiles against,
// and they import nothing outside java.base.
//
// NOTE: like maddi-support, this module deliberately does NOT apply `java-library-conventions`.
// That plugin adds `api(platform(project(":platform")))` plus org.jetbrains:annotations and
// org.slf4j:slf4j-api, all of which leak into the published POM and Gradle module metadata. The
// internal io.codelaser:platform BOM is not on Maven Central, so a consumer of such a POM cannot
// resolve it at all.
//
// This artifact must have ZERO dependencies in every published variant -- there is no sibling it is
// allowed to depend on. (maddi-support may depend on THIS one; not the other way round.)
// Check before every release, per PUBLISHING.md.
//
// Targets Java 17 while the rest of the build is 25/26, for the same reason maddi-support does: it
// is the one library user code compiles against.
plugins {
    `java-library`
    id("org.jreleaser")   // version in the root build script, deliberately
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

// group and version come from the root gradle.properties (single release train -- see PUBLISHING.md)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("maddi-annotation")
                description = "Annotations for Maddi, a modification analyzer for duplication detection and immutability."

                groupId = project.group.toString()
                artifactId = "maddi-annotation"
                version = project.version.toString()

                url.set("https://github.com/CodeLaser/maddi")
                // Permissively licensed: this is the artifact user code compiles against. The
                // analyzer itself stays LGPL-3.0. See PUBLISHING.md.
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Bart Naudts")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/CodeLaser/maddi.git")
                    developerConnection.set("scm:git:ssh://github.com/CodeLaser/maddi.git")
                    url.set("https://github.com/CodeLaser/maddi")
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch = true

    project {
        name.set("maddi-annotation")
        description = "Annotations for Maddi, a modification analyzer for duplication detection and immutability."
        license.set("Apache-2.0")
        authors.set(listOf("Bart Naudts"))
        copyright.set("2020-2026 Bart Naudts")

        links {
            homepage.set("https://github.com/CodeLaser/maddi")
            documentation.set("https://github.com/CodeLaser/maddi/road-to-immutability")
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored = true
        mode = org.jreleaser.model.Signing.Mode.FILE
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("${buildFile.parent}/build/staging-deploy")
                }
            }
        }
    }
}
