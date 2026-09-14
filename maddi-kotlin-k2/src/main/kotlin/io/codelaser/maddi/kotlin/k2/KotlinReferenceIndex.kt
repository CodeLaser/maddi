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

import io.codelaser.maddi.cst.api.element.Source
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Where every declaration of a Kotlin project is named: its declaration and each reference to it, as the exact
 * range of the identifier, resolved by K2 while the parse's session is alive. The CST is for what the code does;
 * this is for where an editor has to write. (On detekt the CST records only ~30% of these references at an editable
 * position; the index has all that K2 resolves.)
 *
 * **Identity is a position.** A declaration is keyed by where its name stands ([DeclarationKey]: file and the
 * identifier's first character). A CST [Info] reaches the same key through its own name detail ([keyOf]), so a
 * caller holding a `MethodInfo` finds its references without any name mangling. A constructor is keyed by its class,
 * since a call spells the class's name.
 *
 * **Overrides are families.** A callable and everything it overrides or is overridden by, within the project, share
 * one family ([family]); renaming a member renames the family. A family that overrides a declaration outside the
 * project cannot be renamed at all ([overridesOutsideProject]).
 *
 * Not included: KDoc links (`[name]`), string contents, and references K2 does not resolve.
 */
class KotlinReferenceIndex : KotlinParseObserver() {

    /** A declaration, by the position of the first character of its name. */
    data class DeclarationKey(val uri: String, val line: Int, val column: Int)

    /**
     * One spelling of a declaration's name: 1-based, end inclusive, as every maddi [Source]. [sharedWith] lists the
     * other declarations the same spelling names (an import of an overloaded name): renaming one of them alone
     * cannot rewrite this occurrence.
     */
    data class Occurrence(val uri: String, val beginLine: Int, val beginPos: Int, val endLine: Int, val endPos: Int,
                          val name: String, val isDeclaration: Boolean,
                          val sharedWith: Set<DeclarationKey> = emptySet())

    private val declarations = HashMap<DeclarationKey, Occurrence>()
    private val references = HashMap<DeclarationKey, MutableList<Occurrence>>()
    private val parent = HashMap<DeclarationKey, DeclarationKey>()
    private val outsideProject = HashSet<DeclarationKey>()
    var referenceCount = 0; private set
    var unresolvedReferences = 0; private set

    // ---------------------------------------------------------------------------------------------------------------
    // building

    override fun observe(runtime: Runtime, ktFiles: List<KtFile>, types: List<TypeInfo>,
                         sourceSetName: (KtFile) -> String) {
        val walker = KotlinReferenceWalker(ktFiles.mapNotNull { it.virtualFile?.url }.toHashSet())
        for (ktFile in ktFiles) {
            val url = ktFile.virtualFile?.url ?: continue
            analyze(ktFile) {
                indexDeclarations(runtime, ktFile, url, walker)
                with(walker) {
                    walk(ktFile) { reference, symbols ->
                        val keys = symbols.mapNotNull { namedDeclaration(it)?.let { d -> keyOf(runtime, d) } }.toSet()
                        for (key in keys) {
                            references.getOrPut(key) { ArrayList() } += occurrence(runtime, url,
                                reference.getReferencedNameElement(), reference.getReferencedName(), false)
                                .copy(sharedWith = keys - key)
                        }
                        if (keys.isNotEmpty()) referenceCount++
                    }
                }
            }
        }
        unresolvedReferences += walker.unresolvedReferences
    }

    /** Every named declaration of the file, and the override edges of its callables. */
    private fun KaSession.indexDeclarations(runtime: Runtime, ktFile: KtFile, url: String, walker: KotlinReferenceWalker) {
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                super.visitNamedDeclaration(declaration)
                val nameIdentifier = declaration.nameIdentifier ?: return
                val key = keyOf(runtime, declaration) ?: return
                declarations[key] = occurrence(runtime, url, nameIdentifier, declaration.name ?: return, true)
                // only these can override; asking K2 for the symbol of anything else can throw (a named parameter
                // of a function type has none)
                val canOverride = declaration is KtNamedFunction || declaration is KtProperty
                                  || declaration is KtParameter && declaration.hasValOrVar()
                if (!canOverride) return
                val overriddenSymbols = try {
                    (declaration.symbol as? KaCallableSymbol)?.allOverriddenSymbols?.toList() ?: return
                } catch (e: RuntimeException) {
                    return
                }
                for (overridden in overriddenSymbols) {
                    val overriddenDeclaration = with(walker) {
                        if (isProjectDeclaration(overridden)) namedDeclaration(overridden) else null
                    }
                    val overriddenKey = overriddenDeclaration?.let { keyOf(runtime, it) }
                    if (overriddenKey == null) outsideProject += key else union(key, overriddenKey)
                }
            }
        })
    }

    private fun keyOf(runtime: Runtime, declaration: KtNamedDeclaration): DeclarationKey? {
        val url = declaration.containingFile?.virtualFile?.url ?: return null
        val name = sourceOf(runtime, declaration.nameIdentifier ?: return null, "-")
        return DeclarationKey(url, name.beginLine(), name.beginPos())
    }

    private fun occurrence(runtime: Runtime, url: String, identifier: com.intellij.psi.PsiElement, name: String,
                           isDeclaration: Boolean): Occurrence {
        val s = sourceOf(runtime, identifier, "-")
        return Occurrence(url, s.beginLine(), s.beginPos(), s.endLine(), s.endPos(), name, isDeclaration)
    }

    private fun find(key: DeclarationKey): DeclarationKey {
        var root = key
        while (true) root = parent[root] ?: return root
    }

    private fun union(a: DeclarationKey, b: DeclarationKey) {
        val ra = find(a)
        val rb = find(b)
        if (ra != rb) parent[ra] = rb
    }

    // ---------------------------------------------------------------------------------------------------------------
    // reading

    /**
     * The key of a CST declaration: its compilation unit and the position its name was recorded at (the name detail
     * of its source). Null when the CST recorded no name position for it, e.g. a synthesized accessor.
     */
    fun keyOf(info: Info): DeclarationKey? {
        val uri = info.compilationUnit()?.uri()?.toString() ?: return null
        val name = info.simpleName()
        var at: Source? = null
        info.source()?.detailedSources()?.forEach { key, source ->
            if (at == null && key is String && key == name && source.beginLine() > 0) at = source
        }
        return at?.let { DeclarationKey(uri, it.beginLine(), it.beginPos()) }
    }

    /** Where the declaration with [key] spells its name, if it was indexed. */
    fun declaration(key: DeclarationKey): Occurrence? = declarations[key]

    /** Every reference to the declaration with [key], in no particular order. */
    fun references(key: DeclarationKey): List<Occurrence> = references[key].orEmpty()

    /** The declaration and everything overriding or overridden by it within the project; just [key] if nothing. */
    fun family(key: DeclarationKey): Set<DeclarationKey> {
        val root = find(key)
        val members = (declarations.keys + parent.keys).filterTo(HashSet()) { find(it) == root }
        members += key
        return members
    }

    /** True when some member of [key]'s family overrides a declaration outside the project (a library contract). */
    fun overridesOutsideProject(key: DeclarationKey): Boolean = family(key).any { it in outsideProject }

    /** Every occurrence a rename of [key]'s family has to rewrite: each member's declaration and references. */
    fun occurrencesOfFamily(key: DeclarationKey): List<Occurrence> =
        family(key).flatMap { listOfNotNull(declarations[it]) + references(it) }

    fun declarationCount(): Int = declarations.size
}
