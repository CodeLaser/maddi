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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A computed property's block-bodied getter, `val x: T get() { … }`: its `bodyExpression` is the block itself, and
 * it was converted as ONE expression -- a single `k2-block-not-a-single-expression` for the whole body (coil's
 * `Uri.pathSegments`, detekt's `leftMostElementOfLeftSubtree`).
 */
class BlockGetterTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("bg/Bg.kt", """
            package bg
            class Node(val left: Node?)
            val Node.leftMost: Node
                get() {
                    val l = left ?: return this
                    return l.leftMost
                }
            class K(private val items: List<String>) {
                val count: Int
                    get() {
                        var n = 0
                        for (i in items) n += 1
                        return n
                    }
            }
            """.trimIndent() + "\n")
    }

    @Test
    fun theBodyIsStatements() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val count = types.first { it.simpleName() == "K" }.findUniqueMethod("getCount", 0)
        assertEquals("{int n=0;for(String i:this.items){n+=1;}return n;}", count.methodBody().toString())
        val leftMost = types.first { it.simpleName() == "BgKt" }.findUniqueMethod("getLeftMost", 1)
        // the control-flow elvis applies in an accessor too: a guard, then the declaration
        assertEquals("{if(\$receiver.left==null){return \$receiver;}Node l=\$receiver.left;return BgKt.getLeftMost(l);}",
            leftMost.methodBody().toString())
    }
}
