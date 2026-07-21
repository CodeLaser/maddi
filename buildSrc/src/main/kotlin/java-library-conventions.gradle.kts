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
    `java-library`
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Fast/slow test split, to keep the everyday `test` loop quick.
//   - `test` (and `check`/`build`) runs everything EXCEPT tests tagged "slow".
//   - `slowTest` runs ONLY the "slow" tests: the large-corpus smoke tests (fernflower, guava,
//     clonebench, langchain4j, ...). It runs one fork at a time and inherits the module's own test
//     jvmArgs / heap (e.g. the -Xmx from TESTXMX, the javac --add-exports) and system properties,
//     so no per-module duplication is needed. Run it explicitly: `./gradlew slowTest`.
// Tests become "slow" by adding `@org.junit.jupiter.api.Tag("slow")` to the class; until then this
// frame is inert (no test is tagged, so `slowTest` selects nothing and `test` is unchanged).
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("slow") }
}

val slowTest = tasks.register<Test>("slowTest") {
    group = "verification"
    description = "Runs the slow, large-corpus smoke tests (JUnit tag \"slow\"), one fork at a time."
    useJUnitPlatform { includeTags("slow") }
    shouldRunAfter(tasks.named("test"))
}

// Mirror the module's own `test` configuration onto `slowTest` after the module's build script has
// finished configuring `test` (each module sets its own jvmArgs / heap / corpus system properties).
afterEvaluate {
    val test = tasks.named<Test>("test").get()
    slowTest.configure {
        testClassesDirs = test.testClassesDirs
        classpath = test.classpath
        // Heap must be mirrored separately: Gradle lifts -Xmx out of jvmArgs into maxHeapSize, so by the time
        // we copy, test.jvmArgs no longer contains it. Without this, slowTest ran on Gradle's default heap
        // (512m) no matter what TESTXMX said -- which is why the large-corpus proving ground OOM'd rather than
        // reporting a result, in the one module (maddi-run-openjdk) that sets its heap this way.
        maxHeapSize = test.maxHeapSize
        test.jvmArgs?.let { setJvmArgs(it) }
        systemProperties(test.systemProperties)
        maxParallelForks = 1
    }
}

group = "io.codelaser"

dependencies {
    api(platform(project(":platform")))

    implementation("org.jetbrains:annotations")

    // common logging
    implementation("org.slf4j:slf4j-api")
    testImplementation("ch.qos.logback:logback-classic")

    // common test
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

