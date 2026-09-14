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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.Block
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.cst.api.statement.Statement
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * An `init` block is code of the constructor kotlinc compiles it into -- the primary one, else the first that calls
 * super -- appended to its body as a nested block, after the `this(...)`/`super(...)` invocation and the
 * parameter-property assignments. Until it was converted, an assignment in one was invisible (a `val` assigned in
 * `init` looked never assigned), and so was an `object :` expression in one and the overrides it declares.
 */
class InitBlockTest : KotlinScanTestBase() {

    private val source = """
        package q

        interface Greeter {
            fun hi(): String
        }
        open class Base(val v: Int)

        class A(n: Int) {
            val x: Int
            init {
                x = n + 1
                val g = object : Greeter {
                    override fun hi(): String = "a"
                }
                g.hi()
            }
            val y = 2
            init {
                println(y)
            }
        }
        class B(val m: Int) : Base(1) {
            init {
                println(m)
            }
        }
        class C {
            companion object {
                val z: Int
                init {
                    z = 3
                }
            }
        }
        object O {
            val w: Int
            init {
                w = 4
            }
        }
        fun f(): Greeter = object : Greeter {
            val q: Int
            init {
                q = 5
            }
            override fun hi(): String = "f"
        }
        class S {
            val s: Int
            constructor(a: Int) : this(a, 0)
            constructor(a: Int, b: Int) {
                println(b)
            }
            init {
                s = 6
            }
        }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("q/Q.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    /** The fields [element] assigns. */
    private fun assigned(element: Element): List<String> {
        val names = ArrayList<String>()
        element.visit { e -> ((e as? Assignment)?.variableTarget() as? FieldReference)?.let { names += it.fieldInfo().name() }; true }
        return names
    }

    private fun <T> collect(element: Element, pick: (Element) -> T?): List<T> {
        val found = ArrayList<T>()
        element.visit { e -> pick(e)?.let { found += it }; true }
        return found
    }

    /** Every statement's index extends its enclosing statement's, and a block's are in order. */
    private fun assertIndices(block: Block, parent: String) {
        val indices = block.statements().map { it.source().index() }
        indices.forEach { assertTrue(parent.isEmpty() || it.startsWith("$parent."), "$it under $parent") }
        assertEquals(indices.sorted(), indices)
        block.statements().filterIsInstance<Block>().forEach { assertIndices(it, it.source().index()) }
    }

    @Test
    fun anInitBlockIsANestedBlockOfThePrimaryConstructor() {
        parse()
        val constructor = type("A").constructors().single()
        val body = constructor.methodBody()
        assertEquals(2, body.statements().size, "$body")
        assertTrue(body.statements().all { it is Block })
        assertIndices(body, "")
        assertEquals(listOf("x"), assigned(body.statements()[0]))
        // `n` is the constructor's parameter
        val reads = collect(body.statements()[0]) { (it as? io.codelaser.maddi.cst.api.expression.VariableExpression)?.variable() as? ParameterInfo }
        assertEquals(listOf(constructor.parameters().single()), reads)
    }

    @Test
    fun anObjectExpressionInAnInitBlockExistsAndIsReferenced() {
        parse()
        val constructor = type("A").constructors().single()
        val anonymous = collect(constructor.methodBody()) { (it as? ConstructorCall)?.anonymousClass() }.single()
        val hi = anonymous.methods().single { it.name() == "hi" }
        assertSame(constructor, anonymous.enclosingMethod())
        assertEquals(setOf(type("Greeter").methods().single { it.name() == "hi" }), hi.overrides())
        // `g.hi()` names the object's override, which exists before references are recorded: the block is
        // converted in pass B1, and the reference is recorded on the constructor
        assertEquals(listOf("15:11"), constructor.source().detailedSources().references(hi)
            .map { "${it.beginLine()}:${it.beginPos()}" })
    }

    @Test
    fun theInitBlockFollowsTheInvocationAndTheParameterProperties() {
        parse()
        val body = type("B").constructors().single().methodBody()
        val statements: List<Statement> = body.statements()
        assertEquals(3, statements.size, "$body")
        assertTrue(statements[0] is ExplicitConstructorInvocation, "${statements[0]}")
        assertEquals(listOf("m"), assigned(statements[1]))
        assertTrue(statements[2] is Block)
        assertIndices(body, "")
    }

    @Test
    fun aCompanionsInitBlockIsInItsConstructor() {
        parse()
        assertEquals(listOf("z"), assigned(type("Companion").constructors().single().methodBody()))
    }

    @Test
    fun anObjectsInitBlockIsInItsConstructor() {
        parse()
        val o = type("O")
        val holder: MethodInfo = o.constructors().firstOrNull() ?: o.methods().single { it.isInstanceInitializer }
        assertEquals(listOf("w"), assigned(holder.methodBody()))
    }

    @Test
    fun anObjectExpressionsInitBlockIsItsInstanceInitializer() {
        parse()
        val f = type("QKt").methods().single { it.name() == "f" }
        val anonymous = collect(f.methodBody()) { (it as? ConstructorCall)?.anonymousClass() }.single()
        assertEquals(listOf("q"), assigned(anonymous.methods().single { it.isInstanceInitializer }.methodBody()))
    }

    @Test
    fun withoutAPrimaryConstructorItIsTheFirstThatCallsSuper() {
        parse()
        val (delegating, callsSuper) = type("S").constructors().sortedBy { it.parameters().size }
        assertEquals(listOf<String>(), assigned(delegating.methodBody()))
        assertEquals(listOf("s"), assigned(callsSuper.methodBody()))
    }

    @Test
    fun everythingIsCommitted() {
        parse()
        val all = ArrayList<TypeInfo>()
        types.flatMap { it.recursiveSubTypeStream().toList() }.forEach { t ->
            all += t
            t.constructorAndMethodStream().forEach { m -> all += collect(m.methodBody()) { (it as? ConstructorCall)?.anonymousClass() } }
        }
        val members = all.flatMap { t -> listOf<Info>(t) + t.methods() + t.fields() + t.constructors() }
        assertTrue(members.all { it.hasBeenInspected() }, "uncommitted: ${members.filterNot { it.hasBeenInspected() }}")
    }
}
