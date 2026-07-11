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
    id("java-library-conventions")
    `maven-publish`
    // NOTE: the Maven-plugin descriptor is NOT generated from the annotations — the usual tool,
    // id("de.benediktritter.maven-plugin-development") 0.4.3 (latest), is incompatible with Gradle 9 (it calls the
    // removed ProjectDependency.getDependencyProject()). Instead a hand-maintained descriptor lives at
    // src/main/resources/META-INF/maven/plugin.xml (kept in sync with the @Mojo/@Parameter annotations by hand;
    // @project.version@ is substituted below). It is packaged into the jar, so `mvn maddi:<goal>` resolves the goals.
    // Shadow bundles the (unpublished) analyzer modules into the jar, mirroring the Gradle plugin, so the plugin is
    // self-contained and Central-consumable.
    id("com.gradleup.shadow") version "9.2.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// version comes from the root gradle.properties (single release train — see PUBLISHING.md)

val mavenVersion = "3.9.9"
val mavenPluginToolsVersion = "3.15.1"

// Everything in `shade` is bundled into the shadow jar; `implementation` extends it so the same artifacts
// are on the compile/runtime classpath. Deliberately NOT shaded (provided by the Maven runtime that hosts
// the plugin): the Maven API, the Aether resolver (`org.eclipse.aether.*` objects are created by Maven core's
// injected ProjectDependenciesResolver — a second bundled copy would cause LinkageError/ClassCastException),
// and slf4j-api (Maven core exports it and its own binding to plugins).
val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.named("implementation") { extendsFrom(shade) }

dependencies {
    // maddi modules (in-tree, same coordinates as the Gradle plugin) — bundled
    shade(project(":maddi-inspection-api"))
    shade(project(":maddi-inspection-resource"))
    shade(project(":maddi-inspection-integration"))
    shade(project(":maddi-inspection-openjdk"))
    shade(project(":maddi-cst-api"))
    shade(project(":maddi-cst-impl"))
    shade(project(":maddi-cst-io"))
    shade(project(":maddi-cst-print"))
    shade(project(":maddi-cst-analysis"))
    shade(project(":maddi-modification-common"))
    shade(project(":maddi-modification-prepwork"))
    shade(project(":maddi-modification-link"))
    shade(project(":maddi-modification-analyzer"))
    shade(project(":maddi-aapi-parser"))
    shade(project(":maddi-graph"))
    shade(project(":maddi-util"))
    shade(project(":maddi-run-config"))
    shade(project(":maddi-run-main")) // Main constants + exit codes (same as the Gradle plugin)
    shade(project(":maddi-run-openjdk")) // the openjdk-parser-based RunAnalyzer
    shade(project(":maddi-aapi-archive")) // the shipped analysis-result jars (resource:.../*.jar)

    // Maven plugin API — provided by the Maven runtime that hosts the plugin (never bundled)
    compileOnly("org.apache.maven:maven-plugin-api:$mavenVersion")
    compileOnly("org.apache.maven:maven-core:$mavenVersion")
    compileOnly("org.apache.maven:maven-artifact:$mavenVersion")
    compileOnly("org.apache.maven:maven-model:$mavenVersion")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginToolsVersion")

    // Maven resolver (Aether) — provided by Maven core to plugins; compileOnly so it is NOT bundled
    compileOnly("org.apache.maven.resolver:maven-resolver-api:1.8.2")
    compileOnly("org.apache.maven.resolver:maven-resolver-util:1.8.2")

    shade("com.fasterxml.jackson.core:jackson-databind")
    shade("ch.qos.logback:logback-classic")
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shade)
    // slf4j-api arrives transitively via the maddi modules; Maven core provides it, so keep it out of the jar
    // (two copies of the API would clash with the binding). maddi's own class names are not relocated — the
    // mojos reference RunAnalyzer etc. by their real names.
    dependencies {
        exclude(dependency("org.slf4j:slf4j-api"))
    }
    mergeServiceFiles()
}

// The plain jar yields its place to the shadow jar as the plugin artifact.
tasks.named<Jar>("jar") { archiveClassifier.set("plain") }
tasks.named("assemble") { dependsOn(tasks.shadowJar) }

val localPluginRepoDir = layout.buildDirectory.dir("local-plugin-repo")

publishing {
    repositories {
        maven {
            name = "localPluginRepo"
            url = uri(localPluginRepoDir)
        }
    }
    publications {
        // Publish the self-contained shadow jar with a maven-plugin POM. The publication is built from the
        // artifact alone (not from the java component), so the POM carries NO dependencies — everything a
        // consumer needs is either bundled in the jar or provided by the Maven runtime.
        create<MavenPublication>("mavenPlugin") {
            artifact(tasks.shadowJar)
            pom {
                packaging = "maven-plugin"
                name.set("maddi Maven plugin")
                description.set("Run the maddi analyzer (modification analysis for duplication detection and " +
                        "immutability) from Maven.")
                url.set("https://github.com/CodeLaser/maddi")
                licenses {
                    license {
                        name.set("LGPL-3.0-or-later")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.en.html")
                    }
                }
            }
        }
    }
}

// The hand-maintained Maven plugin descriptor carries @project.version@; substitute the gradle.properties
// version at copy time. ReplaceTokens uses '@...@', so Maven's own ${...} expressions are left untouched.
tasks.processResources {
    val descriptorVersion = project.version.toString()
    inputs.property("descriptorVersion", descriptorVersion)
    filesMatching("META-INF/maven/plugin.xml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf("project.version" to descriptorVersion)
        )
    }
}
