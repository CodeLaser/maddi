/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
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
    `java-gradle-plugin`
    id("java-library-conventions")
    // Shadow: bundle the (Kotlin-free) Java analyzer into the plugin jar so it is self-contained.
    // We do not publish the fine-grained analyzer modules (see PUBLISHING.md), so the plugin cannot
    // declare Maven dependencies on them — it ships them inside its own jar instead.
    id("com.gradleup.shadow") version "9.2.2"
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// The analyzer modules and their third-party transitives (jackson, logback, asm, congocc, ...) go into
// `shade`: everything here is bundled into the shadow jar. `implementation` extends it so the same
// artifacts are on the compile/runtime classpath. gradleApi() (added to `api` by java-gradle-plugin)
// is deliberately NOT in `shade` — Gradle provides it at runtime, so it must not be bundled.
val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.named("implementation") { extendsFrom(shade) }

dependencies {
    shade(project(":maddi-inspection-api"))
    shade(project(":maddi-modification-common"))
    shade(project(":maddi-modification-prepwork"))
    shade(project(":maddi-modification-link"))
    shade(project(":maddi-modification-analyzer"))
    shade(project(":maddi-graph"))
    shade(project(":maddi-util"))
    shade(project(":maddi-cst-analysis"))

    shade(project(":maddi-cst-impl"))
    shade(project(":maddi-cst-io"))
    shade(project(":maddi-cst-print"))
    shade(project(":maddi-inspection-parser"))
    shade(project(":maddi-inspection-integration"))
    shade(project(":maddi-inspection-resource"))
    shade(project(":maddi-java-bytecode"))
    shade(project(":maddi-java-parser"))
    shade(project(":maddi-aapi-parser"))
    testRuntimeOnly(project(":maddi-aapi-archive"))

    shade(project(":maddi-run-config"))
    shade(project(":maddi-run-main")) // GeneralConfiguration/InputConfiguration property mapping
    shade(project(":maddi-run-openjdk")) // the openjdk-parser-based RunAnalyzer, run in a forked worker

    shade("ch.qos.logback:logback-classic")
    shade("com.fasterxml.jackson.core:jackson-databind")

    // GRADLE PLUGIN
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.shadowJar {
    // The shadow jar replaces the thin jar as the plugin artifact (no classifier).
    archiveClassifier.set("")
    configurations = listOf(shade)
    // Keep maddi's own class names intact: the forked worker references RunAnalyzer by its real name,
    // so relocating org.e2immu.* would break it. The analyzer runs in an isolated worker process, so
    // no relocation of third-party deps is needed either.
    mergeServiceFiles()
}

// The plain jar yields its place to the shadow jar as the plugin artifact.
tasks.named<Jar>("jar") { archiveClassifier.set("plain") }
tasks.named("assemble") { dependsOn(tasks.shadowJar) }

gradlePlugin {
    plugins {
        create("e2immuAnalyzerPlugin") {
            id = "org.e2immu.analyzer-plugin"
            implementationClass = "org.e2immu.gradleplugin.AnalyzerPlugin"
            displayName = "e2immu's gradle plugin"
        }
        description = "Run the e2immu analyzer from Gradle"
        isAutomatedPublishing = true
    }
}
