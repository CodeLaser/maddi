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

package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Phase 5 (consuming side): `MixedInspector.parseFromConfiguration` reads a mixed Java+Kotlin
 * `InputConfiguration` (the shape `ParseMixedList` produces — a Kotlin source set and a Java source set that
 * depends on it) from disk and resolves the cross-language reference to one shared `TypeInfo`.
 */
class TestMixedFromConfiguration {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root: each call still gets its own unique
     * directory, and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @field:TempDir
    lateinit var tempRoot: Path

    @Test
    fun javaSourceSetResolvesKotlinSourceSetFromDisk() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-cfg")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        val jDir = tmp.resolve("proj/src/main/java")
        Files.createDirectories(kDir.resolve("a"))
        Files.createDirectories(jDir.resolve("b"))
        Files.writeString(kDir.resolve("a/Foo.kt"), "package a\nclass Foo(val id: Int)\n")
        Files.writeString(jDir.resolve("b/UseFoo.java"),
            "package b;\npublic class UseFoo {\n    public a.Foo foo;\n}\n")

        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri())
            .setDependencies(listOf(kotlinSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(kotlinSet)
            .addSourceSets(javaSet)
            .build()

        val result = MixedInspector().parseFromConfiguration(config)

        val foo = result.kotlinTypes.first { it.simpleName() == "Foo" }
        val useFoo = result.javaTypes.first { it.simpleName() == "UseFoo" }
        assertTrue(useFoo.getFieldByName("foo", true) != null)
        // Java -> Kotlin across the source-set boundary, from disk: one shared instance
        assertSame(foo, useFoo.getFieldByName("foo", true).type().typeInfo())
    }
}
