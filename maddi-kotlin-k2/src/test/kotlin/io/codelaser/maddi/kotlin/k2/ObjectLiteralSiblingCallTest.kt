package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A member of an `object : … { … }` that calls a sibling declared after it, or itself. Each method's body was
 * converted as soon as its own signature existed, so `return r(e)` reached `r` before it was declared and came out as
 * `return;`: fernflower's NestedClassProcessor iterator lost every modification its `replaceExprent` made
 * (maddi-mod#145). All signatures now exist before the first body, as for a class's members.
 */
class ObjectLiteralSiblingCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ob/Ob.kt", """
            package ob
            interface B { fun p(e: String?): String? }
            class N {
                fun later(x: String?) = object : B {
                    override fun p(e: String?): String? { return r(e) }
                    fun r(e: String?): String? = e
                }.p(x)
                fun recursive(x: String?) = object : B {
                    override fun p(e: String?): String? = if (e == null || e.isEmpty()) e else p(e.substring(1))
                }.p(x)
            }
            enum class En {
                A { override fun f(): Int = g() + 1; fun g(): Int = 2 };
                abstract fun f(): Int
            }
            """.trimIndent())
    }

    private fun anonymous(owner: String, method: String): String {
        val m = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == owner }
            .methods().first { it.name() == method }
        val sb = StringBuilder()
        m.methodBody().visit { e ->
            if (e is ConstructorCall && e.anonymousClass() != null) {
                e.anonymousClass().methods().forEach { sb.append(it.name()).append(": ").append(it.methodBody().statements().joinToString(" ")).append('\n') }
            }
            true
        }
        return sb.toString()
    }

    @Test
    fun aSiblingDeclaredLater() {
        assertEquals("p: return r(e);\nr: return e;\n", anonymous("N", "later"))
    }

    @Test
    fun itself() {
        assertEquals("p: return e==null||StringsKt.isEmpty(e)?e:p(StringsKt.substring(e,1));\n", anonymous("N", "recursive"))
    }

    @Test
    fun anEnumEntryBody() {
        val en = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "En" }
        val a = en.fields().first { it.name() == "A" }.initializer() as ConstructorCall
        val f = a.anonymousClass().methods().first { it.name() == "f" }
        assertEquals("return g()+1;", f.methodBody().statements().joinToString(" "))
    }
}
