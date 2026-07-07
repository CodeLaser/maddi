package org.e2immu.language.kotlin.k2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Phase 2 de-risk: can K2 resolve a Java SOURCE type referenced from Kotlin, when the .java is in the source root? */
class MixedSourceTest : KotlinScanTestBase() {

    @Test
    fun k2ResolvesAJavaSourceType() {
        val types = KotlinScan(runtime, sourceSet).parse(
            mapOf("K.kt" to "package a.b\nclass K { fun f(foo: Foo): Int = foo.x }\n"),
            mapOf("a/b/Foo.java" to "package a.b;\npublic class Foo { public int x; }\n")
        )
        val paramType = types.first().findUniqueMethod("f", 1).parameters().first().parameterizedType()
        // if K2 read Foo.java, the parameter resolves to a.b.Foo (not Object / a placeholder)
        assertEquals("a.b.Foo", paramType.typeInfo()?.fullyQualifiedName())
    }
}
