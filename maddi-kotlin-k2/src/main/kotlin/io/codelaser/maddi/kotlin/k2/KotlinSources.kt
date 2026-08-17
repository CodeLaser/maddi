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

import com.intellij.psi.PsiElement
import org.e2immu.language.cst.api.element.DetailedSources
import org.e2immu.language.cst.api.element.Source
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.type.ParameterizedType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Real 1-based source positions (end column inclusive — maddi's convention) for a PSI element, carrying
 * a statement [index]. Computed from the file's IntelliJ `Document` (line offsets cached, so O(log n) per
 * element); the K2 PSI gives every token a faithful `textRange`, which is what makes detailed sub-element
 * sources possible from this single parse. Falls back to an indexed `noSource()` for elements without a
 * document (e.g. synthetic ones). Shared by `KotlinScan` (declarations) and `KotlinBodyConverter` (bodies).
 */
internal fun sourceOf(runtime: Runtime, psi: PsiElement, index: String): Source {
    val document = psi.containingFile?.viewProvider?.document ?: return runtime.noSource().withIndex(index)
    val range = psi.textRange ?: return runtime.noSource().withIndex(index)
    val lastOffset = (range.endOffset - 1).coerceAtLeast(range.startOffset) // inclusive last character
    val startLine = document.getLineNumber(range.startOffset)
    val endLine = document.getLineNumber(lastOffset)
    return runtime.newParserSource(index,
        startLine + 1, range.startOffset - document.getLineStartOffset(startLine) + 1,
        endLine + 1, lastOffset - document.getLineStartOffset(endLine) + 1)
}

/** Put `key -> position of [psi]` into this builder, if [psi] is present. */
internal fun DetailedSources.Builder.putPsi(runtime: Runtime, key: Any, psi: PsiElement?) {
    if (psi != null) put(key, sourceOf(runtime, psi, "-"))
}

/**
 * Record the source position of each EXPLICIT modifier keyword of [owner], keyed by the runtime modifier
 * object [mapper] maps it to (mirroring java-openjdk's `attachModifiers`: keyed by the same singleton the
 * builder holds). Modifiers that are implicit in Kotlin (e.g. the default `public`/`final`, or `val` as
 * finality) have no keyword, so they correctly get no source. A `null` from [mapper] is skipped.
 */
internal fun DetailedSources.Builder.attachModifiers(runtime: Runtime, owner: KtModifierListOwner,
                                                     mapper: Runtime.(KtModifierKeywordToken) -> Any?) {
    val modifierList = owner.modifierList ?: return
    KtTokens.MODIFIER_KEYWORDS_ARRAY.forEach { token ->
        val keyword = modifierList.getModifier(token) ?: return@forEach
        runtime.mapper(token)?.let { put(it, sourceOf(runtime, keyword, "-")) }
    }
}

/** A Kotlin visibility keyword to its runtime type modifier (shared shape; per-element families differ). */
private inline fun <T> KtModifierKeywordToken.visibility(pub: () -> T, priv: () -> T, prot: () -> T, internal: () -> T): T? =
    when (this) {
        KtTokens.PUBLIC_KEYWORD -> pub()
        KtTokens.PRIVATE_KEYWORD -> priv()
        KtTokens.PROTECTED_KEYWORD -> prot()
        KtTokens.INTERNAL_KEYWORD -> internal()
        else -> null
    }

internal fun Runtime.typeModifierFor(token: KtModifierKeywordToken): Any? =
    token.visibility(::typeModifierPublic, ::typeModifierPrivate, ::typeModifierProtected, ::typeModifierInternal)
        ?: when (token) {
            KtTokens.ABSTRACT_KEYWORD -> typeModifierAbstract()
            KtTokens.SEALED_KEYWORD -> typeModifierSealed()
            KtTokens.FINAL_KEYWORD -> typeModifierFinal()
            else -> null
        }

internal fun Runtime.methodModifierFor(token: KtModifierKeywordToken): Any? =
    token.visibility(::methodModifierPublic, ::methodModifierPrivate, ::methodModifierProtected, ::methodModifierInternal)
        ?: when (token) {
            KtTokens.ABSTRACT_KEYWORD -> methodModifierAbstract()
            KtTokens.FINAL_KEYWORD -> methodModifierFinal()
            else -> null
        }

internal fun Runtime.fieldModifierFor(token: KtModifierKeywordToken): Any? =
    token.visibility(::fieldModifierPublic, ::fieldModifierPrivate, ::fieldModifierProtected, ::fieldModifierInternal)
        ?: when (token) {
            KtTokens.FINAL_KEYWORD -> fieldModifierFinal()
            else -> null
        }

/**
 * Recursively detail a type reference (mirroring java-openjdk's `convertTree`): each nested type's
 * `TypeInfo` (or `TypeParameter` for a type variable) keyed to its identifier position, plus
 * `TYPE_ARGUMENT_COMMAS` per generic argument list (e.g. the comma in `Map<K, V>`). Shared by KotlinScan
 * (declarations) and KotlinBodyConverter (local variables).
 */
internal fun DetailedSources.Builder.putTypeReference(runtime: Runtime, type: ParameterizedType, typeReference: KtTypeReference?) {
    val userType = typeReference?.typeElement as? KtUserType ?: return
    val referenceExpression = userType.referenceExpression
    type.typeInfo()?.let { putPsi(runtime, it, referenceExpression) }
    type.typeParameter()?.let { putPsi(runtime, it, referenceExpression) }
    val argumentList = userType.typeArgumentList ?: return
    val commas = argumentList.node.getChildren(null).orEmpty()
        .filter { it.elementType == KtTokens.COMMA }.map { sourceOf(runtime, it.psi, "-") }
    if (commas.isNotEmpty()) putList(DetailedSources.TYPE_ARGUMENT_COMMAS, commas)
    argumentList.arguments.forEachIndexed { i, projection ->
        type.parameters().getOrNull(i)?.let { putTypeReference(runtime, it, projection.typeReference) }
    }
}
