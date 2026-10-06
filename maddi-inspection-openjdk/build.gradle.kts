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
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-annotation"))
    implementation(project(":maddi-cst-api"))
    testImplementation(project(":maddi-support"))
    testImplementation(project(":maddi-util"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-java-openjdk"))
    // the home-made (congocc) parser + its Context building blocks: used ONLY to parse a module-info.java
    // descriptor when javac compiles with ignoreModule (unnamed-module mode) and therefore never produces a
    // ModuleInfo of its own. See JavaInspectorImpl.parseModuleInfoDescriptor.
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-inspection-parser"))
    testImplementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-inspection-resource"))

}

// The real Lombok jar, for TestLombok and TestStopPolicy: on the class path maddi PARSES, so javac runs the Lombok
// processor from it -- and deliberately NOT on the test JVM's own class path. javac's processor class loader asks
// its parent first, so a Lombok on the test class path answers for every source set's Lombok jar: the tests then
// never load the processor the way production does (from the source set's jar), and TestLombokFallback's broken
// processor would be shadowed by the working one.
val lombokJar = configurations.create("lombokJar") {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}
dependencies {
    lombokJar("org.projectlombok:lombok:1.18.48")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    )
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 4
    inputs.files(lombokJar).withPropertyName("lombokJar")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dmaddi.test.lombokJar=" + lombokJar.singleFile.absolutePath)
    })
    // Alternative-JRE test (TestAlternativeJRE): forward a JDK 21 home to the forked test JVM so it can
    // exercise --system against a JDK where java.applet.Applet still exists. Absent -> the test skips.
    System.getProperty("test.jdk21.home")?.let { systemProperty("test.jdk21.home", it) }
    System.getenv("JDK21_HOME")?.let { environment("JDK21_HOME", it) }
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}