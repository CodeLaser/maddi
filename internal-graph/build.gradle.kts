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
    `java-library`
    `maven-publish`
}

group = "org.e2immu"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

tasks.test {
    useJUnitPlatform()
}

val slf4jVersion = project.findProperty("slf4jVersion") as String
val jupiterApiVersion = project.findProperty("jupiterApiVersion") as String
val jupiterEngineVersion = project.findProperty("jupiterEngineVersion") as String
val logbackClassicVersion = project.findProperty("logbackClassicVersion") as String
val jgraphtCoreVersion = project.findProperty("jgraphtCoreVersion") as String
val jgraphtIoVersion = project.findProperty("jgraphtIoVersion") as String

dependencies {
    implementation("org.e2immu:e2immu-external-support:$version")
    implementation("org.jgrapht:jgrapht-core:$jgraphtCoreVersion")
    implementation("org.jgrapht:jgrapht-io:$jgraphtIoVersion")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterApiVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterEngineVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackClassicVersion")
}

publishing {
    repositories {
        maven {
            url = uri(project.findProperty("publishPublicUri") as String)
            credentials {
                username = project.findProperty("publishUsername") as String
                password = project.findProperty("publishPassword") as String
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "util-internal-graph of e2immu analyser"
                description = "Static code analyser focusing on modification and immutability. " +
                        "This module contains some graph algorithms used by the analyser."
                url = "https://e2immu.org"
                scm {
                    url = "https://github.com/e2immu"
                }
                licenses {
                    license {
                        name = "GNU Lesser General Public License, version 3.0"
                        url = "https://www.gnu.org/licenses/lgpl-3.0.html"
                    }
                }
                developers {
                    developer {
                        id = "bnaudts"
                        name = "Bart Naudts"
                        email = "bart.naudts@e2immu.org"
                    }
                }
            }
        }
    }
}