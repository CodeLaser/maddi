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

import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A written accessor of a property with a backing field is converted: its body, where `field` is the backing field,
 * and a setter's parameter as named. It is not tagged as the field's getter/setter, which would let the analyzer
 * read `obj.n = 5` as a plain field write and skip the body. A default accessor is still synthesized and tagged.
 */
class CustomAccessorTest : KotlinScanTestBase() {

    private val source = """
        package a

        class P {
            var n: Int = 0
                set(v) { field = maxOf(v, min()) }
            val s: String = " x "
                get() = field.trim()
            private var hidden: Int = 1
                get() = field + min()
            var plain: Int = 2
            fun min() = 0
        }
        """.trimIndent() + "\n"

    private lateinit var p: TypeInfo

    private fun parse() {
        p = KotlinScan(runtime, sourceSet).parse("a/P.kt", source).single { it.simpleName() == "P" }
    }

    private fun method(name: String): MethodInfo = p.methods().single { it.name() == name }

    @Test
    fun aWrittenSetterIsItsBody() {
        parse()
        val setN = method("setN")
        assertEquals("v", setN.parameters().single().name())
        val assignment = (setN.methodBody().statements().single() as ExpressionAsStatement).expression() as Assignment
        assertEquals("n", (assignment.variableTarget() as FieldReference).fieldInfo().name())
        assertTrue(assignment.value() is MethodCall, assignment.value().toString())
        assertNull(setN.getSetField().field(), "not a plain setter")
        val min = method("min")
        assertEquals(listOf("5:35"), setN.source().detailedSources().references(min)
            .map { "${it.beginLine()}:${it.beginPos()}" }, "recorded on the setter")
        assertEquals(listOf("5:32"), setN.source().detailedSources().references(setN.parameters().single())
            .map { "${it.beginLine()}:${it.beginPos()}" })
    }

    @Test
    fun aWrittenGetterIsItsBody() {
        parse()
        val getS = method("getS")
        val returned = (getS.methodBody().statements().single() as ReturnStatement).expression() as MethodCall
        assertEquals("trim", returned.methodInfo().name())
        assertNull(getS.getSetField().field())
    }

    @Test
    fun aPrivatePropertysWrittenGetterIsAMethod() {
        parse()
        val getHidden = method("getHidden")
        assertTrue(getHidden.access().isPrivate(), getHidden.access().toString())
        assertEquals(1, getHidden.methodBody().statements().size)
    }

    @Test
    fun aDefaultAccessorIsStillSynthesized() {
        parse()
        assertEquals("plain", method("getPlain").getSetField().field().name())
        assertEquals("plain", method("setPlain").getSetField().field().name())
    }
}
