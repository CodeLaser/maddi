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

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A jump (`?: throw`, `?: return`, a `try` whose catch jumps) inside a LARGER expression, javalin's last
 * lowering sites: `(plugins.find { … } ?: throw E()) as T` (PluginManager), an elvis chain whose middle operand is a
 * value-`if` (Validation.convertValue), `x = a ?: try { f() } catch (e: Exception) { return@l v }` in a lambda
 * (Validator's `errors` delegate).
 */
class JumpInExpressionTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("je/Je.kt", """
            package je
            class K {
                var typed: Any? = null
                fun cast(l: List<Any>): CharSequence = (l.firstOrNull { it is String } ?: throw IllegalStateException()) as CharSequence
                fun chain(m: Map<String, String>, k: String, e: Boolean): String {
                    val c = m[k] ?: if (e) "x" else null ?: throw IllegalStateException(k)
                    return c
                }
                fun compute(b: () -> Int): Int = b()
                fun lazyTry(t: Any?, f: () -> Any): Int = compute {
                    typed = t ?: try {
                        f()
                    } catch (e: Exception) {
                        return@compute 1
                    }
                    2
                }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            cast: Object ${'$'}elvis0=CollectionsKt.firstOrNull(l,it->it instanceof String); Object ${'$'}elvis1; if(${'$'}elvis0!=null){${'$'}elvis1=${'$'}elvis0;}else{throw new IllegalStateException();} return (CharSequence)${'$'}elvis1;
            chain: String ${'$'}elvis2=m.get(k); String ${'$'}elvis3; if(${'$'}elvis2!=null){${'$'}elvis3=${'$'}elvis2;}else{if(e){${'$'}elvis3="x";}else{if(null==null){throw new IllegalStateException(k);}${'$'}elvis3=null;}} String c=${'$'}elvis3; return c;
            lazyTry: return compute(()->{Object ${'$'}elvis4;if(t!=null){${'$'}elvis4=t;}else{try{${'$'}elvis4=f.invoke();}catch(Exception e){return 1;}}this.typed=${'$'}elvis4;return 2;});
            """.trimIndent(), listOf("cast", "chain", "lazyTry").joinToString("\n") { "$it: ${body(it)}" })
    }
}
