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

// The IntelliJ plugin front-end. Deliberately standalone: it does NOT apply
// java-library-conventions and has NO maddi dependency. It runs on the IDE's JBR (21) and speaks
// only plain JSON to the maddi daemon (JDK 25) over a loopback socket, so the JDK split stays clean.

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    // version omitted: the settings plugin (org.jetbrains.intellij.platform.settings) already puts
    // the IntelliJ Platform Gradle Plugin on the build classpath.
    id("org.jetbrains.intellij.platform")
}

group = "io.codelaser"
version = "0.1.0"

// Target the IDE's runtime (JBR 21). Compile with the Gradle daemon JDK (26) but emit 21 bytecode.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    intellijPlatform {
        // Since 2025.3 Community/Ultimate ship as one distribution (license-tiered), so use intellijIdea(...).
        intellijIdea("2025.3")
        bundledPlugin("com.intellij.java") // Java PSI, for mapping analysis results onto declarations
        // Platform + Java test fixtures (LightJavaCodeInsightFixtureTestCase etc.) for surface tests.
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    // IDE-agnostic daemon client (DTOs, socket client, launcher). Brings Jackson transitively; the plugin
    // only uses it internally (never across the platform boundary), so bundling our own copy is safe.
    implementation(project(":maddi-ide-client"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The IntelliJ platform test fixtures are JUnit 4 (LightJavaCodeInsightFixtureTestCase extends
    // junit.framework.TestCase), needed at compile time; the vintage engine runs them under the JUnit
    // Platform alongside our JUnit 5 tests.
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:6.0.3")
}

intellijPlatform {
    projectName = "maddi" // deterministic plugin directory name (bundled daemon lives under <plugin>/daemon)
    buildSearchableOptions = false // optional headless-IDE indexing step; not needed and fails in CI/sandbox
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253" // 2025.3
            untilBuild = provider { null } // open-ended
        }
    }
}

// Bundle the maddi daemon distribution inside the plugin, at <plugin>/daemon, for both runIde and the
// installable zip. MaddiAnalysisService.resolveInstallDir() falls back to getPluginPath()/daemon.
tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask> {
    dependsOn(":maddi-ide-daemon:installDist")
    from(project(":maddi-ide-daemon").layout.buildDirectory.dir("install/maddi-ide-daemon")) {
        into("maddi/daemon")
    }
}

// For runIde (developing/verifying interactively): point the sandboxed IDE at the freshly built daemon
// distribution and a JDK 25+ (the Gradle daemon JVM), via the dev system-property fallbacks the plugin reads.
tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask> {
    dependsOn(":maddi-ide-daemon:installDist")
    val installDir = project(":maddi-ide-daemon").layout.buildDirectory.dir("install/maddi-ide-daemon")
    systemProperty("maddi.daemon.install", installDir.get().asFile.absolutePath)
    systemProperty("maddi.jdk.home", System.getProperty("java.home"))
}

// The plugin launches the daemon distribution. Tests exercise that launch against the real install.
tasks.test {
    dependsOn(":maddi-ide-daemon:installDist")
    val installDir = project(":maddi-ide-daemon").layout.buildDirectory.dir("install/maddi-ide-daemon")
    systemProperty("maddi.daemon.install", installDir.get().asFile.absolutePath)
    // The plugin test JVM runs on the IDE's JBR (21); maddi needs a JDK 25+ to run and to read java.base.
    // Pass the Gradle daemon JVM (25+) so the daemon runs on it and uses it as the analysis SDK.
    systemProperty("maddi.test.jdkHome", System.getProperty("java.home"))
    useJUnitPlatform()
}
