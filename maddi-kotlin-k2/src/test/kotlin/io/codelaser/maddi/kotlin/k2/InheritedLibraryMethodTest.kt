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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * <b>A call through a library subtype binds to the method the supertype declares</b>, as it does when the type is
 * read from its class file. A library type built from K2 symbols (this fixture has no `CompiledTypesManager`) used to
 * copy every inherited function onto itself, so the call bound to a copy owned by the subtype, and the supertype's
 * annotated-API contracts never reached it: `Json.Default.encodeToString(..)` against `Json`'s (CodeLaser/maddi#218).
 */
class InheritedLibraryMethodTest : KotlinScanTestBase() {

    private fun parse(body: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse("P.kt", "class P {\n$body\n}\n").first()

    private fun callees(type: TypeInfo, method: String, parameters: Int): List<String> {
        val found = mutableListOf<String>()
        type.findUniqueMethod(method, parameters).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found
    }

    /** `FileReader` declares no `read()`: `InputStreamReader` does. */
    @Test
    fun anInheritedMethodBindsToItsDeclaringSupertype() {
        val p = parse("    fun f(r: java.io.FileReader) = r.read()")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        assertEquals(listOf("java.io.InputStreamReader.read()"), callees(p, "f", 1))
    }

    /** ...while `kotlin.Any`'s members stay on the subtype: the predefined `java.lang.Object` has none of them. */
    @Test
    fun anAnyMemberStillResolves() {
        val p = parse("    fun f(r: java.io.FileReader) = r.hashCode()")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        assertEquals(1, callees(p, "f", 1).size)
    }
}
