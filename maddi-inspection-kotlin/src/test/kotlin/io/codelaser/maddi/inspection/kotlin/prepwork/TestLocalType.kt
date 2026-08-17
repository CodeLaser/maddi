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

package org.e2immu.language.inspection.kotlin.prepwork

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestLocalType.test2`: a method holds a local class whose method
 * captures the enclosing method's parameter (`t`) and reads a Java static field (`System.out`). The
 * `VariableData` of the local-type-declaration statement lists exactly the enclosing variables it captures.
 */
class TestLocalType : CommonKotlinPrep() {

    @Test
    fun test2() {
        val x = parse("X.kt", """
            package a.b
            class X {
                interface A { fun method(s: String) }
                fun make(t: String): A {
                    class C : A {
                        override fun method(s: String) {
                            System.out.println(s + t)
                        }
                    }
                    return C()
                }
            }
        """.trimIndent())
        PrepAnalyzer(runtime).doPrimaryType(x)

        val make = x.findUniqueMethod("make", 1)
        val ltd = make.methodBody().statements().first() as LocalTypeDeclaration
        val vd0 = VariableDataImpl.of(ltd)
        assertEquals("a.b.X.make(String):0:t, java.lang.System.out", vd0.knownVariableNamesToString())
    }
}
