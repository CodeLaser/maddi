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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Return-type-differentiated overloads: two functions with identical erased params (both take a `Function1`)
 * that differ only by return type — legal at the JVM bytecode level, used by kotlin-stdlib's inline numeric
 * specializations (`maxOf((T)->Double):Double` vs `:Float`). The front-end must keep BOTH (part 1: MethodMapImpl
 * nests by return type) and resolve a call to the right one (part 2: resolveCallee disambiguates by return type).
 */
class TestReturnTypeOverload {

    @Test
    fun bothOverloadsSurviveAndResolveByReturnType() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types: List<TypeInfo> = inspector.parse(
            sourceSet, mapOf(
                "A.kt" to """
                    package p
                    inline fun g(s: (Int) -> Double): Double = s(0)
                    inline fun g(s: (Int) -> Float): Float = s(0)
                    fun useD(): Double { val f: (Int) -> Double = { 1.0 }; return g(f) }
                    fun useF(): Float { val f: (Int) -> Float = { 1.0f }; return g(f) }
                """.trimIndent()
            )
        )

        val facade = types.first { it.simpleName() == "AKt" }
        // part 1: both g overloads are stored (they would have collided as "Two methods with the same FQN")
        assertEquals(2, facade.methods().count { it.name() == "g" })

        // part 2: the call in useD resolves to the Double-returning g, the call in useF to the Float-returning g
        assertEquals("double", callReturnFqn(facade, "useD"))
        assertEquals("float", callReturnFqn(facade, "useF"))
    }

    private fun callReturnFqn(facade: TypeInfo, methodName: String): String {
        val method = facade.findUniqueMethod(methodName, 0)
        val ret = method.methodBody().statements().filterIsInstance<ReturnStatement>().first()
        val call = ret.expression() as MethodCall
        return call.methodInfo().returnType().fullyQualifiedName()
    }
}
