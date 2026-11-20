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
 * <b>A `try` used as a value</b> where no lowering reached it: assigned (`x = try { … }`, `field = try { … }` in
 * Exposed's metadata getters) and as a getter's expression body (`get() = try { connection.catalog } catch …`,
 * Exposed's JdbcConnectionImpl). Each was a placeholder that hid every call in the try (maddi#220).
 */
class TryAsValueTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    @Test
    fun aTryIsLoweredWhereverItIsAValue() {
        val types = parse("""
            package a
            class P(private val s: String) {
                var x: Int = 0
                fun assign() { x = try { s.toInt() } catch (e: NumberFormatException) { -1 } }
                fun assignIf(c: Boolean) { x = if (c) 0 else try { s.toInt() } catch (e: NumberFormatException) { -1 } }
                val parsed: Int
                    get() = try { s.toInt() } catch (e: NumberFormatException) { 0 }
                var cached: String? = null
                    get() {
                        if (field == null) field = try { s.trim() } catch (e: Exception) { "" }
                        return field
                    }
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val body = { name: String -> p.methods().first { it.name() == name }.methodBody().toString() }
        assertEquals("{try{this.x=StringsKt.toInt(this.s);}catch(NumberFormatException e){this.x=-1;}}", body("assign"))
        assertEquals("{if(c){this.x=0;}else{try{this.x=StringsKt.toInt(this.s);}catch(NumberFormatException e){this.x=-1;}}}",
            body("assignIf"))
        assertEquals("{try{return StringsKt.toInt(this.s);}catch(NumberFormatException e){return 0;}}", body("getParsed"))
        assertEquals("{if(this.cached==null){try{this.cached=StringsKt.trim(this.s);}catch(Exception e){this.cached=\"\";}}return this.cached;}",
            body("getCached"))
    }
    /** `a + (x ?: throw E())`: the elvis is on the statement's spine when the left operand is stable (Exposed). */
    @Test
    fun aJumpElvisAsARightOperandIsHoisted() {
        val types = parse("""
            package a
            class P(private val base: Set<String>) {
                fun all(x: Set<String>?): Set<String> = base + (x ?: throw IllegalStateException("none"))
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals("{Set<String> \$elvis0;if(x!=null){\$elvis0=x;}else{throw new IllegalStateException(\"none\");}return SetsKt.plus(this.base,\$elvis0);}",
            p.findUniqueMethod("all", 1).methodBody().toString())
    }
    /**
     * A `try` as an elvis's left operand (`get() = try { c.catalog } catch … ?: ""`, Exposed's JdbcConnectionImpl), and
     * an expression-body `if` with a `throw` branch (Exposed's MysqlDataTypeProvider).
     */
    @Test
    fun aTryOnTheSpineAndAThrowingBranchAreLowered() {
        val types = parse("""
            package a
            class P(private val s: String) {
                val catalog: String
                    get() = try { s.trim() } catch (e: Exception) { null } ?: ""
                fun type(c: Boolean): String = if (c) { "X" } else { throw IllegalStateException("no") }
                fun all(xs: List<String>): List<String> = xs.map { x ->
                    try { x.trim() } catch (e: Exception) { try { x.lowercase() } catch (e: Exception) { null } ?: "?" }
                }
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val body = { name: String -> p.methods().first { it.name() == name }.methodBody().toString() }
        assertEquals("{String \$try0;try{\$try0=StringsKt.trim(this.s);}catch(Exception e){\$try0=null;}return \$try0==null?\"\":\$try0;}",
            body("getCatalog"))
        assertEquals("{if(c){return \"X\";}else{throw new IllegalStateException(\"no\");}}", body("type"))
        assertEquals("{return CollectionsKt.map(xs,x->{try{return StringsKt.trim(x);}catch(Exception e){String \$try1;" +
            "try{\$try1=x.toLowerCase(Locale.ROOT);}catch(Exception e){\$try1=null;}return \$try1==null?\"?\":\$try1;}});}",
            body("all"))
    }
}
