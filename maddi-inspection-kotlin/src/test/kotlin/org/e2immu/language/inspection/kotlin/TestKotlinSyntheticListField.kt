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

package org.e2immu.language.inspection.kotlin

import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * The synthetic-list GetSet field (`_synthetic_list`, see [org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet]
 * and notes/getset-list-element-getter-npe.md) must also be present when working through the Kotlin front-end.
 * Kotlin reuses java.util.List: the K2 front-end delegates library-type loading to the shared, bytecode-authoritative
 * [org.e2immu.language.inspection.api.resource.CompiledTypesManager] (see [TestSharedJdkCore]). So the java.util.List
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

        val javaList = ctm.getOrLoad("java.util.List", javaInspector.javaBase())
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
