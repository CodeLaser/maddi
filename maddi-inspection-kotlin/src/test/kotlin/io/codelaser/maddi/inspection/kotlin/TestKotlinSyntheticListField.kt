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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.api.integration.JavaInspector
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * The synthetic-list GetSet field (`_synthetic_list`, see [io.codelaser.maddi.inspection.api.util.CreateSyntheticFieldsForGetSet]
 * and notes/getset-list-element-getter-npe.md) must also be present when working through the Kotlin front-end.
 * Kotlin reuses java.util.List: the K2 front-end delegates library-type loading to the shared, bytecode-authoritative
 * [io.codelaser.maddi.inspection.api.resource.CompiledTypesManager] (see [TestSharedJdkCore]). So the java.util.List
 * the openjdk load synthesized `_synthetic_list` onto is the very same TypeInfo Kotlin's `List<Int>` resolves to.
 */
class TestKotlinSyntheticListField {

    /** An openjdk inspector with `java.util` preloaded from bytecode (this is where _synthetic_list is created). */
    private fun openjdkWithJavaUtil(): JavaInspector {
        val javaInspector = JavaInspectorImpl()
        javaInspector.preload("java.base::java.util.")
        javaInspector.initialize(
            InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build()
        )
        javaInspector.onlyPreload()
        return javaInspector
    }

    private fun kotlinSourceSet() = SourceSetImpl.Builder().setName("kotlin").setUri(URI.create("file:/")).build()

    @Test
    fun kotlinListGetHasSyntheticField() {
        val javaInspector = openjdkWithJavaUtil()
        val ctm = javaInspector.compiledTypesManager()

        val javaList = ctm.type("java.util.List", javaInspector.javaBase())
        assertNotNull(javaList, "the Java front-end should have preloaded java.util.List")

        // Kotlin's `List<Int>` maps to java.util.List; its TypeInfo comes from the shared manager
        val types = KotlinScan(javaInspector.runtime(), kotlinSourceSet(), InfoByFqn(), ctm)
            .parse("K.kt", "class K { fun f(xs: List<Int>): Int = xs[0] }\n")
        val kotlinList: TypeInfo = types.first().findUniqueMethod("f", 1)
            .parameters().first().parameterizedType().typeInfo()
        assertEquals("java.util.List", kotlinList.fullyQualifiedName())
        assertSame(javaList, kotlinList, "java.util.List must be ONE shared TypeInfo across both front-ends")

        // and that shared java.util.List carries the synthetic-list GetSet field
        val get = kotlinList.findUniqueMethod("get", 1)
        val fv = get.getSetField()
        assertNotNull(fv.field(), "java.util.List.get, as seen through Kotlin, must carry _synthetic_list")
        assertEquals("_synthetic_list", fv.field()!!.name())
        assertTrue(fv.list())
    }
}
