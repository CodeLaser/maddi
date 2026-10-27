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
import io.codelaser.maddi.cst.api.expression.MethodReference
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Same-arity constructor overloads are told apart by the constructor K2 resolved, never by which one the JDK model
 * happens to list first. `ArrayList(xs)` bound `ArrayList(int)` once the shared-JDK materialisation reordered the
 * overloads: a CST that compiles, counts no placeholder, and links nothing to `xs`. (`java.util.ArrayList` is
 * imported: a reference to the `kotlin.collections.ArrayList` typealias's constructor is a counted placeholder.)
 */
class ConstructorOverloadTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("co/Co.kt", """
            package co
            import java.util.ArrayList
            class C {
                fun copy(xs: List<String>): ArrayList<String> = ArrayList(xs)
                fun sized(): ArrayList<String> = ArrayList(10)
                fun empty(): ArrayList<String> = ArrayList()
                fun ref(): (Collection<String>) -> ArrayList<String> = ::ArrayList
            }
            """.trimIndent() + "\n")
    }

    private fun constructor(method: String): String {
        var found: String? = null
        types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "C" }
            .methods().first { it.name() == method }.methodBody().visit { e ->
                if (found == null) when (e) {
                    is ConstructorCall -> found = e.constructor()?.fullyQualifiedName()
                    is MethodReference -> found = e.methodInfo().fullyQualifiedName()
                }
                true
            }
        return found!!
    }

    @Test
    fun theResolvedOverload() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        assertEquals("java.util.ArrayList.<init>(java.util.Collection)", constructor("copy"))
        assertEquals("java.util.ArrayList.<init>(int)", constructor("sized"))
        assertEquals("java.util.ArrayList.<init>()", constructor("empty"))
        assertEquals("java.util.ArrayList.<init>(java.util.Collection)", constructor("ref"))
    }
}
