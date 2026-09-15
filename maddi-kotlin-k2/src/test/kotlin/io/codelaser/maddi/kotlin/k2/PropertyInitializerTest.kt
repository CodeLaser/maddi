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

import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.expression.EmptyExpression
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A property initializer is converted into its field's initializer, in the context kotlinc compiles it into: an
 * instance property's primary constructor (a companion's included), a top-level property's facade static initializer,
 * an `object :` expression's instance initializer. That context is what an `object :` expression or a lambda in the
 * initializer is enclosed by -- and until initializers were converted, such an `object :` and the overrides it
 * declares did not exist in the CST at all.
 */
class PropertyInitializerTest : KotlinScanTestBase() {

    private val source = """
        package p

        interface Greeter {
            fun hi(): String
        }
        fun helper(): Int = 1

        class A(n: Int) {
            val doubled = n * 2
            val greeter: Greeter = object : Greeter {
                override fun hi(): String = "a"
            }
        }
        class B {
            companion object {
                fun make(): Int = 2
                val made = make()
                val empty: Greeter = object : Greeter {
                    override fun hi(): String = "b"
                }
            }
        }
        val top: Greeter = object : Greeter {
            override fun hi(): String = "t"
        }
        private val count = helper()
        fun f(): Greeter = object : Greeter {
            val x = helper()
            override fun hi(): String = "f" + x
        }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("p/P.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun initializer(type: String, field: String): Expression =
        type(type).fields().single { it.name() == field }.initializer()

    /** The anonymous type an initializer creates, and its `hi` override. */
    private fun anonymousHi(initializer: Expression): MethodInfo {
        val anonymous = (initializer as ConstructorCall).anonymousClass()
        return anonymous.methods().single { it.name() == "hi" }
    }

    private val greeterHi get() = type("Greeter").methods().single { it.name() == "hi" }

    @Test
    fun anInstanceInitializerIsCodeOfThePrimaryConstructor() {
        parse()
        val constructor = type("A").constructors().single()
        val doubled = initializer("A", "doubled")
        assertFalse(doubled is EmptyExpression, "$doubled")
        // `n` is a constructor parameter, not a property: it resolves as one, which it only can in the constructor
        val reads = ArrayList<ParameterInfo>()
        doubled.visit { e -> ((e as? VariableExpression)?.variable() as? ParameterInfo)?.let { reads += it }; true }
        assertEquals(listOf(constructor.parameters().single()), reads)

        val hi = anonymousHi(initializer("A", "greeter"))
        assertSame(constructor, hi.typeInfo().enclosingMethod())
        assertEquals(setOf(greeterHi), hi.overrides())
        assertEquals(11, hi.source().detailedSources().detail(hi.name()).beginLine())
    }

    @Test
    fun aCompanionHasItsPrivateConstructorAndItsInitializersRunThere() {
        parse()
        val companion = type("Companion")
        val constructor = companion.constructors().single()
        assertTrue(constructor.isSynthetic && constructor.access().isPrivate, "$constructor")

        val made = initializer("Companion", "made")
        assertEquals("make", (made as MethodCall).methodInfo().name())

        val hi = anonymousHi(initializer("Companion", "empty"))
        assertSame(constructor, hi.typeInfo().enclosingMethod())
        assertEquals(setOf(greeterHi), hi.overrides())
    }

    @Test
    fun aTopLevelInitializerIsCodeOfTheFacadesStaticInitializer() {
        parse()
        val facade = type("PKt")
        val staticInitializer = facade.methods().single { it.isStaticInitializer }
        assertTrue(staticInitializer.isSynthetic)

        val hi = anonymousHi(initializer("PKt", "top"))
        assertSame(staticInitializer, hi.typeInfo().enclosingMethod())
        assertEquals(setOf(greeterHi), hi.overrides())
        assertEquals("helper", (initializer("PKt", "count") as MethodCall).methodInfo().name())
    }

    @Test
    fun anObjectExpressionsInitializerIsCodeOfItsInstanceInitializer() {
        parse()
        var anonymous: TypeInfo? = null
        type("PKt").methods().single { it.name() == "f" }.methodBody().visit { e ->
            (e as? ConstructorCall)?.anonymousClass()?.let { anonymous = it }
            true
        }
        val x = anonymous!!.fields().single { it.name() == "x" }.initializer()
        assertEquals("helper", (x as MethodCall).methodInfo().name())
        assertTrue(anonymous!!.methods().single { it.isInstanceInitializer }.isSynthetic)
    }

    @Test
    fun everythingIsCommitted() {
        parse()
        val all = ArrayList<TypeInfo>()
        types.flatMap { it.recursiveSubTypeStream().toList() }.forEach { t ->
            all += t
            t.fields().forEach { f -> f.initializer().visit { e -> (e as? ConstructorCall)?.anonymousClass()?.let { all += it }; true } }
        }
        val members = all.flatMap { t -> listOf<Info>(t) + t.methods() + t.fields() + t.constructors() }
        assertTrue(members.all { it.hasBeenInspected() }, "uncommitted: ${members.filterNot { it.hasBeenInspected() }}")
    }
}
