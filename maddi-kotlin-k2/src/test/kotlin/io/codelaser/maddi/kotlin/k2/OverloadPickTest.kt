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
