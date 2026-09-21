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
 */
data class PlaceholderCensus(
    val total: Int,
    val byKind: Map<String, Int>,
    val types: Int,
    val members: Int,
    val typesVisited: Int,
    val membersVisited: Int
) {
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
            private var total = 0
            private var typesWith = 0
            private var membersWith = 0
            private var typesVisited = 0
            private var membersVisited = 0

            fun result() = PlaceholderCensus(total, byKind.toMap(), typesWith, membersWith,
                typesVisited, membersVisited)

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
                methodInfo.methodBody()?.visit { count(it) }
                if (total > before) ++membersWith
            }

            private fun walkField(fieldInfo: FieldInfo) {
                if (seen.put(fieldInfo, true) != null) return
                ++membersVisited
                val before = total
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
                    }
                }
                return true
            }
        }
    }
}
