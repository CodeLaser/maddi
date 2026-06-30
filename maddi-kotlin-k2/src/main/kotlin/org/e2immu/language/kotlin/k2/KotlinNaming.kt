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

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

// Pure file-facade naming, shared by KotlinScan (facade creation) and KotlinBodyConverter (extension
// call routing) — no scan state, so top-level.

/**
 * Kotlin's JVM file-facade class name: `@file:JvmName("X")` wins; otherwise the file name (sans
 * extension), first letter upper-cased, + "Kt".
 */
internal fun facadeSimpleName(ktFile: KtFile): String {
    jvmNameOverride(ktFile)?.let { return it }
    val base = ktFile.name.substringAfterLast('/').removeSuffix(".kts").removeSuffix(".kt")
    return base.replaceFirstChar { it.uppercaseChar() } + "Kt"
}

/** The string in a `@file:JvmName("…")` annotation, or null. */
private fun jvmNameOverride(ktFile: KtFile): String? {
    val jvmName = ktFile.fileAnnotationList?.annotationEntries
        ?.firstOrNull { it.shortName?.asString() == "JvmName" } ?: return null
    val literal = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
    return (literal?.entries?.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
}
