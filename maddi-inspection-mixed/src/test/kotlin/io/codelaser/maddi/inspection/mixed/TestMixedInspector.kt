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

package org.e2immu.language.inspection.mixed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * The mixed-language driver end-to-end: one `MixedInspector.parse` call over a Kotlin `K` and a Java `UseK`
 * that references it. Asserts the shared-core invariants — the Java field's type IS the Kotlin `K` instance,
 * and `java.util.List` used by both languages is one instance.
 */
class TestMixedInspector {

    @Test
    fun javaAndKotlinShareTypesAcrossTheBoundary() {
        val result = MixedInspector().parse(
            mapOf(
                "K.kt" to "package a.b\n" +
                    "class K(val id: Int) {\n" +
                    "    fun items(xs: java.util.List<String>): java.util.List<String> = xs\n" +
                    "}\n"
            ),
            mapOf(
                "a.b.UseK" to "package a.b;\n" +
                    "public class UseK {\n" +
                    "    public K field;\n" +
                    "    public java.util.List<String> use(K k, java.util.List<String> xs) { return k.items(xs); }\n" +
                    "}\n"
            )
        )
        val kotlinK = result.kotlinTypes.first { it.simpleName() == "K" }
        val useK = result.javaTypes.first { it.simpleName() == "UseK" }

        // Java -> Kotlin source reference: UseK's field is the SAME Kotlin K instance
        assertSame(kotlinK, useK.getFieldByName("field", true).type().typeInfo())

        // shared JDK core: java.util.List used by both languages is ONE shared instance
        val listFromKotlin = kotlinK.findUniqueMethod("items", 1).returnType().typeInfo()
        val listFromJava = useK.findUniqueMethod("use", 2).returnType().typeInfo()
        assertEquals("java.util.List", listFromKotlin.fullyQualifiedName())
        assertSame(listFromKotlin, listFromJava)
    }
}
