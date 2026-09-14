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

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.fakeOverrideOriginal
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Every name reference of a Kotlin file, resolved by K2, split into references to the project's own declarations
 * (handed to the consumer) and the rest (counted). Shared by the observers so they agree on what "a project
 * reference" is: a `KtNameReferenceExpression` spelled as an identifier (not an operator), outside the package
 * directive, resolving to a declaration in one of [projectFiles] (or to Java source).
 *
 * A reference can name several declarations at once: `import a.path` imports every overload of `path`. The
 * consumer gets all of them that belong to the project, never an empty list.
 */
internal class KotlinReferenceWalker(private val projectFiles: Set<String>) {
    var libraryReferences = 0; private set
    var unresolvedReferences = 0; private set
    var failedReferences = 0; private set

    @OptIn(KaExperimentalApi::class)
    fun KaSession.walk(ktFile: KtFile, onUnresolved: (KtNameReferenceExpression) -> Unit = {},
                       consumer: KaSession.(KtNameReferenceExpression, List<KaSymbol>) -> Unit) {
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)
                val reference = expression as? KtNameReferenceExpression ?: return
                if (reference.getReferencedNameElementType() != KtTokens.IDENTIFIER) return
                if (PsiTreeUtil.getParentOfType(reference, KtPackageDirective::class.java) != null) return
                // resolveSymbol answers expression positions; a type reference or import segment needs the
                // PSI reference instead
                val symbols = try {
                    reference.resolveSymbol()?.let { listOf(it) }
                        ?: reference.mainReference.resolveToSymbol()?.let { listOf(it) }
                        ?: reference.mainReference.resolveToSymbols().toList() // an import of an overloaded name
                } catch (e: RuntimeException) {
                    failedReferences++
                    return
                }
                if (symbols.isEmpty()) {
                    unresolvedReferences++
                    onUnresolved(reference)
                    return
                }
                if (symbols.all { it is KaPackageSymbol }) return
                val project = symbols.filter { it !is KaPackageSymbol && isProjectDeclaration(it) }
                if (project.isEmpty()) {
                    libraryReferences++
                    return
                }
                consumer(reference, project)
            }
        })
    }

    fun KaSession.isProjectDeclaration(symbol: KaSymbol): Boolean {
        if (symbol.origin == KaSymbolOrigin.JAVA_SOURCE) return true
        val url = declarationPsi(symbol)?.containingFile?.virtualFile?.url ?: return false
        return url in projectFiles
    }

    /** The declaration's PSI, looking through a fake (substitution/intersection) override to what it copies. */
    fun KaSession.declarationPsi(symbol: KaSymbol): PsiElement? =
        symbol.psi ?: (symbol as? KaCallableSymbol)?.fakeOverrideOriginal?.psi

    /**
     * The named declaration a reference to [symbol] renames with: the declaration itself, except that a constructor
     * is named by its class (a call `Foo()` spells the class's name).
     */
    fun KaSession.namedDeclaration(symbol: KaSymbol): KtNamedDeclaration? =
        when (val psi = declarationPsi(symbol)) {
            is KtConstructor<*> -> psi.getContainingClassOrObject()
            is KtNamedDeclaration -> psi
            else -> null
        }
}
