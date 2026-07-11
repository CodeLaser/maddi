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
    // NOTE: the Maven-plugin descriptor is NOT generated from the annotations — the usual tool,
    // id("de.benediktritter.maven-plugin-development") 0.4.3 (latest), is incompatible with Gradle 9 (it calls the
    // removed ProjectDependency.getDependencyProject()). Instead a hand-maintained descriptor lives at
    // src/main/resources/META-INF/maven/plugin.xml (kept in sync with the @Mojo/@Parameter annotations by hand;
    // @project.version@ is substituted below). It is packaged into the jar, so `mvn maddi:<goal>` resolves the
    // goals. Still pending for a Central-consumable plugin: shading the (unpublished) analyzer modules into the jar.
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// version comes from the root gradle.properties (single release train — see PUBLISHING.md)

val mavenVersion = "3.9.9"
val mavenPluginToolsVersion = "3.15.1"

dependencies {
    // maddi modules (in-tree, same coordinates as the Gradle plugin)
    implementation(project(":maddi-inspection-api"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-inspection-integration"))
    implementation(project(":maddi-inspection-openjdk"))
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-modification-analyzer"))
    implementation(project(":maddi-aapi-parser"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-main")) // Main constants + exit codes (same as the Gradle plugin)
    implementation(project(":maddi-run-openjdk")) // the openjdk-parser-based RunAnalyzer
    runtimeOnly(project(":maddi-aapi-archive")) // the shipped analysis-result jars (resource:.../*.jar)

    // Maven plugin API (provided by the Maven runtime that hosts the plugin)
    compileOnly("org.apache.maven:maven-plugin-api:$mavenVersion")
    compileOnly("org.apache.maven:maven-core:$mavenVersion")
    compileOnly("org.apache.maven:maven-artifact:$mavenVersion")
    compileOnly("org.apache.maven:maven-model:$mavenVersion")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginToolsVersion")

    // Maven resolver (Aether) — used to resolve the project's dependency classpath
    implementation("org.apache.maven.resolver:maven-resolver-api:1.8.2")
    implementation("org.apache.maven.resolver:maven-resolver-util:1.8.2")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("ch.qos.logback:logback-classic")
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
