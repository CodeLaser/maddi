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

import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Mixed-language integration, Phase 2 — Kotlin resolving a Java SOURCE type. The openjdk (javac) front-end
 * parses `a.b.Foo` from source (committing it to the shared CompiledTypesManager); a Kotlin file references
 * `Foo`, K2 reads `Foo.java` from its source root, and the Kotlin front-end reuses the openjdk-built `Foo`
 * TypeInfo (via the Phase-1 CTM delegation) rather than rebuilding a K2 view. So one Java source type is a
 * single shared instance across both parsers.
 */
class TestMixedSourceReference {

    private val fooJava = "package a.b;\npublic class Foo { public int x; }\n"

    @Test
    fun kotlinReferencesAJavaSourceType() {
        // 1) the openjdk front-end parses the Java SOURCE class -> committed to the shared CompiledTypesManager
        val javaInspector = JavaInspectorImpl()
        javaInspector.initialize(
            InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build()
        )
        val javaFoo = javaInspector.parse("a.b.Foo", fooJava)
        val ctm = javaInspector.compiledTypesManager()
        assertSame(javaFoo, ctm.get("a.b.Foo", javaInspector.javaBase()), "Foo should be committed to the shared manager")

        // 2) a Kotlin file references a.b.Foo; K2 reads Foo.java from its own source root to resolve it, and
        //    the Kotlin front-end reuses the shared manager's Foo (Phase-1 delegation) instead of a K2 rebuild
        val kotlinSourceSet = SourceSetImpl.Builder().setName("kotlin").setUri(URI.create("file:/")).build()
        val kotlinFoo = KotlinScan(javaInspector.runtime(), kotlinSourceSet, InfoByFqn(), ctm).parse(
            mapOf("K.kt" to "package a.b\nclass K { fun f(foo: Foo): Int = foo.x }\n"),
            mapOf("a/b/Foo.java" to fooJava)
        ).first().findUniqueMethod("f", 1).parameters().first().parameterizedType().typeInfo()

        assertEquals("a.b.Foo", kotlinFoo.fullyQualifiedName())
        assertSame(javaFoo, kotlinFoo, "a.b.Foo must be ONE shared TypeInfo across the Java and Kotlin front-ends")
    }
}
