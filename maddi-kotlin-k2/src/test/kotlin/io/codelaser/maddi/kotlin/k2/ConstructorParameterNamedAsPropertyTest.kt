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

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Only a primary constructor's `val`/`var` parameter declares a property, and so is assigned to its field before the
 * `init` blocks. A plain parameter named as a property is not: `class Copy(lines: List<String>) { val lines =
 * lines.toList() }` is assigned its copy only. An extra `this.lines = lines` before the copy linked the field to the
 * parameter: the class was @Dependent where its Java twin with `List.copyOf` is @ImmutableContainer.
 */
class ConstructorParameterNamedAsPropertyTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("cp/Cp.kt", """
            package cp
            class Copy(lines: List<String>) { val lines: List<String> = lines.toList() }
            class Declared(val lines: List<String>, count: Int) { val size: Int = count }
            class Secondary(val m: Int) {
                var n: Int = 0
                constructor(n: Int, k: Int) : this(n + k)
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
        .constructors().joinToString(" | ") { it.methodBody().statements().joinToString(" ") }

    @Test
    fun onlyAPropertyParameterIsAssigned() {
        assertEquals("this.lines=CollectionsKt.toList(lines);", body("Copy"))
        assertEquals("this.lines=lines; this.size=count;", body("Declared"))
        assertEquals("this.m=m; | this(n+k);", body("Secondary"))
    }
}
