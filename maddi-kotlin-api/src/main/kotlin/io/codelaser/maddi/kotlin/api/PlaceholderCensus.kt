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

package io.codelaser.maddi.kotlin.api

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.EmptyExpression
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import java.util.IdentityHashMap

/**
 * The marker this front end puts on an [EmptyExpression] that stands in for code it could not convert.
 * ⚠ Read by [PlaceholderCensus], by `KotlinBodyConverter.isPlaceholder`, by `ReferenceRecall` (a placeholder's
 * range is not coverage) and by the refactoring engine, which re-reads the source text in that range rather
 * than trust the tree. One spelling, in one place.
 */
const val K2_PLACEHOLDER_PREFIX: String = "k2-"

/**
 * <b>How much of a Kotlin parse the front end could not read, counted rather than left silent.</b>
 *
 * <h2>⭐ Why a run has to say this out loud</h2>
 * A construct this front end does not convert becomes a `k2-…` [EmptyExpression] keeping the range of the code
 * it replaces. That is deliberate and safe — but downstream a placeholder is simply EMPTY: the link engine
 * contributes no links, no reads, no modifications for it, so the analysis has a HOLE where the code was, and
 * a type whose body was half-read produces verdicts that look exactly like a type with nothing to say. The
 * by-name lane took the same view of its blind spot and counts `unresolvedSinkCalls`; this is that number for
 * the Kotlin front end.
 *
 * <h2>⚠ The denominators are part of the result</h2>
 * `0 placeholders` is also what a census that walked nothing reports, so [typesVisited] and [membersVisited]
 * travel with the count: a clean parse and a census that resolved no bodies are then distinguishable, which
 * they are not in a total alone.
 *
 * @param total          every placeholder found
 * @param byKind         count per marker (`k2-unsupported-expr:KtNamedFunction`, `k2-unresolved-call`, …)
 * @param types          types holding at least one
 * @param members        methods, constructors and field initialisers holding at least one
 * @param typesVisited   types walked, nested types included
 * @param membersVisited methods, constructors and fields walked
 * @param sites          one entry per placeholder: its kind, the member holding it, and where. A count
 *                       names a problem; only a site makes it a worklist -- the corpus's biggest kind was
 *                       one nobody guessed from the name alone
 */
data class PlaceholderCensus(
    val total: Int,
    val byKind: Map<String, Int>,
    val types: Int,
    val members: Int,
    val typesVisited: Int,
    val membersVisited: Int,
    val sites: List<Site> = listOf()
) {
    /**
     * Where one placeholder is. [owner] is the member whose body holds it. Line and position are the range
     * of the code it stands for; 0 when the front end had no range to give.
     */
    data class Site(val kind: String, val owner: String, val line: Int, val pos: Int) {
        override fun toString(): String = kind + "\t" + owner + "\t" + line + ":" + pos
    }

    /** The sites as text, kind-major then owner, for `-Dmaddi.placeholderDump`. */
    fun dumpLines(): List<String> = sites
        .sortedWith(compareBy({ it.kind }, { it.owner }, { it.line }))
        .map { it.toString() }

    /** One log line: the count, its denominators, and the markers that dominate it. */
    @JvmOverloads
    fun report(topKinds: Int = 8): String {
        if (typesVisited == 0) return "Kotlin placeholders: nothing walked (no Kotlin types in this parse)"
        if (total == 0) return "Kotlin placeholders: none, in $typesVisited type(s) / $membersVisited member(s)"
        val top = byKind.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
            .thenBy { it.key }).take(topKinds).joinToString(", ") { "${it.key}=${it.value}" }
        val rest = byKind.size - minOf(topKinds, byKind.size)
        return "Kotlin placeholders: $total in $types of $typesVisited type(s), $members of $membersVisited" +
                " member(s); the analysis reads NOTHING at these positions. Top kinds: $top" +
                if (rest > 0) " (and $rest more kind(s))" else ""
    }

    companion object {
        /**
         * Walks the bodies of [types] — nested types, constructors, methods and field initialisers — exactly
         * as `ReferenceRecall` walks them, so the two instruments cover the same tree.
         */
        @JvmStatic
        fun of(types: Collection<TypeInfo>): PlaceholderCensus = Walk().apply { types.forEach { walkType(it) } }.result()

        private class Walk {
            private val seen = IdentityHashMap<Any, Boolean>()
            private val byKind = HashMap<String, Int>()
            private val sites = ArrayList<Site>()
            private var owner: String = "?"
            private var total = 0
            private var typesWith = 0
            private var membersWith = 0
            private var typesVisited = 0
            private var membersVisited = 0

            fun result() = PlaceholderCensus(total, byKind.toMap(), typesWith, membersWith,
                typesVisited, membersVisited, sites.toList())

            fun walkType(typeInfo: TypeInfo) {
                if (seen.put(typeInfo, true) != null) return
                ++typesVisited
                typeInfo.subTypes().forEach { walkType(it) } // counted as their own types, not this one's
                val before = total
                (typeInfo.constructors() + typeInfo.methodStream().toList()).forEach { walkMethod(it) }
                typeInfo.fields().forEach { walkField(it) }
                if (total > before) ++typesWith
            }

            private fun walkMethod(methodInfo: MethodInfo) {
                if (seen.put(methodInfo, true) != null) return
                ++membersVisited
                val before = total
                owner = methodInfo.fullyQualifiedName()
                methodInfo.methodBody()?.visit { count(it) }
                if (total > before) ++membersWith
            }

            private fun walkField(fieldInfo: FieldInfo) {
                if (seen.put(fieldInfo, true) != null) return
                ++membersVisited
                val before = total
                owner = fieldInfo.fullyQualifiedName()
                fieldInfo.initializer()?.visit { count(it) }
                if (total > before) ++membersWith
            }

            private fun count(element: Element): Boolean {
                if (seen.put(element, true) != null) return false
                if (element is EmptyExpression) {
                    val msg = element.msg()
                    if (msg != null && msg.startsWith(K2_PLACEHOLDER_PREFIX)) {
                        ++total
                        byKind.merge(msg, 1) { a, b -> a + b }
                        val source = element.source()
                        sites.add(Site(msg, owner, source?.beginLine() ?: 0, source?.beginPos() ?: 0))
                    }
                }
                return true
            }
        }
    }
}
