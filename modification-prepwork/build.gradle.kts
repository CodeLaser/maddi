/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */

plugins {
    `java-library`
    `maven-publish`
}

group = "org.e2immu"


repositories {
    maven {
        url = uri(project.findProperty("codeartifactPublicUri") as String)
        credentials {
            username = "aws"
            password = project.findProperty("codeartifactToken") as String
        }
    }
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

val slf4jVersion = project.findProperty("slf4jVersion") as String
val jupiterApiVersion = project.findProperty("jupiterApiVersion") as String
val jupiterEngineVersion = project.findProperty("jupiterEngineVersion") as String
val logbackClassicVersion = project.findProperty("logbackClassicVersion") as String
val jetBrainsAnnotationsVersion = project.findProperty("jetBrainsAnnotationsVersion") as String

dependencies {
    api("org.e2immu:e2immu-external-support:$version")
    api("org.e2immu:e2immu-cst-api:$version")
    api("org.e2immu:e2immu-inspection-api:$version")
    implementation("org.e2immu:e2immu-internal-graph:$version")
    implementation("org.e2immu:e2immu-internal-util:$version")
    implementation("org.e2immu:e2immu-cst-analysis:$version")
    implementation("org.e2immu:e2immu-cst-io:$version")
    implementation("org.e2immu:e2immu-cst-impl:$version")
    implementation("org.e2immu:e2immu-modification-common:$version")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.jetbrains:annotations:$jetBrainsAnnotationsVersion")
    testImplementation("org.e2immu:e2immu-cst-print:$version")
    testImplementation("org.e2immu:e2immu-inspection-parser:$version")
    testImplementation("org.e2immu:e2immu-java-parser:$version")
    testImplementation("org.e2immu:e2immu-java-bytecode:$version")
    testImplementation("org.e2immu:e2immu-inspection-integration:$version")
    testImplementation("org.e2immu:e2immu-inspection-resource:$version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterApiVersion")
    testImplementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterEngineVersion")
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2G"
    maxParallelForks = 4
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
                name = "analyzer-modification-prepwork of e2immu analyser"
                description = "Static code analyser focusing on modification and immutability. " +
                        "This module contains the first phase of the modification analyzer."
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