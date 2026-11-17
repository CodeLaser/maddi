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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A local SUSPEND function is a suspend function value: its `invoke` takes the continuation last, as a suspend
 * lambda's does, so a call, which passes the caller's continuation, resolves. An omitted argument whose default is
 * a constant is that constant: a local function has no `$default`. Exposed's `executeBatch` (CodeLaser/maddi#220).
 */
class LocalSuspendFunctionTest : KotlinScanTestBase() {

    private val source = """
        class B { fun add() {} }
        class P {
            suspend fun step() {}
            suspend fun f(xs: List<String>, b: B) {
                suspend fun B.handle(last: Boolean = false, body: B.() -> Unit) {
                    body()
                    if (last) step()
                }
                for (x in xs) {
                    b.handle { add() }
                    b.handle(true) { add() }
                }
            }
        }
        """.trimIndent() + "\n"

    @Test
    fun aLocalSuspendFunctionTakesTheContinuation() {
        val p = KotlinScan(runtime, sourceSet).parse("P.kt", source).first { it.simpleName() == "P" }
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val f = p.findUniqueMethod("f", 3)
        val handle = f.methodBody().statements().filterIsInstance<LocalVariableCreation>().single().localVariable()
        assertEquals("Function4", handle.parameterizedType().typeInfo().simpleName())
        val invokes = mutableListOf<MethodCall>()
        f.methodBody().visit { e: Element -> if (e is MethodCall && e.methodInfo().name() == "invoke" && e.`object`().toString() == "handle") invokes += e; true }
        // receiver, last, body, and the continuation; the omitted `last` is its default, false
        assertEquals(listOf("false", "true"), invokes.map { it.parameterExpressions()[1].toString() }, invokes.toString())
        invokes.forEach { assertEquals(4, it.parameterExpressions().size, it.toString()) }
    }
}
