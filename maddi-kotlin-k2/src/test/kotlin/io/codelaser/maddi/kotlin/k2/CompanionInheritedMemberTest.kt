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
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * <b>A companion's inherited property, read through the class name.</b> Exposed declares
 * `abstract class DialectNameProvider(val dialectName: String)` and `companion object : DialectNameProvider("H2")` on
 * each dialect, then reads `H2Dialect.dialectName`: 74 placeholders on that corpus, the largest single kind.
 */
class CompanionInheritedMemberTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    @Test
    fun anInheritedCompanionPropertyResolves() {
        val types = parse("""
            package a
            abstract class Base {
                abstract class NameProvider(val dialectName: String)
            }
            class H2 : Base() {
                companion object : NameProvider("H2")
            }
            class P {
                fun name(): String = H2.dialectName
                val names = mapOf("h2" to H2.dialectName)
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
    }

    /** ...and the companion's model has the supertype: a direct one, read both through the class and the companion. */
    @Test
    fun theCompanionHasItsDeclaredParent() {
        val types = parse("""
            package a
            abstract class NameProvider(val dialectName: String)
            class H2 {
                companion object : NameProvider("H2")
            }
            class P {
                fun name(): String = H2.dialectName
                fun name2(): String = H2.Companion.dialectName
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val companion = types.first { it.simpleName() == "H2" }.subTypes().single()
        assertEquals("a.NameProvider", companion.parentClass().typeInfo().fullyQualifiedName())
    }
    /** The companion's private constructor calls the supertype's, as statement "0", before an init block. */
    @Test
    fun theCompanionConstructorCallsTheSuperConstructor() {
        val types = parse("""
            package a
            abstract class NameProvider(val dialectName: String)
            class H2 {
                companion object : NameProvider("H2") {
                    init { println(dialectName) }
                }
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val constructor = types.first { it.simpleName() == "H2" }.subTypes().single().constructors().single()
        val statements = constructor.methodBody().statements()
        val superCall = statements.first() as ExplicitConstructorInvocation
        assertTrue(superCall.isSuper)
        assertEquals("a.NameProvider.<init>(String)", superCall.methodInfo().fullyQualifiedName())
        assertEquals(2, statements.size, statements.toString())
    }
}
