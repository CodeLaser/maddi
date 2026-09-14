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
import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.element.Source
import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.MethodReference
import io.codelaser.maddi.cst.api.expression.TypeExpression
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.info.TypeParameter
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration
import io.codelaser.maddi.cst.api.type.ParameterizedType
import io.codelaser.maddi.cst.api.variable.Variable
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.fakeOverrideOriginal
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgumentName
import java.util.EnumMap
import java.util.IdentityHashMap

/**
 * An instrument, not part of the parse: how much of what a source **editor** has to update is visible in the CST.
 *
 * The oracle is K2 itself. Every name reference in the Kotlin PSI (`KtNameReferenceExpression`: variable and
 * property reads, callees, type references, imports, named arguments, callable references) is resolved; those that
 * resolve to a declaration of the project (source origin, in a project file) are the references a rename or a move
 * must rewrite. Each is then looked up in the CST converted from the same file, by name and position, and graded:
 *
 * - [Tier.EXACT]: a CST element referring to that name either carries a detailed source at exactly the identifier,
 *   or has a range that starts or ends exactly at it: the identifier's range is known, an edit can be made.
 * - [Tier.CONTAINED]: a CST element referring to that name has a range containing the identifier, but not anchored
 *   at it: the reference is known, its position must be searched for inside the range.
 * - [Tier.COVERED]: no CST element names the reference, but the expression it is part of (the name itself, the call
 *   it is the callee of, or the qualified expression it sits in) has a CST expression with exactly its range: the
 *   construct was converted, the reference was desugared away (e.g. `Obj.f()` routed through a synthetic receiver).
 * - [Tier.DROPPED]: neither: the reference never reached the CST (e.g. inside a lambda passed to a library
 *   extension, a property initializer, an import). Being inside a converted *statement* does not count: a statement
 *   whose initializer became a placeholder keeps its range.
 *
 * Names match modulo JVM accessor naming (`x` matches `x`, `getX`, `setX`, `x$delegate`), since a property access
 * becomes an accessor call in the CST. Positions use [sourceOf], the computation the CST itself uses, so equal
 * means equal. Nothing is asserted here: a caller reads [report] or [rows].
 */
class ReferenceRecall(private val samplesPerCell: Int = 6) : KotlinParseObserver() {

    enum class Tier { EXACT, CONTAINED, COVERED, DROPPED }

    /** The syntactic position of the reference. */
    enum class Site { IMPORT, TYPE_REFERENCE, ANNOTATION, SUPER_TYPE, CALLEE, CALLABLE_REFERENCE, NAMED_ARGUMENT, NAME }

    /** The innermost construct the reference sits in; [LAMBDA_TO_LIBRARY] is a lambda argument of a library call. */
    enum class Region {
        IMPORT, DECLARATION_HEADER, FUNCTION_BODY, LAMBDA, LAMBDA_TO_LIBRARY, PROPERTY_INITIALIZER, ACCESSOR,
        INIT_BLOCK, DEFAULT_VALUE, OTHER
    }

    /** What the reference resolves to. */
    enum class Target { TYPE, TYPE_ALIAS, CONSTRUCTOR, FUNCTION, PROPERTY, PARAMETER, LOCAL, TYPE_PARAMETER, ENUM_ENTRY, OTHER }

    data class Row(val sourceSet: String, val file: String, val line: Int, val column: Int, val name: String,
                   val site: Site, val region: Region, val target: Target, val tier: Tier) {
        /** Written by hand, not by a build: not under a `build/` directory, and not a build script. */
        val handWritten: Boolean get() = "/build/" !in file && !file.endsWith(".kts")
    }

    private val rows = ArrayList<Row>()
    private val sourceLines = HashMap<String, List<String>>()
    private val unresolvedSamples = ArrayList<String>()
    private var walker = KotlinReferenceWalker(emptySet())
    val libraryReferences get() = walker.libraryReferences
    val unresolvedReferences get() = walker.unresolvedReferences
    val failedReferences get() = walker.failedReferences
    var filesWithoutCst = 0; private set

    fun rows(): List<Row> = rows

    // ---------------------------------------------------------------------------------------------------------------
    // measuring: called by the scan, while its K2 session is alive and after every file has been converted

    override fun observe(runtime: Runtime, ktFiles: List<KtFile>, types: List<TypeInfo>,
                         sourceSetName: (KtFile) -> String) {
        walker = KotlinReferenceWalker(ktFiles.mapNotNull { it.virtualFile?.url }.toHashSet())
        val typesByUri = types.groupBy { it.compilationUnit().uri().toString() }
        for (ktFile in ktFiles) {
            val url = ktFile.virtualFile?.url ?: continue
            val cstTypes = typesByUri[url].orEmpty()
            if (cstTypes.isEmpty()) filesWithoutCst++
            val index = CstIndex().also { idx -> cstTypes.forEach { idx.walkType(it) } }
            sourceLines[url] = ktFile.text.lines()
            val sourceSet = sourceSetName(ktFile)
            analyze(ktFile) { measureFile(runtime, ktFile, sourceSet, url, index) }
        }
    }

    private fun KaSession.measureFile(runtime: Runtime, ktFile: KtFile, sourceSet: String, url: String,
                                      index: CstIndex) = with(walker) {
        walk(ktFile, onUnresolved = { reference ->
            if (unresolvedSamples.size < samplesPerCell * 2) {
                val at = sourceOf(runtime, reference, "-")
                unresolvedSamples += "${url.substringAfterLast('/')}:${at.beginLine()}:${at.beginPos()} " +
                                     "'${reference.getReferencedName()}'  |  " +
                                     (reference.parent?.text?.lineSequence()?.firstOrNull()?.trim()?.take(100) ?: "")
            }
        }) { reference, symbols ->
            val symbol = symbols.first()
            val identifier = sourceOf(runtime, reference.getReferencedNameElement(), "-")
            val name = reference.getReferencedName()
            val enclosing = enclosingExpressions(reference).map { sourceOf(runtime, it, "-") }
            rows += Row(sourceSet, url, identifier.beginLine(), identifier.beginPos(), name, site(reference),
                region(reference), target(symbol), index.grade(name, identifier, enclosing))
        }
    }

    /**
     * The expressions a converted reference would be part of: the name itself, the call it is the callee of, the
     * qualified expression it is the selector or receiver of. A CST expression with exactly one of these ranges means
     * the construct was converted even if no CST element names the reference.
     */
    private fun enclosingExpressions(reference: KtNameReferenceExpression): List<PsiElement> {
        val result = ArrayList<PsiElement>()
        var node: PsiElement = reference
        repeat(3) {
            result += node
            val parent = node.parent
            node = when {
                parent is KtCallExpression && parent.calleeExpression == node -> parent
                parent is org.jetbrains.kotlin.psi.KtQualifiedExpression -> parent
                else -> return result
            }
        }
        return result
    }

    private fun site(reference: KtNameReferenceExpression): Site {
        val parent = reference.parent
        return when {
            PsiTreeUtil.getParentOfType(reference, KtImportDirective::class.java) != null -> Site.IMPORT
            parent is KtUserType && PsiTreeUtil.getParentOfType(reference, KtAnnotationEntry::class.java) != null -> Site.ANNOTATION
            parent is KtUserType && PsiTreeUtil.getParentOfType(reference, KtSuperTypeListEntry::class.java) != null -> Site.SUPER_TYPE
            parent is KtUserType -> Site.TYPE_REFERENCE
            parent is KtCallExpression && parent.calleeExpression == reference -> Site.CALLEE
            parent is KtCallableReferenceExpression && parent.callableReference == reference -> Site.CALLABLE_REFERENCE
            parent is KtValueArgumentName -> Site.NAMED_ARGUMENT
            else -> Site.NAME
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.region(reference: KtNameReferenceExpression): Region {
        if (PsiTreeUtil.getParentOfType(reference, KtImportDirective::class.java) != null) return Region.IMPORT
        var child: PsiElement = reference
        var node: PsiElement? = reference.parent
        while (node != null && node !is KtFile) {
            when (node) {
                is KtLambdaExpression -> return if (isLibraryCallArgument(node)) Region.LAMBDA_TO_LIBRARY else Region.LAMBDA
                is KtPropertyAccessor -> return Region.ACCESSOR
                is KtAnonymousInitializer -> return Region.INIT_BLOCK
                is KtParameter -> if (child == node.defaultValue) return Region.DEFAULT_VALUE
                is KtProperty -> if (!node.isLocal && (child == node.initializer || child == node.delegate))
                    return Region.PROPERTY_INITIALIZER
                is KtNamedFunction -> return if (child == node.bodyExpression) Region.FUNCTION_BODY else Region.DECLARATION_HEADER
                is KtClassOrObject -> return Region.DECLARATION_HEADER
            }
            child = node
            node = node.parent
        }
        return Region.OTHER
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.isLibraryCallArgument(lambda: KtLambdaExpression): Boolean {
        val call = PsiTreeUtil.getParentOfType(lambda, KtCallExpression::class.java) ?: return false
        if (!PsiTreeUtil.isAncestor(call.valueArgumentList ?: call, lambda, false)
            && call.lambdaArguments.none { PsiTreeUtil.isAncestor(it, lambda, false) }) return false
        val callee = try {
            call.resolveSymbol()
        } catch (e: RuntimeException) {
            null
        } ?: return false
        val origin = callee.fakeOverrideOriginal.origin
        return origin == KaSymbolOrigin.LIBRARY || origin == KaSymbolOrigin.JAVA_LIBRARY
    }

    private fun target(symbol: KaSymbol): Target = when (symbol) {
        is KaConstructorSymbol -> Target.CONSTRUCTOR
        is KaTypeAliasSymbol -> Target.TYPE_ALIAS
        is KaClassSymbol -> Target.TYPE
        is KaEnumEntrySymbol -> Target.ENUM_ENTRY
        is KaPropertySymbol -> Target.PROPERTY
        is KaValueParameterSymbol -> Target.PARAMETER
        is KaLocalVariableSymbol -> Target.LOCAL
        is KaTypeParameterSymbol -> Target.TYPE_PARAMETER
        is KaFunctionSymbol -> Target.FUNCTION
        else -> Target.OTHER
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the CST side: every recorded position of one file, bucketed by line

    /** A position the CST recorded for [name]: a detailed source ([point]) or the range of a referring expression. */
    private class Rec(val name: String, val point: Boolean,
                      val beginLine: Int, val beginPos: Int, val endLine: Int, val endPos: Int) {
        fun contains(s: Source): Boolean =
            before(beginLine, beginPos, s.beginLine(), s.beginPos()) && before(s.endLine(), s.endPos(), endLine, endPos)

        private fun before(l1: Int, c1: Int, l2: Int, c2: Int) = l1 < l2 || (l1 == l2 && c1 <= c2)
    }

    private class CstIndex {
        private val byLine = HashMap<Int, MutableList<Rec>>()
        private val expressionRanges = HashSet<List<Int>>()
        private val seen = IdentityHashMap<Any, Boolean>()

        private fun rangeKey(s: Source): List<Int> = listOf(s.beginLine(), s.beginPos(), s.endLine(), s.endPos())

        fun walkType(typeInfo: TypeInfo) {
            if (seen.put(typeInfo, true) != null) return
            addDetails(typeInfo)
            typeInfo.subTypes().forEach { walkType(it) }
            (typeInfo.constructors() + typeInfo.methodStream().toList()).forEach { walkMethod(it) }
            typeInfo.fields().forEach { walkField(it) }
        }

        private fun walkMethod(methodInfo: MethodInfo) {
            if (seen.put(methodInfo, true) != null) return
            addDetails(methodInfo)
            methodInfo.parameters().forEach { addDetails(it) }
            methodInfo.methodBody()?.visit { visitElement(it) }
        }

        private fun walkField(fieldInfo: FieldInfo) {
            addDetails(fieldInfo)
            fieldInfo.initializer()?.visit { visitElement(it) }
        }

        private fun visitElement(element: Element): Boolean {
            if (seen.put(element, true) != null) return false
            addDetails(element)
            val source = element.source()
            if (source != null && source.beginLine() > 0 && element is Expression) {
                expressionRanges += rangeKey(source)
                referenceName(element)?.let { name ->
                    add(Rec(name, false, source.beginLine(), source.beginPos(), source.endLine(), source.endPos()))
                }
            }
            when (element) {
                is ConstructorCall -> element.anonymousClass()?.let { walkType(it) }
                is LocalTypeDeclaration -> walkType(element.typeInfo())
                is Lambda -> element.methodInfo().parameters().forEach { addDetails(it) }
            }
            return true
        }

        private fun addDetails(element: Element) {
            element.source()?.detailedSources()?.forEach { key, source ->
                val name = keyName(key)
                if (name != null && source != null && source.beginLine() > 0) {
                    add(Rec(name, true, source.beginLine(), source.beginPos(), source.endLine(), source.endPos()))
                }
            }
        }

        private fun add(rec: Rec) {
            val last = minOf(rec.endLine, rec.beginLine + MAX_BUCKETED_LINES)
            for (line in rec.beginLine..last) byLine.getOrPut(line) { ArrayList() }.add(rec)
        }

        fun grade(name: String, identifier: Source, enclosing: List<Source>): Tier {
            val names = acceptedNames(name)
            val candidates = byLine[identifier.beginLine()].orEmpty()
            var best = Tier.DROPPED
            for (rec in candidates) {
                if (rec.name !in names || !rec.contains(identifier)) continue
                val tier = when {
                    rec.point -> if (rec.beginLine == identifier.beginLine() && rec.beginPos == identifier.beginPos()
                                     && rec.endLine == identifier.endLine() && rec.endPos == identifier.endPos())
                        Tier.EXACT else Tier.CONTAINED
                    (rec.beginLine == identifier.beginLine() && rec.beginPos == identifier.beginPos())
                            || (rec.endLine == identifier.endLine() && rec.endPos == identifier.endPos()) -> Tier.EXACT
                    else -> Tier.CONTAINED
                }
                if (tier.ordinal < best.ordinal) best = tier
                if (best == Tier.EXACT) return best
            }
            if (best == Tier.DROPPED && enclosing.any { rangeKey(it) in expressionRanges }) return Tier.COVERED
            return best
        }

        companion object {
            const val MAX_BUCKETED_LINES = 2_000

            fun acceptedNames(name: String): Set<String> {
                val capitalized = name.replaceFirstChar { it.uppercaseChar() }
                return setOf(name, "get$capitalized", "set$capitalized", "$name\$delegate")
            }

            fun referenceName(element: Element): String? = when (element) {
                is VariableExpression -> element.variable().simpleName()
                is MethodCall -> element.methodInfo().name()
                is ConstructorCall -> element.constructor()?.typeInfo()?.simpleName()
                    ?: element.parameterizedType()?.typeInfo()?.simpleName()
                is MethodReference -> element.methodInfo().name()
                is TypeExpression -> element.parameterizedType().typeInfo()?.simpleName()
                else -> null
            }

            fun keyName(key: Any?): String? = when (key) {
                is String -> key
                is TypeInfo -> key.simpleName()
                is MethodInfo -> if (key.isConstructor) key.typeInfo().simpleName() else key.name()
                is FieldInfo -> key.name()
                is Variable -> key.simpleName()
                is TypeParameter -> key.simpleName()
                is ParameterizedType -> key.typeInfo()?.simpleName() ?: key.typeParameter()?.simpleName()
                else -> null
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // reading the result

    /**
     * The tables are over the **hand-written** rows ([Row.handWritten]): generated sources and build scripts are
     * counted in the headline and the per-source-set table, but they are nobody's refactoring target.
     */
    fun report(): String = buildString {
        val handWritten = rows.filter { it.handWritten }
        appendLine("reference recall: ${rows.size} project reference(s) in the PSI " +
                   "(skipped: $libraryReferences to library declarations, $unresolvedReferences unresolved, " +
                   "$failedReferences failed to resolve; $filesWithoutCst file(s) with no CST type)")
        appendLine(tierLine("all", rows))
        appendLine(tierLine("hand-written", handWritten))
        appendLine()
        appendLine("(tables below: hand-written only)")
        table("by site", handWritten, Site.entries) { it.site }
        table("by region", handWritten, Region.entries) { it.region }
        table("by target", handWritten, Target.entries) { it.target }
        appendLine("by source set (all rows)")
        rows.groupBy { it.sourceSet }.entries.sortedByDescending { it.value.size }
            .forEach { (set, rs) -> appendLine(tierLine(set.takeLast(22), rs)) }
        appendLine()
        if (unresolvedSamples.isNotEmpty()) {
            appendLine("unresolved samples:")
            unresolvedSamples.forEach { appendLine("    $it") }
            appendLine()
        }
        appendLine("samples (hand-written, per region, not EXACT; spread over the corpus):")
        for (region in Region.entries) {
            for (tier in listOf(Tier.DROPPED, Tier.COVERED, Tier.CONTAINED)) {
                val cell = handWritten.filter { it.region == region && it.tier == tier }
                if (cell.isEmpty()) continue
                val step = (cell.size / samplesPerCell).coerceAtLeast(1)
                appendLine("  $region / $tier (${cell.size})")
                cell.filterIndexed { i, _ -> i % step == 0 }.take(samplesPerCell)
                    .forEach { appendLine("    ${describe(it)}") }
            }
        }
    }

    private fun <K : Enum<K>> StringBuilder.table(title: String, rs: List<Row>, keys: List<K>, key: (Row) -> K) {
        appendLine(title)
        val grouped = rs.groupBy(key)
        for (k in keys) grouped[k]?.let { appendLine(tierLine(k.name, it)) }
        appendLine()
    }

    private fun tierLine(label: String, rs: List<Row>): String {
        val counts = EnumMap<Tier, Int>(Tier::class.java)
        rs.forEach { counts.merge(it.tier, 1, Int::plus) }
        val n = rs.size.coerceAtLeast(1)
        fun pct(t: Tier) = "%5.1f%%".format(100.0 * (counts[t] ?: 0) / n)
        return "  %-22s %8d   exact %s   contained %s   covered %s   dropped %s".format(
            label, rs.size, pct(Tier.EXACT), pct(Tier.CONTAINED), pct(Tier.COVERED), pct(Tier.DROPPED))
    }

    private fun describe(row: Row): String {
        val line = sourceLines[row.file]?.getOrNull(row.line - 1)?.trim()?.take(110) ?: ""
        return "${row.file.substringAfterLast('/')}:${row.line}:${row.column} ${row.site} ${row.target} " +
               "'${row.name}'  |  $line"
    }
}
