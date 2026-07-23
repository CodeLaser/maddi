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

// NOTE: this module deliberately does NOT apply `java-library-conventions`.
//
// maddi-support is the only published, user-facing artifact, and it must stay dependency-free: it
// contains annotations and small support classes and imports nothing outside java.base (see
// module-info.java, which has no `requires`). The conventions plugin adds
// `api(platform(project(":platform")))` plus org.jetbrains:annotations and org.slf4j:slf4j-api,
// all of which leak into the published POM and Gradle module metadata. The internal
// io.codelaser:platform BOM is not published to Maven Central, so a consumer of such a POM cannot
// resolve it at all -- and slf4j would be dragged in as a runtime dependency of an annotations jar.
//
// 0.8.2 on Central has zero dependencies in every variant; keep it that way. The same reasoning is
// why both build plugins strip <dependencies>/<dependencyManagement> from their POMs
// (see maddi-gradleplugin/build.gradle.kts). Here we simply never add them.
//
// It is also the only module targeting Java 17 (the rest is 25/26), because it is the one library a
// user's own code compiles against.
plugins {
    `java-library`
    id("org.jreleaser") version "1.19.0"
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

// Explicit versions rather than the platform BOM, so nothing enters the published metadata.
// Test-only: these do not appear in any published variant.
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
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
