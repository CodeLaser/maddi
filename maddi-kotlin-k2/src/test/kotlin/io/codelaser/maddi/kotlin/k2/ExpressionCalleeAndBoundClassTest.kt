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
 * Three shapes from Exposed (maddi#220), each a placeholder that hid what it read:
 * - a callee that is an expression, `defaultValueFun!!()`: the value's `invoke()`;
 * - `x::class` / `x::class.java` on a value: the object's `getClass()`;
 * - `statementCount++` on an inherited `var`, read through its getter: `setStatementCount(getStatementCount() + 1)`.
 */
class ExpressionCalleeAndBoundClassTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    @Test
    fun theThreeShapesConvert() {
        val types = parse("""
            package a
            abstract class Base { var statementCount: Int = 0 }
            class P(private val defaultValueFun: (() -> String)?) : Base() {
                fun callee(): String = defaultValueFun!!()
                fun kClass(e: Any): String? = e::class.simpleName
                fun jClass(): Class<*> = this::class.java
                fun count() { statementCount++ }
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val body = { name: String -> p.methods().first { it.name() == name }.methodBody().toString() }
        assertEquals("{return this.defaultValueFun.invoke();}", body("callee"))
        assertEquals("{return Reflection.getOrCreateKotlinClass(e.getClass()).simpleName;}", body("kClass"))
        assertEquals("{return this.getClass();}", body("jClass"))
        assertEquals("{this.setStatementCount(this.getStatementCount()+1);}", body("count"))
    }
    /** `==` with no `equals` to bind on the left -- a type parameter -- is kotlinc's `Intrinsics.areEqual(a, b)`. */
    @Test
    fun anEqualityWithoutAnEqualsIsAreEqual() {
        val types = parse("""
            package a
            object Marker
            class P {
                fun <T> changed(current: T?, value: T): Boolean = current != value
                fun marked(raw: Any?): Boolean = raw == Marker
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals("{return !Intrinsics.areEqual(current,value);}", p.findUniqueMethod("changed", 2).methodBody().toString())
    }
}
