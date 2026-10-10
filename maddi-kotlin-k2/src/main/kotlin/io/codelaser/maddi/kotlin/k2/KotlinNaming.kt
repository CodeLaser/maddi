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

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

// Pure file-facade naming, shared by KotlinScan (facade creation) and KotlinBodyConverter (extension
// call routing) — no scan state, so top-level.

/**
 * Kotlin's JVM file-facade class name: `@file:JvmName("X")` wins; otherwise the file name (sans
 * extension), sanitized as a Java class name, + "Kt".
 */
internal fun facadeSimpleName(ktFile: KtFile): String {
    jvmNameOverride(ktFile)?.let { return it }
    val base = ktFile.name.substringAfterLast('/').removeSuffix(".kts").removeSuffix(".kt")
    return asJavaClassName(base) + "Kt"
}

/**
 * kotlinc's `PackagePartClassUtils.getFilePartShortName`, mirrored: every character that cannot occur in a
 * Java identifier becomes `_`, a name that cannot *start* one is prefixed with `_`, and the first character
 * is upper-cased.
 *
 * The substitution is not cosmetic. Kotlin-multiplatform projects routinely qualify a file name with its
 * source set — `utils.nonAndroid.kt`, `RealImageLoader.nonApple.kt` — and kotlinc compiles those to
 * `Utils_nonAndroidKt` / `RealImageLoader_nonAppleKt`. Merely upper-casing the first letter left the `.` in
 * place, so the facade's *simple* name contained a dot: it did not match the class kotlinc emits (breaking
 * any link to that facade in compiled bytecode), and it is not a name javac accepts, which is where it first
 * surfaced — `JavaStubGenerator` emitted `public class Utils.nonAndroidKt`, 24 errors on coil's JVM slice.
 */
private fun asJavaClassName(base: String): String {
    if (base.isEmpty()) return "_"
    val sanitized = base.map { if (Character.isJavaIdentifierPart(it)) it else '_' }.joinToString("")
    val started = if (Character.isJavaIdentifierStart(sanitized[0])) sanitized else "_$sanitized"
    return started.replaceFirstChar { it.uppercaseChar() }
}

/** The string in a `@file:JvmName("…")` annotation, or null. */
private fun jvmNameOverride(ktFile: KtFile): String? =
    jvmNameFromEntries(ktFile.fileAnnotationList?.annotationEntries)

/**
 * The string in a function's `@JvmName("…")` annotation, or null. Kotlin uses it to disambiguate overloads
 * that erase to the same JVM signature (e.g. stdlib's `flatMap` taking `(T)->Iterable` vs `(T)->Sequence`,
 * the latter `@JvmName("flatMapSequence")`), so the JVM method name must reflect it or the two collide.
 */
internal fun jvmNameOverride(function: KtNamedFunction): String? =
    jvmNameFromEntries(function.annotationEntries)

private fun jvmNameFromEntries(entries: List<KtAnnotationEntry>?): String? {
    val jvmName = entries?.firstOrNull { it.shortName?.asString() == "JvmName" } ?: return null
    val literal = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
    return (literal?.entries?.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
}

private val JVM_NAME_ANNOTATION = ClassId.fromString("kotlin/jvm/JvmName")

/**
 * The JVM name `@JvmName` gives [symbol], or null: from its PSI for a source function, else from its annotations,
 * which is the only route for a LIBRARY symbol (deserialized from a class file, no PSI). The stdlib renames
 * overloads that erase alike -- `Iterable<Int>.sum()` is `sumOfInt`, `Iterable<Long>.sum()` `sumOfLong` -- and a
 * library type built under the Kotlin name kept one of the seven and bound every `sum()` to it.
 */
internal fun jvmNameOf(symbol: KaCallableSymbol?): String? {
    if (symbol == null) return null
    (symbol.psi as? KtNamedFunction)?.let { jvmNameOverride(it) }?.let { return it }
    val annotation = symbol.annotations.firstOrNull { it.classId == JVM_NAME_ANNOTATION } ?: return null
    return (annotation.arguments.firstOrNull()?.expression as? KaAnnotationValue.ConstantValue)?.value?.value as? String
}
