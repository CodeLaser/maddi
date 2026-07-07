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

import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Phase 1 of the mixed-language integration — a shared JDK/library core. The openjdk (javac) Java front-end
 * and the Kotlin (K2) front-end, sharing ONE [Runtime] and ONE [CompiledTypesManager], must resolve
 * `java.util.List` to the SAME [org.e2immu.language.cst.api.info.TypeInfo] instance: the Kotlin front-end
 * delegates library-type loading to the injected manager (bytecode-authoritative) rather than building its
 * own view from K2 symbols. If this holds, a Java method and a Kotlin method that both mention `List<String>`
 * link against one shared type.
 */
class TestSharedJdkCore {

    @Test
    fun javaAndKotlinShareJavaUtilList() {
        // 1) the openjdk Java front-end, with java.util preloaded from bytecode
        val javaInspector = JavaInspectorImpl()
        javaInspector.preload("java.base::java.util.")
        javaInspector.initialize(
            InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build()
        )
        javaInspector.onlyPreload()

        val runtime: Runtime = javaInspector.runtime()
        val ctm: CompiledTypesManager = javaInspector.compiledTypesManager()
        val javaList = ctm.getOrLoad("java.util.List", javaInspector.javaBase())
        assertNotNull(javaList, "the Java front-end should have loaded java.util.List")

        // 2) the Kotlin front-end, sharing that runtime + CompiledTypesManager
        val kotlinSourceSet = SourceSetImpl.Builder().setName("kotlin").setUri(URI.create("file:/")).build()
        val types = KotlinScan(runtime, kotlinSourceSet, InfoByFqn(), ctm).parse(
            "K.kt", "class K { fun f(xs: List<String>) {} }\n"
        )
        // Kotlin's `List<String>` maps to java.util.List; its TypeInfo comes from the shared manager
        val kotlinList = types.first().findUniqueMethod("f", 1).parameters().first().parameterizedType().typeInfo()

        // 3) ONE shared java.util.List across both parsers
        assertSame(javaList, kotlinList, "java.util.List must be a single shared TypeInfo instance")
    }
}
