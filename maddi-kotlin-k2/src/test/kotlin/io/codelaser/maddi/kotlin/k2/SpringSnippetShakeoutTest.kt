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

import io.codelaser.maddi.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A shakeout: parse a chunk of idiomatic, Spring-Core-flavoured Kotlin and collect every `k2-*` placeholder
 * the body converter emits, to surface what real code still doesn't resolve. Not a pass/fail assertion --
 * the captured set is the deliverable.
 */
class SpringSnippetShakeoutTest : KotlinScanTestBase() {

    private val source = """
        package org.example.core

        data class Resource(val name: String, val size: Long, val tags: List<String>)

        class ResourceRegistry {
            private val resources = HashMap<String, Resource>()

            fun register(resource: Resource) {
                resources[resource.name] = resource
            }

            fun find(name: String): Resource? = resources[name]

            fun totalSize(): Long {
                var total = 0L
                for (r in resources.values) {
                    total += r.size
                }
                return total
            }

            fun has(name: String): Boolean = resources.containsKey(name)

            fun names(): List<String> = ArrayList(resources.keys)

            companion object {
                fun empty(): ResourceRegistry = ResourceRegistry()
            }
        }

        fun Resource.describe(): String = name + " (" + size + " bytes)"

        fun classifySize(resource: Resource): String = when {
            resource.size > 1000 -> "large"
            resource.size > 100 -> "medium"
            else -> "small"
        }

        fun safeName(resource: Resource?): String = resource?.name ?: "unknown"

        fun firstResource(registry: ResourceRegistry, name: String): Resource {
            val found = registry.find(name)
            return found!!
        }

        fun describeAll(registry: ResourceRegistry): String {
            val builder = StringBuilder()
            for (n in registry.names()) {
                builder.append(n)
            }
            return builder.toString()
        }
    """.trimIndent()

    @Test
    fun shakeout() {
        val types = KotlinScan(runtime, sourceSet).parse("Shakeout.kt", source)
        val placeholders = sortedMapOf<String, Int>()
        types.forEach { type ->
            (type.methods() + type.constructors()).forEach { m ->
                m.methodBody().visit { e ->
                    if (e is EmptyExpression && e.msg().startsWith("k2-")) {
                        placeholders.merge(e.msg(), 1) { a, b -> a + b }
                    }
                    true
                }
            }
        }
        // the whole snippet resolves without placeholders; any regression lists the offending constructs
        assertEquals("", placeholders.entries.joinToString("\n") { "${it.value}x ${it.key}" })
    }
}
