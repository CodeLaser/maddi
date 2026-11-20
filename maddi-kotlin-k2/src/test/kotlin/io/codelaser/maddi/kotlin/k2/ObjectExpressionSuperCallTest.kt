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

import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * <b>`object : Point(6) { … }` calls the superclass constructor</b> with its arguments, as the Java front end records
 * `new Point(6) { … }` (CodeLaser/maddi#194). The call was built with no constructor and no arguments, so the
 * arguments' code was invisible to every client (CodeLaser/maddi#213).
 */
class ObjectExpressionSuperCallTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    private fun creation(types: List<TypeInfo>, method: String, arity: Int): ConstructorCall {
        var found: ConstructorCall? = null
        types.first { it.simpleName() == "P" }.findUniqueMethod(method, arity).methodBody().visit { e ->
            if (e is ConstructorCall && e.anonymousClass() != null) found = e
            true
        }
        return found!!
    }

    @Test
    fun theSuperConstructorAndItsArgumentsAreKept() {
        val types = parse("""
            package a
            open class Point(val x: Int, val label: String = "p")
            interface Shape { fun area(): Int }
            class P {
                fun point(n: Int): Point = object : Point(n * 2) { override fun toString() = "q" }
                fun named(n: Int): Point = object : Point(label = "l", x = n) {}
                fun shape(): Shape = object : Shape { override fun area() = 1 }
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        // an omitted default: the `$default` constructor, with the mask, as any call omitting one
        val point = creation(types, "point", 1)
        assertEquals("a.Point.<init>(int,String,int,kotlin.jvm.internal.DefaultConstructorMarker)", point.constructor().fullyQualifiedName())
        assertEquals("[n*2, null, 2, null]", point.parameterExpressions().toString())
        // named arguments, in parameter order
        val named = creation(types, "named", 1)
        assertEquals("a.Point.<init>(int,String)", named.constructor().fullyQualifiedName())
        assertEquals(2, named.parameterExpressions().size)
        // interfaces only: Object's constructor, when the runtime has one; never arguments
        val shape = creation(types, "shape", 0)
        assertEquals(0, shape.parameterExpressions().size)
        shape.constructor()?.let { assertEquals("java.lang.Object.<init>()", it.fullyQualifiedName()) }
    }
}
