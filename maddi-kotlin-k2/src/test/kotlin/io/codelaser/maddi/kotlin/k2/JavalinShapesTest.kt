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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Shapes from javalin whose placeholders held back fieldCouldBeFinal (a var named in unconverted code is not reported):
 * a destructuring through `Lazy.value` (JettyServer), a generic superclass's protected property read from an `inner`
 * class (TestPlugins' `pluginConfig`), and `Byte.toString()` on a Java method's `byte` (TestWebSocket).
 */
class JavalinShapesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("jv/Jv.kt", """
            package jv
            data class Entry(val init: String?, val servlet: String)
            class Cfg { @JvmField var servlet: Lazy<Entry> = lazy { Entry(null, "s") } }
            abstract class Plugin<C>(defaultConfig: C) { protected var pluginConfig: C = defaultConfig }
            class Conf(var directory: String = "...")
            class Rendy : Plugin<Conf>(Conf()) {
                inner class Extension(var context: String) {
                    fun render(path: String): String = pluginConfig.directory + path + context
                }
            }
            class K {
                fun start(cfg: Cfg): String { val (init, servlet) = cfg.servlet.value; return servlet + init }
                fun third(b: java.nio.ByteBuffer): String = b.get(2).toString()
                fun id(i: Int?): String = if (i != null) i.toString() else ""
            }
            """.trimIndent() + "\n")
    }

    private fun all() = types.flatMap { it.recursiveSubTypeStream().toList() }

    private fun body(type: String, name: String): String =
        all().first { it.simpleName() == type }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            start: String init=cfg.servlet.value.component1(),servlet=cfg.servlet.value.component2(); return servlet+init;
            third: return String.valueOf((int)b.get(2));
            id: return !(i==null)?String.valueOf(i):"";
            render: return this.getPluginConfig().directory+path+this.context;
            """.trimIndent(), "start: " + body("K", "start") + "\nthird: " + body("K", "third") + "\nid: " + body("K", "id") + "\nrender: " + body("Extension", "render"))
    }
}
