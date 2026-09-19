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
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
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
                val named = symbols.map { companionWrittenAsItsClass(reference, it) }
                val project = named.filter { it !is KaPackageSymbol && isProjectDeclaration(it) }
                if (project.isEmpty()) {
                    libraryReferences++
                    return
                }
                consumer(reference, project)
            }
        })
    }

    /**
     * The class, when [reference] is a companion object reached through its class's name: `Widget.create(1)` for a
     * `create` on `Widget`'s companion. Otherwise [symbol] unchanged.
     *
     * ⛔ THE EXPRESSION DENOTES THE COMPANION, THE NAME SPELLS THE CLASS. Kotlin inserts the companion implicitly,
     * so K2 resolves the name `Widget` to `Widget.Companion` -- but the text at that position is the CLASS's simple
     * name, and it is the class's rename that must change it. Recording the companion there told a renamer that
     * `Widget.create(1)` names nothing it was renaming, and it left the call spelling the old name.
     *
     * A companion written out (`Widget.Companion.create(1)`) is left alone: there the text really is the
     * companion's own name, and `Widget` is a reference to the class in its own right.
     */
    private fun KaSession.companionWrittenAsItsClass(reference: KtNameReferenceExpression, symbol: KaSymbol): KaSymbol {
        if (symbol !is KaNamedClassSymbol || symbol.classKind != KaClassKind.COMPANION_OBJECT) return symbol
        val written = reference.getReferencedName()
        if (written == symbol.name.asString()) return symbol
        val owner = symbol.containingDeclaration as? KaNamedClassSymbol ?: return symbol
        return if (written == owner.name.asString()) owner else symbol
    }

    /**
     * Every **implicit lambda label** of a Kotlin file: the `before` of `routes.before { … return@before … }`. It is
     * spelled with the CALLED FUNCTION'S NAME, so renaming the function must rename it -- kotlinc says
     * `unresolved label` otherwise -- but it is not a name reference, and [walk] rightly does not see it: K2 resolves
     * a label to the lambda, not to the function whose name it borrows. The desugared CST is no help either, giving a
     * lambda no source range to search. So the binding is made here, syntactically, where the call is still written.
     *
     * A label belongs to the INNERMOST enclosing lambda argument whose call bears its name, so the inner `it` of
     * `first { … first { return@first x } … }` wins, as Kotlin's own scoping gives it. Two labels are not the
     * function's and are skipped: one on an explicitly labelled lambda (`before label@{ … }` -- `label` is the
     * author's word and `before` no longer labels anything), and one naming a function in no call around it (a
     * `this@name` inside the declaration NAMES ITS OWN receiver, which is not this).
     */
    fun KaSession.walkLabels(ktFile: KtFile, consumer: KaSession.(KtLabelReferenceExpression, KaSymbol) -> Unit) {
        for (label in PsiTreeUtil.findChildrenOfType(ktFile, KtLabelReferenceExpression::class.java)) {
            val name = label.getReferencedName()
            var p: PsiElement? = label.parent
            while (p != null) {
                // the author's own label of an enclosing expression: it, not a function, is what `@name` names
                if (p is KtLabeledExpression && p.getLabelName() == name) break
                val call = (p as? KtLambdaArgument)?.takeIf { it.getLambdaExpression()?.parent !is KtLabeledExpression }
                    ?.let { it.parent as? KtCallExpression }
                if (call != null && (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == name) {
                    val symbol = try {
                        call.resolveToCall()?.singleFunctionCallOrNull()?.symbol
                    } catch (e: RuntimeException) {
                        failedReferences++
                        null
                    }
                    if (symbol != null && isProjectDeclaration(symbol)) consumer(label, symbol)
                    break
                }
                p = p.parent
            }
        }
    }

    /**
     * Every KDoc name of a Kotlin file that K2 resolves to a project declaration: a link `[Config.subConfig]`, the
     * subject of `@see`, `@throws`, `@param`, ... Each segment of a qualified name is a name of its own (`Config`
     * resolves to the class, `subConfig` to the member). Separate from [walk]: a documentation link is not a
     * reference in code, and must not be counted or recorded as one.
     */
    fun KaSession.walkDocs(ktFile: KtFile, consumer: KaSession.(KDocName, List<KaSymbol>) -> Unit) {
        for (name in PsiTreeUtil.findChildrenOfType(ktFile, KDocName::class.java)) {
            val symbols = try {
                name.mainReference.resolveToSymbols().toList()
            } catch (e: RuntimeException) {
                continue
            }
            val project = symbols.filter { it !is KaPackageSymbol && isProjectDeclaration(it) }
            if (project.isNotEmpty()) consumer(name, project)
        }
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
