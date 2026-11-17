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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Exposed's `Table.default`: inside the member extension `fun CompositeColumn<T>.default(v)`, a `with(this@Table)`
 * block calls the OTHER member extension `Column<T>.default(v)` on each real column. The call's dispatch receiver
 * is the `with` lambda's receiver, its extension receiver the cast column (CodeLaser/maddi#220).
 */
class MemberExtensionInWithTest : KotlinScanTestBase() {

    private val source = """
        open class Column<T>
        class Composite<T> { fun parts(): List<Column<Any>> = listOf() }
        class Table {
            fun <T> Column<T>.default(v: T): Column<T> = this
            fun <T> Composite<T>.default(v: T): Composite<T> = apply {
                with(this@Table) {
                    for (c in this@default.parts()) {
                        (c as Column<Any>).default(v as Any)
                    }
                }
            }
        }
        """.trimIndent() + "\n"

    @Test
    fun theDispatchReceiverIsTheWithReceiver() {
        val types = KotlinScan(runtime, sourceSet).parse("T.kt", source)
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val table = types.first { it.simpleName() == "Table" }
        val composite = table.methods().single { it.name() == "default" && it.parameters()[0].parameterizedType().typeInfo()?.simpleName() == "Composite" }
        // `this@default` is the extension receiver; the member extension's dispatch receiver, `this@Table`, the class's `this`
        assertEquals("{{{for(Column<Object> c:\$receiver.parts()){default((Column<Object>)c,(Object)v);}}}return \$receiver;}",
            composite.methodBody().toString())
    }
}
