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
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * <b>A call through an import alias binds to the aliased declaration.</b> `import a.wrap as topLevelWrap` makes
 * `topLevelWrap(t)` a call of `wrap`; the converter looked the written name up, found nothing, and made the call a
 * placeholder with its arguments (Exposed's SQLExpressionBuilder, which imports `wrap`, `rem`, `inList` and
 * `notInList` under aliases to get past its own same-named members).
 */
class ImportAliasTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    private fun callees(types: List<TypeInfo>, method: String): List<String> {
        val found = mutableListOf<String>()
        types.first { it.simpleName() == "P" }.findUniqueMethod(method, 1).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().name())
            true
        }
        return found
    }

    private val source = """
        package a
        import a.wrap as topLevelWrap
        import a.rem as topLevelRem
        fun wrap(x: Int): Int = x + 1
        infix fun Int.rem(y: Int): Int = this % y
        class P {
            fun wrap(): Int = 0
            fun call(t: Int) = topLevelWrap(t)
            fun infix(t: Int) = t topLevelRem 3
        }
    """.trimIndent()

    @Test
    fun anAliasedCallBindsToTheDeclaration() {
        val types = parse(source)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals(listOf("wrap"), callees(types, "call"))
        assertEquals(listOf("rem"), callees(types, "infix"))
    }
}
