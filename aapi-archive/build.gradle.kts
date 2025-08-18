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
val logbackClassicVersion = project.findProperty("logbackClassicVersion") as String
val jupiterApiVersion = project.findProperty("jupiterApiVersion") as String

dependencies {
    api("org.e2immu:e2immu-external-support:$version")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    implementation("org.junit.jupiter:junit-jupiter-api:$jupiterApiVersion")
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
                name = "analyzer-aapi-archive of e2immu analyser"
                description = "Static code analyser focusing on modification and immutability. " +
                        "This module contains Annotated API files, both in source and processed JSON form."
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