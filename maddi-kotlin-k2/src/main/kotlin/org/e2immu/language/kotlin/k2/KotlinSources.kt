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
import org.e2immu.language.cst.api.element.Source
import org.e2immu.language.cst.api.runtime.Runtime

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
