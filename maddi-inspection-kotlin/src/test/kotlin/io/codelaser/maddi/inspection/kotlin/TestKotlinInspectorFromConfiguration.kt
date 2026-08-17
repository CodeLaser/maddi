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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.nio.file.Files

/**
 * Phase 4: `KotlinInspector.parseFromConfiguration` builds a real multi-module K2 session from an
 * `InputConfiguration` — source **directories** on disk, a library **classpath** (kotlin-stdlib) — and links
 * two source sets so a type in the dependent set resolves to the very same `TypeInfo` produced for the upstream
 * set (over the one shared registry).
 */
class TestKotlinInspectorFromConfiguration {

    @Test
    fun dependentSourceSetResolvesUpstreamType() {
        val tmp = Files.createTempDirectory("k-cfg")
        val aSrc = tmp.resolve("moduleA/src")
        val bSrc = tmp.resolve("moduleB/src")
        Files.createDirectories(aSrc.resolve("a"))
        Files.createDirectories(bSrc.resolve("b"))
        Files.writeString(aSrc.resolve("a/Foo.kt"), "package a\nclass Foo(val id: Int)\n")
        Files.writeString(bSrc.resolve("b/Bar.kt"), "package b\nimport a.Foo\nclass Bar(val foo: Foo)\n")

        // kotlin-stdlib from the running process's classpath, presented as a configured library
        val stdlib = System.getProperty("java.class.path").split(File.pathSeparator)
            .first { it.contains("kotlin-stdlib") }
        val stdlibSet = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(listOf())
            .setUri(URI.create("file:$stdlib")).setLibrary(true).setExternalLibrary(true).build()

        val setA = SourceSetImpl.Builder().setName("moduleA/main")
            .setSourceDirectories(listOf(aSrc)).setUri(aSrc.toUri()).build()
        val setB = SourceSetImpl.Builder().setName("moduleB/main")
            .setSourceDirectories(listOf(bSrc)).setUri(bSrc.toUri())
            .setDependencies(listOf(setA, stdlibSet)).build()

        val config = InputConfigurationImpl.Builder()
            .addClassPathParts(stdlibSet)
            .addSourceSets(setA)
            .addSourceSets(setB)
            .build()

        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)
        val result = inspector.parseFromConfiguration()

        val foo = result[setA]!!.first { it.simpleName() == "Foo" }
        val bar = result[setB]!!.first { it.simpleName() == "Bar" }
        assertNotNull(foo)
        assertTrue(bar.fields().any { it.name() == "foo" })
        // cross-source-set: Bar.foo's type IS the Foo built for moduleA (one shared instance)
        assertSame(foo, bar.getFieldByName("foo", true).type().typeInfo())
    }
}
