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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Phase 5, multi-source-set: `MixedProjectInspector` places each type in its OWN CST source set (Kotlin `Foo`
 * in `kotlin/main`, Java `UseFoo` in `java/main`) — not flattened — while the cross-language reference still
 * resolves to one shared `TypeInfo`.
 */
class TestMixedProjectInspector {

    @Test
    fun typesLandInTheirOwnSourceSetsAndStillCrossResolve() {
        val tmp = Files.createTempDirectory("mixed-proj")
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

        val result = MixedProjectInspector().parse(config)

        val foo = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Foo" }
        val useFoo = result.javaTypes.first { it.simpleName() == "UseFoo" }

        // each type is in its OWN source set (not one flattened bag)
        assertEquals("kotlin/main", foo.compilationUnit().sourceSet().name())
        assertEquals("java/main", useFoo.compilationUnit().sourceSet().name())
        // and the Java->Kotlin reference resolves to the one shared instance
        assertSame(foo, useFoo.getFieldByName("foo", true).type().typeInfo())
    }

    /** The other direction: a Kotlin source set depends on a Java source set (Java-first order). */
    @Test
    fun kotlinSourceSetResolvesUpstreamJavaSourceType() {
        val tmp = Files.createTempDirectory("mixed-k2j")
        val jDir = tmp.resolve("proj/src/main/java")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        Files.createDirectories(jDir.resolve("a"))
        Files.createDirectories(kDir.resolve("b"))
        Files.writeString(jDir.resolve("a/JavaFoo.java"),
            "package a;\npublic class JavaFoo {\n    public int id;\n}\n")
        Files.writeString(kDir.resolve("b/Bar.kt"), "package b\nimport a.JavaFoo\nclass Bar(val foo: JavaFoo)\n")

        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri())
            .setDependencies(listOf(javaSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(javaSet)
            .addSourceSets(kotlinSet)
            .build()

        val result = MixedProjectInspector().parse(config)

        val javaFoo = result.javaTypes.first { it.simpleName() == "JavaFoo" }
        val bar = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Bar" }
        assertEquals("java/main", javaFoo.compilationUnit().sourceSet().name())
        assertEquals("kotlin/main", bar.compilationUnit().sourceSet().name())
        // Kotlin -> Java source across the boundary: one shared instance
        assertSame(javaFoo, bar.getFieldByName("foo", true).type().typeInfo())
    }

    /** Two Java sets (B depends on A) plus a Kotlin set: exercises Java->Java AND Java->Kotlin in one project. */
    @Test
    fun multiJavaModuleAndKotlinResolveTogether() {
        val tmp = Files.createTempDirectory("mixed-multi")
        val aDir = tmp.resolve("a/src/main/java")
        val bDir = tmp.resolve("b/src/main/java")
        val kDir = tmp.resolve("k/src/main/kotlin")
        Files.createDirectories(aDir.resolve("a"))
        Files.createDirectories(bDir.resolve("b"))
        Files.createDirectories(kDir.resolve("k"))
        Files.writeString(aDir.resolve("a/A.java"), "package a;\npublic class A { public int x; }\n")
        Files.writeString(kDir.resolve("k/K.kt"), "package k\nclass K(val id: Int)\n")
        Files.writeString(bDir.resolve("b/UseAll.java"),
            "package b;\npublic class UseAll {\n    public a.A a;\n    public k.K k;\n}\n")

        val javaA = SourceSetImpl.Builder().setName("javaA/main")
            .setSourceDirectories(listOf(aDir)).setUri(aDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaB = SourceSetImpl.Builder().setName("javaB/main")
            .setSourceDirectories(listOf(bDir)).setUri(bDir.toUri())
            .setDependencies(listOf(javaA, kotlinSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(javaA).addSourceSets(kotlinSet).addSourceSets(javaB).build()

        val result = MixedProjectInspector().parse(config)

        val a = result.javaTypes.first { it.simpleName() == "A" }
        val k = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "K" }
        val useAll = result.javaTypes.first { it.simpleName() == "UseAll" }
        assertEquals("javaA/main", a.compilationUnit().sourceSet().name())
        assertEquals("javaB/main", useAll.compilationUnit().sourceSet().name())
        // Java -> Java across source sets, and Java -> Kotlin, both to one shared instance
        assertSame(a, useAll.getFieldByName("a", true).type().typeInfo())
        assertSame(k, useAll.getFieldByName("k", true).type().typeInfo())
    }
}
