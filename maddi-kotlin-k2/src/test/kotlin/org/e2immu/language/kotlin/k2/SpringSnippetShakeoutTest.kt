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

package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.expression.EmptyExpression
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
