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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Kotlin-multiplatform: an `expect class` realised by an **`actual typealias`** rather than an `actual class`.
 * `TestExpectActual` (maddi-inspection-kotlin) covers only the class/class pair.
 *
 * Coil is where this bites: `expect class Bitmap` in commonMain, `actual typealias Bitmap =
 * org.jetbrains.skia.Bitmap` in nonAndroidMain, and `internal actual typealias WeakReference<T> =
 * java.lang.ref.WeakReference<T>`. `KotlinScan` drops `expect` declarations, so unless the reference resolves
 * through the alias, nothing builds a type for that FQN and a shell named `coil3.Bitmap` — a name no JVM class
 * has — reaches the analysis and the generated Java stubs. The targets here are all JDK types.
 */
class ExpectActualTypealiasTest : KotlinScanTestBase() {

    @Test
    fun expectClassRealisedByActualTypealias() {
        val types = KotlinScan(runtime, sourceSet).parse(
            mapOf(
                "Common.kt" to "package p\nexpect class Handle\nclass Holder {\n    fun get(): Handle = TODO()\n}\n",
                "Jvm.kt" to "package p\nactual typealias Handle = java.lang.StringBuilder\n",
            )
        )
        val holder = types.single { it.simpleName() == "Holder" }
        assertEquals("java.lang.StringBuilder",
                holder.findUniqueMethod("get", 0).returnType().typeInfo()!!.fullyQualifiedName())
    }

    /** The generic case: the use-site type argument must survive the expansion, not degrade to Object. */
    @Test
    fun genericActualTypealiasKeepsTheUseSiteArgument() {
        val types = KotlinScan(runtime, sourceSet).parse(
            mapOf(
                "Common.kt" to "package q\nexpect class Ref<T : Any>(referred: T)\n"
                        + "class Holder {\n    fun get(): Ref<String> = TODO()\n}\n",
                "Jvm.kt" to "package q\nactual typealias Ref<T> = java.lang.ref.WeakReference<T>\n",
            )
        )
        val holder = types.single { it.simpleName() == "Holder" }
        val returnType = holder.findUniqueMethod("get", 0).returnType()
        assertEquals("java.lang.ref.WeakReference", returnType.typeInfo()!!.fullyQualifiedName())
        assertEquals(listOf("java.lang.String"),
                returnType.parameters().map { it.typeInfo()?.fullyQualifiedName() })
    }

    /**
     * Kotlin interface delegation: `class C(d: I) : I by d` makes kotlinc generate a forwarding override of
     * every member of `I`. K2 does not surface those and they have no PSI, so `C` used to come out with **no
     * members at all** — a type that implements an interface yet has none of its methods. Coil's
     * `FaultHidingSink : Sink by delegate` is the real instance; `okio.Sink` also shows why inherited abstract
     * members must be included, since it extends `Closeable`/`Flushable`.
     */
    @Test
    fun interfaceDelegationMaterialisesTheForwardedMembers() {
        val types = KotlinScan(runtime, sourceSet).parse(
            "Delegation.kt",
            "package s\ninterface Base {\n    fun a(): Int\n    fun b(x: String)\n}\n"
                    + "interface Derived : Base {\n    fun c()\n}\n"
                    + "class Holder(private val delegate: Derived) : Derived by delegate\n"
        )
        val holder = types.single { it.simpleName() == "Holder" }
        // inherited abstract members count too: `c` from Derived, `a`/`b` from the Base it extends
        assertEquals(setOf("a", "b", "c"), holder.methods().map { it.name() }.toSet())
        // Any's members are never forwarded by Kotlin delegation
        assertEquals(emptySet<String>(),
                holder.methods().map { it.name() }.toSet().intersect(setOf("equals", "hashCode", "toString")))
    }

    /**
     * Kotlin's primitive array classes are the unboxed JVM arrays, not `Array<T>` (which boxes) — so
     * `ByteArray` should be `byte[]`, not a shell type named `kotlin.ByteArray`.
     *
     * Disabled because the mapping cannot go in `mapClassType` yet. It is correct in isolation, but adding it
     * changes the order in which library types are first reached, and `maxMemberDepth`'s first-visit-wins rule
     * makes that order decide whether a type keeps its members: it stranded `java.util.Iterator` as a shell
     * (reached at depth 2 while loading `java.lang.String`), breaking
     * `TypeResolutionTest.chainedLibraryCallResolves`. Raising the depth to 3 traded one failure for four.
     * The prerequisite is a loader that deepens a shell on a later, shallower visit. Until then
     * `JavaStubGenerator` translates these names so the generated Java is at least valid.
     */
    @Disabled("blocked on library-loader order-dependence (maxMemberDepth first-visit-wins); see the javadoc")
    @Test
    fun primitiveArrayClassesAreJvmPrimitiveArrays() {
        val types = KotlinScan(runtime, sourceSet).parse(
            "Arrays.kt",
            "package r\nclass A {\n"
                    + "    fun bytes(): ByteArray = TODO()\n"
                    + "    fun longs(): LongArray = TODO()\n"
                    + "    fun flags(): BooleanArray = TODO()\n"
                    + "    fun boxed(): Array<Int> = TODO()\n"
                    + "}\n"
        )
        val a = types.single { it.simpleName() == "A" }
        fun returnTypeOf(name: String) = a.findUniqueMethod(name, 0).returnType()

        assertEquals("byte", returnTypeOf("bytes").typeInfo()!!.fullyQualifiedName())
        assertEquals(1, returnTypeOf("bytes").arrays())
        assertEquals("long", returnTypeOf("longs").typeInfo()!!.fullyQualifiedName())
        assertEquals("boolean", returnTypeOf("flags").typeInfo()!!.fullyQualifiedName())
        // contrast: Array<Int> boxes its element, so it is Integer[], not int[]
        assertEquals("java.lang.Integer", returnTypeOf("boxed").typeInfo()!!.fullyQualifiedName())
        assertEquals(1, returnTypeOf("boxed").arrays())
    }
}
