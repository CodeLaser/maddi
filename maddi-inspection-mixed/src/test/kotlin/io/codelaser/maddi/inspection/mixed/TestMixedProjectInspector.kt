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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Phase 5, multi-source-set: `MixedProjectInspector` places each type in its OWN CST source set (Kotlin `Foo`
 * in `kotlin/main`, Java `UseFoo` in `java/main`) — not flattened — while the cross-language reference still
 * resolves to one shared `TypeInfo`.
 */
class TestMixedProjectInspector {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root: each call still gets its own unique
     * directory, and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @field:TempDir
    lateinit var tempRoot: Path

    @Test
    fun typesLandInTheirOwnSourceSetsAndStillCrossResolve() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-proj")
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
        val tmp = Files.createTempDirectory(tempRoot, "mixed-k2j")
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
        val tmp = Files.createTempDirectory(tempRoot, "mixed-multi")
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

    /**
     * A Kotlin reference to a Java-source declaration is recorded against the Java front end's Info, as one to a Kotlin
     * declaration is: the override's `super.run(n)`, a call, a call inside a lambda passed to a library function (which
     * the Kotlin CST keeps as a placeholder), the Java type in an import, and a field read. A rename of the Java
     * method reads these to find its Kotlin spellings.
     */
    @Test
    fun kotlinReferencesToJavaSourceDeclarationsAreRecorded() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-refs")
        val jDir = tmp.resolve("proj/src/main/java")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        Files.createDirectories(jDir.resolve("q"))
        Files.createDirectories(kDir.resolve("p"))
        Files.writeString(jDir.resolve("q/J.java"), """
            package q;
            public class J {
                public int size;
                public String run(int n) { return "x".repeat(n); }
                public String run(String s) { return s; }
            }
            """.trimIndent() + "\n")
        Files.writeString(kDir.resolve("p/K.kt"), """
            package p
            import q.J
            class K : J() {
                override fun run(n: Int): String = super.run(n) + "k"
            }
            fun call(j: J) = j.run(2) + listOf(1).map { j.run(it) }.joinToString() + j.run("s") + j.size
            """.trimIndent() + "\n")
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri())
            .setDependencies(listOf(javaSet)).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(javaSet).addSourceSets(kotlinSet).build()

        val result = MixedProjectInspector().parse(config)

        val j = result.javaTypes.first { it.simpleName() == "J" }
        val runInt = j.methods().single { it.name() == "run" && it.parameters()[0].parameterizedType().isPrimitiveExcludingVoid }
        val runString = j.methods().single { it.name() == "run" && !it.parameters()[0].parameterizedType().isPrimitiveExcludingVoid }
        val size = j.getFieldByName("size", true)
        val kotlinTypes = result.kotlinTypes.flatMap { it.recursiveSubTypeStream().toList() }
        val k = kotlinTypes.first { it.simpleName() == "K" }
        val facade = kotlinTypes.first { it.simpleName() == "KKt" }
        fun at(host: io.codelaser.maddi.cst.api.info.Info, target: io.codelaser.maddi.cst.api.info.Info) =
            host.source().detailedSources().references(target).map { "${it.beginLine()}:${it.beginPos()}" }.sorted()
        val call = facade.methods().single { it.name() == "call" }
        assertEquals(listOf("4:46"), at(k.methods().single { it.name() == "run" }, runInt), "super.run(n)")
        assertEquals(listOf("6:20", "6:47"), at(call, runInt), "j.run(2), and j.run(it) inside the lambda")
        // j.run(it) inside the lambda: K2 answers the call with both overloads there (resolveSymbol gives none, the
        // reference's candidates are both), so it is recorded against both -- a rename of one is refused as sharing
        // the spelling, not planned wrong
        assertEquals(listOf("6:47", "6:76"), at(call, runString), "the other overload, by its parameter type")
        assertEquals(listOf("6:89"), at(call, size), "a Java field")
        assertEquals(listOf("2:10"), at(facade, j), "the import")
    }
}
