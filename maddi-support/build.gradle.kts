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

plugins {
    id("java-library-conventions")
    id("org.jreleaser") version "1.19.0"
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

// group and version come from the root gradle.properties (single release train — see PUBLISHING.md)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("maddi-support")
                description = "Support library for Maddi, a modification analyser for duplication detection and immutability."

                groupId = project.group.toString()
                artifactId = "maddi-support"
                version = project.version.toString()

                url.set("https://github.com/CodeLaser/maddi")
                // maddi-support is the one artifact user code compiles against, so it is permissively
                // licensed; the analyzer itself stays LGPL-3.0. Versions up to and including 0.8.2 were
                // published under LGPL-3.0-or-later and remain so. See PUBLISHING.md.
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
        name.set("maddi-support")
        description = "Support library for Maddi, a modification analyser for duplication detection and immutability."
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
