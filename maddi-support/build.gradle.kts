/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    id("java-library-conventions")
    id("org.jreleaser") version "1.19.0"
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

group = "io.codelaser"
version = "0.8.2"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("maddi-support")
                description = "Support library for Maddi, a modification analyser for duplication detection and immutability."

                groupId = "io.codelaser"
                artifactId = "maddi-support"
                version = "0.8.2"

                url.set("https://github.com/CodeLaser/maddi")
                licenses {
                    license {
                        name.set("LGPL-3.0-or-later")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.en.html")
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
        license.set("LGPL-3.0-or-later")
        authors.set(listOf("Bart Naudts"))
        copyright.set("2020-2025 Bart Naudts")

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
