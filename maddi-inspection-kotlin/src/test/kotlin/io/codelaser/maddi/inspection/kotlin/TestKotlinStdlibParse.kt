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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root: each call still gets its own unique
     * directory, and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @field:TempDir
    lateinit var tempRoot: Path

    @Test
    fun parseStdlibSources() {
        val sourcesJar = System.getProperty("kotlin.stdlib.sources")?.let { Path.of(it) }
            ?: error("system property kotlin.stdlib.sources not set (see build.gradle.kts)")
        val stdlibBytecode = System.getProperty("java.class.path").split(File.pathSeparator)
            .first { Regex("kotlin-stdlib-[0-9].*\\.jar$").containsMatchIn(it) }

        val extract = Files.createTempDirectory(tempRoot, "kstdlib-src")
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
