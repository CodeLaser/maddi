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
