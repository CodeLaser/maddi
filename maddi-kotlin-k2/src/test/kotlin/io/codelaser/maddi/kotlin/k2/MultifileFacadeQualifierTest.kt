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
import io.codelaser.maddi.cst.api.expression.TypeExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * #67 item 4: a stdlib top-level function lives in a package-private multifile PART
 * (`CollectionsKt__CollectionsKt`), which Java cannot name. A call is qualified by the public FACADE
 * (`CollectionsKt`), as the Java front end qualifies a Java call to it; the method stays the part's, where it is
 * declared, which javac binds and the kotlin archive contracts. A single-file facade (a source `MfKt`) is its own.
 */
class MultifileFacadeQualifierTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("mf/Mf.kt", """
            package mf
            class K {
                fun make(a: StringBuilder): List<StringBuilder> = mutableListOf(a)
                fun copy(xs: List<StringBuilder>): List<StringBuilder> = xs.toList()
                fun say(s: String) { top(s) }
            }
            fun top(s: String): Int = 1
            """.trimIndent() + "\n")
    }

    private fun method(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }

    private fun call(name: String): MethodCall {
        var found: MethodCall? = null
        method(name).methodBody().visit { e -> if (found == null && e is MethodCall) found = e; true }
        return found!!
    }

    @Test
    fun qualifiedByTheFacade() {
        assertEquals("return CollectionsKt.mutableListOf(a);", method("make").methodBody().statements().single().toString())
        assertEquals("return CollectionsKt.toList(xs);", method("copy").methodBody().statements().single().toString())
        assertEquals("MfKt.top(s);", method("say").methodBody().statements().single().toString())
    }

    @Test
    fun theMethodStaysThePartsAndTheFacadeExtendsIt() {
        val make = call("make")
        val facade = (make.`object`() as TypeExpression).parameterizedType().typeInfo()
        assertEquals("kotlin.collections.CollectionsKt", facade.fullyQualifiedName())
        assertEquals("kotlin.collections.CollectionsKt__CollectionsKt", make.methodInfo().typeInfo().fullyQualifiedName())
        assertTrue(facade.access().isPublic, facade.access().toString())
        // the same facade for a function of another part of the group
        val copy = call("copy")
        assertEquals("kotlin.collections.CollectionsKt___CollectionsKt", copy.methodInfo().typeInfo().fullyQualifiedName())
        assertEquals(facade, (copy.`object`() as TypeExpression).parameterizedType().typeInfo())
    }
}
