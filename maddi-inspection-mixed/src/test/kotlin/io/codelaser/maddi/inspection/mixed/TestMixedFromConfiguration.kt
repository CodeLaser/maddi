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

package org.e2immu.language.inspection.mixed

import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Phase 5 (consuming side): `MixedInspector.parseFromConfiguration` reads a mixed Java+Kotlin
 * `InputConfiguration` (the shape `ParseMixedList` produces — a Kotlin source set and a Java source set that
 * depends on it) from disk and resolves the cross-language reference to one shared `TypeInfo`.
 */
class TestMixedFromConfiguration {

    @Test
    fun javaSourceSetResolvesKotlinSourceSetFromDisk() {
        val tmp = Files.createTempDirectory("mixed-cfg")
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
