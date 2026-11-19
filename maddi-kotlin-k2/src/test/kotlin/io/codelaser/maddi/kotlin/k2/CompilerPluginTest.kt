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
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * The compiler plugins a source set's kotlinc applied ([CompilerPlugins]), as Gradle's kotlin-dsl applies its two, with
 * the fixture's own annotations in Gradle's place: sam-with-receiver makes an `Action<T>` lambda's parameter its
 * implicit receiver, assignment reads `p = v` on a `Property` as `p.assign(v)`. Without the options, the same code does
 * not resolve: the control.
 */
class CompilerPluginTest : KotlinScanTestBase() {

    private val source = """
        package gp
        annotation class HasImplicitReceiver
        annotation class SupportsAssignment
        @HasImplicitReceiver
        fun interface Action<T> { fun execute(t: T) }
        @SupportsAssignment
        class Property<T> { var v: T? = null; fun set(value: T) { v = value } }
        fun <T> Property<T>.assign(value: T) = set(value)
        class Task { val group: Property<String> = Property(); fun describe(s: String) {} }
        fun configure(t: Task, a: Action<Task>) { a.execute(t) }
        class B {
            fun sam(t: Task) { configure(t) { describe("x") } }
            fun assignment(t: Task) { t.group = "y" }
        }
        """.trimIndent() + "\n"

    private fun parse(options: List<String>): List<TypeInfo> {
        val set = SourceSetImpl.Builder().setName("build-logic").setUri(URI.create("file:/"))
            .setKotlinPluginOptions(options).build()
        return KotlinScan(runtime, set).parse("gp/Gp.kt", source)
    }

    private fun body(types: List<TypeInfo>, name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun withThePlugins() {
        val types = parse(listOf("plugin:${CompilerPlugins.SAM_WITH_RECEIVER}:annotation=gp.HasImplicitReceiver",
            "plugin:${CompilerPlugins.ASSIGNMENT}:annotation=gp.SupportsAssignment"))
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        assertEquals("GpKt.configure(t,${'$'}receiver->${'$'}receiver.describe(\"x\"));", body(types, "sam"))
        assertEquals("GpKt.assign(t.group,\"y\");", body(types, "assignment"))
    }

    @Test
    fun withoutThemTheControl() {
        val types = parse(listOf())
        // the assignment converts as a write of the `val` itself, wrong but no placeholder; the corpora's, through a
        // getter on a Gradle type, were `k2-assign-target`
        assertEquals(1, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
    }
}
