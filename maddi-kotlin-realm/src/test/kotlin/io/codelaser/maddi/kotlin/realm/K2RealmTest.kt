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
package io.codelaser.maddi.kotlin.realm

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * ⭐ <b>The test the whole design rests on.</b> The Kotlin compiler is loaded in a realm; the CST is not. If
 * the two ever disagree about what {@code TypeInfo} is, nothing throws in an obvious place — the parse simply
 * stops agreeing with itself, and the mixed inspector's core invariant (a cross-language reference resolves
 * to ONE {@code TypeInfo}) quietly becomes false. So these assert identity, not equality.
 */
class K2RealmTest {

    private val k2Classpath: String = System.getProperty("maddi.k2.classpath")
        ?: error("the test JVM needs -Dmaddi.k2.classpath (see this module's build.gradle.kts)")

    private fun loadable(fqn: String): Class<*>? =
        try {
            Class.forName(fqn, false, javaClass.classLoader)
        } catch (_: ClassNotFoundException) {
            null
        }

    /**
     * ⛔ The precondition. If the compiler were on this test's own classpath, every assertion below would pass
     * without the realm doing anything at all — which is exactly how a green suite hides a broken boundary.
     */
    @Test
    fun theCompilerIsNotOnTheHostClasspath() {
        assertNull(loadable("org.jetbrains.kotlin.psi.KtFile"), "the host must not see the compiler")
        assertNull(loadable("org.antlr.v4.runtime.CharStreams"), "…nor the libraries it shadows")
        assertNotNull(loadable("io.codelaser.maddi.cst.api.info.TypeInfo"), "the CST is shared, so it is here")
    }

    @Test
    fun theFrontEndLoadsInsideTheRealm() {
        val frontEnd = K2Realm.create(k2Classpath)
        // the implementation is realm-loaded: its classloader is NOT the host's
        assertTrue(frontEnd.javaClass.classLoader !== javaClass.classLoader,
            "the front end was loaded by the host, so the realm did nothing")
    }

    @Test
    fun theCstCrossesTheBoundaryAsTheSameClass() {
        val frontEnd = K2Realm.create(k2Classpath)
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/src/")).build()
        val types = frontEnd.sourceScan(runtime, sourceSet, InfoByFqn(), null)
            .parse(mapOf("p/A.kt" to "package p\nclass A(val id: Int) {\n    fun f(): Int = id + 1\n}\n"))

        assertEquals(listOf("p.A"), types.map { it.fullyQualifiedName() })
        // ⭐ the identity that matters: the realm handed back an instance of the HOST's TypeInfo
        val fromRealm: Class<*> = types.first().javaClass
        assertSame(TypeInfo::class.java, TypeInfo::class.java.classLoader
            .loadClass("io.codelaser.maddi.cst.api.info.TypeInfo"))
        assertTrue(TypeInfo::class.java.isAssignableFrom(fromRealm),
            "a realm-loaded TypeInfo would not be assignable to the host's: the CST was loaded twice")
        assertSame(javaClass.classLoader, TypeInfo::class.java.classLoader,
            "the CST must come from the host, not the realm")
    }

    /** A host-side walk over realm-produced CST: the census reads the same tree the front end built. */
    @Test
    fun aHostSideWalkReadsTheRealmsTree() {
        val frontEnd = K2Realm.create(k2Classpath)
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/src/")).build()
        val types = frontEnd.sourceScan(runtime, sourceSet, InfoByFqn(), null)
            .parse(mapOf("p/B.kt" to "package p\nclass B {\n    fun f(c: Boolean): Int = if (c) 1 else 2\n}\n"))
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
    }

    /**
     * ⭐ <b>The invariant, as the front end actually states it:</b> "a type in a dependent set resolves to the
     * very same [TypeInfo] produced for its upstream set". That is a property of one project scan over
     * source sets in dependency order, sharing one registry — and it is precisely what a second copy of the
     * CST classes inside the realm would break, silently.
     *
     * <p>⚠ The first version of this test used two independent in-memory scans and failed with
     * `expected p.U but was java.lang.Object` — not an identity failure at all: a second scan's K2 session
     * has no `U.kt` in it, so nothing resolved. The assertion message blamed the boundary for a mistake in
     * the test. Measure the mechanism you mean.
     */
    @Test
    fun aDependentSourceSetGetsTheSameTypeInfo(@TempDir root: Path) {
        val upstreamDir = Files.createDirectories(root.resolve("up/p"))
        val downstreamDir = Files.createDirectories(root.resolve("down/p"))
        Files.writeString(upstreamDir.resolve("U.kt"), "package p\nclass U(val id: Int)\n")
        Files.writeString(downstreamDir.resolve("D.kt"), "package p\nclass D {\n    fun make(): U = U(1)\n}\n")

        val up = SourceSetImpl.Builder().setName("up").setUri(root.resolve("up").toUri())
            .setSourceDirectories(listOf(root.resolve("up"))).build()
        val down = SourceSetImpl.Builder().setName("down").setUri(root.resolve("down").toUri())
            .setSourceDirectories(listOf(root.resolve("down"))).setDependencies(listOf(up)).build()

        val frontEnd = K2Realm.create(k2Classpath)
        val bySourceSet = frontEnd.projectScan(RuntimeImpl(), InfoByFqn(), null)
            .parse(listOf(up, down), emptyList(), Paths.get(System.getProperty("java.home")),
                emptyList(), emptyList())

        val u = bySourceSet.getValue(up).single { it.fullyQualifiedName() == "p.U" }
        val returned = bySourceSet.getValue(down).single().findUniqueMethod("make", 0).returnType().typeInfo()
        assertSame(u, returned,
            "the dependent set made its own p.U: one TypeInfo per FQN did not survive the boundary")
    }
}
