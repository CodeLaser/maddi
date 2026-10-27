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

package io.codelaser.maddi.inspection.mixed

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
