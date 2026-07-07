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
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Mixed-language integration, Phase 1 — a shared JDK/library core. The openjdk (javac) Java front-end and the
 * Kotlin (K2) front-end share ONE [Runtime] and ONE [CompiledTypesManager]; the Kotlin front-end delegates
 * library-type loading to that manager (bytecode-authoritative) rather than building its own view from K2.
 */
class TestSharedJdkCore {

    /** An openjdk inspector with `java.util` preloaded from bytecode. */
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

    /** The `TypeInfo` of the single parameter of the single method `f` in the Kotlin snippet. */
    private fun kotlinParamType(runtime: Runtime, ctm: CompiledTypesManager, source: String): TypeInfo {
        val types = KotlinScan(runtime, kotlinSourceSet(), InfoByFqn(), ctm).parse("K.kt", source)
        return types.first().findUniqueMethod("f", 1).parameters().first().parameterizedType().typeInfo()
    }

    @Test
    fun javaAndKotlinShareJavaUtilList() {
        val javaInspector = openjdkWithJavaUtil()
        val ctm = javaInspector.compiledTypesManager()

        val javaList = ctm.getOrLoad("java.util.List", javaInspector.javaBase())
        assertNotNull(javaList, "the Java front-end should have preloaded java.util.List")

        // Kotlin's `List<String>` maps to java.util.List; its TypeInfo comes from the shared manager
        val kotlinList = kotlinParamType(javaInspector.runtime(), ctm, "class K { fun f(xs: List<String>) {} }\n")
        assertSame(javaList, kotlinList, "java.util.List must be ONE shared TypeInfo instance across both parsers")
    }

    @Test
    fun kotlinTriggersLazyLoadOfANonPreloadedJavaType() {
        val javaInspector = openjdkWithJavaUtil()
        val ctm = javaInspector.compiledTypesManager()

        // java.time was NOT preloaded -- nothing has loaded LocalDate yet
        assertNull(ctm.get("java.time.LocalDate", javaInspector.javaBase()), "precondition: not yet loaded")

        // a Kotlin reference to java.time.LocalDate forces getOrLoad -> a lazy bytecode load via the shared manager
        val kotlinLocalDate = kotlinParamType(
            javaInspector.runtime(), ctm, "class K { fun f(d: java.time.LocalDate) {} }\n"
        )
        assertEquals("java.time.LocalDate", kotlinLocalDate.fullyQualifiedName())

        // it is now bytecode-loaded and cached in the shared manager -- the SAME instance
        assertSame(kotlinLocalDate, ctm.get("java.time.LocalDate", javaInspector.javaBase()),
            "the lazily loaded type is cached and shared")
    }
}
