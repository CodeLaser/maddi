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
import org.e2immu.language.cst.api.element.CompilationUnit
import org.e2immu.language.cst.api.element.DetailedSources
import org.e2immu.language.cst.api.element.RecordPattern
import org.e2immu.language.cst.api.element.Source
import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.expression.EmptyExpression
import org.e2immu.language.cst.api.expression.Expression
import org.e2immu.language.cst.api.expression.Lambda
import org.e2immu.language.cst.api.expression.VariableExpression
import org.e2immu.language.cst.api.info.FieldInfo
import org.e2immu.language.cst.api.info.MethodInfo
import org.e2immu.language.cst.api.info.MethodModifier
import org.e2immu.language.cst.api.info.ParameterInfo
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.info.Variance
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.Block
import org.e2immu.language.cst.api.statement.Statement
import org.e2immu.language.cst.api.statement.SwitchEntry
import org.e2immu.language.cst.api.variable.LocalVariable
import org.e2immu.language.cst.api.variable.Variable
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.api.type.ParameterizedType
import org.e2immu.language.cst.api.type.TypeNature
import org.e2immu.language.inspection.resource.InfoByFqn
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance as KotlinVariance
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import java.net.URI
import java.nio.file.Files

/**
 * Builds member declarations (their bodies). Injected into [KotlinBodyConverter] to break the
 * bodies<->declarations cycle: an `object : … { … }` expression (a body) builds the anonymous type's
 * members, which in turn build bodies.
 */
internal interface MemberConverter {
    fun KaSession.buildAnonProperty(owner: TypeInfo, property: KaPropertySymbol)
    fun KaSession.buildAnonMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo
}

/**
 * Converts Kotlin function bodies — statements, expressions, calls, lambdas, anonymous objects, and the
 * reference/overload resolution they need. Extracted from `KotlinScan`. Each conversion function is a
 * `KaSession` member extension: the K2 Analysis API (`.symbol`, `.expressionType`, …) needs `KaSession`
 * as the *receiver*, and Kotlin 2.4 context parameters do not act as implicit receivers for it — so the
 * receiver style stays, with `KotlinScan` reaching these via thin `with(bodyConverter) { … }` forwarders
 * (and this class reaching `KotlinTypeMapper` the same way). Member-building flows back through the
 * injected [MemberConverter], breaking the bodies<->declarations cycle.
 */
internal class KotlinBodyConverter(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    private val sourceSet: SourceSet,
    private val typeMapper: KotlinTypeMapper,
) {
    // set by KotlinScan after construction (the bodies<->declarations cycle)
    lateinit var memberConverter: MemberConverter

    // type mapping lives on the collaborator; it's a KaSession member extension, so reach it via with(…)
    private fun KaSession.mapType(type: KaType, owner: TypeInfo) = with(typeMapper) { mapType(type, owner) }

    /**
     * Convert a function body into a CST [Block]. Handles the two Kotlin body forms — a block body
     * `{ … }` and an expression body `= expr` (which becomes a single `return expr`, or an expression
     * statement when the function returns Unit). M3 scope: literal constants and returns; expressions
     * not yet understood become an explicit [Runtime.newEmptyExpression] placeholder rather than failing.
     */
    internal fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType,
                                      method: MethodInfo): Block {
        val psi = function.psi as? KtNamedFunction ?: return runtime.newBlockBuilder().build()
        val locals = mutableMapOf<String, Variable>() // names in scope; references resolve against this
        if (psi.hasBlockBody()) {
            return statementsToBlock(psi.bodyBlockExpression?.statements.orEmpty(), method, locals, "")
        }
        val block = runtime.newBlockBuilder()
        psi.bodyExpression?.let { body ->
            val expr = convertExpression(body, method, locals)
            val statement = if (returnType == runtime.voidParameterizedType()) runtime.newExpressionAsStatement(expr)
            else runtime.newReturnStatement(expr)
            block.addStatement(indexed(statement, "0"))
        }
        return block.build()
    }

    /** The analyzer requires every statement to carry a hierarchical source index ("0", "1", "1.0.0", …). */
    internal fun indexed(statement: Statement, index: String): Statement =
        statement.withSource(runtime.noSource().withIndex(index))

    /** Zero-pad [i] to the width of the largest index in a block of [n] statements (so they sort in order). */
    internal fun pad(i: Int, n: Int): String =
        i.toString().padStart((n - 1).coerceAtLeast(0).toString().length, '0')

    /** Build a block whose statements are indexed `<blockIndex>.<j>` (or just `<j>` at the method root). */
    private fun KaSession.statementsToBlock(statements: List<KtExpression>, method: MethodInfo,
                                            locals: MutableMap<String, Variable>, blockIndex: String): Block {
        val block = runtime.newBlockBuilder()
        if (blockIndex.isNotEmpty()) block.setSource(runtime.noSource().withIndex(blockIndex))
        statements.forEachIndexed { j, s ->
            val childIndex = if (blockIndex.isEmpty()) pad(j, statements.size) else "$blockIndex.${pad(j, statements.size)}"
            block.addStatement(convertStatement(s, method, locals, childIndex))
        }
        return block.build()
    }

    /** Convert one statement: a local `val`/`var`, an assignment, a `return`, or an expression statement. */
    internal fun KaSession.convertStatement(statement: KtExpression, method: MethodInfo,
                                           locals: MutableMap<String, Variable>, index: String): Statement {
        val raw = rawStatement(statement, method, locals, index)
        // apply the statement's full range + index, keeping any DetailedSources rawStatement attached (e.g. a
        // local-variable name)
        val whole = source(statement, index)
        val detailed = raw.source()?.detailedSources()
        return raw.withSource(if (detailed == null) whole else whole.withDetailedSources(detailed))
    }

    private fun source(psi: PsiElement, index: String): Source = sourceOf(runtime, psi, index)

    /**
     * A single-entry [DetailedSources] recording a source-form [marker] (e.g. `NULL_COALESCING`) at the
     * operator token [psi], so the refactoring engine can reproduce the original surface syntax of a
     * desugared node. Empty if [psi] is absent.
     */
    private fun marker(marker: Any, psi: PsiElement?): DetailedSources =
        runtime.newDetailedSourcesBuilder().also { if (psi != null) it.put(marker, source(psi, "-")) }.build()

    private fun KaSession.rawStatement(statement: KtExpression, method: MethodInfo,
                                       locals: MutableMap<String, Variable>, index: String): Statement = when {
        statement is KtProperty && statement.isLocal -> {
            val name = statement.name ?: "_"
            val type = (statement.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val initializer = statement.initializer?.let { convertExpression(it, method, locals) }
                ?: runtime.newEmptyExpression()
            val local = runtime.newLocalVariable(name, type, initializer)
            locals[name] = local
            // detail: name keyed by both the name String and the LocalVariable, plus the type reference
            val dsb = runtime.newDetailedSourcesBuilder()
            statement.nameIdentifier?.let { nameId ->
                val nameSource = source(nameId, "-")
                dsb.put(local.simpleName(), nameSource).put(local, nameSource)
            }
            dsb.putTypeReference(runtime, type, statement.typeReference)
            runtime.newLocalVariableCreation(local)
                .withSource(runtime.noSource().withDetailedSources(dsb.build()))
        }
        statement is KtBinaryExpression && isAssignment(statement.operationToken) -> {
            val left = statement.left
            val value = statement.right?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
            if (left is KtArrayAccessExpression && statement.operationToken == KtTokens.EQ) {
                runtime.newExpressionAsStatement(convertIndexedSet(left, value, method, locals)) // a[i] = v -> a.set(i, v)
            } else {
                val target = left?.let { convertExpression(it, method, locals) } as? VariableExpression
                if (target == null) runtime.newExpressionAsStatement(runtime.newEmptyExpression("k2-assign-target"))
                else {
                    val builder = runtime.newAssignmentBuilder().setTarget(target).setValue(value).setSource(runtime.noSource())
                    augmentedOperator(statement.operationToken)?.let { builder.setAssignmentOperator(it) } // x += y
                    runtime.newExpressionAsStatement(builder.build())
                }
            }
        }
        statement is KtReturnExpression -> runtime.newReturnStatement(
            statement.returnedExpression?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
        )
        statement is KtIfExpression -> runtime.newIfElseBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
            .setIfBlock(convertBlock(statement.then, method, locals, "$index.0"))
            .setElseBlock(convertBlock(statement.`else`, method, locals, "$index.1"))
            .setSource(runtime.noSource()).build()
        statement is KtWhileExpression -> runtime.newWhileBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
            .setBlock(convertBlock(statement.body, method, locals, "$index.0"))
            .setSource(runtime.noSource()).build()
        statement is KtForExpression -> {
            // for (x in iterable) { … } -> ForEachStatement; x is a local in scope for the body
            val parameter = statement.loopParameter
            val name = parameter?.name ?: "_"
            val type = (parameter?.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val loopVariable = runtime.newLocalVariable(name, type, runtime.newEmptyExpression())
            runtime.newForEachBuilder()
                .setInitializer(runtime.newLocalVariableCreation(loopVariable))
                .setExpression(statement.loopRange?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
                .setBlock(convertBlock(statement.body, method, locals + (name to loopVariable), "$index.0"))
                .setSource(runtime.noSource()).build()
        }
        statement is KtWhenExpression -> convertWhen(statement, method, locals, index)
        statement is KtDoWhileExpression -> runtime.newDoBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
            .setBlock(convertBlock(statement.body, method, locals, "$index.0"))
            .setSource(runtime.noSource()).build()
        statement is KtBreakExpression -> runtime.newBreakBuilder()
            .also { b -> statement.getLabelName()?.let { b.setGoToLabel(it) } } // break@label
            .setSource(runtime.noSource()).build()
        statement is KtContinueExpression -> runtime.newContinueBuilder()
            .also { b -> statement.getLabelName()?.let { b.setGoToLabel(it) } } // continue@label
            .setSource(runtime.noSource()).build()
        statement is KtThrowExpression -> runtime.newThrowBuilder()
            .setExpression(statement.thrownExpression?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
            .setSource(runtime.noSource()).build()
        statement is KtTryExpression -> convertTry(statement, method, locals, index)
        statement is KtDestructuringDeclaration -> convertDestructuring(statement, method, locals)
        else -> runtime.newExpressionAsStatement(convertExpression(statement, method, locals))
    }

    /**
     * `val (a, b) = p` -> a multi-variable [LocalVariableCreation] where each local's initializer is the
     * matching `p.componentN()` call (when that component resolves on `p`'s type; else a placeholder). The
     * locals enter scope so later references resolve.
     */
    private fun KaSession.convertDestructuring(statement: KtDestructuringDeclaration, method: MethodInfo,
                                               locals: MutableMap<String, Variable>): Statement {
        val initializer = statement.initializer?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
        val sourceType = initializer.parameterizedType().typeInfo()
        val variables = statement.entries.mapIndexed { i, entry ->
            val name = entry.name ?: "_"
            val type = (entry.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val component = sourceType?.let { resolveCallee(it, "component${i + 1}", listOf()) }
            val componentInit = component?.let {
                runtime.newMethodCallBuilder().setObject(initializer).setObjectIsImplicit(false).setMethodInfo(it)
                    .setParameterExpressions(listOf()).setConcreteReturnType(type).setTypeArguments(listOf())
                    .setSource(runtime.noSource()).build()
            } ?: runtime.newEmptyExpression("k2-component${i + 1}")
            runtime.newLocalVariable(name, type, componentInit).also { locals[name] = it }
        }
        if (variables.isEmpty()) return runtime.newExpressionAsStatement(runtime.newEmptyExpression("k2-destructuring"))
        val builder = runtime.newLocalVariableCreationBuilder().setLocalVariable(variables.first())
        variables.drop(1).forEach { builder.addOtherLocalVariable(it) }
        return builder.setSource(runtime.noSource()).build()
    }

    /**
     * `try { … } catch (e: T) { … } finally { … }` -> [org.e2immu.language.cst.api.statement.TryStatement].
     * Each sub-block is indexed like the Java parser: try block `index.0`, catch i `index.(i+1)`, finally
     * last. Kotlin catch is single-type (no Java-style union), and the catch variable is in scope for its
     * block.
     */
    private fun KaSession.convertTry(statement: KtTryExpression, method: MethodInfo,
                                     locals: MutableMap<String, Variable>, index: String): Statement {
        val builder = runtime.newTryBuilder().setSource(runtime.noSource())
        builder.setBlock(convertBlock(statement.tryBlock, method, locals, "$index.0"))
        statement.catchClauses.forEachIndexed { i, catch ->
            val parameter = catch.catchParameter
            val type = (parameter?.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val name = parameter?.name ?: "e"
            val catchVariable = runtime.newLocalVariable(name, type, runtime.newEmptyExpression())
            builder.addCatchClause(
                runtime.newCatchClauseBuilder()
                    .addType(type)
                    .setCatchVariable(catchVariable)
                    .setFinal(false)
                    .setBlock(convertBlock(catch.catchBody, method, locals + (name to catchVariable), "$index.${i + 1}"))
                    .setSource(runtime.noSource()).build()
            )
        }
        statement.finallyBlock?.let { finally ->
            builder.setFinallyBlock(convertBlock(finally.finalExpression, method, locals, "$index.${statement.catchClauses.size + 1}"))
        }
        return builder.build()
    }

    /**
     * `when (subject) { v -> …; a, b -> …; else -> … }` -> [SwitchStatementNewStyle]. Each arm becomes a
     * [SwitchEntry] (its `when`-condition expressions are the case labels; `else` is a single
     * `EmptyExpression`), the body a block. A subject-less `when { … }` uses a `true` placeholder selector
     * (per the CST-for-Kotlin assessment). `is`/`in` conditions are skipped for now.
     */
    private fun KaSession.convertWhen(statement: KtWhenExpression, method: MethodInfo,
                                      locals: Map<String, Variable>, index: String): Statement =
        runtime.newSwitchStatementNewStyleBuilder()
            .setSelector(whenSelector(statement, method, locals))
            .addSwitchEntries(whenEntries(statement, method, locals, index))
            .setSource(runtime.noSource()).build()

    private fun KaSession.whenSelector(statement: KtWhenExpression, method: MethodInfo,
                                       locals: Map<String, Variable>): Expression =
        statement.subjectExpression?.let { convertExpression(it, method, locals) } ?: runtime.newBoolean(true)

    /**
     * Build the switch entries for a `when`. Following the modern-Java pattern-switch model: a `is T` arm
     * is a **type pattern** carried on [SwitchEntry.patternVariable] (a [RecordPattern]), *not* a condition
     * expression — so it is compatible with how the analyzer reads pattern switches. Value arms (`v ->`)
     * are case-label conditions; `in range` is a `contains` call condition (`!in` negated).
     */
    private fun KaSession.whenEntries(statement: KtWhenExpression, method: MethodInfo,
                                      locals: Map<String, Variable>, prefix: String): List<SwitchEntry> {
        val subject = statement.subjectExpression?.let { convertExpression(it, method, locals) }
        return statement.entries.mapIndexed { k, entry ->
            val blockIndex = if (prefix.isEmpty()) "$k" else "$prefix.$k"
            val builder = runtime.newSwitchEntryBuilder()
                .setWhenExpression(runtime.newEmptyExpression()) // no Kotlin guard
                .setStatement(convertBlock(entry.expression, method, locals, blockIndex))
                .setSource(runtime.noSource())
            if (entry.isElse) {
                builder.addConditions(listOf(runtime.newEmptyExpression())) // default
            } else {
                val conditions = mutableListOf<Expression>()
                entry.conditions.forEach { condition ->
                    when (condition) {
                        is KtWhenConditionWithExpression ->
                            condition.expression?.let { conditions.add(convertExpression(it, method, locals)) }
                        is KtWhenConditionIsPattern -> // `is T` -> a type pattern on patternVariable
                            if (!condition.isNegated) typePattern(condition, method)?.let { builder.setPatternVariable(it) }
                        is KtWhenConditionInRange -> condition.rangeExpression
                            ?.let { convertExpression(it, method, locals) }
                            ?.let { range -> containsCall(range, subject)?.let { conditions.add(maybeNegate(it, condition.isNegated)) } }
                    }
                }
                builder.addConditions(conditions)
            }
            builder.build()
        }
    }

    /** A Kotlin `is T` arm as a type-pattern [RecordPattern] (Kotlin smartcasts the subject, so the bound
     * variable is synthetic). Negated `!is` is not representable as a pattern and is dropped. */
    private fun KaSession.typePattern(condition: KtWhenConditionIsPattern, method: MethodInfo): RecordPattern? {
        val type = condition.typeReference?.type?.let { mapType(it, method.typeInfo()) } ?: return null
        return runtime.newRecordPatternBuilder()
            .setLocalVariable(runtime.newLocalVariable("it", type))
            .setSource(runtime.noSource()).build()
    }

    private fun maybeNegate(expression: Expression, negated: Boolean): Expression =
        if (!negated) expression
        else runtime.newUnaryOperator(listOf(), runtime.noSource(), runtime.logicalNotOperatorBool(),
            expression, runtime.precedenceUnary())

    /** `x in range` -> `range.contains(x)`, when the range type has a unary `contains` method. */
    private fun containsCall(range: Expression, subject: Expression?): Expression? {
        val containsOn = range.parameterizedType().typeInfo() ?: return null
        val contains = containsOn.methods().firstOrNull { it.name() == "contains" && it.parameters().size == 1 } ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(range).setObjectIsImplicit(false).setMethodInfo(contains)
            .setParameterExpressions(listOf(subject ?: runtime.newEmptyExpression()))
            .setConcreteReturnType(runtime.booleanParameterizedType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    private fun isAssignment(token: com.intellij.psi.tree.IElementType): Boolean =
        token == KtTokens.EQ || augmentedOperator(token) != null

    /** The compound-assignment operator method for `+=`/`-=`/etc., or null for plain `=` (or a non-assignment). */
    private fun augmentedOperator(token: com.intellij.psi.tree.IElementType): MethodInfo? = when (token) {
        KtTokens.PLUSEQ -> runtime.assignPlusOperatorInt()
        KtTokens.MINUSEQ -> runtime.assignMinusOperatorInt()
        KtTokens.MULTEQ -> runtime.assignMultiplyOperatorInt()
        KtTokens.DIVEQ -> runtime.assignDivideOperatorInt()
        KtTokens.PERCEQ -> runtime.assignRemainderOperatorInt()
        else -> null
    }

    /** Convert a control-flow branch/body (a `{ … }` block or a single statement) into a CST [Block]. */
    private fun KaSession.convertBlock(body: KtExpression?, method: MethodInfo,
                                       locals: Map<String, Variable>, blockIndex: String): Block {
        val childLocals = locals.toMutableMap() // a nested block has its own scope
        return when (body) {
            null -> runtime.newBlockBuilder().setSource(runtime.noSource().withIndex(blockIndex)).build()
            is KtBlockExpression -> statementsToBlock(body.statements, method, childLocals, blockIndex)
            else -> statementsToBlock(listOf(body), method, childLocals, blockIndex)
        }
    }

    /**
     * Convert one expression in the context of the enclosing [method]. Handles compile-time constants,
     * `this`, and bare references (to a parameter or a field/property of the enclosing type). Calls,
     * operators, qualified access and the rest become a labelled placeholder, filled in incrementally.
     */
    internal fun KaSession.convertExpression(expression: KtExpression, method: MethodInfo,
                                             locals: Map<String, Variable>): Expression {
        val raw = convertExpressionRaw(expression, method, locals)
        if (raw is EmptyExpression) return raw // singleton; rejects withSource
        // apply the element's full range, but keep any DetailedSources a converter (e.g. convertCall) attached
        val rangeSource = source(expression, "-")
        val detailed = raw.source()?.detailedSources()
        return raw.withSource(if (detailed == null) rangeSource else rangeSource.withDetailedSources(detailed))
    }

    private fun KaSession.convertExpressionRaw(expression: KtExpression, method: MethodInfo,
                                               locals: Map<String, Variable>): Expression {
        expression.evaluate()?.let { constant ->
            return when (val value = constant.value) {
                is Int -> runtime.newInt(value)
                is Long -> runtime.newLong(value)
                is Short -> runtime.newShort(value)
                is Byte -> runtime.newByte(value)
                is Double -> runtime.newDouble(value)
                is Float -> runtime.newFloat(value)
                is Char -> runtime.newChar(value)
                is Boolean -> runtime.newBoolean(value)
                is String -> runtime.newStringConstant(value)
                null -> runtime.nullConstant()
                else -> runtime.newEmptyExpression("k2-unsupported-constant:${value::class.simpleName}")
            }
        }
        return when (expression) {
            // in an extension function body, `this` is the receiver (the synthetic first parameter)
            is KtThisExpression -> receiverParam(method)?.let { variableExpression(it) }
                ?: variableExpression(runtime.newThis(method.typeInfo().asParameterizedType()))
            is KtNameReferenceExpression -> resolveReference(expression.getReferencedName(), method, locals)
                ?: runtime.newEmptyExpression("k2-unresolved-ref:${expression.getReferencedName()}")
            is KtBinaryExpression -> convertBinary(expression, method, locals)
            is KtPostfixExpression -> convertUnary(expression.baseExpression, expression.operationToken, false, method, locals)
            is KtPrefixExpression -> convertUnary(expression.baseExpression, expression.operationToken, true, method, locals)
            is KtCallExpression -> convertCall(expression, null, true, method, locals) // f(...) on implicit this
            is KtQualifiedExpression -> convertQualified(expression, method, locals) // obj.f(...)/obj.x, incl. obj?.f()
            is KtBinaryExpressionWithTypeRHS -> convertCastOrSafeCast(expression, method, locals) // x as T / x as? T
            is KtIsExpression -> convertIsExpression(expression, method, locals) // x is T / x !is T
            is KtArrayAccessExpression -> convertArrayAccess(expression, method, locals) // a[i] -> a.get(i)
            is KtLambdaExpression -> convertLambda(expression, method, locals)
            is KtObjectLiteralExpression -> convertObjectLiteral(expression, method, locals)
            is KtIfExpression -> runtime.newInlineConditionalBuilder() // if as an expression: a ? b : c
                .setCondition(expression.condition?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
                .setIfTrue(expression.then?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
                .setIfFalse(expression.`else`?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression())
                .setSource(runtime.noSource()).build(runtime)
            is KtStringTemplateExpression -> { // "$x ${e} literal" -> folded StringConcat of the parts
                val parts = expression.entries.map { entry ->
                    when (entry) {
                        is KtLiteralStringTemplateEntry -> runtime.newStringConstant(entry.text)
                        is KtEscapeStringTemplateEntry -> runtime.newStringConstant(entry.unescapedValue)
                        is KtStringTemplateEntryWithExpression ->
                            entry.expression?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
                        else -> runtime.newStringConstant("")
                    }
                }
                parts.reduceOrNull { acc, part -> runtime.newStringConcat(acc, part) } ?: runtime.newStringConstant("")
            }
            is KtWhenExpression -> runtime.newSwitchExpressionBuilder() // when as an expression
                .setSelector(whenSelector(expression, method, locals))
                .addSwitchEntries(whenEntries(expression, method, locals, ""))
                .setParameterizedType(expression.expressionType?.let { mapType(it, method.typeInfo()) }
                    ?: runtime.objectParameterizedType())
                .setSource(runtime.noSource()).build()
            else -> runtime.newEmptyExpression("k2-unsupported-expr:${expression::class.simpleName}")
        }
    }

    /** `obj.f(...)` (method call) or `obj.x` (property/field access). */
    private fun KaSession.convertQualified(expression: KtQualifiedExpression, method: MethodInfo,
                                           locals: Map<String, Variable>): Expression {
        val receiver = convertExpression(expression.receiverExpression, method, locals)
        val receiverType = expression.receiverExpression.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        val selectorResult = when (val selector = expression.selectorExpression) {
            is KtCallExpression -> convertCall(selector, receiver to receiverType, false, method, locals)
            is KtNameReferenceExpression -> {
                val name = selector.getReferencedName()
                val field = receiverType?.fields()?.firstOrNull { it.name() == name }
                when {
                    field != null -> variableExpression(runtime.newFieldReference(field, receiver, field.type())) // obj.x
                    // property idiom backed by an accessor method: `list.size`->size(), `obj.name`->getName()
                    else -> receiverType?.let { resolveAccessor(it, name) }?.let { accessorCall(receiver, it) }
                        ?: runtime.newEmptyExpression("k2-unresolved-access:$name")
                }
            }
            else -> runtime.newEmptyExpression("k2-unsupported-selector")
        }
        // safe call `x?.foo()` -> `if (x == null) null else x.foo()`, marked NULL_SAFE at the `?.` token
        return if (expression is KtSafeQualifiedExpression) runtime.newInlineConditionalBuilder()
            .setCondition(runtime.newEquals(receiver, runtime.nullConstant()))
            .setIfTrue(runtime.nullConstant()).setIfFalse(selectorResult)
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.NULL_SAFE, expression.operationTokenNode.psi)))
            .build(runtime)
        else selectorResult
    }

    /** `Foo(args)` -> a CST [ConstructorCall]: the constructed type's constructor matching the argument count. */
    private fun KaSession.convertConstructorCall(call: KtCallExpression, arguments: List<Expression>, method: MethodInfo): Expression {
        val type = call.expressionType?.let { mapType(it, method.typeInfo()) }
            ?: return runtime.newEmptyExpression("k2-ctor-type")
        val constructor = type.typeInfo()?.constructors()?.firstOrNull { it.parameters().size == arguments.size }
            ?: return runtime.newEmptyExpression("k2-ctor-unresolved:${type.typeInfo()?.simpleName()}")
        return runtime.newConstructorCallBuilder()
            .setConstructor(constructor)
            .setConcreteReturnType(type)
            .setParameterExpressions(arguments)
            .setDiamond(runtime.diamondNo())
            .setTypeArguments(listOf())
            .setSource(runtime.noSource())
            .build()
    }

    /** `x as T` / `x as? T` -> a CST [org.e2immu.language.cst.api.expression.Cast] to T. */
    private fun KaSession.convertCastOrSafeCast(expression: KtBinaryExpressionWithTypeRHS, method: MethodInfo,
                                                locals: Map<String, Variable>): Expression {
        val value = convertExpression(expression.left, method, locals)
        val type = expression.right?.type?.let { mapType(it, method.typeInfo()) }
            ?: return runtime.newEmptyExpression("k2-cast")
        return runtime.newCast(value, type)
    }

    /** `x is T` / `x !is T` -> a CST [org.e2immu.language.cst.api.expression.InstanceOf] (`!is` negated by a logical-not). */
    private fun KaSession.convertIsExpression(expression: KtIsExpression, method: MethodInfo,
                                              locals: Map<String, Variable>): Expression {
        val value = convertExpression(expression.leftHandSide, method, locals)
        val testType = expression.typeReference?.type?.let { mapType(it, method.typeInfo()) }
            ?: return runtime.newEmptyExpression("k2-is")
        val instanceOf = runtime.newInstanceOfBuilder().setExpression(value).setTestType(testType)
            .setSource(runtime.noSource()).build()
        return if (expression.isNegated) runtime.newUnaryOperator(listOf(), runtime.noSource(),
            runtime.logicalNotOperatorBool(), instanceOf, runtime.precedenceUnary()) else instanceOf
    }

    /** `a[i]` -> `a.get(i)` method call (when `get` resolves on the receiver type). */
    private fun KaSession.convertArrayAccess(expression: KtArrayAccessExpression, method: MethodInfo,
                                             locals: Map<String, Variable>): Expression {
        val array = expression.arrayExpression?.let { convertExpression(it, method, locals) }
            ?: return runtime.newEmptyExpression("k2-index")
        val indices = expression.indexExpressions.map { convertExpression(it, method, locals) }
        val arrayType = expression.arrayExpression?.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        val get = arrayType?.let { resolveCallee(it, "get", indices) }
            ?: return runtime.newEmptyExpression("k2-index-get-unresolved")
        // marked INDEX_ACCESS at the `[` so the engine knows this get() was written as indexing
        return runtime.newMethodCallBuilder().setObject(array).setObjectIsImplicit(false).setMethodInfo(get)
            .setParameterExpressions(indices).setConcreteReturnType(get.returnType()).setTypeArguments(listOf())
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.INDEX_ACCESS, expression.leftBracket)))
            .build()
    }

    /** `a[i] = v` -> `a.set(i, v)` method call (when `set` resolves), marked INDEX_ACCESS at the `[`. */
    private fun KaSession.convertIndexedSet(arrayAccess: KtArrayAccessExpression, value: Expression,
                                            method: MethodInfo, locals: Map<String, Variable>): Expression {
        val array = arrayAccess.arrayExpression?.let { convertExpression(it, method, locals) }
            ?: return runtime.newEmptyExpression("k2-indexed-set")
        val arguments = arrayAccess.indexExpressions.map { convertExpression(it, method, locals) } + value
        val arrayType = arrayAccess.arrayExpression?.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        // `set` on List/arrays is a member; on a Map, Kotlin's `map[k]=v` set-operator is a stdlib extension
        // that delegates to `put`, so fall back to put (same key,value arguments)
        val set = arrayType?.let { resolveCallee(it, "set", arguments) ?: resolveCallee(it, "put", arguments) }
            ?: return runtime.newEmptyExpression("k2-indexed-set-unresolved")
        return runtime.newMethodCallBuilder().setObject(array).setObjectIsImplicit(false).setMethodInfo(set)
            .setParameterExpressions(arguments).setConcreteReturnType(set.returnType()).setTypeArguments(listOf())
            .setSource(source(arrayAccess, "-").withDetailedSources(marker(DetailedSources.INDEX_ACCESS, arrayAccess.leftBracket)))
            .build()
    }

    /**
     * Convert a Kotlin lambda `{ x -> … }` to a CST [Lambda]: a synthetic anonymous type implementing the
     * lambda's functional-interface type, with a single `invoke` method carrying the parameters, the
     * (concrete) return type, and the converted body. The three [ParameterizedType]s — functional
     * interface, return type, parameter types — come from the lambda's resolved function type. The
     * lambda's own parameters resolve via the SAM method; outer locals are captured (outer parameters are
     * not yet resolved). Implicit `it` is materialised when the function type has one parameter.
     */
    private fun KaSession.convertLambda(lambda: KtLambdaExpression, method: MethodInfo,
                                        locals: Map<String, Variable>): Expression {
        val enclosingType = method.typeInfo()
        val anonymousType = runtime.newAnonymousType(enclosingType, enclosingType.builder().getAndIncrementAnonymousTypes())
        anonymousType.builder()
            .setAccess(runtime.accessPrivate())
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())

        val functionType = lambda.expressionType as? KaFunctionType
        val functionalType = lambda.expressionType?.let { mapType(it, enclosingType) } ?: runtime.objectParameterizedType()
        val sam = runtime.newMethod(anonymousType, "invoke", runtime.methodTypeMethod())
        val samBuilder = sam.builder()
        val outputVariants = mutableListOf<Lambda.OutputVariant>()

        val parameters = lambda.valueParameters
        if (parameters.isNotEmpty()) {
            parameters.forEachIndexed { i, p ->
                val type = (p.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, enclosingType) }
                    ?: functionType?.parameterTypes?.getOrNull(i)?.let { mapType(it, enclosingType) }
                    ?: runtime.objectParameterizedType()
                samBuilder.addParameter(p.name ?: "p$i", type)
                outputVariants.add(runtime.lambdaOutputVariantEmpty())
            }
        } else if (functionType != null && functionType.parameterTypes.size == 1) {
            samBuilder.addParameter("it", mapType(functionType.parameterTypes[0], enclosingType)) // implicit `it`
        }
        val returnType = functionType?.returnType?.let { mapType(it, enclosingType) } ?: runtime.objectParameterizedType()
        samBuilder.setReturnType(returnType).setAccess(runtime.accessPublic()).setSynthetic(true).commitParameters()

        // body: the lambda's block; its last expression becomes the (implicit) return value.
        // Converted in the *enclosing* method's context (so captured outer params/fields resolve), with
        // the lambda's own parameters added to the in-scope variables.
        val block = runtime.newBlockBuilder()
        val bodyScope: MutableMap<String, Variable> = locals.toMutableMap()
        sam.parameters().forEach { bodyScope[it.name()] = it }
        val statements = lambda.bodyExpression?.statements.orEmpty()
        val voidReturn = returnType == runtime.voidParameterizedType()
        statements.forEachIndexed { i, stmt ->
            val index = pad(i, statements.size)
            block.addStatement(
                if (i == statements.lastIndex && !voidReturn && isLambdaResultExpression(stmt))
                    indexed(runtime.newReturnStatement(convertExpression(stmt, method, bodyScope)), index)
                else convertStatement(stmt, method, bodyScope, index)
            )
        }
        samBuilder.setMethodBody(block.build()).commit()

        anonymousType.builder()
            .addMethod(sam)
            .addInterfaceImplemented(functionalType)
            .setEnclosingMethod(method)
            .setSingleAbstractMethod(sam)
            .commit()
        return runtime.newLambdaBuilder().setMethodInfo(sam).setOutputVariants(outputVariants).setSource(runtime.noSource()).build()
    }

    /**
     * Resolve the callee by name + arity on [type] or its supertypes (so inherited callees resolve), then
     * disambiguate overloads by matching the parameter types against the argument types: an exact
     * [ParameterizedType] match wins, then a match on the erased [TypeInfo], else the first candidate.
     */
    /**
     * The no-arg accessor method behind a Kotlin property idiom on a Java type: `size`->`size()`,
     * `name`->`getName()`, `empty`->`isEmpty()`. Used when `obj.x` has no field of that name.
     */
    private fun resolveAccessor(type: TypeInfo, propertyName: String): MethodInfo? {
        val capitalized = propertyName.replaceFirstChar { it.uppercaseChar() }
        return resolveCallee(type, propertyName, listOf())          // size(), length()
            ?: resolveCallee(type, "get$capitalized", listOf())     // getName()
            ?: resolveCallee(type, "is$capitalized", listOf())      // isEmpty() (boolean)
    }

    /** A no-arg getter call `receiver.getter()` (the desugaring of a property idiom). */
    private fun accessorCall(receiver: Expression, getter: MethodInfo): Expression =
        runtime.newMethodCallBuilder().setObject(receiver).setObjectIsImplicit(false).setMethodInfo(getter)
            .setParameterExpressions(listOf()).setConcreteReturnType(getter.returnType()).setTypeArguments(listOf())
            .setSource(runtime.noSource()).build()

    private fun resolveCallee(type: TypeInfo, name: String, arguments: List<Expression>): MethodInfo? {
        val candidates = mutableListOf<MethodInfo>()
        collectMethods(type, name, arguments.size, mutableSetOf(), candidates)
        if (candidates.size <= 1) return candidates.firstOrNull()
        val argTypes = arguments.map { it.parameterizedType() }
        fun matches(predicate: (ParameterizedType, ParameterizedType) -> Boolean) = candidates.firstOrNull { c ->
            c.parameters().withIndex().all { (i, p) -> predicate(p.parameterizedType(), argTypes[i]) }
        }
        return matches { p, a -> p == a }
            ?: matches { p, a -> p.typeInfo() != null && p.typeInfo() == a.typeInfo() }
            ?: candidates.first()
    }

    /** Collect every method named [name] with [arity] parameters on [type] and its supertypes. */
    private fun collectMethods(type: TypeInfo, name: String, arity: Int, visited: MutableSet<TypeInfo>,
                               acc: MutableList<MethodInfo>) {
        if (!visited.add(type)) return
        type.methods().filterTo(acc) { it.name() == name && it.parameters().size == arity }
        type.parentClass()?.typeInfo()?.let { collectMethods(it, name, arity, visited, acc) }
        type.interfacesImplemented().forEach { iface ->
            iface.typeInfo()?.let { collectMethods(it, name, arity, visited, acc) }
        }
    }

    /**
     * Convert an anonymous `object : Super { … }` expression to a CST [ConstructorCall] of a synthetic
     * anonymous type (the JVM model — like a lambda's type, but with arbitrary members and supertypes).
     * The supertypes are split into a parent class + implemented interfaces; the members become the
     * anonymous type's members. (Captured outer variables in member bodies are a later refinement.)
     */
    private fun KaSession.convertObjectLiteral(expression: KtObjectLiteralExpression, method: MethodInfo,
                                               locals: Map<String, Variable>): Expression {
        val symbol = expression.objectDeclaration.symbol as? KaClassSymbol
        val enclosing = method.typeInfo()
        val anon = runtime.newAnonymousType(enclosing, enclosing.builder().getAndIncrementAnonymousTypes())
        val builder = anon.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setAccess(runtime.accessPrivate())
            .setEnclosingMethod(method)

        var parentClass: ParameterizedType? = null
        val interfaces = mutableListOf<ParameterizedType>()
        symbol?.superTypes?.forEach { superType ->
            val pt = mapType(superType, enclosing)
            if (pt.isJavaLangObject) return@forEach
            val kind = (superType as? KaClassType)?.symbol?.let { (it as? KaClassSymbol)?.classKind }
            if (kind == KaClassKind.INTERFACE) interfaces.add(pt) else parentClass = pt
        }
        builder.setParentClass(parentClass ?: runtime.objectParameterizedType())
        interfaces.forEach { builder.addInterfaceImplemented(it) }
        val concreteReturnType = parentClass ?: interfaces.firstOrNull() ?: runtime.objectParameterizedType()

        symbol?.declaredMemberScope?.declarations?.filterIsInstance<KaPropertySymbol>()
            ?.forEach { property -> with(memberConverter) { buildAnonProperty(anon, property) } }
        symbol?.declaredMemberScope?.declarations?.filterIsInstance<KaNamedFunctionSymbol>()
            ?.forEach { function -> anon.builder().addMethod(with(memberConverter) { buildAnonMethod(anon, function) }) }
        builder.commit()

        return runtime.newConstructorCallBuilder()
            .setConcreteReturnType(concreteReturnType)
            .setAnonymousClass(anon)
            .setDiamond(runtime.diamondNo())
            .setParameterExpressions(listOf())
            .setTypeArguments(listOf())
            .setSource(runtime.noSource())
            .build()
    }

    /** Whether a lambda's last statement is a value-producing expression (so it becomes the return value). */
    private fun isLambdaResultExpression(statement: KtExpression): Boolean = when (statement) {
        is KtProperty, is KtReturnExpression, is KtForExpression, is KtWhileExpression, is KtDoWhileExpression,
        is KtBreakExpression, is KtContinueExpression -> false
        is KtBinaryExpression -> !isAssignment(statement.operationToken)
        else -> true
    }

    /**
     * Build a CST [org.e2immu.language.cst.api.expression.MethodCall]. The callee [MethodInfo] is resolved
     * by name + arity on the receiver's type (or the enclosing type for an implicit `this`), searching
     * supertypes and disambiguating overloads by argument type (see [resolveCallee]); an unresolved call
     * falls back to a placeholder.
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtCallElement)
    private fun KaSession.convertCall(
        call: KtCallExpression, receiver: Pair<Expression, TypeInfo?>?, implicitThis: Boolean, method: MethodInfo,
        locals: Map<String, Variable>,
    ): Expression {
        val name = (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
            ?: return runtime.newEmptyExpression("k2-unsupported-callee")
        val valueArgs = call.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e, method, locals) } }
        val trailingLambdas = call.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertExpression(l, method, locals) } }
        val arguments = valueArgs + trailingLambdas

        // a constructor call `Foo(args)` -> ConstructorCall (the call resolves to a constructor, not a method)
        if (call.resolveSymbol() is KaConstructorSymbol) return convertConstructorCall(call, arguments, method)

        // an extension call `recv.ext(args)` routes to the facade's static `ext(recv, args)` (receiver as arg 0)
        val calleeSymbol = call.resolveSymbol() as? KaNamedFunctionSymbol
        if (receiver != null && calleeSymbol?.receiverParameter != null) {
            extensionCall(name, receiver.first, arguments, calleeSymbol, call, method)?.let { return it }
        }
        // a companion call `Outer.member(args)` routes through the singleton: `Outer.Companion.member(args)`
        companionCall(name, calleeSymbol, arguments, call, method)?.let { return it }
        // a qualified call on a named object `Object.member(args)` routes through `Object.INSTANCE`
        if (receiver != null) objectCall(name, calleeSymbol, arguments, call, method)?.let { return it }

        val ownerType = receiver?.second ?: method.typeInfo()
        val callee = resolveCallee(ownerType, name, arguments)
            ?: return runtime.newEmptyExpression("k2-unresolved-call:$name")
        val obj = receiver?.first ?: variableExpression(runtime.newThis(method.typeInfo().asParameterizedType()))
        val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        // DetailedSources (layer 2), mirroring exactly what the Java parser records for a method call: the
        // closing parenthesis (END_OF_ARGUMENT_LIST) and the argument commas (ARGUMENT_COMMAS) -- both shared
        // marker singletons, hence language-unaware. Java reads these from its 2nd-pass scanResult; the K2
        // PSI gives them faithfully from this single parse. (Java records no call-site *name* here.)
        val argumentList = call.valueArgumentList
        val dsb = runtime.newDetailedSourcesBuilder()
        // the call-site method name, keyed by callee.name() (the MethodInfo's String -- as the Java parser
        // does, so consumers look it up via methodCall.methodInfo().name())
        call.calleeExpression?.let { dsb.put(callee.name(), source(it, "-")) }
        argumentList?.rightParenthesis?.let { dsb.put(DetailedSources.END_OF_ARGUMENT_LIST, source(it, "-")) }
        val commas = argumentList?.node?.getChildren(null).orEmpty()
            .filter { it.elementType == KtTokens.COMMA }.map { source(it.psi, "-") }
        if (commas.isNotEmpty()) dsb.putList(DetailedSources.ARGUMENT_COMMAS, commas)
        return runtime.newMethodCallBuilder()
            .setObject(obj)
            .setObjectIsImplicit(implicitThis)
            .setMethodInfo(callee)
            .setParameterExpressions(arguments)
            .setConcreteReturnType(returnType)
            .setTypeArguments(listOf())
            .setSource(runtime.noSource().withDetailedSources(dsb.build()))
            .build()
    }

    /**
     * Build an extension-function call as a static call on the file facade with the receiver as argument 0
     * (the JVM shape): `recv.ext(args)` → `<File>Kt.ext(recv, args)`. Returns null when the facade or the
     * static method can't be resolved (e.g. a library extension whose facade isn't in this compilation).
     */
    private fun KaSession.extensionCall(name: String, receiverExpr: Expression, arguments: List<Expression>,
                                        symbol: KaNamedFunctionSymbol, call: KtCallExpression, method: MethodInfo): Expression? {
        val facade = extensionFacade(symbol) ?: return null
        val facadeArgs = listOf(receiverExpr) + arguments
        val callee = resolveCallee(facade, name, facadeArgs) ?: return null
        val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false)
            .setMethodInfo(callee)
            .setParameterExpressions(facadeArgs)
            .setConcreteReturnType(returnType)
            .setTypeArguments(listOf())
            .setSource(runtime.noSource())
            .build()
    }

    /**
     * Build a companion-member call `Outer.member(args)` as `Outer.Companion.member(args)`: the call's
     * object is a field access of the `Companion` singleton on the enclosing class. Returns null when the
     * callee isn't a companion member or its types aren't in this compilation.
     */
    private fun KaSession.companionCall(name: String, calleeSymbol: KaNamedFunctionSymbol?, arguments: List<Expression>,
                                        call: KtCallExpression, method: MethodInfo): Expression? {
        val companionDecl = (calleeSymbol?.psi as? KtNamedFunction)?.containingClassOrObject as? KtObjectDeclaration ?: return null
        if (!companionDecl.isCompanion()) return null
        val enclosingFqn = (companionDecl.containingClassOrObject?.symbol as? KaNamedClassSymbol)?.classId?.asFqNameString()
            ?: return null
        val enclosing = infoByFqn.getType(enclosingFqn, sourceSet) ?: return null
        val companionName = companionDecl.name ?: "Companion"
        val companion = enclosing.subTypes().firstOrNull { it.simpleName() == companionName } ?: return null
        val companionField = enclosing.fields().firstOrNull { it.name() == companionName } ?: return null
        val callee = resolveCallee(companion, name, arguments) ?: return null
        return singletonMemberCall(enclosing, companionField, callee, arguments, call, method)
    }

    /**
     * Build a call `Object.member(args)` through a singleton: `<holder>.<field>.member(args)`, where the
     * call's object is a field access of the singleton (`Outer.Companion` or `Object.INSTANCE`).
     */
    private fun KaSession.objectCall(name: String, calleeSymbol: KaNamedFunctionSymbol?, arguments: List<Expression>,
                                     call: KtCallExpression, method: MethodInfo): Expression? {
        val objectDecl = (calleeSymbol?.psi as? KtNamedFunction)?.containingClassOrObject as? KtObjectDeclaration ?: return null
        if (objectDecl.isCompanion()) return null // companions go through companionCall
        val fqn = (objectDecl.symbol as? KaNamedClassSymbol)?.classId?.asFqNameString() ?: return null
        val objectType = infoByFqn.getType(fqn, sourceSet) ?: return null
        val instanceField = objectType.fields().firstOrNull { it.name() == "INSTANCE" } ?: return null
        val callee = resolveCallee(objectType, name, arguments) ?: return null
        return singletonMemberCall(objectType, instanceField, callee, arguments, call, method)
    }

    /** The `MethodCall` of a singleton member: object = a field access of [singletonField] on [holder]. */
    private fun KaSession.singletonMemberCall(holder: TypeInfo, singletonField: FieldInfo, callee: MethodInfo,
                                              arguments: List<Expression>, call: KtCallExpression, method: MethodInfo): Expression {
        val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        return runtime.newMethodCallBuilder()
            .setObject(singletonAccess(holder, singletonField)).setObjectIsImplicit(false).setMethodInfo(callee)
            .setParameterExpressions(arguments).setConcreteReturnType(returnType)
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** A field-access expression `Holder.field` of the singleton [field] (scoped to the [holder] type). */
    internal fun singletonAccess(holder: TypeInfo, field: FieldInfo): Expression =
        runtime.newVariableExpressionBuilder()
            .setVariable(runtime.newFieldReference(field,
                runtime.newTypeExpression(holder.asParameterizedType(), runtime.diamondNo()), field.type()))
            .setSource(runtime.noSource()).build()

    /** The file-facade [TypeInfo] that holds a (source) top-level extension function, via its containing file. */
    private fun extensionFacade(symbol: KaNamedFunctionSymbol): TypeInfo? {
        val ktFile = (symbol.psi as? KtNamedFunction)?.containingKtFile ?: return null
        val pkg = ktFile.packageFqName
        val fqn = (if (pkg.isRoot) "" else pkg.asString() + ".") + facadeSimpleName(ktFile)
        return infoByFqn.getType(fqn, sourceSet)
    }

    /**
     * Prefix/postfix unary: `++`/`--` become an [org.e2immu.language.cst.api.expression.Assignment]
     * (`prefixPrimitiveOperator` distinguishes `++i` from `i++`); `-x` and `!x` become a `UnaryOperator`.
     */
    private fun KaSession.convertUnary(base: KtExpression?, token: com.intellij.psi.tree.IElementType,
                                       prefix: Boolean, method: MethodInfo, locals: Map<String, Variable>): Expression {
        val operand = base?.let { convertExpression(it, method, locals) } ?: return runtime.newEmptyExpression("k2-unary")
        return when (token) {
            KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> {
                val target = operand as? VariableExpression ?: return runtime.newEmptyExpression("k2-incr-target")
                val isPlus = token == KtTokens.PLUSPLUS
                runtime.newAssignmentBuilder()
                    .setAssignmentOperator(if (isPlus) runtime.assignPlusOperatorInt() else runtime.assignMinusOperatorInt())
                    .setPrefixPrimitiveOperator(prefix)
                    .setAssignmentOperatorIsPlus(isPlus)
                    .setBinaryOperator(if (isPlus) runtime.plusOperatorInt() else runtime.minusOperatorInt())
                    .setTarget(target)
                    .setValue(runtime.intOne(runtime.noSource()))
                    .setSource(runtime.noSource()).build()
            }
            // `x!!` non-null assertion: transparent for the CST (same value/type), but marked NON_NULL_ASSERTION
            KtTokens.EXCLEXCL -> if (operand is EmptyExpression) operand
            else operand.withSource(operand.source().mergeDetailedSources(marker(DetailedSources.NON_NULL_ASSERTION, base)))
            KtTokens.MINUS -> runtime.newUnaryOperator(listOf(), runtime.noSource(),
                runtime.unaryMinusOperatorInt(), operand, runtime.precedenceUnary())
            KtTokens.EXCL -> runtime.newUnaryOperator(listOf(), runtime.noSource(),
                runtime.logicalNotOperatorBool(), operand, runtime.precedenceUnary())
            else -> runtime.newEmptyExpression("k2-unsupported-unary:$token")
        }
    }

    /**
     * Convert a binary expression to a CST [org.e2immu.language.cst.api.expression.BinaryOperator] whose
     * operator is the corresponding `Runtime` operator method (e.g. `plusOperatorInt`). Only built-in
     * operators on primitive/String operands are emitted; overloaded operators (and Kotlin `==` on
     * objects, which is `.equals()`) fall back to a placeholder, to be handled as method calls.
     */
    private fun KaSession.convertBinary(expression: KtBinaryExpression, method: MethodInfo,
                                        locals: Map<String, Variable>): Expression {
        val left = expression.left?.let { convertExpression(it, method, locals) }
        val right = expression.right?.let { convertExpression(it, method, locals) }
        if (left == null || right == null) return runtime.newEmptyExpression("k2-binary-operand")
        // elvis `a ?: b` -> `if (a == null) b else a`, marked NULL_COALESCING at the `?:` token
        if (expression.operationToken == KtTokens.ELVIS) return runtime.newInlineConditionalBuilder()
            .setCondition(runtime.newEquals(left, runtime.nullConstant()))
            .setIfTrue(right).setIfFalse(left)
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.NULL_COALESCING, expression.operationReference)))
            .build(runtime)
        // `a in coll` -> `coll.contains(a)`; `a !in coll` -> `!coll.contains(a)` (receiver is the RIGHT operand)
        if (expression.operationToken == KtTokens.IN_KEYWORD || expression.operationToken == KtTokens.NOT_IN) {
            val collectionType = expression.right?.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
            val contains = collectionType?.let { resolveCallee(it, "contains", listOf(left)) }
                ?: return runtime.newEmptyExpression("k2-in-unresolved")
            val call = runtime.newMethodCallBuilder().setObject(right).setObjectIsImplicit(false).setMethodInfo(contains)
                .setParameterExpressions(listOf(left)).setConcreteReturnType(contains.returnType()).setTypeArguments(listOf())
                .setSource(runtime.noSource()).build()
            return if (expression.operationToken == KtTokens.NOT_IN) runtime.newUnaryOperator(listOf(),
                runtime.noSource(), runtime.logicalNotOperatorBool(), call, runtime.precedenceUnary()) else call
        }
        val numeric = left.isNumeric && right.isNumeric
        val stringPlus = left.parameterizedType().isJavaLangString || right.parameterizedType().isJavaLangString
        val opAndPrecedence = when (expression.operationToken) {
            KtTokens.PLUS -> when {
                stringPlus -> runtime.plusOperatorString() to runtime.precedenceAdditive()
                numeric -> runtime.plusOperatorInt() to runtime.precedenceAdditive()
                else -> null
            }
            KtTokens.MINUS -> if (numeric) runtime.minusOperatorInt() to runtime.precedenceAdditive() else null
            KtTokens.MUL -> if (numeric) runtime.multiplyOperatorInt() to runtime.precedenceMultiplicative() else null
            KtTokens.DIV -> if (numeric) runtime.divideOperatorInt() to runtime.precedenceMultiplicative() else null
            KtTokens.PERC -> if (numeric) runtime.remainderOperatorInt() to runtime.precedenceMultiplicative() else null
            KtTokens.LT -> if (numeric) runtime.lessOperatorInt() to runtime.precedenceRelational() else null
            KtTokens.GT -> if (numeric) runtime.greaterOperatorInt() to runtime.precedenceRelational() else null
            KtTokens.LTEQ -> if (numeric) runtime.lessEqualsOperatorInt() to runtime.precedenceRelational() else null
            KtTokens.GTEQ -> if (numeric) runtime.greaterEqualsOperatorInt() to runtime.precedenceRelational() else null
            KtTokens.EQEQ -> if (numeric) runtime.equalsOperatorInt() to runtime.precedenceEquality() else null
            KtTokens.EXCLEQ -> if (numeric) runtime.notEqualsOperatorInt() to runtime.precedenceEquality() else null
            KtTokens.ANDAND -> runtime.andOperatorBool() to runtime.precedenceLogicalAnd()
            KtTokens.OROR -> runtime.orOperatorBool() to runtime.precedenceLogicalOr()
            else -> null
        } ?: return operatorFunctionCall(expression, left, right, method)
        val (operator, precedence) = opAndPrecedence
        val resultType = expression.expressionType?.let { mapType(it, method.typeInfo()) } ?: operator.returnType()
        return runtime.newBinaryOperatorBuilder()
            .setLhs(left).setRhs(right).setOperator(operator)
            .setPrecedence(precedence).setParameterizedType(resultType)
            .setSource(runtime.noSource()).build()
    }

    /**
     * Fallback for a binary expression that is not a built-in operator: an overloaded operator
     * (`a + b` → `a.plus(b)`) or a named infix call (`a foo b` → `a.foo(b)`). The Kotlin operator-function
     * name is fixed per token; an infix call uses the reference name. Resolved on the left operand's type.
     */
    private fun KaSession.operatorFunctionCall(expression: KtBinaryExpression, left: Expression, right: Expression,
                                               method: MethodInfo): Expression {
        val functionName = when (expression.operationToken) {
            KtTokens.PLUS -> "plus"
            KtTokens.MINUS -> "minus"
            KtTokens.MUL -> "times"
            KtTokens.DIV -> "div"
            KtTokens.PERC -> "rem"
            KtTokens.IDENTIFIER -> expression.operationReference.getReferencedName() // infix function
            else -> null
        } ?: return runtime.newEmptyExpression("k2-unsupported-operator:${expression.operationToken}")
        val callee = left.parameterizedType().typeInfo()?.let { resolveCallee(it, functionName, listOf(right)) }
            ?: return runtime.newEmptyExpression("k2-unresolved-operator:$functionName")
        return runtime.newMethodCallBuilder()
            .setObject(left).setObjectIsImplicit(false).setMethodInfo(callee)
            .setParameterExpressions(listOf(right))
            .setConcreteReturnType(expression.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** The synthetic extension-receiver parameter (`$receiver`) of an extension method, or null. */
    private fun receiverParam(method: MethodInfo): ParameterInfo? =
        method.parameters().firstOrNull()?.takeIf { it.name() == "\$receiver" }

    /** Resolve a bare name: a local, else a parameter, else a field (locals shadow params shadow fields). */
    private fun resolveReference(name: String, method: MethodInfo, locals: Map<String, Variable>): Expression? {
        locals[name]?.let { return variableExpression(it) }
        method.parameters().firstOrNull { it.name() == name }?.let { return variableExpression(it) }
        method.typeInfo().fields().firstOrNull { it.name() == name }
            ?.let { return variableExpression(runtime.newFieldReference(it)) }
        // unqualified access to a member of the extension receiver: `name` means `$receiver.name`
        receiverParam(method)?.let { receiver ->
            receiver.parameterizedType().typeInfo()?.fields()?.firstOrNull { it.name() == name }?.let { field ->
                return runtime.newVariableExpressionBuilder()
                    .setVariable(runtime.newFieldReference(field, variableExpression(receiver), field.type()))
                    .setSource(runtime.noSource()).build()
            }
        }
        return null
    }

    internal fun variableExpression(variable: Variable): Expression =
        runtime.newVariableExpressionBuilder().setVariable(variable).setSource(runtime.noSource()).build()
}
