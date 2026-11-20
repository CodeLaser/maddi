package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `xs.forEach { … }` is the loop kotlinc inlines it to. As a call with a lambda, a modification of the lambda's parameter
 * did not reach the collection's elements (maddi-mod#145). `return@forEach` is the next element, a bare `return` the
 * enclosing function's.
 */
class ForEachLoweringTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("fe/Fe.kt", """
            package fe
            class B {
                fun plain(xs: List<StringBuilder>) { xs.forEach { it.append("x") } }
                fun named(xs: List<StringBuilder>) { xs.forEach { sb -> sb.append("x") } }
                fun array(xs: Array<StringBuilder>) { xs.forEach { it.append("x") } }
                fun skip(xs: List<StringBuilder>) { xs.forEach { if (it.isEmpty()) return@forEach; it.append("x") } }
                fun nested(xs: List<StringBuilder>, ys: List<String>) { xs.forEach { for (y in ys) { if (y.isEmpty()) return@forEach; it.append(y) } } }
                fun leave(xs: List<StringBuilder>): Int { xs.forEach { if (it.isEmpty()) return 1 }; return 0 }
                fun map(m: Map<String, StringBuilder>) { m.forEach { (k, v) -> v.append(k) } }
                fun count(xs: List<String>): Int { var n = 0; xs.forEach { if (it.isEmpty()) n++ }; return n }
                fun countSeq(xs: List<String>): Int { var n = 0; xs.asSequence().forEach { if (it.isEmpty()) n++ }; return n }
            }
            """.trimIndent())
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun theShapes() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val names = listOf("plain", "named", "array", "skip", "nested", "leave", "map")
        assertEquals("""
            plain: ${'$'}forEach0: for(StringBuilder it:xs){it.append("x");}
            named: ${'$'}forEach1: for(StringBuilder sb:xs){sb.append("x");}
            array: ${'$'}forEach2: for(StringBuilder it:xs){it.append("x");}
            skip: ${'$'}forEach3: for(StringBuilder it:xs){if(it.length()==0){continue ${'$'}forEach3;}it.append("x");}
            nested: ${'$'}forEach4: for(StringBuilder it:xs){for(String y:ys){if(StringsKt.isEmpty(y)){continue ${'$'}forEach4;}it.append(y);}}
            leave: ${'$'}forEach5: for(StringBuilder it:xs){if(it.length()==0){return 1;}} return 0;
            map: MapsKt.forEach(m,${'$'}dstr0->{String k=MapsKt.component1(${'$'}dstr0),v=MapsKt.component2(${'$'}dstr0);v.append(k);});
            """.trimIndent(), names.joinToString("\n") { "$it: ${body(it)}" })
    }

    // a var assigned in a lowered literal is a plain local, as in kotlinc's inlined loop; a lambda left a call still holds it in a Ref
    @Test
    fun aVarAssignedInALoweredLiteralIsNoRef() {
        assertEquals("int n=0; \$forEach6: for(String it:xs){if(StringsKt.isEmpty(it)){n++;}} return n;", body("count"))
        assert(body("countSeq").startsWith("IntRef n=new IntRef();")) { body("countSeq") }
    }
}
