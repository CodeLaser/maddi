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

package org.e2immu.language.inspection.kotlin

import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

/**
 * Integration: parse the whole kotlin-stdlib *sources* (the "core kotlin module", the analogue of running the
 * openjdk front-end over a JDK module) against its own bytecode on the classpath. Exercises, composed, the
 * multiplatform-support fixes: @JvmMultifileClass facade merge, expect/actual, @JvmName, mapped builtins,
 * Array<T> reification, return-type overloads, heterogeneous destructuring, const/private accessors. The
 * bytecode dependency is what lets K2 resolve the unsigned intrinsics (UInt, …) the standalone session cannot
 * resolve from source alone.
 */
class TestKotlinStdlibParse {

    @Test
    fun parseStdlibSources() {
        val sourcesJar = System.getProperty("kotlin.stdlib.sources")?.let { Path.of(it) }
            ?: error("system property kotlin.stdlib.sources not set (see build.gradle.kts)")
        val stdlibBytecode = System.getProperty("java.class.path").split(File.pathSeparator)
            .first { Regex("kotlin-stdlib-[0-9].*\\.jar$").containsMatchIn(it) }

        val extract = Files.createTempDirectory("kstdlib-src")
        extractJar(sourcesJar, extract)
        // each immediate sub-directory of commonMain/ and jvmMain/ (kotlin, generated, jdkN, …) is a package root
        val roots = listOf("commonMain", "jvmMain").flatMap { top ->
            val d = extract.resolve(top)
            if (Files.isDirectory(d)) Files.list(d).use { s -> s.filter { Files.isDirectory(it) }.toList() }
            else emptyList()
        }
        assertTrue(roots.isNotEmpty(), "no source roots found under $extract")

        val lib = SourceSetImpl.Builder().setName("kotlin-stdlib-bin").setSourceDirectories(listOf())
            .setUri(URI.create("file:$stdlibBytecode")).setLibrary(true).setExternalLibrary(true).build()
        val stdlib = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(roots)
            .setUri(extract.toUri()).setDependencies(listOf(lib)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(lib).addSourceSets(stdlib).build()

        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)
        val committed = inspector.parseFromConfiguration().values.sumOf { it.size }
        // ~676 primary types for 2.4.0; assert a healthy floor so version drift doesn't make this brittle
        assertTrue(committed > 600, "expected the full stdlib to commit >600 primary types, got $committed")
    }

    private fun extractJar(jar: Path, target: Path) {
        ZipInputStream(Files.newInputStream(jar)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".kt")) {
                    val out = target.resolve(entry.name)
                    Files.createDirectories(out.parent)
                    Files.newOutputStream(out).use { zip.copyTo(it) }
                }
                entry = zip.nextEntry
            }
        }
    }
}
