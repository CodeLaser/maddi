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

import io.codelaser.maddi.cst.api.expression.MethodCall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ⛔ **A wrong callee is worse than a hole.** The census counts what the front end could not read; it cannot
 * count what the front end read WRONGLY, because a guessed overload is a resolved call in the tree like any
 * other. Measured before this tier existed: `s.replace("a", "b")` bound to `java.lang.String.replace(char,
 * char)` — two String literals against two chars — because the exact-type tiers missed and the fallback took
 * whichever overload came first.
 */
class OverloadPickTest : KotlinScanTestBase() {

    private fun callees(source: String, method: String, parameters: Int): List<String> {
        val m = KotlinScan(runtime, sourceSet).parse("P.kt", source.trimIndent() + "\n").first()
        val found = mutableListOf<String>()
        m.findUniqueMethod(method, parameters).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found
    }

    @Test
    fun anArgumentThatCannotBePassedRulesTheOverloadOut() {
        val callees = callees("""
            class P {
                fun f(s: String) = s.replace("a", "b")
            }
            """, "f", 1)
        assertEquals(1, callees.size, callees.toString())
        // ⚠ The pinned property is that a String argument never reaches a `char` parameter — NOT which of
        // the acceptable overloads wins. This asserted `CharSequence` until the library-arity fix landed,
        // after which Kotlin's own `StringsKt.replace(String, String, String, ignoreCase)` extension
        // resolves, which is what kotlinc binds. Both are right; `replace(char, char)` is the defect.
        assertFalse(callees.single().contains("char,char"), callees.single())
        assertTrue(callees.single().contains("CharSequence") || callees.single().contains("StringsKt"),
            "expected an overload a String can be passed to; got ${callees.single()}")
    }

    /** The exact-match tiers still win: an argument of the parameter's own type picks that overload. */
    @Test
    fun anExactMatchIsStillPreferred() {
        val callees = callees("""
            class P {
                fun g(sb: StringBuilder, c: Char) = sb.append(c)
            }
            """, "g", 2)
        assertTrue(callees.single().endsWith("append(char)"), callees.single())
    }

    /** ⚠ The residual guess is counted, since nothing downstream can detect it. */
    @Test
    fun theCounterExists() {
        val scan = KotlinScan(runtime, sourceSet)
        scan.parse("Q.kt", "class Q { fun f(s: String) = s.replace(\"a\", \"b\") }\n")
        assertEquals(0, scan.ambiguousBindings, "this call is decided by assignability, not guessed")
    }
}
