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
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.integration.JavaInspector
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.k2.KotlinScan
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

        val javaList = ctm.type("java.util.List", javaInspector.javaBase())
        assertNotNull(javaList, "the Java front-end should have preloaded java.util.List")

        // Kotlin's `List<String>` maps to java.util.List; its TypeInfo comes from the shared manager
        val kotlinList = kotlinParamType(javaInspector.runtime(), ctm, "class K { fun f(xs: List<String>) {} }\n")
        assertSame(javaList, kotlinList, "java.util.List must be ONE shared TypeInfo instance across both parsers")
    }

    @Test
    fun kotlinTriggersLazyLoadOfANonPreloadedJavaType() {
        val javaInspector = openjdkWithJavaUtil()
        val ctm = javaInspector.compiledTypesManager()

        // javax.crypto was NOT preloaded, and no java.util signature names Cipher: nothing has loaded it yet
        // (a type java.util only mentions, java.time.LocalDate, is committed by the preload pass since maddi#119)
        assertNull(ctm.typeIfLoaded("javax.crypto.Cipher", javaInspector.javaBase()), "precondition: not yet loaded")

        // a Kotlin reference to javax.crypto.Cipher forces type() past the registry -> a lazy bytecode load
        val kotlinCipher = kotlinParamType(
            javaInspector.runtime(), ctm, "class K { fun f(c: javax.crypto.Cipher) {} }\n"
        )
        assertEquals("javax.crypto.Cipher", kotlinCipher.fullyQualifiedName())

        // it is now bytecode-loaded and cached in the shared manager -- the SAME instance
        assertSame(kotlinCipher, ctm.typeIfLoaded("javax.crypto.Cipher", javaInspector.javaBase()),
            "the lazily loaded type is cached and shared")
    }
}
