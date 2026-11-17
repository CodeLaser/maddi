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
        assertEquals("{try{return StringsKt.toInt(this.s);}catch(NumberFormatException e){return 0;}}", body("getParsed"))
        assertEquals("{if(this.cached==null){try{this.cached=StringsKt.trim(this.s);}catch(Exception e){this.cached=\"\";}}return this.cached;}",
            body("getCached"))
    }
}
