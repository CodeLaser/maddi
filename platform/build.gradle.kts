/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
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
    `java-platform`
    `maven-publish` // published to Maven local so the Tycho/Eclipse build can resolve maddi-ide-client's POM (imports this BOM)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"]) // io.codelaser:platform:<version>
        }
    }
}

dependencies {
    constraints {
        api("org.jgrapht:jgrapht-core:1.5.2")
        api("org.jgrapht:jgrapht-io:1.5.2")

        api("org.junit.jupiter:junit-jupiter-api:6.0.3")

        api("org.slf4j:slf4j-api:2.0.17")
        api("ch.qos.logback:logback-classic:1.5.32")

        api("org.jetbrains:annotations:26.1.0")
        api("com.fasterxml.jackson.core:jackson-databind:2.19.2")
        api("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
        api("io.micrometer:micrometer-core:1.15.10")
        api("commons-cli:commons-cli:1.11.0")

        api("org.ow2.asm:asm:9.10.1")
    }
}

