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

import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A library class this front end loads keeps the visibility its members were declared with. K2 calls Java's
 * `protected` PACKAGE_PROTECTED, not PROTECTED, and every protected library method used to come out package-private;
 * library fields came out public whatever they were.
 */
class LibraryVisibilityTest : KotlinScanTestBase() {

    @Test
    fun aJavaProtectedMemberStaysProtected() {
        val types = KotlinScan(runtime, sourceSet).parse("l/L.kt", """
            package l

            class L {
                fun list(): java.util.AbstractList<String>? = null
            }
            """.trimIndent() + "\n")
        val abstractList = types.first().findUniqueMethod("list", 0).returnType().typeInfo()
        assertEquals("java.util.AbstractList", abstractList.fullyQualifiedName())
        val removeRange = abstractList.methods().first { it.name() == "removeRange" }
        assertEquals(runtime.accessProtected(), removeRange.access(), "$removeRange")
        val modCount = abstractList.getFieldByName("modCount", true)
        assertEquals(runtime.accessProtected(), modCount.access(), "$modCount")
        assertEquals(runtime.accessPublic(), abstractList.methods().first { it.name() == "iterator" }.access())
    }

    /**
     * A Java class's INSTANCE field, read from a Kotlin subclass in each spelling. The front end loaded a library
     * class's static fields and Kotlin properties but not its instance fields, and looked a name up only among the
     * type's OWN fields: `modCount` was a placeholder three times over.
     */
    @Test
    fun anInheritedJavaFieldResolves() {
        val types = KotlinScan(runtime, sourceSet).parse("l/L.kt", """
            package l

            class L : java.util.AbstractList<String>() {
                override val size: Int get() = 0
                override fun get(index: Int): String = ""
                fun bare(): Int = modCount
                fun qualified(o: L): Int = o.modCount
                fun viaThis(): Int = this.modCount
            }
            """.trimIndent() + "\n")
        val l = types.first { it.simpleName() == "L" }
        for ((name, arity) in listOf("bare" to 0, "qualified" to 1, "viaThis" to 0)) {
            val returned = (l.findUniqueMethod(name, arity).methodBody().statements().single() as ReturnStatement)
                .expression()
            val field = ((returned as? VariableExpression)?.variable() as? FieldReference)?.fieldInfo()
            assertEquals("java.util.AbstractList.modCount", field?.fullyQualifiedName(), "$name: $returned")
        }
    }
}
