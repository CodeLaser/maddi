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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A test source set sees its main set's `internal` declarations, as kotlinc compiles it (a friend): javalin's tests
 * call `internal object CorsUtils` 21 times, and each was an unresolved reference.
 */
class FriendSourceSetTest : KotlinScanTestBase() {

    @Test
    fun aTestSetSeesMainsInternals(@TempDir root: Path) {
        Files.createDirectories(root.resolve("src/main/kotlin/p")).resolve("Cors.kt").let {
            Files.writeString(it, "package p\ninternal object CorsUtils { fun valid(o: String): Boolean = o.isNotEmpty() }\n")
        }
        Files.createDirectories(root.resolve("src/test/kotlin/p")).resolve("TestCors.kt").let {
            Files.writeString(it, "package p\nclass TestCors { fun check(): Boolean = CorsUtils.valid(\"x\") }\n")
        }
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf(root.resolve("src/main/kotlin"))).build()
        val test = SourceSetImpl.Builder().setName("test").setUri(root.toUri())
            .setSourceDirectories(listOf(root.resolve("src/test/kotlin"))).setDependencies(listOf(main)).build()
        val parsed = KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main, test), emptyList(), Paths.get(System.getProperty("java.home")), emptyList(), listOf())
        val tests: List<TypeInfo> = parsed.getValue(test)
        val census = PlaceholderCensus.of(tests)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        assertEquals("return CorsUtils.INSTANCE.valid(\"x\");", tests.single { it.simpleName() == "TestCors" }
            .methods().single { it.name() == "check" }.methodBody().statements().joinToString(" "))
    }

    /**
     * javalin's TestPlugins: an inner class of a TEST type reads a `@JvmField` its outer class inherits from MAIN. ⚠ Not a
     * discriminating test: it passes with and without the inherited-field lookup in implicitMemberAccess, because here
     * the name resolves by another route; only the javalin corpus showed the failure (§7.71). It guards the shape.
     */
    @Test
    fun anInheritedPropertyThroughTheOuterThis(@TempDir root: Path) {
        Files.createDirectories(root.resolve("src/main/kotlin/p")).resolve("Plugin.kt").let {
            Files.writeString(it, "package p\nabstract class Plugin<C>(d: C?) { @JvmField protected var pluginConfig: C = d as C }\n" +
                "abstract class ContextPlugin<C, E>(d: C?) : Plugin<C>(d) { abstract fun createExtension(context: String): E }\n")
        }
        Files.createDirectories(root.resolve("src/test/kotlin/p")).resolve("TestPlugins.kt").let {
            Files.writeString(it, """
                package p
                class TestPlugins {
                    class Rendy : ContextPlugin<Rendy.Config, Rendy.Extension>(Config()) {
                        override fun createExtension(context: String) = Extension(context)
                        class Config(var directory: String = "...")
                        inner class Extension(var context: String) { fun render(path: String): String = pluginConfig.directory + path }
                    }
                }
                """.trimIndent() + "\n")
        }
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf(root.resolve("src/main/kotlin"))).build()
        val test = SourceSetImpl.Builder().setName("test").setUri(root.toUri())
            .setSourceDirectories(listOf(root.resolve("src/test/kotlin"))).setDependencies(listOf(main)).build()
        val tests = KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main, test), emptyList(), Paths.get(System.getProperty("java.home")), emptyList(), listOf())
            .getValue(test)
        val census = PlaceholderCensus.of(tests)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }
}
