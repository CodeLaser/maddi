package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A member function of an `object :` expression captures the enclosing method's parameters and locals, as a Java
 * anonymous class does. Before, `child.sup = x` read `child` as an unresolved reference, and the modification of the
 * captured parameter was lost (fernflower's NestedClassProcessor, maddi-mod#145).
 */
class ObjectLiteralCaptureTest : KotlinScanTestBase() {

    @Test
    fun aMemberOfAnObjectExpressionSeesTheEnclosingParametersAndLocals() {
        val types = KotlinScan(runtime, sourceSet).parse("ob/Ob.kt", """
            package ob
            class Box { var n: Int = 0 }
            class Node { var sup: Box? = null }
            interface Plain { fun f(x: Box?): Box? }
            class Walk { fun eachPlain(it: Plain) { } }
            class User {
                fun viaObject(w: Walk, child: Node) {
                    val local = Node()
                    w.eachPlain(object : Plain { override fun f(x: Box?): Box? { child.sup = x; local.sup = x; return x } })
                }
            }
            """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val user = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "User" }
        val method = user.methods().first { it.name() == "viaObject" }
        assertEquals("Node local=new Node(); w.eachPlain(new Plain(){public Box f(Box x){child.sup=x;local.sup=x;return x;}});",
            method.methodBody().statements().joinToString(" "))
    }

    /**
     * Its property initializers and `init` blocks capture them too: `val v: S = e` was `v = <unresolved e>`, and
     * fernflower's SFormsFastMapDirect.entryList returned entries linked to nothing (maddi-mod#145).
     */
    @Test
    fun anInitializerOfAnObjectExpressionSeesTheEnclosingLocals() {
        val types = KotlinScan(runtime, sourceSet).parse("ob/Ob.kt", """
            package ob
            class S { var n: Int = 0 }
            interface Holder { fun get(): S }
            class User {
                fun viaInitializer(xs: List<S>): List<Holder> {
                    val out = ArrayList<Holder>()
                    for (e in xs) {
                        val k = e.n
                        out.add(object : Holder {
                            val v: S = e
                            var m: Int = 0
                            init { m = k + xs.size }
                            override fun get(): S = v
                        })
                    }
                    return out
                }
            }
            """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val anon = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "User" }
            .methods().first { it.name() == "viaInitializer" }.let { m ->
                var found: io.codelaser.maddi.cst.api.info.TypeInfo? = null
                m.methodBody().visit { e ->
                    if (e is io.codelaser.maddi.cst.api.expression.ConstructorCall && e.anonymousClass() != null) found = e.anonymousClass()
                    true
                }
                found!!
            }
        assertEquals("e", anon.fields().first { it.name() == "v" }.initializer().toString())
        assertEquals("{this.m=k+xs.size;}", anon.methods().first { it.isInstanceInitializer }.methodBody().statements().joinToString(" "))
    }
}
