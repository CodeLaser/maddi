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
    java
    id("org.e2immu.analyzer-plugin") version "0.8.2"
}

/*
 Analyze ONE maddi module from source: maddi-cst-impl, because it holds TypeInfoImpl, whose `inspection`
 field is the EventuallyFinalOnDemand that stage 2 of eventual immutability is meant to recognize.

 Its maddi dependencies come in as the jars the real build produces. Merging several modules into one
 Gradle project does NOT work: each maddi module is a JPMS module, and five module-info.java files in one
 compilation collide. Keeping the module intact also keeps its `requires` correct, which is what lets the
 openjdk front end resolve slf4j and the rest off javac's module path -- see
 TestJavaInspector5RealClasspathModule, and the module detection in the plugin's ComputeSourceSets.
*/
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sourceSets {
    main {
        java { setSrcDirs(listOf("../maddi-cst-impl/src/main/java")) }
        resources { setSrcDirs(emptyList<String>()) }
    }
}

dependencies {
    listOf("maddi-support", "maddi-util", "maddi-cst-api", "maddi-cst-analysis").forEach {
        implementation(":$it:0.8.2")
    }
    // the third-party artifacts java-library-conventions gives every maddi module; versions from the
    // platform BOM (../platform/build.gradle.kts)
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.jetbrains:annotations:26.1.0")
}

e2immu {
    sourcePackages = "org.e2immu."
    jmods = "java.base"
}
