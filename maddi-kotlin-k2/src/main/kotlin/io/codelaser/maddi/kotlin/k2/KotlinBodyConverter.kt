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
import io.codelaser.maddi.kotlin.api.K2_PLACEHOLDER_PREFIX
import io.codelaser.maddi.cst.api.element.CompilationUnit
import io.codelaser.maddi.cst.api.element.DetailedSources
import io.codelaser.maddi.cst.api.element.RecordPattern
import io.codelaser.maddi.cst.api.element.Source
import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.expression.EmptyExpression
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.NullConstant
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.variable.This
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.MethodModifier
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.info.Variance
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.api.statement.Block
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.cst.api.statement.Statement
import io.codelaser.maddi.cst.api.statement.SwitchEntry
import io.codelaser.maddi.cst.api.variable.LocalVariable
import io.codelaser.maddi.cst.api.variable.Variable
import io.codelaser.maddi.cst.api.type.NullableState
import io.codelaser.maddi.cst.api.type.ParameterizedType
import io.codelaser.maddi.cst.api.expression.TypeExpression
import io.codelaser.maddi.cst.api.type.TypeNature
import io.codelaser.maddi.inspection.resource.InfoByFqn
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.resolution.KaImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
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
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtFunctionLiteral
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
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
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
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtSuperExpression
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

    /** Convert the property initializers and `init` blocks of the anonymous type [owner] while it is still open. */
    fun KaSession.finishAnonMembers(owner: TypeInfo, declaration: KtObjectDeclaration)

    /** Build a method-local type declaration (`class C : A { … }`) as a full source type, capturing [outerLocals]. */
    fun KaSession.buildLocalType(enclosingMethod: MethodInfo, declaration: KtClassOrObject,
                                 outerLocals: Map<String, Variable>): TypeInfo
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
private val REFLECTION = org.jetbrains.kotlin.name.ClassId.fromString("kotlin/jvm/internal/Reflection")

private val ARRAY_LITERALS = setOf("arrayOf", "intArrayOf", "longArrayOf", "doubleArrayOf", "floatArrayOf",
    "booleanArrayOf", "charArrayOf", "byteArrayOf", "shortArrayOf")

private val PRIMITIVE_CONVERSIONS =
    setOf("toInt", "toLong", "toDouble", "toFloat", "toShort", "toByte", "toChar")

internal class KotlinBodyConverter(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    private val sourceSet: SourceSet,
    private val typeMapper: KotlinTypeMapper,
) {
    // set by KotlinScan after construction (the bodies<->declarations cycle)
    lateinit var memberConverter: MemberConverter

    // set by KotlinScan: a local variable converted from its declaration PSI, so the uses K2 resolves to that
    // declaration can be recorded under it (KotlinReferenceRegistry.local)
    var localDeclared: (PsiElement, LocalVariable) -> Unit = { _, _ -> }

    /**
     * Calls bound to an overload NONE of whose parameter types the arguments fit — a guess, kept because
     * some callee is better than none, and counted because the CST cannot show it. ⚠ It is the blind spot
     * BESIDE the placeholder census: a placeholder says "nothing was read here", and this says "something
     * was read here and it may be the wrong thing", which no walk over the tree can detect afterwards.
     */
    var ambiguousBindings: Int = 0

    /**
     * ⭐ Elvis lowerings whose left operand is re-evaluated. Since the temporary landed this must stay 0 —
     * it is an invariant check, not a tally, and it is logged on every project parse so a regression shows
     * up as a number rather than as a wrong verdict nobody looks for.
     */
    var elvisReEvaluations: Int = 0

    /** Temporaries introduced to evaluate an elvis's left operand exactly once. */
    var elvisTemporaries: Int = 0

    /**
     * PSI already bound to a temporary for this statement, so [convertExpression] returns a read instead of
     * converting -- and so EVALUATING -- it again. Cleared per statement by [hoistNullSafeSpine].
     */
    private val hoistedReads = java.util.IdentityHashMap<KtExpression, Variable>()

    /** Temporaries introduced for a null-safe spine; see [hoistNullSafeSpine]. */
    var nullSafeTemporaries: Int = 0

    /**
     * Safe calls whose null test the CALLER supplies as a statement, so the conversion must hand back the
     * bare selector call rather than wrap it in a ternary. See [safeCallAsStatementLowering].
     */
    private val unwrappedSafeCalls = java.util.IdentityHashMap<KtExpression, Boolean>()

    // set by KotlinScan: the `$default` synthetic a call omitting an argument of this declaration calls (see callArguments)
    var defaultsOf: (PsiElement?) -> MethodInfo? = { null }

    // type mapping lives on the collaborator; it's a KaSession member extension, so reach it via with(…)
    private fun KaSession.mapType(type: KaType, owner: TypeInfo, method: MethodInfo? = null) =
        with(typeMapper) { mapType(type, owner, method) }

    /**
     * Convert a function body into a CST [Block]. Handles the two Kotlin body forms — a block body
     * `{ … }` and an expression body `= expr` (which becomes a single `return expr`, or an expression
     * statement when the function returns Unit). M3 scope: literal constants and returns; expressions
     * not yet understood become an explicit [Runtime.newEmptyExpression] placeholder rather than failing.
     */
    internal fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType,
                                      method: MethodInfo, outerLocals: Map<String, Variable> = emptyMap()): Block {
        val psi = function.psi as? KtNamedFunction ?: return runtime.newBlockBuilder().build()
        // start from the enclosing method's captured variables (for a local class's methods); references
        // resolve against this, so an enclosing parameter/local read inside a local type binds to it.
        val locals = outerLocals.toMutableMap()
        if (psi.hasBlockBody()) {
            return statementsToBlock(psi.bodyBlockExpression?.statements.orEmpty(), method, locals, "")
        }
        val block = runtime.newBlockBuilder()
        psi.bodyExpression?.let { body ->
            val returning = returnType != runtime.voidParameterizedType()
            // `fun f(): T = try { … } catch { … }` is the commonest try-as-a-value shape there is — three of
            // detekt's four. The expression body IS the returned value, so the whole statement context the
            // lowering needs is right here: no temporary, the branches just return.
            val statement = when {
                body is KtTryExpression -> convertTry(body, method, locals, "0", returning = returning)
                body is KtIfExpression && body.hasAMultiStatementBranch() ->
                    convertValueIf(body, method, locals, "0", returning, assignTo = null)
                else -> null
            }
            if (statement != null) {
                block.addStatement(indexed(statement, "0"))
            } else {
                // `fun f() = a?.b()?.c()` is a tail too: hoist its spine above the single returned statement
                val (hoisted, tailIndex) = hoistBefore(body, method, locals, "0")
                hoisted.forEach { block.addStatement(it) }
                val expr = convertExpression(body, method, locals)
                hoistedReads.clear()
                block.addStatement(indexed(
                    if (returning) runtime.newReturnStatement(expr) else runtime.newExpressionAsStatement(expr),
                    tailIndex))
            }
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
            // ⚠ ONE source statement can become TWO (see controlFlowElvisLowering). They are indexed
            // `<childIndex>.0` and `.1` rather than renumbered as siblings: the indexes only have to SORT
            // (prepwork compares them as strings), and renumbering would shift every statement after them.
            val lowered = loweredStatements(s, method, locals, childIndex)
            if (lowered != null) lowered.forEach { block.addStatement(it) }
            else convertHoisting(s, method, locals, childIndex).forEach { block.addStatement(it) }
        }
        return block.build()
    }

    /** Convert one statement: a local `val`/`var`, an assignment, a `return`, or an expression statement. */
    /**
     * `val x = <init>` as a [Statement]. [initializerOverride] replaces the written initializer, which is how
     * the control-flow-elvis lowering keeps the declaration while its guard takes the `?: return` half.
     */
    private fun KaSession.localVariableCreation(statement: KtProperty, method: MethodInfo,
                                                locals: MutableMap<String, Variable>,
                                                initializerOverride: Expression?): Statement {
        val name = statement.name ?: "_"
        val type = (statement.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
            ?: runtime.objectParameterizedType()
        val initializer = initializerOverride
            ?: statement.initializer?.let { convertExpression(it, method, locals) }
            ?: runtime.newEmptyExpression()
        val local = runtime.newLocalVariable(name, type, initializer)
        locals[name] = local
        localDeclared(statement, local)
        // detail: name keyed by both the name String and the LocalVariable, plus the type reference
        val dsb = runtime.newDetailedSourcesBuilder()
        statement.nameIdentifier?.let { nameId ->
            val nameSource = source(nameId, "-")
            dsb.put(local.simpleName(), nameSource).put(local, nameSource)
        }
        dsb.putTypeReference(runtime, type, statement.typeReference)
        return runtime.newLocalVariableCreation(local)
            .withSource(runtime.noSource().withDetailedSources(dsb.build()))
    }

    /**
     * <b>`a?.b?.c` and `a ?: b` evaluate their tested operand once, not 2ⁿ times.</b>
     *
     * <p>Both lower to a ternary in which the tested operand stands TWICE — in the null test and in the
     * value — and the CST is a tree, so the same instance cannot be used for both (#32: prep then throws
     * "Trying to overwrite a value for property variableData"). Converting it a second time was the answer,
     * and it is wrong: converting evaluates. Down a chain it compounds, because each level's receiver is
     * itself a null-safe expression. ⭐ Measured on detekt before this: 331 sites twice over, 116 four
     * times, 28 eight times, 6 sixteen times and 4 <b>thirty-two</b> times — one call appearing 32 times in
     * the tree — and 20% of the placeholder census was duplicates.
     *
     * <p>The fix is a temporary, as in [controlFlowElvisLowering]. The question is where it may go, and the
     * answer is narrow on purpose:
     *
     * <h2>⛔ Only the unconditionally-evaluated spine</h2>
     * Hoisting an evaluation out of a conditional position CHANGES it: in `x?.foo(g())`, `g()` runs only
     * when `x != null`, so lifting it above the statement would call it always. So this walks only the
     * <b>spine</b> — from the statement's own expression down through safe-call receivers and elvis left
     * operands — which is by construction evaluated unconditionally, every time, before anything is tested.
     * Everything off the spine (arguments, the right-hand side of `?:`, lambda bodies, branch arms) is left
     * exactly as it was, still converted twice, and still counted.
     *
     * <p>Innermost first, so `a?.b?.c` yields `t0 = a.b; t1 = (t0 == null) ? null : t0.c` — linear in the
     * chain rather than exponential. A stable operand (a name, `this`, a constant, a dotted chain of those)
     * gets no temporary: re-reading it evaluates nothing.
     *
     * @return the temporaries' declarations, in evaluation order; empty when the statement has no spine
     *         worth hoisting. [hoistedReads] is left populated for the conversion that follows.
     */
    private fun KaSession.hoistNullSafeSpine(statement: KtExpression, method: MethodInfo,
                                             locals: MutableMap<String, Variable>,
                                             index: String): List<Statement> {
        hoistedReads.clear()
        val root = when (statement) {
            is KtProperty -> if (statement.isLocal) statement.initializer else null
            is KtReturnExpression -> statement.returnedExpression
            is KtBinaryExpression -> if (statement.operationToken == KtTokens.EQ) statement.right else statement
            else -> statement
        } ?: return listOf()
        val raw = ArrayList<Statement>()
        hoistSpineOf(root, method, locals, index, raw)
        // ⚠ indexed only now: the declarations and the statement they precede must sort, and `pad` needs the
        // total (9 temporaries and a statement would otherwise index .10 before .2, as strings)
        return raw.mapIndexed { i, d -> indexed(d, "$index.${pad(i, raw.size + 1)}") }
    }

    /** The index the hoisted statement itself takes, after [n] declarations. */
    private fun hoistedStatementIndex(index: String, n: Int): String = "$index.${pad(n, n + 1)}"

    /** Depth-first along the spine, so an inner receiver is bound before the outer one that reads it. */
    private fun KaSession.hoistSpineOf(expression: KtExpression, method: MethodInfo,
                                       locals: MutableMap<String, Variable>, index: String,
                                       declarations: MutableList<Statement>) {
        val tested = when {
            expression is KtSafeQualifiedExpression -> expression.receiverExpression
            expression is KtBinaryExpression && expression.operationToken == KtTokens.ELVIS -> expression.left
            expression is KtParenthesizedExpression -> {
                expression.expression?.let { hoistSpineOf(it, method, locals, index, declarations) }
                return
            }
            else -> return
        } ?: return
        hoistSpineOf(tested, method, locals, index, declarations)   // innermost first
        if (isStableReference(tested)) return                       // re-reading it evaluates nothing
        val type = tested.expressionType?.let { mapType(it, method.typeInfo()) }
            ?: runtime.objectParameterizedType()
        val name = "\$nullSafe${nullSafeTemporaries++}"
        val temporary = runtime.newLocalVariable(name, type, convertExpression(tested, method, locals))
        declarations.add(runtime.newLocalVariableCreation(temporary))
        hoistedReads[tested] = temporary
    }

    /**
     * The temporaries a TAIL expression's spine needs, and the index the tail itself then takes. A block's
     * or a lambda's value is evaluated unconditionally, so hoisting above it is safe — and it is where the
     * worst chains live: detekt's 32× site is the result expression of an `analyze(this) { … }` lambda.
     * ⚠ The caller must convert the tail while [hoistedReads] is still populated, and clear it afterwards.
     */
    private fun KaSession.hoistBefore(expression: KtExpression, method: MethodInfo,
                                      locals: MutableMap<String, Variable>,
                                      index: String): Pair<List<Statement>, String> {
        val hoisted = hoistNullSafeSpine(expression, method, locals, index)
        return hoisted to if (hoisted.isEmpty()) index else hoistedStatementIndex(index, hoisted.size)
    }

    /**
     * One statement, preceded by whatever temporaries its null-safe spine needs ([hoistNullSafeSpine]).
     * Almost always a list of one: only a chain like `a?.b?.c` or `f() ?: d` produces anything to hoist.
     */
    private fun KaSession.convertHoisting(annotated: KtExpression, method: MethodInfo,
                                          locals: MutableMap<String, Variable>,
                                          index: String): List<Statement> {
        val s = unannotated(annotated)
        val hoisted = hoistNullSafeSpine(s, method, locals, index)
        if (hoisted.isEmpty()) {
            hoistedReads.clear()
            return listOf(convertStatement(s, method, locals, index))
        }
        // the conversion below READS the temporaries, through hoistedReads; clear it after, never before
        val statement = convertStatement(s, method, locals, hoistedStatementIndex(index, hoisted.size))
        hoistedReads.clear()
        return hoisted + statement
    }

    /**
     * <b>`b?.f()` alone on a line is an `if`, not a ternary.</b>
     *
     * <p>A safe call lowers to `(b == null) ? null : b.f()`. In VALUE position that is right. In STATEMENT
     * position, where the value is discarded and the selector may return `Unit`, it produces a conditional
     * whose arms are a null constant and a <b>void call</b> — a shape Java cannot write, and one the
     * modification analysis does not follow: measured, `b?.next()?.add(t)` left the parameter
     * {@code unmodified=true} while the hand-written Java equivalent said {@code false}. The parse was
     * clean, prep isolated nothing, and the census saw no hole. Only comparing the VERDICT against the Java
     * the lowering claims to produce could show it (`TestLoweredShapesVsJava`).
     *
     * <p>So: `if (b != null) b.f();`, with the receiver spine hoisted as everywhere else.
     */
    private fun KaSession.safeCallAsStatementLowering(statement: KtExpression, method: MethodInfo,
                                                      locals: MutableMap<String, Variable>,
                                                      index: String): List<Statement>? {
        if (statement !is KtSafeQualifiedExpression) return null
        val receiverPsi = statement.receiverExpression
        // ⚠ the WHOLE safe call, not its receiver: the node whose tested operand needs a temporary is this
        // one, and walking from the receiver skips exactly that. Measured by the probe -- the receiver
        // ternary was still inlined twice, in the condition and in the call.
        val hoisted = hoistNullSafeSpine(statement, method, locals, index)
        val callIndex = if (hoisted.isEmpty()) index else hoistedStatementIndex(index, hoisted.size)
        val receiver = convertExpression(receiverPsi, method, locals)
        unwrappedSafeCalls[statement] = true
        val call = try {
            convertExpression(statement, method, locals)
        } finally {
            unwrappedSafeCalls.remove(statement)
            hoistedReads.clear()
        }
        val notNull = runtime.newUnaryOperator(listOf(), runtime.noSource(), runtime.logicalNotOperatorBool(),
            runtime.newEquals(receiver, runtime.nullConstant()), runtime.precedenceUnary())
        val guarded = runtime.newIfElseBuilder()
            .setExpression(notNull)
            .setIfBlock(runtime.newBlockBuilder().setSource(runtime.noSource().withIndex("$callIndex.0"))
                .addStatement(indexed(runtime.newExpressionAsStatement(call), "$callIndex.0.0")).build())
            .setElseBlock(runtime.newBlockBuilder()
                .setSource(runtime.noSource().withIndex("$callIndex.1")).build())
            .setSource(source(statement, callIndex))
            .build()
        return hoisted + guarded
    }

    /**
     * The two statement-level lowerings, in one place. ⚠ A statement list lives in FOUR places — a block
     * body, a lambda body, and the branch blocks of a value-yielding `try`/`if` — and a lowering that
     * reaches only the first sees a fraction of the sites: measured on detekt, all four remaining
     * `try`-as-a-value sites were outside {@code statementsToBlock} (three expression bodies, one inside a
     * lambda), so the first cut of this removed <b>none</b> of them.
     */
    private fun KaSession.loweredStatements(annotated: KtExpression, method: MethodInfo,
                                            locals: MutableMap<String, Variable>,
                                            index: String): List<Statement>? {
        val s = unannotated(annotated)
        return controlFlowElvisLowering(s, method, locals, index)
            ?: statementAsValueLowering(s, method, locals, index)
            ?: safeCallAsStatementLowering(s, method, locals, index)
    }

    /**
     * `@Suppress("…") expr` -> `expr`: an annotation on an EXPRESSION is for the compiler and linters (detekt's 18 are
     * all `@Suppress`); it has no run-time meaning, and Java cannot write one.
     */
    private fun unannotated(expression: KtExpression): KtExpression =
        generateSequence(expression) { (it as? KtAnnotatedExpression)?.baseExpression }.last()

    /** `if (c) { … } else { … }` in the position of a VALUE: each branch returns or assigns its tail. */
    private fun KaSession.convertValueIf(statement: KtIfExpression, method: MethodInfo,
                                         locals: MutableMap<String, Variable>, index: String,
                                         returning: Boolean, assignTo: Variable?): Statement =
        runtime.newIfElseBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-condition", statement))
            .setIfBlock(convertValueBlock(statement.then, method, locals, "$index.0", returning, assignTo))
            .setElseBlock(convertValueBlock(statement.`else`, method, locals, "$index.1", returning, assignTo))
            .setSource(source(statement, index)).build()

    /** A block in the position of a VALUE: its tail returns, assigns to [assignTo], or is plain. */
    private fun KaSession.convertValueBlock(body: KtExpression?, method: MethodInfo,
                                            locals: Map<String, Variable>, blockIndex: String,
                                            returning: Boolean, assignTo: Variable?): Block = when {
        assignTo != null -> convertAssigningBlock(assignTo, body, method, locals, blockIndex)
        returning -> convertReturningBlock(body, method, locals, blockIndex)
        else -> convertBlock(body, method, locals, blockIndex)
    }

    /**
     * <b>`x ?: return v` and `x ?: throw E()` — an elvis whose right-hand side transfers control.</b> Kotlin
     * writes it as an expression; the CST has no expression that leaves the method, which is why these were
     * `k2-unsupported-expr:KtReturnExpression` (270 on detekt, the largest construct kind after blocks).
     *
     * <h2>The lowering</h2>
     * <pre>
     *   val t = s ?: return 0      ->   if (s == null) return 0;
     *                                   val t = s;
     * </pre>
     * No temporary is needed in these positions: the guard tests the value and the declaration names it, as
     * kotlinc's own lowering does. ⚠ The left operand is converted TWICE, deliberately — the CST is a tree
     * and sharing a node makes every walker visit its statements twice (#32), which is the rule the elvis
     * expression conversion already follows.
     *
     * <h2>⛔ Only where the lowering cannot move an evaluation</h2>
     * The elvis must BE the statement, the whole initializer, or the whole returned value. `f(a(), x ?: return)`
     * is excluded: hoisting the guard would run the check before `a()`, which the source runs after. Those keep
     * their placeholder and stay counted, exactly as {@code cannotBePassed} leaves what it cannot prove.
     *
     * <h2>⛔ A labelled return is not this function's return</h2>
     * `?: return@mapNotNull null` leaves a LAMBDA, and lowering it to a plain `return` would emit a return
     * from the wrong method — silently, which is the one outcome worth avoiding. Refused, and counted.
     */
    private fun KaSession.controlFlowElvisLowering(statement: KtExpression, method: MethodInfo,
                                                   locals: MutableMap<String, Variable>,
                                                   index: String): List<Statement>? {
        val elvis = when {
            isControlFlowElvis(statement) -> statement as KtBinaryExpression
            statement is KtProperty && statement.isLocal && isControlFlowElvis(statement.initializer) ->
                statement.initializer as KtBinaryExpression
            statement is KtReturnExpression && isControlFlowElvis(statement.returnedExpression) ->
                statement.returnedExpression as KtBinaryExpression
            // `x = f() ?: return false`
            statement is KtBinaryExpression && statement.operationToken == KtTokens.EQ && isControlFlowElvis(statement.right) ->
                statement.right as KtBinaryExpression
            else -> return null
        }
        val left = elvis.left ?: return null
        val control = elvis.right ?: return null
        val isWholeStatement = statement === elvis

        // ⛔ The left operand is needed TWICE -- to test for null, and as the value. Converting it twice
        // EVALUATES it twice, which for `f() ?: return` means two calls where the source has one: a CST that
        // is well formed and says something the source does not, invisible to the placeholder census.
        // Measured before this was fixed: 190 such sites on detekt, 3 on coil. So a left operand that is not
        // a stable reference is bound to a temporary first, exactly as a hand-written Java version would.
        val statements = ArrayList<Statement>()
        val needsTemporary = !isWholeStatement && !isStableReference(left)
        val leftValue: () -> Expression = if (!needsTemporary) {
            { convertExpression(left, method, locals) }   // a name/this/dotted chain: re-reading it is free
        } else {
            val type = left.expressionType?.let { mapType(it, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val name = "\$elvis${elvisTemporaries++}"
            val temporary = runtime.newLocalVariable(name, type, convertExpression(left, method, locals))
            locals[name] = temporary
            statements.add(indexed(runtime.newLocalVariableCreation(temporary), "$index.0"))
            val read = { runtime.newVariableExpressionBuilder().setVariable(temporary)
                .setSource(runtime.noSource()).build() as Expression }
            read
        }
        // with a temporary the guard is the SECOND statement, so the indexes shift by one
        val guardIndex = when {
            isWholeStatement -> index
            needsTemporary -> "$index.1"
            else -> "$index.0"
        }
        val guard = runtime.newIfElseBuilder()
            .setExpression(runtime.newEquals(leftValue(), runtime.nullConstant()))
            .setIfBlock(statementsToBlock(listOf(control), method, locals, "$guardIndex.0"))
            .setElseBlock(runtime.newBlockBuilder()
                .setSource(runtime.noSource().withIndex("$guardIndex.1")).build())
            .setSource(source(elvis, guardIndex))
            .build()
        statements.add(guard)
        if (isWholeStatement) return statements
        val value = leftValue()
        val raw = when (statement) {
            is KtProperty -> localVariableCreation(statement, method, locals, value)
            is KtReturnExpression -> runtime.newReturnStatement(value)
            is KtBinaryExpression -> assignmentStatement(statement, value, method, locals)
            else -> return null
        }
        val whole = source(statement, if (needsTemporary) "$index.2" else "$index.1")
        val detailed = raw.source()?.detailedSources()
        statements.add(raw.withSource(if (detailed == null) whole else whole.withDetailedSources(detailed)))
        return statements
    }

    /**
     * <b>`val v = try { … } catch { … }` and `val v = if (c) { …; e } else { … }` — a STATEMENT used as a
     * value.</b> The CST has neither as an expression, so the declaration is split from the assignment and
     * each branch assigns into it, which is the shape Java writes by hand and kotlinc compiles to:
     * <pre>
     *   val v = try { X } catch (e) { Y }   ->   T v;
     *                                            try { v = X } catch (e) { v = Y }
     * </pre>
     * ⚠ Only for a branch the expression path cannot already handle: a single-expression `if` branch became
     * an `InlineConditional` in §7.12 and stays one, which is the better node. This picks up what is left —
     * a block of several statements, and every `try`.
     */
    private fun KaSession.statementAsValueLowering(statement: KtExpression, method: MethodInfo,
                                                   locals: MutableMap<String, Variable>,
                                                   index: String): List<Statement>? {
        if (statement !is KtProperty || !statement.isLocal) return null
        val initializer = statement.initializer
        val needsLowering = initializer is KtTryExpression
                || (initializer is KtIfExpression && initializer.hasAMultiStatementBranch())
        if (!needsLowering) return null
        // the declaration, WITHOUT an initializer: a local that is assigned in each branch, as Java writes it
        val declaration = localVariableCreation(statement, method, locals, runtime.newEmptyExpression())
        val target = locals[statement.name ?: "_"] ?: return null
        val body = when (initializer) {
            // ⚠ stamped here: `convertTry` sets no index of its own — the normal path gets one from
            // `convertStatement` afterwards, and prepwork reads `source().index()` on every statement
            is KtTryExpression -> convertTry(initializer, method, locals, "$index.1", assignTo = target)
                .withSource(source(initializer, "$index.1"))
            is KtIfExpression -> convertValueIf(initializer, method, locals, "$index.1",
                returning = false, assignTo = target)
            else -> return null
        }
        val whole = source(statement, "$index.0")
        val detailed = declaration.source()?.detailedSources()
        return listOf(
            declaration.withSource(if (detailed == null) whole else whole.withDetailedSources(detailed)),
            body)
    }

    /** A branch that is a block of anything but one expression — what §7.12's expression arm cannot take. */
    private fun KtIfExpression.hasAMultiStatementBranch(): Boolean =
        listOf(then, `else`).any { branch ->
            branch is KtBlockExpression && (branch.statements.size != 1
                    || branch.statements.single() !is KtExpression
                    || branch.statements.single() is KtDeclaration)
        }

    /** An elvis whose right-hand side leaves the method: `?: return v` (unlabelled) or `?: throw E()`. */
    /**
     * Whether re-evaluating [expression] is free of consequence: a name, `this`, a constant, or a dotted
     * chain of those. Anything else — a call, an index, a constructor — must not be evaluated twice.
     */
    private fun isStableReference(expression: KtExpression?): Boolean = when (expression) {
        is KtNameReferenceExpression, is KtThisExpression, is KtConstantExpression -> true
        is KtParenthesizedExpression -> isStableReference(expression.expression)
        is KtDotQualifiedExpression ->
            isStableReference(expression.receiverExpression) && isStableReference(expression.selectorExpression)
        else -> false
    }

    private fun isControlFlowElvis(expression: KtExpression?): Boolean {
        if (expression !is KtBinaryExpression || expression.operationToken != KtTokens.ELVIS) return false
        // `?: return`, `?: return@label v` (from the lambda, as a lambda's return statement is), `?: throw`,
        // `?: continue`, `?: break`: a jump Java writes as the body of an `if (x == null)`
        return when (expression.right) {
            is KtThrowExpression, is KtReturnExpression, is KtContinueExpression, is KtBreakExpression -> true
            else -> false
        }
    }

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
     * ⚠ A BARE [Runtime.newEmptyExpression] and a `k2-…` placeholder are different claims, and the remaining
     * bare ones in this file are deliberate. "There is no expression here" is a fact about well-formed Kotlin
     * — `val x: Int` with no initializer, a `for` loop's variable, a `when` with no guard, an `else` arm,
     * `return` from a Unit function — and the CST spells that as an empty expression, exactly as the Java
     * front end does. A REQUIRED child that is missing is a different thing: it happens only on source that
     * does not parse, and an unmarked empty then hides the hole from the census, from the refactoring
     * engine's range scan, and from the reader. Those are `k2-absent-…`.
     */

    /** One of this front end's placeholders (`k2-…`) without a range yet. */
    /**
     * Kotlin properties of the mapped collection types whose JVM getter follows no naming rule: kotlinc compiles
     * `map.keys` to `Map.keySet()` and `map.entries` to `entrySet()` (`values` is `values()`, found by name). Tried
     * last, so a type's own `keys`/`getKeys` still wins.
     */
    private val MAPPED_PROPERTIES = mapOf("keys" to "keySet", "entries" to "entrySet")

    private fun isPlaceholder(e: Expression): Boolean =
        e is EmptyExpression && e.msg()?.startsWith(K2_PLACEHOLDER_PREFIX) == true && e.source() == null

    /** A placeholder standing for all of [statement], which was not converted. */
    private fun placeholder(msg: String, statement: PsiElement): Expression =
        runtime.newEmptyExpression(msg).withSource(source(statement, "-"))

    /**
     * A single-entry [DetailedSources] recording a source-form [marker] (e.g. `NULL_COALESCING`) at the
     * operator token [psi], so the refactoring engine can reproduce the original surface syntax of a
     * desugared node. Empty if [psi] is absent.
     */
    private fun marker(marker: Any, psi: PsiElement?): DetailedSources =
        runtime.newDetailedSourcesBuilder().also { if (psi != null) it.put(marker, source(psi, "-")) }.build()

    /** `target = value` / `target op= value`, [value] already converted (the control-flow elvis lowering passes its own). */
    private fun KaSession.assignmentStatement(statement: KtBinaryExpression, value: Expression, method: MethodInfo,
                                              locals: MutableMap<String, Variable>): Statement {
        val left = statement.left
        return if (left is KtArrayAccessExpression && statement.operationToken == KtTokens.EQ) {
            runtime.newExpressionAsStatement(convertIndexedSet(left, value, method, locals)) // a[i] = v -> a.set(i, v)
        } else if (left is KtArrayAccessExpression) {
            // a[i] op= v -> a.set(i, a.get(i) op v)  (numeric/string; else placeholder)
            val combined = augmentedCombine(convertArrayAccess(left, method, locals), value, statement.operationToken)
            runtime.newExpressionAsStatement(
                if (combined == null) placeholder("k2-augmented-index:${statement.operationToken}", statement)
                else convertIndexedSet(left, combined, method, locals))
        } else {
            val leftExpression = left?.let { convertExpression(it, method, locals) }
            val target = leftExpression as? VariableExpression
            // a property with no backing field reads as a call of its getter: assigning to it is a call of its
            // setter, `c.computed = v` -> `c.setComputed(v)`, which is what kotlinc compiles too (#36)
            val setterCall = if (target != null || statement.operationToken != KtTokens.EQ) null
            else (leftExpression as? MethodCall)?.let { setterCall(it, value) }
            if (target == null) runtime.newExpressionAsStatement(
                setterCall ?: placeholder("k2-assign-target", statement))
            else {
                val builder = runtime.newAssignmentBuilder().setTarget(target).setValue(value).setSource(runtime.noSource())
                augmentedOperator(statement.operationToken)?.let { builder.setAssignmentOperator(it) } // x += y
                runtime.newExpressionAsStatement(builder.build())
            }
        }
    }

    private fun KaSession.rawStatement(statement: KtExpression, method: MethodInfo,
                                       locals: MutableMap<String, Variable>, index: String,
                                       label: String? = null): Statement = when {
        // `loop@ for (…) { … }`: a labelled statement -> convert the base and attach the label (so
        // `break@loop`/`continue@loop` have a named target). Any non-loop base just keeps the label too.
        statement is KtLabeledExpression -> rawStatement(
            statement.baseExpression ?: return runtime.newExpressionAsStatement(placeholder("k2-empty-label", statement)),
            method, locals, index, statement.getLabelName())
        statement is KtProperty && statement.isLocal -> localVariableCreation(statement, method, locals, null)
        statement is KtNamedFunction && statement.isLocal -> convertLocalFunction(statement, method, locals)
            ?: runtime.newExpressionAsStatement(placeholder("k2-unsupported-expr:KtNamedFunction", statement))
        statement is KtBinaryExpression && isAssignment(statement.operationToken) -> assignmentStatement(statement,
            statement.right?.let { convertExpression(it, method, locals) } ?: placeholder("k2-absent-assignment-value", statement),
            method, locals)
        // `return try { … } catch { … }`: lower the try-as-value to a try statement whose branches `return`
        statement is KtReturnExpression && statement.returnedExpression is KtTryExpression ->
            convertTry(statement.returnedExpression as KtTryExpression, method, locals, index, returning = true)
        statement is KtReturnExpression -> runtime.newReturnStatement(
            statement.returnedExpression?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
        )
        statement is KtIfExpression -> runtime.newIfElseBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-condition", statement))
            .setIfBlock(convertBlock(statement.then, method, locals, "$index.0"))
            .setElseBlock(convertBlock(statement.`else`, method, locals, "$index.1"))
            .setSource(runtime.noSource()).build()
        statement is KtWhileExpression -> runtime.newWhileBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-condition", statement))
            .setBlock(convertBlock(statement.body, method, locals, "$index.0"))
            .also { b -> label?.let { b.setLabel(it) } }
            .setSource(runtime.noSource()).build()
        statement is KtForExpression -> {
            // for (x in iterable) { … } -> ForEachStatement; x is a local in scope for the body
            val parameter = statement.loopParameter
            val name = parameter?.name ?: "_"
            val type = (parameter?.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val loopVariable = runtime.newLocalVariable(name, type, runtime.newEmptyExpression())
            parameter?.let { localDeclared(it, loopVariable) }
            runtime.newForEachBuilder()
                .setInitializer(runtime.newLocalVariableCreation(loopVariable))
                .setExpression(statement.loopRange?.let { convertExpression(it, method, locals) }
                    ?: placeholder("k2-absent-loop-range", statement))
                .setBlock(convertBlock(statement.body, method, locals + (name to loopVariable), "$index.0"))
                .also { b -> label?.let { b.setLabel(it) } }
                .setSource(runtime.noSource()).build()
        }
        statement is KtWhenExpression -> convertWhen(statement, method, locals, index)
        statement is KtDoWhileExpression -> runtime.newDoBuilder()
            .setExpression(statement.condition?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-condition", statement))
            .setBlock(convertBlock(statement.body, method, locals, "$index.0"))
            .also { b -> label?.let { b.setLabel(it) } }
            .setSource(runtime.noSource()).build()
        statement is KtBreakExpression -> runtime.newBreakBuilder()
            .also { b -> statement.getLabelName()?.let { b.setGoToLabel(it) } } // break@label
            .setSource(runtime.noSource()).build()
        statement is KtContinueExpression -> runtime.newContinueBuilder()
            .also { b -> statement.getLabelName()?.let { b.setGoToLabel(it) } } // continue@label
            .setSource(runtime.noSource()).build()
        statement is KtThrowExpression -> runtime.newThrowBuilder()
            .setExpression(statement.thrownExpression?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-thrown", statement))
            .setSource(runtime.noSource()).build()
        statement is KtTryExpression -> convertTry(statement, method, locals, index)
        statement is KtDestructuringDeclaration -> convertDestructuring(statement, method, locals)
        // a local class/interface declared in the body: `class C : A { … }` -> a LocalTypeDeclaration statement
        statement is KtClassOrObject -> convertLocalType(statement, method, locals)
        else -> runtime.newExpressionAsStatement(convertExpression(statement, method, locals))
    }

    /**
     * A local type declaration `class C : A { … }` inside a method body. Builds the local type (via the member
     * converter, so it gets its full members) with the enclosing method's parameters and locals available for
     * capture, and wraps it in a [io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration].
     */
    private fun KaSession.convertLocalType(declaration: KtClassOrObject, method: MethodInfo,
                                           locals: Map<String, Variable>): Statement {
        // capture set: the enclosing method's locals PLUS its parameters (parameters are not in `locals`),
        // so a read of an enclosing variable inside the local type's methods resolves to it
        val captured = locals + method.parameters().associateBy { it.name() }
        val typeInfo = with(memberConverter) { buildLocalType(method, declaration, captured) }
        return runtime.newLocalTypeDeclarationBuilder().setTypeInfo(typeInfo).setSource(runtime.noSource()).build()
    }

    /**
     * `val (a, b) = p` -> a multi-variable [LocalVariableCreation] where each local's initializer is the
     * matching `p.componentN()` call (when that component resolves on `p`'s type; else a placeholder). The
     * locals enter scope so later references resolve.
     */
    private fun KaSession.convertDestructuring(statement: KtDestructuringDeclaration, method: MethodInfo,
                                               locals: MutableMap<String, Variable>): Statement {
        val initializer = statement.initializer?.let { convertExpression(it, method, locals) }
            ?: placeholder("k2-absent-destructuring-value", statement)
        val variables = destructure(statement.entries, { initializer }, statement, method, locals)
        if (variables.isEmpty()) return runtime.newExpressionAsStatement(placeholder("k2-destructuring", statement))
        val builder = runtime.newLocalVariableCreationBuilder().setLocalVariable(variables.first())
        variables.drop(1).forEach { builder.addOtherLocalVariable(it) }
        return builder.setSource(runtime.noSource()).build()
    }

    /**
     * A LOCAL function, `fun g(x: X): R { … }` in a body, as a local variable holding its function object:
     * `Function1<X, R> g = x -> { … }`, a lambda over an anonymous type implementing `kotlin.jvm.functions.FunctionN`
     * -- the shape a function value already has, so `g(x)` is `g.invoke(x)` and `::g` is `g`. A local EXTENSION
     * takes its receiver as the first parameter, `$receiver`, as a receiver lambda does. The variable is in scope in
     * its own body (a recursive call). Captured locals stay closure reads, as in any lambda.
     * (kotlinc compiles a non-capturing one to a synthetic static method `outer$g` instead; both call one body.)
     */
    private fun KaSession.convertLocalFunction(fn: KtNamedFunction, method: MethodInfo,
                                               locals: MutableMap<String, Variable>): Statement? {
        val symbol = fn.symbol as? KaNamedFunctionSymbol ?: return null
        val name = fn.name ?: return null
        val enclosingType = method.typeInfo()
        val receiverType = symbol.receiverParameter?.let { mapType(it.returnType, enclosingType, method) }
        val parameters = listOfNotNull(receiverType?.let { "\$receiver" to it }) +
            symbol.valueParameters.map { it.name.asString() to mapType(it.returnType, enclosingType, method) }
        val returnType = mapType(symbol.returnType, enclosingType, method)
        val functionN = (findClass(org.jetbrains.kotlin.name.ClassId.fromString("kotlin/jvm/functions/Function${parameters.size}"))
            as? KaNamedClassSymbol)?.let { classTypeInfo(it) } ?: return null
        val boxedReturn = if (returnType == runtime.voidParameterizedType()) runtime.objectParameterizedType()
                          else returnType.ensureBoxed(runtime)
        val functionalType = runtime.newParameterizedType(functionN,
            parameters.map { it.second.ensureBoxed(runtime) } + boxedReturn)

        val anonymousType = runtime.newAnonymousType(enclosingType, enclosingType.builder().getAndIncrementAnonymousTypes())
        anonymousType.builder().setAccess(runtime.accessPrivate()).setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
        val sam = runtime.newMethod(anonymousType, "invoke", runtime.methodTypeMethod())
        val samBuilder = sam.builder()
        parameters.forEach { (n, t) -> samBuilder.addParameter(n, t) }
        samBuilder.setReturnType(returnType).setAccess(runtime.accessPublic()).setSynthetic(true).commitParameters()

        // declared first, so the body can call itself
        val variable = runtime.newLocalVariable(name, functionalType)
        locals[name] = variable
        val bodyScope: MutableMap<String, Variable> = locals.toMutableMap()
        sam.parameters().forEach { bodyScope[it.name()] = it }
        val body = fn.bodyBlockExpression?.let { statementsToBlock(it.statements, method, bodyScope, "") }
            ?: fn.bodyExpression?.let { e ->
                val value = convertExpression(e, method, bodyScope)
                runtime.newBlockBuilder().addStatement(indexed(
                    if (returnType == runtime.voidParameterizedType()) runtime.newExpressionAsStatement(value)
                    else runtime.newReturnStatement(value), "0")).build()
            } ?: runtime.emptyBlock()
        samBuilder.setMethodBody(body).commit()
        anonymousType.builder().addMethod(sam).addInterfaceImplemented(functionalType).setEnclosingMethod(method)
            .setSingleAbstractMethod(sam).commit()
        val lambda = runtime.newLambdaBuilder().setMethodInfo(sam)
            .setOutputVariants(parameters.map { runtime.lambdaOutputVariantEmpty() }).setSource(runtime.noSource()).build()
        return runtime.newLocalVariableCreationBuilder()
            .setLocalVariable(runtime.newLocalVariable(name, functionalType, lambda).also { locals[name] = it })
            .setSource(runtime.noSource()).build()
    }

    /**
     * The local variables of a destructuring -- `val (a, b) = x`, or a lambda's `(a, b) ->` -- each initialised by
     * the component its entry reads from [source] (evaluated once per entry, as kotlinc reads a stable value). `_`
     * declares nothing. The component is the source type's `componentN()`, or, when K2 resolves it to the stdlib's
     * `Map.Entry` extension (`@InlineOnly`, absent from bytecode), the `getKey()`/`getValue()` kotlinc inlines.
     */
    private fun KaSession.destructure(entries: List<KtDestructuringDeclarationEntry>, source: () -> Expression,
                                      psi: PsiElement, method: MethodInfo,
                                      locals: MutableMap<String, Variable>): List<LocalVariable> =
        entries.mapIndexedNotNull { i, entry ->
            val name = entry.name ?: return@mapIndexedNotNull null
            if (name == "_") return@mapIndexedNotNull null
            val type = (entry.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val value = source()
            val sourceType = value.parameterizedType().typeInfo()?.let { members(it) }
            val component = sourceType?.let { resolveCallee(it, "component${i + 1}", listOf()) }?.let { it to listOf() }
                ?: sourceType?.let { inlinedComponent(entry, it, i) }
            val init = component?.let { (callee, arguments) ->
                runtime.newMethodCallBuilder().setObject(value).setObjectIsImplicit(false).setMethodInfo(callee)
                    .setParameterExpressions(arguments).setConcreteReturnType(type).setTypeArguments(listOf())
                    .setSource(runtime.noSource()).build()
            } ?: placeholder("k2-component${i + 1}", psi)
            runtime.newLocalVariable(name, type, init).also { locals[name] = it; localDeclared(entry, it) }
        }

    /**
     * A stdlib `componentN` extension, `@InlineOnly` and so absent from bytecode, as the call kotlinc inlines --
     * when K2 says that is the call: `Map.Entry`'s `getKey()`/`getValue()`, a `List`'s `get(N-1)`.
     */
    private fun KaSession.inlinedComponent(entry: KtDestructuringDeclarationEntry, sourceType: TypeInfo,
                                           i: Int): Pair<MethodInfo, List<Expression>>? {
        val callee = entry.resolveToCall()?.singleFunctionCallOrNull()?.symbol as? KaNamedFunctionSymbol ?: return null
        if (callee.receiverParameter == null || callee.callableId?.packageName?.asString() != "kotlin.collections") return null
        return when (callee.receiverParameter?.returnType?.expandedSymbol?.classId?.asFqNameString()) {
            "kotlin.collections.Map.Entry" ->
                resolveCallee(sourceType, if (i == 0) "getKey" else "getValue", listOf())?.takeIf { i <= 1 }?.let { it to listOf() }
            "kotlin.collections.List" -> listOf<Expression>(runtime.newInt(i)).let { index ->
                resolveCallee(sourceType, "get", index)?.let { it to index } }
            else -> null
        }
    }

    /**
     * `try { … } catch (e: T) { … } finally { … }` -> [io.codelaser.maddi.cst.api.statement.TryStatement].
     * Each sub-block is indexed like the Java parser: try block `index.0`, catch i `index.(i+1)`, finally
     * last. Kotlin catch is single-type (no Java-style union), and the catch variable is in scope for its
     * block.
     */
    private fun KaSession.convertTry(statement: KtTryExpression, method: MethodInfo,
                                     locals: MutableMap<String, Variable>, index: String,
                                     returning: Boolean = false, assignTo: Variable? = null): Statement {
        // a try USED AS A VALUE yields the tail of the try/catch blocks; Java has no try-expression, so it
        // lowers to a try STATEMENT whose branches `return` their tail (`return try { … }`) or ASSIGN it
        // (`val v = try { … }`, where the declaration is split off ahead of the statement).
        val convert = { body: KtExpression?, i: String ->
            convertValueBlock(body, method, locals, i, returning, assignTo)
        }
        val builder = runtime.newTryBuilder().setSource(runtime.noSource())
        builder.setBlock(convert(statement.tryBlock, "$index.0"))
        statement.catchClauses.forEachIndexed { i, catch ->
            val parameter = catch.catchParameter
            val type = (parameter?.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val name = parameter?.name ?: "e"
            val catchVariable = runtime.newLocalVariable(name, type, runtime.newEmptyExpression())
            parameter?.let { localDeclared(it, catchVariable) }
            val catchLocals = (locals + (name to catchVariable)).toMutableMap()
            builder.addCatchClause(
                runtime.newCatchClauseBuilder()
                    .addType(type)
                    .setCatchVariable(catchVariable)
                    .setFinal(false)
                    .setBlock(convertValueBlock(catch.catchBody, method, catchLocals, "$index.${i + 1}",
                            returning, assignTo))
                    .setSource(runtime.noSource()).build()
            )
        }
        // a finally never yields the try's value; the CST TryStatement always needs a finally block (empty if absent)
        builder.setFinallyBlock(statement.finallyBlock?.let {
            convertBlock(it.finalExpression, method, locals, "$index.${statement.catchClauses.size + 1}")
        } ?: runtime.newBlockBuilder().setSource(runtime.noSource()).build())
        return builder.build()
    }

    /**
     * Convert a block whose tail expression is its value, turning that tail into a `return` (Kotlin's
     * last-expression-is-the-value rule, for a block used as a value -- e.g. a `return try { … }` branch).
     * Only a plain expression tail is rewritten; a control-flow tail (if/when/loop) keeps its statement form.
     */
    private fun KaSession.convertReturningBlock(body: KtExpression?, method: MethodInfo,
                                                locals: Map<String, Variable>, blockIndex: String): Block {
        val childLocals = locals.toMutableMap()
        val statements = when (body) {
            null -> emptyList()
            is KtBlockExpression -> body.statements
            else -> listOf(body)
        }
        val block = runtime.newBlockBuilder()
        if (blockIndex.isNotEmpty()) block.setSource(runtime.noSource().withIndex(blockIndex))
        statements.forEachIndexed { j, s ->
            val childIndex = if (blockIndex.isEmpty()) pad(j, statements.size) else "$blockIndex.${pad(j, statements.size)}"
            val lowered = if (j == statements.lastIndex) null else loweredStatements(s, method, childLocals, childIndex)
            if (lowered != null) return@forEachIndexed lowered.forEach { block.addStatement(it) }
            if (j != statements.lastIndex) {
                return@forEachIndexed convertHoisting(s, method, childLocals, childIndex)
                    .forEach { block.addStatement(it) }
            }
            val (hoisted, tailIndex) = hoistBefore(s, method, childLocals, childIndex)
            hoisted.forEach { block.addStatement(it) }
            val stmt = convertStatement(s, method, childLocals, tailIndex)
            hoistedReads.clear()
            // the `return` takes the index of the statement it replaces: the analyzer requires one on every statement
            block.addStatement(if (stmt is ExpressionAsStatement)
                indexed(runtime.newReturnStatement(stmt.expression()), tailIndex) else stmt)
        }
        return block.build()
    }

    /**
     * As [convertReturningBlock], but the block's value goes to [target] instead of out of the method:
     * `{ …; e }` becomes `{ …; target = e }`. This is what lets a `try` or a multi-statement `if` be used as
     * a VALUE — the CST has neither as an expression, so the declaration is split from the assignment and
     * each branch assigns.
     */
    private fun KaSession.convertAssigningBlock(target: Variable, body: KtExpression?, method: MethodInfo,
                                                locals: Map<String, Variable>, blockIndex: String): Block {
        val childLocals = locals.toMutableMap()
        val statements = when (body) {
            null -> emptyList()
            is KtBlockExpression -> body.statements
            else -> listOf(body)
        }
        val block = runtime.newBlockBuilder()
        if (blockIndex.isNotEmpty()) block.setSource(runtime.noSource().withIndex(blockIndex))
        statements.forEachIndexed { j, s ->
            val childIndex = if (blockIndex.isEmpty()) pad(j, statements.size) else "$blockIndex.${pad(j, statements.size)}"
            val lowered = if (j == statements.lastIndex) null else loweredStatements(s, method, childLocals, childIndex)
            if (lowered != null) return@forEachIndexed lowered.forEach { block.addStatement(it) }
            if (j != statements.lastIndex) {
                return@forEachIndexed convertHoisting(s, method, childLocals, childIndex)
                    .forEach { block.addStatement(it) }
            }
            val (hoisted, tailIndex) = hoistBefore(s, method, childLocals, childIndex)
            hoisted.forEach { block.addStatement(it) }
            val stmt = convertStatement(s, method, childLocals, tailIndex)
            hoistedReads.clear()
            block.addStatement(if (stmt is ExpressionAsStatement)
                indexed(runtime.newExpressionAsStatement(
                    runtime.newAssignment(runtime.newVariableExpressionBuilder().setVariable(target)
                        .setSource(runtime.noSource()).build(), stmt.expression())), tailIndex)
            else stmt)
        }
        return block.build()
    }

    /**
     * `when (subject) { v -> …; a, b -> …; else -> … }` -> [SwitchStatementNewStyle]. Each arm becomes a
     * [SwitchEntry] (its `when`-condition expressions are the case labels; `else` is a single
     * `EmptyExpression`), the body a block. A subject-less `when { … }` uses a `true` placeholder selector
     * (per the CST-for-Kotlin assessment). `is`/`in` conditions are skipped for now.
     */
    private fun KaSession.convertWhen(statement: KtWhenExpression, method: MethodInfo,
                                      locals: Map<String, Variable>, index: String): Statement {
        val (selector, entryLocals) = whenSubject(statement, method, locals)
        return runtime.newSwitchStatementNewStyleBuilder()
            .setSelector(selector)
            .addSwitchEntries(whenEntries(statement, method, selector, entryLocals, index))
            .setSource(runtime.noSource()).build()
    }

    /**
     * The `when` selector expression and the locals visible to the arms. For `when (val v = e)` the subject
     * VARIABLE `v` binds a local whose value is the selector, so the arms can reference `v`; the selector is
     * that same local (single evaluation). Otherwise the selector is the plain subject (or `true` if absent).
     */
    private fun KaSession.whenSubject(statement: KtWhenExpression, method: MethodInfo,
                                      locals: Map<String, Variable>): Pair<Expression, Map<String, Variable>> {
        statement.subjectVariable?.let { subjectVar ->
            val name = subjectVar.name ?: "\$subject"
            val type = (subjectVar.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, method.typeInfo()) }
                ?: runtime.objectParameterizedType()
            val init = subjectVar.initializer?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-when-subject", subjectVar)
            val local = runtime.newLocalVariable(name, type, init)
            localDeclared(subjectVar, local)
            return variableExpression(local) to (locals + (name to local))
        }
        val selector = statement.subjectExpression?.let { convertExpression(it, method, locals) } ?: runtime.newBoolean(true)
        return selector to locals
    }

    /**
     * Build the switch entries for a `when`. Following the modern-Java pattern-switch model: a `is T` arm
     * is a **type pattern** carried on [SwitchEntry.patternVariable] (a [RecordPattern]), *not* a condition
     * expression — so it is compatible with how the analyzer reads pattern switches. Value arms (`v ->`)
     * are case-label conditions; `in range` is a `contains` call condition (`!in` negated).
     */
    private fun KaSession.whenEntries(statement: KtWhenExpression, method: MethodInfo,
                                      subject: Expression, locals: Map<String, Variable>, prefix: String): List<SwitchEntry> {
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
                        is KtWhenConditionIsPattern -> // `is T` -> a type pattern; `!is T` -> a negated InstanceOf
                            if (condition.isNegated) conditions.add(negatedIsCondition(condition, method, subject))
                            else typePattern(condition, method)?.let { builder.setPatternVariable(it) }
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

    /**
     * A `!is T` arm as `!(subject instanceof T)` — the same node [convertIsExpression] builds for the
     * expression form of `x !is T`, which this arm is the switch-entry spelling of.
     *
     * <h2>⛔ It used to be DROPPED, and silently</h2>
     * A negated `is` is not representable as a type pattern, so the arm was left with no pattern AND no
     * condition — a switch entry indistinguishable from one whose condition is trivially true. Unlike a
     * `k2-…` placeholder this left no trace at all: not in the tree, not in the placeholder census, not in
     * a range a consumer could re-read. The subject is already converted for the `in range` arms beside it,
     * so the node costs nothing extra.
     *
     * ⚠ Falls back to a MARKED placeholder when the type does not map, rather than to silence again.
     */
    private fun KaSession.negatedIsCondition(condition: KtWhenConditionIsPattern, method: MethodInfo,
                                             subject: Expression): Expression {
        val testType = condition.typeReference?.type?.let { mapType(it, method.typeInfo()) }
            ?: return placeholder("k2-when-is-unresolved", condition)
        val instanceOf = runtime.newInstanceOfBuilder().setExpression(subject).setTestType(testType)
            .setSource(runtime.noSource()).build()
        return runtime.newUnaryOperator(listOf(), runtime.noSource(), runtime.logicalNotOperatorBool(),
            instanceOf, runtime.precedenceUnary())
    }

    /** A Kotlin `is T` arm as a type-pattern [RecordPattern] (Kotlin smartcasts the subject, so the bound
     * variable is synthetic). */
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
        val contains = members(containsOn).methods().firstOrNull { it.name() == "contains" && it.parameters().size == 1 } ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(range).setObjectIsImplicit(false).setMethodInfo(contains)
            .setParameterExpressions(listOf(subject ?: runtime.newEmptyExpression()))
            .setConcreteReturnType(runtime.booleanParameterizedType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /**
     * The setter call that [getterCall] -- the read of a property without a backing field -- becomes when it is
     * assigned to: the same receiver, the sibling `setX` of its `getX` (of its `isX`, for a property named `isX`), and
     * the assigned [value] as the argument. Null when there is no such setter, which leaves the caller its placeholder.
     */
    private fun setterCall(getterCall: MethodCall, value: Expression): MethodCall? {
        val getter = getterCall.methodInfo()
        val setterName = when {
            getter.name().startsWith("get") -> "set" + getter.name().substring(3)
            getter.name().startsWith("is") -> "set" + getter.name().substring(2)
            else -> return null
        }
        val setter = getter.typeInfo().methods().singleOrNull {
            it.name() == setterName && it.parameters().size == getter.parameters().size + 1
        } ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(getterCall.`object`()).setObjectIsImplicit(getterCall.objectIsImplicit())
            .setMethodInfo(setter)
            .setParameterExpressions(getterCall.parameterExpressions() + value)
            .setConcreteReturnType(runtime.voidParameterizedType()).setTypeArguments(listOf())
            .setSource(runtime.noSource()).build()
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

    /** The combined value for an augmented indexed assignment `a[i] op= v` -> `a.get(i) op v` (numeric/String), else null. */
    private fun augmentedCombine(left: Expression, right: Expression, token: com.intellij.psi.tree.IElementType): Expression? {
        val numeric = left.isNumeric && right.isNumeric
        val stringPlus = left.parameterizedType().isJavaLangString || right.parameterizedType().isJavaLangString
        val opAndPrecedence = when (token) {
            KtTokens.PLUSEQ -> when {
                stringPlus -> runtime.plusOperatorString() to runtime.precedenceAdditive()
                numeric -> runtime.plusOperatorInt() to runtime.precedenceAdditive()
                else -> null
            }
            KtTokens.MINUSEQ -> if (numeric) runtime.minusOperatorInt() to runtime.precedenceAdditive() else null
            KtTokens.MULTEQ -> if (numeric) runtime.multiplyOperatorInt() to runtime.precedenceMultiplicative() else null
            KtTokens.DIVEQ -> if (numeric) runtime.divideOperatorInt() to runtime.precedenceMultiplicative() else null
            KtTokens.PERCEQ -> if (numeric) runtime.remainderOperatorInt() to runtime.precedenceMultiplicative() else null
            else -> null
        } ?: return null
        return runtime.newBinaryOperatorBuilder().setLhs(left).setRhs(right)
            .setOperator(opAndPrecedence.first).setPrecedence(opAndPrecedence.second)
            .setParameterizedType(left.parameterizedType()).setSource(runtime.noSource()).build()
    }

    /** An `init { … }` block, as a nested [Block] at [index] in [method]'s body, with a scope of its own. */
    internal fun KaSession.convertInitBlock(init: KtAnonymousInitializer, method: MethodInfo, index: String): Block =
        convertBlock(init.body, method, emptyMap(), index)

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
        // ⭐ already bound to a temporary by [hoistNullSafeSpine]: hand back a READ of it. A fresh
        // VariableExpression every time, never a shared instance -- sharing a node is what #32 forbids, and a
        // read of a local is a leaf, so there is nothing to walk twice.
        hoistedReads[expression]?.let { return runtime.newVariableExpressionBuilder().setVariable(it)
            .setSource(source(expression, "-")).build() }
        val raw = convertExpressionRaw(expression, method, locals)
        // a placeholder for code not converted keeps that code's range: what the CST does not represent
        if (raw is EmptyExpression) return if (isPlaceholder(raw)) raw.withSource(source(expression, "-")) else raw
        // apply the element's full range, but keep any DetailedSources a converter (e.g. convertCall) attached
        val rangeSource = source(expression, "-")
        val detailed = raw.source()?.detailedSources()
        return raw.withSource(if (detailed == null) rangeSource else rangeSource.withDetailedSources(detailed))
    }

    private fun KaSession.convertExpressionRaw(expression: KtExpression, method: MethodInfo,
                                               locals: Map<String, Variable>): Expression {
        expression.evaluate()?.let { constant ->
            val value = constant.value
            return constantExpression(runtime, value)
                ?: placeholder("k2-unsupported-constant:${value?.let { it::class.simpleName }}", expression)
        }
        return when (expression) {
            // in an extension function body, `this` is the receiver (the synthetic first parameter)
            is KtThisExpression -> receiverParam(method)?.let { variableExpression(it) } ?: self(method)
            // `super.m()`: `this`, marked writeSuper -> the callee resolves on the parent class (the
            // receiverType in convertQualified comes from `super`'s expressionType = the supertype)
            is KtSuperExpression -> variableExpression(runtime.newThis(method.typeInfo().asParameterizedType(), null, true))
            is KtAnnotatedExpression -> expression.baseExpression?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-unsupported-expr:KtAnnotatedExpression", expression)
            is KtClassLiteralExpression -> kotlinClassLiteral(expression, method)
                ?: placeholder("k2-unsupported-expr:KtClassLiteralExpression", expression)
            is KtNameReferenceExpression -> resolveReference(expression.getReferencedName(), method, locals)
                ?: implicitMemberAccess(expression, method, locals)
                ?: topLevelPropertyAccess(expression, method)
                ?: classAsValue(expression)
                ?: staticPropertyAccess(expression)
                ?: placeholder("k2-unresolved-ref:${expression.getReferencedName()}", expression)
            // ⛔ `(a + b).f()` used to be a placeholder, swallowing everything inside the parentheses with it:
            // 349 of detekt's 6,057 and 20 of coil's 437, the second-biggest kind on either corpus, for a
            // construct that is not a language feature at all. Kotlin's parentheses carry no semantics beyond
            // grouping, which the CST already expresses by its shape, so the inner expression IS the result —
            // the same thing javac's parser does with a JCParens.
            is KtParenthesizedExpression -> expression.expression?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-absent-parenthesized", expression)
            // ⛔ A BLOCK IN EXPRESSION POSITION. `val v = if (c) { a() } else { b() }` is one expression to
            // Kotlin and two blocks to the PSI, so it produced TWO placeholders — 287 of detekt's, the
            // biggest construct kind, for branches that are a single expression each.
            // ⚠ A block of several statements is a different problem and keeps a placeholder, now named for
            // what it is: representing it needs a temporary and a statement context, which is the same
            // lowering `convertTry(returning = true)` does and is not available here.
            is KtBlockExpression -> expression.statements.singleOrNull()
                ?.let { single -> (single as? KtExpression)?.takeIf { single !is KtDeclaration } }
                ?.let { convertExpression(it, method, locals) }
                ?: placeholder("k2-block-not-a-single-expression", expression)
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
                .setCondition(expression.condition?.let { convertExpression(it, method, locals) }
                    ?: placeholder("k2-absent-condition", expression))
                .setIfTrue(expression.then?.let { convertExpression(it, method, locals) }
                    ?: placeholder("k2-absent-branch", expression))
                .setIfFalse(expression.`else`?.let { convertExpression(it, method, locals) }
                    ?: placeholder("k2-absent-branch", expression))
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
            is KtWhenExpression -> { // when as an expression
                val (selector, entryLocals) = whenSubject(expression, method, locals)
                runtime.newSwitchExpressionBuilder()
                    .setSelector(selector)
                    .addSwitchEntries(whenEntries(expression, method, selector, entryLocals, ""))
                    .setParameterizedType(expression.expressionType?.let { mapType(it, method.typeInfo()) }
                        ?: runtime.objectParameterizedType())
                    .setSource(runtime.noSource()).build()
            }
            is KtCallableReferenceExpression -> convertCallableReference(expression, method, locals)
            else -> placeholder("k2-unsupported-expr:${expression::class.simpleName}", expression)
        }
    }

    /** `obj.f(...)` (method call) or `obj.x` (property/field access). */
    private fun KaSession.convertQualified(expression: KtQualifiedExpression, method: MethodInfo,
                                           locals: Map<String, Variable>): Expression {
        // static / companion / nested-object member access `Type.member` (the receiver is a type, not a
        // value): `Color.RED`, `Point.ORIGIN`, `Event.Close`. Value receivers resolve to a variable symbol
        // (not a class) and fall through to the normal `obj.member` handling below.
        staticMemberAccess(expression, method)?.let { return it }
        // `X::class.java` is the Java class literal `X.class` (kotlinc: an `LDC`), not a KClass then unwrapped
        if (expression.receiverExpression is KtClassLiteralExpression &&
            (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() == "java") {
            javaClassLiteral(expression.receiverExpression as KtClassLiteralExpression, method)?.let { return it }
        }
        // `Type.method(args)` where the receiver is a TYPE: a Java static (javalin's `TestUtil.test(app) { … }`).
        // The receiver is no value, so it converted to a placeholder and the call to another, which swallowed the
        // arguments -- including a lambda declaring an `object :` the rename censuses then never saw.
        (expression.selectorExpression as? KtCallExpression)
            ?.let { staticCall(expression.receiverExpression, it, method, locals) }?.let { return it }
        // `E.entries`: a STATIC property, whose receiver is a type, not a value
        (expression.selectorExpression as? KtNameReferenceExpression)?.let { staticPropertyAccess(it) }?.let { return it }
        val receiver = convertExpression(expression.receiverExpression, method, locals)
        val receiverType = superDispatchType(expression, method)
            ?: expression.receiverExpression.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        val selectorResult = when (val selector = expression.selectorExpression) {
            is KtCallExpression -> convertCall(selector, receiver to receiverType, false, method, locals)
            is KtNameReferenceExpression -> {
                val name = selector.getReferencedName()
                val field = receiverType?.let { members(it) }?.fields()?.firstOrNull { it.name() == name }
                when {
                    field != null -> variableExpression(runtime.newFieldReference(field, receiver, field.type())) // obj.x
                    // property idiom backed by an accessor method: `list.size`->size(), `obj.name`->getName()
                    else -> receiverType?.let { resolveAccessor(it, name) }?.let { accessorCall(receiver, it) }
                        // an EXTENSION property is not a member of the receiver's type, so neither lookup
                        // above can find it: `o.doubled` failed for a property declared in the same file,
                        // and `c.java`/`s.lastIndex` for every library one. It compiles to a static getter
                        // on the facade, exactly as an extension FUNCTION compiles to a static function.
                        ?: extensionPropertyAccess(selector, name, receiver, method, locals)
                        ?: memberExtensionPropertyAccess(selector, name, receiver, method, locals)
                        ?: placeholder("k2-unresolved-access:$name", selector)
                }
            }
            else -> placeholder("k2-unsupported-selector", expression)
        }
        // safe call `x?.foo()` -> `if (x == null) null else x.foo()`, marked NULL_SAFE at the `?.` token.
        // The receiver stands in the test as well as in the call, and the CST is a tree: it is converted a second
        // time for the test rather than shared, as the elvis operand above (#32).
        // the caller is making the null test a STATEMENT (`b?.f()` alone on a line): hand back the call
        if (expression is KtSafeQualifiedExpression && unwrappedSafeCalls.containsKey(expression)) return selectorResult
        return if (expression is KtSafeQualifiedExpression) runtime.newInlineConditionalBuilder()
            .setCondition(runtime.newEquals(convertExpression(expression.receiverExpression, method, locals),
                    runtime.nullConstant()))
            .setIfTrue(runtime.nullConstant()).setIfFalse(selectorResult)
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.NULL_SAFE, expression.operationTokenNode.psi)))
            .build(runtime)
        else selectorResult
    }

    /**
     * The supertype a `super.member` dispatches to. `super`'s own expression type is the superclass only while the class
     * has ONE supertype: detekt's `class R : Rule(…), RequiresAnalysisApi` got a type in which `visitCallExpression`
     * was not found, and 112 of its 333 `super.visitX(…)` calls were placeholders. K2's resolved call names the
     * supertype on its dispatch receiver. Null for any other receiver.
     */
    private fun KaSession.superDispatchType(expression: KtQualifiedExpression, method: MethodInfo): TypeInfo? {
        if (expression.receiverExpression !is KtSuperExpression) return null
        val call = expression.selectorExpression?.resolveToCall() ?: return null
        val dispatch = call.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.dispatchReceiver
            ?: call.successfulVariableAccessCall()?.partiallyAppliedSymbol?.dispatchReceiver
        return dispatch?.type?.let { mapType(it, method.typeInfo()).typeInfo() }
    }

    /**
     * A call on a type rather than a value, `Type.method(args)`, when the method is static on the JVM: the object is
     * a type expression, as the Java front end builds it. Null when the receiver is not a type, the type is not
     * known, or the callee is an instance method (a Kotlin `object`'s or companion's member, which
     * [KotlinBodyConverter.convertCall] routes through its singleton).
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtNameReferenceExpression)
    private fun KaSession.staticCall(receiverExpression: KtExpression, call: KtCallExpression, method: MethodInfo,
                                     locals: Map<String, Variable>): Expression? {
        val receiverClass = (receiverExpression as? KtNameReferenceExpression)
            ?.resolveSymbol() as? KaNamedClassSymbol ?: return null
        if ((call.resolveSymbol() as? KaNamedFunctionSymbol)?.isStatic != true) return null
        val fqn = receiverClass.classId?.asFqNameString() ?: return null
        val type = infoByFqn.getType(fqn, sourceSet) ?: with(typeMapper) { loadLibraryClass(receiverClass) } ?: return null
        val scope = runtime.newTypeExpression(type.asParameterizedType(), runtime.diamondNo())
        return convertCall(call, scope to type, false, method, locals)
    }

    /**
     * `Type.member` where the receiver is a type (not a value): a static field (`Color.RED`, a Java
     * static, a `const` companion forwarder), a nested object used as a value (`Event.Close` ->
     * `Close.INSTANCE`), or a (non-const) companion property (`Point.ORIGIN` -> `Point.Companion.ORIGIN`).
     * Returns null when the receiver isn't a known type or the member can't be located (so the caller
     * falls back to the ordinary value-receiver handling).
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtNameReferenceExpression)
    private fun KaSession.staticMemberAccess(expression: KtQualifiedExpression, method: MethodInfo): Expression? {
        val selector = expression.selectorExpression as? KtNameReferenceExpression ?: return null
        val name = selector.getReferencedName()
        val receiverClass = (expression.receiverExpression as? KtNameReferenceExpression)
            ?.resolveSymbol() as? KaNamedClassSymbol ?: return null
        // a source type (enum, companion holder) is already registered; a library type (`java.lang.System`
        // behind `System.out`) is loaded on demand so its static members are available.
        val receiverType = infoByFqn.getType(receiverClass.classId?.asFqNameString() ?: return null, sourceSet)
            ?: with(typeMapper) { loadLibraryClass(receiverClass) }
            ?: return null
        // a field on the type: a static field (enum entry, Java static, `const` companion forwarder) is a
        // direct static access; an instance field of a singleton (`Point.ORIGIN`, where Kotlin resolves the
        // `Point` receiver to its companion) is reached through the singleton handle
        members(receiverType).fields().firstOrNull { it.name() == name }?.let { field ->
            if (field.isStatic) return staticFieldRef(field, receiverType)
            singletonHandle(receiverType, receiverClass)?.let { handle ->
                return variableExpression(runtime.newFieldReference(field, handle, field.type()))
            }
        }
        // a nested object used as a value: `Event.Close` -> the object's `INSTANCE` singleton
        receiverType.subTypes().firstOrNull { it.simpleName() == name }?.let { nested ->
            nested.fields().firstOrNull { it.name() == "INSTANCE" }?.let { return staticFieldRef(it, nested) }
        }
        return null
    }

    /**
     * A class NAME used as a value: Kotlin means its companion object (`ClassId.fromString(…)` is
     * `ClassId.Companion.fromString(…)`, 18× on detekt, a library class) or, for an `object`, the object itself --
     * the static `Companion` / `INSTANCE` field its class file declares. K2 resolves such a name to the companion
     * symbol directly; the class's own symbol is handled too. Null for a class with no companion.
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtNameReferenceExpression)
    private fun KaSession.classAsValue(expression: KtNameReferenceExpression): Expression? {
        val symbol = expression.resolveSymbol() as? KaNamedClassSymbol ?: return null
        // the companion's holder: its outer class, reached through K2 -- a LIBRARY companion is loaded as a type of
        // its own, with no enclosing type to walk up to
        val (holderSymbol, fieldName) = when (symbol.classKind) {
            KaClassKind.COMPANION_OBJECT -> (symbol.classId?.outerClassId?.let { findClass(it) } as? KaNamedClassSymbol
                ?: return null) to symbol.name.asString()
            KaClassKind.OBJECT -> symbol to "INSTANCE"
            else -> symbol to (symbol.companionObject?.name?.asString() ?: return null)
        }
        val holder = classTypeInfo(holderSymbol)?.let { members(it) } ?: return null
        return holder.fields().firstOrNull { it.name() == fieldName && it.isStatic }?.let { staticFieldRef(it, holder) }
    }

    /** The singleton-instance handle for an object/companion type: `Object.INSTANCE`, or `Outer.Companion`. */
    private fun singletonHandle(type: TypeInfo, symbol: KaNamedClassSymbol): Expression? = when (symbol.classKind) {
        KaClassKind.COMPANION_OBJECT -> type.compilationUnitOrEnclosingType().let { if (it.isRight) it.right else null }
            ?.let { enclosing -> enclosing.fields().firstOrNull { it.name() == type.simpleName() && it.isStatic }
                ?.let { staticFieldRef(it, enclosing) } }
        KaClassKind.OBJECT -> type.fields().firstOrNull { it.name() == "INSTANCE" && it.isStatic }
            ?.let { staticFieldRef(it, type) }
        else -> null
    }

    /** A static-field access `Holder.field`, with the holder type as the (type-expression) scope. */
    private fun staticFieldRef(field: FieldInfo, holder: TypeInfo): Expression =
        variableExpression(runtime.newFieldReference(field,
            runtime.newTypeExpression(holder.asParameterizedType(), runtime.diamondNo()), field.type()))

    /**
     * `Foo(args)` -> a CST [ConstructorCall]: the constructed type's constructor matching the argument count, or
     * [defaults], the `$default` synthetic constructor, when the call omits an argument.
     */
    private fun KaSession.convertConstructorCall(call: KtCallExpression, arguments: List<Expression>, method: MethodInfo,
                                                 defaults: MethodInfo?): Expression {
        val type = call.expressionType?.let { mapType(it, method.typeInfo()) }
            ?: return placeholder("k2-ctor-type", call)
        val constructor = defaults
            ?: type.typeInfo()?.let { members(it) }?.constructors()?.firstOrNull { !it.isSynthetic && it.parameters().size == arguments.size }
            ?: return placeholder("k2-ctor-unresolved:${type.typeInfo()?.simpleName()}", call)
        return runtime.newConstructorCallBuilder()
            .setConstructor(constructor)
            .setConcreteReturnType(type)
            .setParameterExpressions(arguments)
            .setDiamond(runtime.diamondNo())
            .setTypeArguments(listOf())
            .setSource(runtime.noSource())
            .build()
    }

    /** `x as T` / `x as? T` -> a CST [io.codelaser.maddi.cst.api.expression.Cast] to T. */
    private fun KaSession.convertCastOrSafeCast(expression: KtBinaryExpressionWithTypeRHS, method: MethodInfo,
                                                locals: Map<String, Variable>): Expression {
        val value = convertExpression(expression.left, method, locals)
        val type = expression.right?.type?.let { mapType(it, method.typeInfo()) }
            ?: return placeholder("k2-cast", expression)
        return runtime.newCast(value, type)
    }

    /** `x is T` / `x !is T` -> a CST [io.codelaser.maddi.cst.api.expression.InstanceOf] (`!is` negated by a logical-not). */
    private fun KaSession.convertIsExpression(expression: KtIsExpression, method: MethodInfo,
                                              locals: Map<String, Variable>): Expression {
        val value = convertExpression(expression.leftHandSide, method, locals)
        val testType = expression.typeReference?.type?.let { mapType(it, method.typeInfo()) }
            ?: return placeholder("k2-is", expression)
        val instanceOf = runtime.newInstanceOfBuilder().setExpression(value).setTestType(testType)
            .setSource(runtime.noSource()).build()
        return if (expression.isNegated) runtime.newUnaryOperator(listOf(), runtime.noSource(),
            runtime.logicalNotOperatorBool(), instanceOf, runtime.precedenceUnary()) else instanceOf
    }

    /** `a[i]` -> `a.get(i)` method call (when `get` resolves on the receiver type). */
    private fun KaSession.convertArrayAccess(expression: KtArrayAccessExpression, method: MethodInfo,
                                             locals: Map<String, Variable>): Expression {
        val array = expression.arrayExpression?.let { convertExpression(it, method, locals) }
            ?: return placeholder("k2-index", expression)
        val indices = expression.indexExpressions.map { convertExpression(it, method, locals) }
        val arrayType = expression.arrayExpression?.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        // Kotlin's indexed get is the `get` operator on most types (List/array/Map/custom), but on a String it
        // is an intrinsic that maps to the JVM `charAt(int)` -- java.lang.String has no `get`. Fall back to it so
        // `s[i]` resolves (and its receiver read is tracked) rather than collapsing to a placeholder.
        val get = arrayType?.let { resolveCallee(it, "get", indices) ?: resolveCallee(it, "charAt", indices) }
            ?: return placeholder("k2-index-get-unresolved", expression)
        // use-site element type (List<Int>[i] -> Int), falling back to the declared (erased) return type
        val returnType = expression.expressionType?.let { mapType(it, method.typeInfo()) } ?: get.returnType()
        // marked INDEX_ACCESS at the `[` so the engine knows this get() was written as indexing
        return runtime.newMethodCallBuilder().setObject(array).setObjectIsImplicit(false).setMethodInfo(get)
            .setParameterExpressions(indices).setConcreteReturnType(returnType).setTypeArguments(listOf())
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.INDEX_ACCESS, expression.leftBracket)))
            .build()
    }

    /** `a[i] = v` -> `a.set(i, v)` method call (when `set` resolves), marked INDEX_ACCESS at the `[`. */
    private fun KaSession.convertIndexedSet(arrayAccess: KtArrayAccessExpression, value: Expression,
                                            method: MethodInfo, locals: Map<String, Variable>): Expression {
        val array = arrayAccess.arrayExpression?.let { convertExpression(it, method, locals) }
            ?: return placeholder("k2-indexed-set", arrayAccess)
        val arguments = arrayAccess.indexExpressions.map { convertExpression(it, method, locals) } + value
        val arrayType = arrayAccess.arrayExpression?.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        // `set` on List/arrays is a member; on a Map, Kotlin's `map[k]=v` set-operator is a stdlib extension
        // that delegates to `put`, so fall back to put (same key,value arguments)
        val set = arrayType?.let { resolveCallee(it, "set", arguments) ?: resolveCallee(it, "put", arguments) }
            ?: return placeholder("k2-indexed-set-unresolved", arrayAccess)
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
                                        locals: Map<String, Variable>, samType: ParameterizedType? = null): Expression {
        val enclosingType = method.typeInfo()
        val anonymousType = runtime.newAnonymousType(enclosingType, enclosingType.builder().getAndIncrementAnonymousTypes())
        anonymousType.builder()
            .setAccess(runtime.accessPrivate())
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())

        val functionType = lambda.expressionType as? KaFunctionType
        // [samType] is the interface a SAM constructor names; otherwise the lambda's own Kotlin function type
        val functionalType = samType ?: lambda.expressionType?.let { mapType(it, enclosingType) }
            ?: runtime.objectParameterizedType()
        val samName = samType?.typeInfo()
            ?.let { t -> runCatching { t.methods().singleOrNull { m -> m.isAbstract }?.name() }.getOrNull() }
            ?: "invoke"
        val sam = runtime.newMethod(anonymousType, samName, runtime.methodTypeMethod())
        val samBuilder = sam.builder()
        val outputVariants = mutableListOf<Lambda.OutputVariant>()

        // ⛔ A RECEIVER LAMBDA'S RECEIVER IS ITS FIRST PARAMETER, as kotlinc compiles it: `T.() -> R` IS a
        // `Function1<T, R>`, and while the parameter was missing the SAM contradicted the interface it claimed to
        // implement (arity 0 against Function1), and the body had nothing to resolve `append` in
        // `sb.apply { append("x") }` against -- a placeholder, swallowing whatever that call's arguments declared.
        functionType?.receiverType?.let {
            samBuilder.addParameter("\$receiver", mapType(it, enclosingType))
            outputVariants.add(runtime.lambdaOutputVariantEmpty())
        }
        val parameters = lambda.valueParameters
        if (parameters.isNotEmpty()) {
            parameters.forEachIndexed { i, p ->
                val type = (p.symbol as? KaVariableSymbol)?.let { mapType(it.returnType, enclosingType) }
                    ?: functionType?.parameterTypes?.getOrNull(i)?.let { mapType(it, enclosingType) }
                    ?: runtime.objectParameterizedType()
                // a destructured parameter `(a, b) ->` is ONE parameter on the JVM; its entries become locals below
                samBuilder.addParameter(p.name ?: if (p.destructuringDeclaration != null) "\$dstr$i" else "p$i", type)
                outputVariants.add(runtime.lambdaOutputVariantEmpty())
            }
        } else if (functionType != null && functionType.parameterTypes.size == 1) {
            samBuilder.addParameter("it", mapType(functionType.parameterTypes[0], enclosingType)) // implicit `it`
            // ⛔ ONE PER PARAMETER, OR LambdaImpl.print READS PAST THE END. Unreachable while `x.let { … }` was a
            // placeholder: nothing built a Lambda for a call that did not resolve, so nothing ever printed one.
            outputVariants.add(runtime.lambdaOutputVariantEmpty())
        }
        val returnType = functionType?.returnType?.let { mapType(it, enclosingType) } ?: runtime.objectParameterizedType()
        samBuilder.setReturnType(returnType).setAccess(runtime.accessPublic()).setSynthetic(true).commitParameters()

        // body: the lambda's block; its last expression becomes the (implicit) return value.
        // Converted in the *enclosing* method's context (so captured outer params/fields resolve), with
        // the lambda's own parameters added to the in-scope variables.
        val block = runtime.newBlockBuilder()
        val bodyScope: MutableMap<String, Variable> = locals.toMutableMap()
        sam.parameters().forEach { bodyScope[it.name()] = it }
        // `$receiver` is the INNERMOST receiver only: an outer lambda's stays reachable under its own key, which is
        // how implicitReceiverValue finds the receiver K2 names inside `with(session) { … }` nested in another one
        if (functionType?.receiverType != null) bodyScope[receiverKey(lambda.functionLiteral)] = sam.parameters()[0]
        val statements = lambda.bodyExpression?.statements.orEmpty()
        val voidReturn = returnType == runtime.voidParameterizedType()
        // `{ (key, value) -> … }`: the body starts by reading each entry from the parameter, as kotlinc compiles it
        val prologue = parameters.mapIndexedNotNull { i, p ->
            val declaration = p.destructuringDeclaration ?: return@mapIndexedNotNull null
            val parameter = sam.parameters()[i + (if (functionType?.receiverType != null) 1 else 0)]
            val variables = destructure(declaration.entries, { variableExpression(parameter) }, declaration, method, bodyScope)
            variables.takeIf { it.isNotEmpty() }?.let { vs ->
                val builder = runtime.newLocalVariableCreationBuilder().setLocalVariable(vs.first())
                vs.drop(1).forEach { builder.addOtherLocalVariable(it) }
                builder.setSource(runtime.noSource()).build()
            }
        }
        val total = prologue.size + statements.size
        prologue.forEachIndexed { k, st -> block.addStatement(indexed(st, pad(k, total))) }
        statements.forEachIndexed { i, stmt ->
            val index = pad(i + prologue.size, total)
            val isResult = i == statements.lastIndex && !voidReturn && isLambdaResultExpression(stmt)
            // a lambda body is a statement list like any other: `val map = try { … } catch { … }` inside one
            // is lowered here too. ⛔ never the result expression — that is the lambda's value, not a statement.
            val lowered = if (isResult) null else loweredStatements(stmt, method, bodyScope, index)
            when {
                lowered != null -> lowered.forEach { block.addStatement(it) }
                isResult -> {
                    val (hoisted, tailIndex) = hoistBefore(stmt, method, bodyScope, index)
                    hoisted.forEach { block.addStatement(it) }
                    block.addStatement(
                        indexed(runtime.newReturnStatement(convertExpression(stmt, method, bodyScope)), tailIndex))
                    hoistedReads.clear()
                }
                // ⭐ a lambda body is where detekt's worst chains live: the 32× site is a chained value elvis
                // inside an `analyze(this) { … }` block, and wiring only the method-body loop missed it
                else -> convertHoisting(stmt, method, bodyScope, index).forEach { block.addStatement(it) }
            }
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
            ?: MAPPED_PROPERTIES[propertyName]?.let { resolveCallee(type, it, listOf()) } // map.keys -> keySet()
    }

    /**
     * A STATIC Kotlin property, `C.x` or a bare `x` inside `C`: a static getter on `C` in the class file. In practice
     * an enum's `entries` (`E.getEntries()`); K2 models it as a static property, where a class-file type has only
     * the method. It was read on the enum's companion, or on the class name typed `Unit`.
     */
    private fun KaSession.staticPropertyAccess(reference: KtNameReferenceExpression): Expression? {
        val property = reference.mainReference.resolveToSymbol() as? KaPropertySymbol ?: return null
        if (!property.isStatic) return null
        val owner = property.callableId?.classId?.let { findClass(it) as? KaNamedClassSymbol }
            ?.let { classTypeInfo(it) }?.let { members(it) } ?: return null
        val getter = resolveAccessor(owner, reference.getReferencedName())?.takeIf { it.isStatic } ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(getter).setParameterExpressions(listOf())
            .setConcreteReturnType(getter.returnType()).setTypeArguments(listOf())
            .setSource(runtime.noSource()).build()
    }

    /**
     * `recv.extProp` where `extProp` is an extension property: `Facade.getExtProp(recv)`. Source and library
     * alike — a source facade is already built with the getter on it (`PKt.getDoubled/1`), and a library
     * facade grows one in `loadLibraryFacadeForProperty`.
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtNameReferenceExpression)
    private fun KaSession.extensionPropertyAccess(selector: KtNameReferenceExpression, name: String,
                                                  receiver: Expression, method: MethodInfo,
                                                  locals: Map<String, Variable>): Expression? {
        val property = selector.resolveSymbol() as? KaPropertySymbol ?: return null
        if (property.receiverParameter == null) return null
        val contexts = contextArguments(selector.resolveToCall()?.successfulVariableAccessCall()
            ?.partiallyAppliedSymbol?.contextArguments, method, locals) ?: return null
        val getterArgs = contexts + listOf(receiver)
        val facade = (property.psi as? KtProperty)?.containingKtFile?.let { facadeOf(it) }
            ?: with(typeMapper) { loadLibraryFacadeForProperty(property) } ?: return null
        val getterName = "get" + name.replaceFirstChar { it.uppercaseChar() }
        val callee = resolveCallee(facade, getterName, getterArgs)
            ?: resolveCallee(facade, name, getterArgs) // a @JvmName'd getter keeps the property's name
            ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(callee)
            .setParameterExpressions(getterArgs)
            .setConcreteReturnType(callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /**
     * `recv.extProp` where `extProp` is a MEMBER extension property -- declared inside a type, so it has a dispatch
     * receiver as well as an extension receiver: detekt's `expression.expressionType` inside `analyze(…) { }` is
     * `$receiver.getExpressionType(expression)` on the JVM, an instance getter of the session with the extension
     * receiver as its argument.
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaSession.memberExtensionPropertyAccess(selector: KtNameReferenceExpression, name: String,
                                                        receiver: Expression, method: MethodInfo,
                                                        locals: Map<String, Variable>): Expression? {
        val access = selector.resolveToCall()?.successfulVariableAccessCall() ?: return null
        if (access.partiallyAppliedSymbol.symbol.receiverParameter == null) return null
        val dispatch = access.partiallyAppliedSymbol.dispatchReceiver
        val obj = implicitReceiverValue(dispatch, method, locals) ?: return null
        val type = receiverLookupType(dispatch, obj, method) ?: return null
        val getterName = "get" + name.replaceFirstChar { it.uppercaseChar() }
        val callee = resolveCallee(type, getterName, listOf(receiver))
            ?: resolveCallee(type, name, listOf(receiver)) // an `is…` property keeps its name
            ?: return null
        val returnType = selector.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        return runtime.newMethodCallBuilder()
            .setObject(obj).setObjectIsImplicit(true).setMethodInfo(callee)
            .setParameterExpressions(listOf(receiver)).setConcreteReturnType(returnType)
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /**
     * A top-level property of ANOTHER file or of a library, by its bare name (`NL`, ktlint's `INDENT_SIZE_PROPERTY`):
     * read through its facade, as Java reads it -- a `const val` or `@JvmField` as the static field, anything else
     * through the static getter (`CoreKt.getNL()`). Its own file's facade holds the field, and [resolveReference] has
     * already found it there.
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaSession.topLevelPropertyAccess(expression: KtNameReferenceExpression, method: MethodInfo): Expression? {
        val access = expression.resolveToCall()?.successfulVariableAccessCall() ?: return null
        val property = access.partiallyAppliedSymbol.symbol as? KaPropertySymbol ?: return null
        if (property.receiverParameter != null || access.partiallyAppliedSymbol.dispatchReceiver != null) return null
        if (property.callableId?.classId != null) return null // a member, not top-level
        val facade = (property.psi as? KtProperty)?.containingKtFile?.let { facadeOf(it) }
            ?: with(typeMapper) { loadLibraryFacadeForProperty(property) } ?: return null
        val name = expression.getReferencedName()
        val asField = (property as? KaKotlinPropertySymbol)?.let { it.isConst || it.backingFieldSymbol?.annotations
            ?.any { a -> a.classId?.asFqNameString() == "kotlin.jvm.JvmField" } == true } == true
        val field = facade.fields().firstOrNull { it.name() == name && it.isStatic }
        if (asField && field != null) return staticFieldRef(field, facade)
        val getterName = if (name.startsWith("is") && name.getOrNull(2)?.isUpperCase() == true) name
                         else "get" + name.replaceFirstChar { it.uppercaseChar() }
        val getter = members(facade).methods().firstOrNull { it.isStatic && it.name() == getterName && it.parameters().isEmpty() }
            ?: return field?.let { staticFieldRef(it, facade) }
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(getter).setParameterExpressions(listOf())
            .setConcreteReturnType(getter.returnType()).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** The file facade TypeInfo for a source file, as `extensionFacade` computes it for a function. */
    private fun facadeOf(ktFile: KtFile): TypeInfo? {
        val pkg = ktFile.packageFqName
        val fqn = (if (pkg.isRoot) "" else pkg.asString() + ".") + facadeSimpleName(ktFile)
        return infoByFqn.getType(fqn, sourceSet)
    }

    /** [type], with its members if it is a class-file shell the Java front end has not completed yet. */
    private fun members(type: TypeInfo): TypeInfo = typeMapper.withMembers(type)

    /** A no-arg getter call `receiver.getter()` (the desugaring of a property idiom). */
    private fun accessorCall(receiver: Expression, getter: MethodInfo): Expression =
        runtime.newMethodCallBuilder().setObject(receiver).setObjectIsImplicit(false).setMethodInfo(getter)
            .setParameterExpressions(listOf()).setConcreteReturnType(getter.returnType()).setTypeArguments(listOf())
            .setSource(runtime.noSource()).build()

    /**
     * A member of a Kotlin primitive, as the Java a human writes for it -- and as kotlinc compiles it, measured with
     * javap on 2.4.0, except where noted: `i.toString()` is `String.valueOf(i)`, `b.not()` is `!b`, `i.toLong()` a
     * primitive conversion (`i2l`), `i.hashCode()` `Integer.hashCode(i)`, `i.compareTo(j)` `Integer.compare(i, j)`
     * (kotlinc: `Intrinsics.compare`), `i.equals(j)` `i == j` (kotlinc boxes both), and `i.plus(j)` `i + j`.
     * Overloads are chosen by the EXACT parameter type, so that no widening can pick `valueOf(char[])`; a shape not
     * listed here, or a mixed-type one (`i.compareTo(l)`), returns null and keeps its placeholder.
     */
    private fun KaSession.primitiveMember(name: String, receiver: Expression, arguments: List<Expression>,
                                          call: KtCallExpression, method: MethodInfo): Expression? {
        val type = receiver.parameterizedType()
        if (!type.isPrimitiveExcludingVoid || type.arrays() > 0) return null
        val primitive = type.typeInfo() ?: return null
        val resultType = call.expressionType?.let { mapType(it, method.typeInfo()) }
        val argument = arguments.singleOrNull()
        val sameType = argument != null && argument.parameterizedType() == type
        fun static(owner: TypeInfo, methodName: String, args: List<Expression>): Expression? {
            val callee = members(owner).methods().firstOrNull { m ->
                m.isStatic && m.name() == methodName && m.parameters().size == args.size &&
                    m.parameters().all { it.parameterizedType() == type }
            } ?: return null
            return runtime.newMethodCallBuilder()
                .setObject(runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo()))
                .setObjectIsImplicit(false).setMethodInfo(callee).setParameterExpressions(args)
                .setConcreteReturnType(callee.returnType()).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
        }
        fun binary(operator: MethodInfo, precedence: io.codelaser.maddi.cst.api.expression.Precedence): Expression =
            runtime.newBinaryOperatorBuilder().setLhs(receiver).setRhs(argument).setOperator(operator)
                .setPrecedence(precedence).setParameterizedType(resultType ?: operator.returnType())
                .setSource(runtime.noSource()).build()
        return when {
            arguments.isEmpty() && name == "toString" -> static(runtime.stringTypeInfo(), "valueOf", listOf(receiver))
            arguments.isEmpty() && name == "not" && type.isBooleanOrBoxedBoolean -> logicalNot(receiver)
            arguments.isEmpty() && name == "hashCode" -> static(runtime.boxed(primitive), "hashCode", listOf(receiver))
            arguments.isEmpty() && name in PRIMITIVE_CONVERSIONS && resultType?.isPrimitiveExcludingVoid == true ->
                if (resultType == type) receiver else runtime.newCast(receiver, resultType)
            sameType && name == "compareTo" -> static(runtime.boxed(primitive), "compare", listOf(receiver, argument!!))
            sameType && name == "equals" -> binary(runtime.equalsOperatorInt(), runtime.precedenceEquality())
            argument != null && receiver.isNumeric && argument.isNumeric -> when (name) {
                "plus" -> binary(runtime.plusOperatorInt(), runtime.precedenceAdditive())
                "minus" -> binary(runtime.minusOperatorInt(), runtime.precedenceAdditive())
                "times" -> binary(runtime.multiplyOperatorInt(), runtime.precedenceMultiplicative())
                "div" -> binary(runtime.divideOperatorInt(), runtime.precedenceMultiplicative())
                "rem" -> binary(runtime.remainderOperatorInt(), runtime.precedenceMultiplicative())
                else -> null
            }
            else -> null
        }
    }

    /**
     * `arrayOf(a, b)` / `intArrayOf(1, 2)` as `new T[]{a, b}`: an array-creation constructor with one empty dimension
     * and an initializer, the shape the Java front end gives `new String[]{"a", "b"}`. Only for `kotlin.arrayOf` and
     * the primitive `…ArrayOf` builders, and not with a spread argument (`arrayOf(*xs)` copies an array).
     */
    private fun KaSession.arrayLiteral(call: KtCallExpression, calleeSymbol: KaNamedFunctionSymbol?,
                                       arguments: List<Expression>, method: MethodInfo): Expression? {
        val id = calleeSymbol?.callableId ?: return null
        if (id.packageName.asString() != "kotlin" || id.className != null) return null
        if (id.callableName.asString() !in ARRAY_LITERALS || call.valueArguments.any { it.getSpreadElement() != null }) return null
        val arrayType = call.expressionType?.let { mapType(it, method.typeInfo()) }?.takeIf { it.arrays() > 0 } ?: return null
        val initializer = runtime.newArrayInitializerBuilder().setSource(runtime.noSource())
            .setCommonType(arrayType.copyWithArrays(arrayType.arrays() - 1)).setExpressions(arguments).build()
        return runtime.newConstructorCallBuilder()
            .setSource(runtime.noSource())
            .setConstructor(runtime.newArrayCreationConstructor(arrayType))
            .setConcreteReturnType(arrayType)
            .setDiamond(runtime.diamondNo())
            .setParameterExpressions(listOf(runtime.newEmptyExpression()))
            .setArrayInitializer(initializer)
            .build()
    }

    /**
     * The Java class literal `X.class` for a Kotlin `X::class` -- null for a reified type parameter (`T::class` in an
     * inline function), which has no Java spelling: kotlinc substitutes the argument at each inlined call site.
     */
    private fun KaSession.javaClassLiteral(expression: KtClassLiteralExpression, method: MethodInfo): Expression? {
        val kClass = expression.expressionType as? KaClassType ?: return null
        val classified = kClass.typeArguments.singleOrNull()?.type ?: return null
        if (classified is KaTypeParameterType) return null
        val type = mapType(classified, method.typeInfo()).takeIf { it.typeInfo() != null } ?: return null
        return runtime.newClassExpressionBuilder(type).setSource(runtime.noSource()).build()
    }

    /**
     * A Kotlin `X::class` (a `KClass`): `Reflection.getOrCreateKotlinClass(X.class)`, the stdlib call kotlinc emits.
     * Null when the stdlib's `kotlin.jvm.internal.Reflection` is not on the class path.
     */
    private fun KaSession.kotlinClassLiteral(expression: KtClassLiteralExpression, method: MethodInfo): Expression? {
        val classLiteral = javaClassLiteral(expression, method) ?: return null
        val reflection = (findClass(REFLECTION) as? KaNamedClassSymbol)?.let { classTypeInfo(it) }?.let { members(it) }
            ?: return null
        val callee = reflection.methods().firstOrNull {
            it.isStatic && it.name() == "getOrCreateKotlinClass" && it.parameters().size == 1
        } ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(reflection.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(callee).setParameterExpressions(listOf(classLiteral))
            .setConcreteReturnType(expression.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** `!e` as a boolean UnaryOperator. */
    private fun logicalNot(e: Expression): Expression =
        runtime.newUnaryOperator(listOf(), runtime.noSource(), runtime.logicalNotOperatorBool(), e, runtime.precedenceUnary())

    private fun resolveCallee(type: TypeInfo, name: String, arguments: List<Expression>,
                             returnTypeFqn: String? = null): MethodInfo? {
        val all = mutableListOf<MethodInfo>()
        collectMethods(type, name, arguments.size, mutableSetOf(), all)
        if (all.size <= 1) return all.firstOrNull()
        // an overload kotlinc adds (KotlinScan.overloadMethods) is Java's to call: a Kotlin call binds to a declaration
        // of the same type. (Not to an inherited one: a data class's synthesized `equals` is the callee, not Object's.)
        all.removeIf { m -> m.isSynthetic && all.any { !it.isSynthetic && it.typeInfo() === m.typeInfo() } }
        if (all.size == 1) return all.first()
        // overloads that share erased params but differ by return type (Kotlin inline numeric specializations,
        // e.g. maxOf((T)->Double):Double vs :Float vs :R): pick the one whose erased return type matches the
        // resolved call. Then disambiguate any remainder by argument type, as before.
        val candidates = returnTypeFqn
            ?.let { rt -> all.filter { it.returnType().erasedForFQN().fullyQualifiedName() == rt }.ifEmpty { all } }
            ?: all
        if (candidates.size == 1) return candidates.first()
        val argTypes = arguments.map { it.parameterizedType() }
        // indexed by ARGUMENT, not by parameter: a varargs candidate has fewer parameters than the call has
        // arguments, and `typeOfParameterHandleVarargs` is what answers "what type does argument i go to".
        fun matches(predicate: (ParameterizedType, ParameterizedType) -> Boolean) = candidates.firstOrNull { c ->
            argTypes.withIndex().all { (i, a) -> predicate(c.typeOfParameterHandleVarargs(i), a) }
        }
        return matches { p, a -> p == a }
            ?: matches { p, a -> p.typeInfo() != null && p.typeInfo() == a.typeInfo() }
            // ⛔ RULE OUT THE IMPOSSIBLE, before the guess. Measured: `s.replace("a", "b")` bound to
            // `java.lang.String.replace(char, char)` — two String literals against two chars — because the
            // first two tiers want an exact type and the fallback took whichever overload came first.
            // ⚠ Assignability is NOT the test here, deliberately: `isAssignableFrom(CharSequence, String)`
            // is FALSE in this front end, because the predefined `java.lang.String` is bootstrapped without
            // its hierarchy — so an assignability tier rejects the right overload along with the wrong one
            // (measured: it left `replace(char,char)` in place). What can be proven without a hierarchy is
            // the other direction: a reference argument can never reach a primitive parameter unless it is
            // that primitive's box.
            ?: matches { p, a -> !cannotBePassed(p, a) }
            // ⚠ Still a guess, and now a COUNTED one. A wrong callee is worse than a placeholder: no walk
            // over the CST can find it afterwards, because a resolved call looks the same either way.
            ?: candidates.first().also { ++ambiguousBindings }
    }

    /**
     * Whether argument type [a] provably cannot reach parameter type [p]. Conservative on purpose: it says
     * true only for the case no type hierarchy is needed to decide — a reference where a primitive is
     * required, its own box excepted (Kotlin unboxes `Int` into `int`, and never `String` into `char`).
     * Everything else is "cannot prove", which leaves the later tiers exactly as they were.
     */
    private fun cannotBePassed(p: ParameterizedType, a: ParameterizedType): Boolean {
        if (!p.isPrimitiveExcludingVoid || p.arrays() > 0) return false
        if (a.isPrimitiveExcludingVoid && a.arrays() == 0) return false // primitive->primitive: widening, not here
        val argumentType = a.typeInfo() ?: return false // unknown argument type proves nothing
        val boxOfParameter = p.typeInfo()?.let { runtime.boxed(it) } ?: return false
        return argumentType != boxOfParameter
    }

    /** The erased-FQN of a call's resolved result type, used to disambiguate return-type overloads. */
    private fun KaSession.callReturnFqn(call: KtCallExpression, method: MethodInfo): String? =
        call.expressionType?.let { mapType(it, method.typeInfo(), method).erasedForFQN().fullyQualifiedName() }

    /**
     * The [TypeInfo] on which to resolve a member of [expr]'s value. Normally that is the expression's
     * own type; for a type-parameter type `T : Comparable<T>` we fall back to the first upper bound that
     * has a [TypeInfo] (so `a > b`, `a in c`, `a == b` on a bounded `T` resolve against the bound).
     */
    private fun KaSession.receiverTypeInfo(expr: KtExpression?, method: MethodInfo): TypeInfo? {
        val pt = expr?.expressionType?.let { mapType(it, method.typeInfo(), method) } ?: return null
        return pt.typeInfo()
            ?: pt.typeParameter()?.typeBounds()?.firstNotNullOfOrNull { it.typeInfo() }
    }

    /**
     * A call's arguments in the callee's parameter order. When the call omits one, [defaults] is the callee's
     * `$default` synthetic, the method the call binds to: it takes the omitted argument's zero value, a bit mask of
     * the omitted parameters (`$mask`), and for a constructor a `null` marker, and evaluates each omitted default in
     * the callee's scope, as kotlinc compiles it (see KotlinScan.defaultsMethod).
     */
    internal class Arguments(val expressions: List<Expression>, val defaults: MethodInfo?)

    /**
     * [call]'s arguments for [callee]: positional arguments fill the leading parameters, named arguments fill by
     * name, a trailing lambda the last parameter. Null when a parameter is filled neither by an argument nor by a
     * default, or when the declaration with the defaults has no `$default` in this project (a library function):
     * the caller falls back to the written arguments.
     */
    internal fun KaSession.callArguments(call: KtCallElement, callee: KaFunctionSymbol, method: MethodInfo,
                                         locals: Map<String, Variable>): Arguments? {
        val lambda = call.valueArguments.lastOrNull()?.takeIf { it is KtLambdaArgument }
        val positional = call.valueArguments.filter { it.getArgumentName() == null && it !== lambda }
        val byName = call.valueArguments.mapNotNull { va ->
            va.getArgumentName()?.asName?.asString()?.let { it to va }
        }.toMap()
        val parameters = callee.valueParameters
        val declaring = declaringDefaults(callee)
        val masks = IntArray((parameters.size + 31) / 32)
        val expressions = parameters.mapIndexed { i, p ->
            val argument = when {
                i < positional.size -> positional[i]
                p.name.asString() in byName -> byName[p.name.asString()]
                i == parameters.lastIndex && lambda != null -> lambda
                else -> null
            }
            if (argument != null) {
                argument.getArgumentExpression()?.let { convertExpression(it, method, locals) } ?: return null
            } else {
                // ⛔ The test used to be "the DECLARATION's PSI has a default", which is null for every
                // library function — so `x.joinToString(",")` (7 JVM parameters, 1 written) found no
                // 2-parameter method and became a placeholder. K2 knows a library parameter is optional
                // without any PSI, and that is the question being asked here.
                if (!p.hasDefaultValue
                    && (declaring?.valueParameters?.get(i)?.psi as? KtParameter)?.defaultValue == null) return null
                masks[i / 32] = masks[i / 32] or (1 shl (i % 32))
                runtime.nullValue(mapType(p.returnType, method.typeInfo(), method))
            }
        }
        if (masks.all { it == 0 }) return Arguments(expressions, null)
        val defaults = defaultsOf(declaring?.psi)
            // ⭐ A LIBRARY callee has no `$default` in this parse, and synthesizing one would be the wrong
            // trade: the AAPI's annotations are keyed to the REAL signature (`joinToString(Iterable,
            // CharSequence, …)`), so binding that method with the omitted parameters filled by their zero
            // value keeps every contract reachable. The mask is dropped with it — it is an argument of
            // `$default`, and there is no `$default` here.
            ?: return Arguments(expressions, null)
        val marker = if (callee is KaConstructorSymbol) listOf(runtime.nullConstant()) else listOf()
        return Arguments(expressions + masks.map { runtime.newInt(it) } + marker, defaults)
    }

    /**
     * The declaration whose default values a call to [symbol] uses: its own, or those of the member it overrides (an
     * override cannot declare defaults of its own). Null for a library declaration: its defaults are not source.
     */
    private fun KaSession.declaringDefaults(symbol: KaFunctionSymbol): KaFunctionSymbol? =
        (sequenceOf(symbol) + symbol.allOverriddenSymbols.filterIsInstance<KaFunctionSymbol>())
            .firstOrNull { s -> s.valueParameters.any { (it.psi as? KtParameter)?.defaultValue != null } }

    /** Collect every method named [name] with [arity] parameters on [type] and its supertypes. */
    /**
     * `::f`, `this::f`, `Type::f`, `::Type` — a Kotlin callable reference is Java's method reference, and the CST
     * has one. Two fields carry the meaning, and both come straight from K2: `methodInfo()`, and whether
     * `scope()` yields a links primary — which IS the bound/unbound distinction the link engine reads
     * (`ExpressionVisitor.methodReference`: a TypeExpression has no primary and its receiver is treated as
     * internal; a value expression has one and its modifications reach the caller).
     *
     * ⚠ `expression.expressionType` is `kotlin.reflect.KFunction1`, NOT the functional interface the reference
     * is being coerced to at the use site. It is recorded as-is rather than guessed at: the engine types the
     * synthetic functional-interface variable with it and reads nothing else from it.
     *
     * A PROPERTY reference (`Q::i`, `String::length`) is its getter — see [propertyReference].
     */
    private fun KaSession.convertCallableReference(expression: KtCallableReferenceExpression, method: MethodInfo,
                                                   locals: Map<String, Variable>): Expression {
        val symbol = expression.callableReference.mainReference.resolveToSymbol()
        val functionalType = expression.expressionType?.let { mapType(it, method.typeInfo()) }
            ?: runtime.objectParameterizedType()

        // `::Foo` — the scope is the type, the callee its constructor, and the engine has a dedicated arm for it
        // (a constructor reference's SAM returns the new object).
        if (symbol is KaConstructorSymbol) {
            val owner = (symbol.containingDeclaration as? KaNamedClassSymbol)?.let { classTypeInfo(it) }
                ?: return placeholder("k2-callable-ref-constructor-owner", expression)
            val ctor = members(owner).constructors().firstOrNull { it.parameters().size == symbol.valueParameters.size }
                ?: return placeholder("k2-callable-ref-constructor", expression)
            return methodReference(runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo()),
                ctor, functionalType, expression)
        }
        if (symbol is KaPropertySymbol) return propertyReference(symbol, expression, functionalType, method, locals)
        val fn = symbol as? KaNamedFunctionSymbol
            ?: return placeholder("k2-callable-ref-unsupported", expression)
        // a local function (`fun f() { fun g() {}; ::g }`) is not modelled by this front end at all
        // a local function is a local variable holding its function object (convertLocalFunction): `::g` is that value
        if ((fn.psi as? KtNamedFunction)?.isLocal == true) {
            return locals[fn.name.asString()]?.let { variableExpression(it) }
                ?: placeholder("k2-callable-ref-local-function", expression)
        }
        if (fn.receiverParameter != null) return extensionReference(fn, expression, functionalType, method, locals)

        val receiver = expression.receiverExpression
        // (scope expression, the type to resolve the callee on)
        val scopeAndOwner: Pair<Expression, TypeInfo>? = when {
            // `::f` with no receiver: a top-level function lives on the file facade (a type), a member of the
            // enclosing class is implicitly `this` (a value). The difference is exactly bound vs unbound.
            receiver == null -> {
                if ((fn.psi as? KtNamedFunction)?.containingClassOrObject == null) {
                    val facade = extensionFacade(fn) ?: with(typeMapper) { loadLibraryFacadeFor(fn) }
                    facade?.let { runtime.newTypeExpression(it.asParameterizedType(), runtime.diamondNo()) to it }
                } else method.typeInfo().let { self(method) to it }
            }
            receiver is KtThisExpression -> method.typeInfo().let { self(method) to it }
            else -> explicitReceiverScope(receiver, method, locals)
        }
        val (scope, owner) = scopeAndOwner ?: return placeholder("k2-callable-ref-scope", expression)
        val callee = resolveCalleeByArity(owner, fn.name.asString(), fn.valueParameters.size)
            ?: return placeholder("k2-callable-ref-unresolved:${fn.name.asString()}", expression)
        return methodReference(scope, callee, functionalType, expression)
    }

    /**
     * `String::toRegex`, `Q::ext` -- an EXTENSION function referenced through its receiver type is the static method
     * kotlinc compiles it to, on its file facade, with the receiver as parameter 0: `(String) -> Regex` is what a Java
     * author writes `StringsKt::toRegex`. So the scope is the facade TYPE (unbound) and the callee has one parameter
     * more than the Kotlin function declares. Found by the corpus: `String::toRegex` and detekt's own
     * `String::pathGlobToRegex` were 26 of detekt's 34 unresolved references.
     *
     * ⛔ A BOUND extension reference -- `s::ext`, or `::ext` inside a function whose implicit receiver supplies it --
     * binds the facade method's first argument, which no Java method reference can spell; it keeps a named
     * placeholder.
     */
    private fun KaSession.extensionReference(fn: KaNamedFunctionSymbol, expression: KtCallableReferenceExpression,
                                             functionalType: ParameterizedType, method: MethodInfo,
                                             locals: Map<String, Variable>): Expression {
        val receiver = expression.receiverExpression
        val unbound = receiver != null && explicitReceiverScope(receiver, method, locals)?.first is TypeExpression
        if (!unbound) return placeholder("k2-callable-ref-bound-extension", expression)
        val facade = extensionFacade(fn) ?: with(typeMapper) { loadLibraryFacadeFor(fn) }
            ?: return placeholder("k2-callable-ref-extension-facade", expression)
        val callee = resolveCalleeByArity(facade, fn.name.asString(), fn.valueParameters.size + 1)
            ?: return placeholder("k2-callable-ref-unresolved:${fn.name.asString()}", expression)
        return methodReference(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()),
            callee, functionalType, expression)
    }

    /**
     * The written receiver of a callable reference, as (scope, the type to resolve the callee on): a TYPE name
     * (`Q::f`, unbound — a [TypeExpression], which the engine reads as having no links-primary) or a VALUE
     * (`q::f`, bound — the value itself, whose modifications reach the caller).
     */
    private fun KaSession.explicitReceiverScope(receiver: KtExpression, method: MethodInfo,
                                                locals: Map<String, Variable>): Pair<Expression, TypeInfo>? {
        // the class a TYPE receiver names, however it is spelled: `Q`, `a.b.Q` (the class is the last selector)
        // or `ArrayList<String>` (a call-shaped node whose callee is the name)
        fun className(e: KtExpression?): KtNameReferenceExpression? = when (e) {
            is KtNameReferenceExpression -> e
            is KtDotQualifiedExpression -> className(e.selectorExpression)
            is KtCallExpression -> e.calleeExpression as? KtNameReferenceExpression
            else -> null
        }
        val asClass = className(receiver)?.mainReference?.resolveToSymbol() as? KaNamedClassSymbol
        return if (asClass != null) classTypeInfo(asClass)?.let {
            runtime.newTypeExpression(it.asParameterizedType(), runtime.diamondNo()) to it
        } else {
            val value = convertExpression(receiver, method, locals)
            value.parameterizedType().typeInfo()?.let { value to it }
        }
    }

    /**
     * `Q::i`, `q::i`, `::i`, `String::length`, `Q::ext`, `::top` — a property reference used as a function IS its
     * getter: `Q::i` is `(Q) -> Int`, which a Java author writes `Q::getI`. So it becomes a method reference to the
     * getter the front end already builds for the property (or, for a library property, the JVM accessor K2's
     * property stands for — `String::length` is `length()`), with the same bound/unbound scope rule as a function
     * reference. The getter is found exactly as a property ACCESS finds it (`resolveAccessor`), so `q::i` and
     * `{ q.i }` reach the same method.
     *
     * ⛔ Still a named placeholder: a property with no getter method (`private`, `const`: the front end reads those
     * as the field, and a method reference cannot name a field), a bound extension reference (`s::lastIndex`, which
     * has no Java spelling), and a top-level non-extension LIBRARY property (its facade is built from getters of
     * extension properties only).
     */
    private fun KaSession.propertyReference(property: KaPropertySymbol, expression: KtCallableReferenceExpression,
                                            functionalType: ParameterizedType, method: MethodInfo,
                                            locals: Map<String, Variable>): Expression {
        val name = property.name.asString()
        val getterName = "get" + name.replaceFirstChar { it.uppercaseChar() }
        val receiver = expression.receiverExpression
        val sourceFacade = (property.psi as? KtProperty)?.containingKtFile?.let { facadeOf(it) }

        // an extension property: a static getter on its facade, the receiver its first parameter (`Q::ext` is
        // `PKt::getExt`). Only the unbound form has a Java spelling.
        if (property.receiverParameter != null) {
            val facade = sourceFacade ?: with(typeMapper) { loadLibraryFacadeForProperty(property) }
                ?: return placeholder("k2-callable-ref-property-facade", expression)
            val typeReceiver = receiver != null && explicitReceiverScope(receiver, method, locals)?.first is TypeExpression
            if (!typeReceiver) return placeholder("k2-callable-ref-property-bound-extension", expression)
            val getter = resolveCalleeByArity(facade, getterName, 1) ?: resolveCalleeByArity(facade, name, 1)
                ?: return placeholder("k2-callable-ref-property-no-getter", expression)
            return methodReference(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()),
                getter, functionalType, expression)
        }

        val scopeAndOwner: Pair<Expression, TypeInfo>? = when {
            // `::p` with no receiver: a top-level property lives on the file facade, a member is implicitly `this`
            receiver == null -> if (property.callableId?.classId == null) {
                sourceFacade?.let { runtime.newTypeExpression(it.asParameterizedType(), runtime.diamondNo()) to it }
            } else method.typeInfo().let { self(method) to it }
            receiver is KtThisExpression -> method.typeInfo().let { self(method) to it }
            else -> explicitReceiverScope(receiver, method, locals)
        }
        val (scope, owner) = scopeAndOwner ?: return placeholder("k2-callable-ref-scope", expression)
        val getter = resolveAccessor(owner, name)
            ?: return placeholder("k2-callable-ref-property-no-getter", expression)
        return methodReference(scope, getter, functionalType, expression)
    }

    private fun methodReference(scope: Expression, callee: MethodInfo, functionalType: ParameterizedType,
                                expression: KtCallableReferenceExpression): Expression =
        runtime.newMethodReferenceBuilder()
            .setScope(scope)
            .setMethod(callee)
            .setConcreteFunctionalType(functionalType)
            .setConcreteParameterTypes(callee.parameters().map { it.parameterizedType() })
            .setConcreteReturnType(callee.returnType())
            .setSource(source(expression, "-"))
            .build()

    /** A class symbol's TypeInfo: from this compilation if it is ours, else loaded from the classpath. */
    private fun KaSession.classTypeInfo(symbol: KaNamedClassSymbol): TypeInfo? {
        val fqn = symbol.classId?.asFqNameString() ?: return null
        return infoByFqn.getType(fqn, sourceSet) ?: with(typeMapper) { loadLibraryClass(symbol) }
    }

    /**
     * [resolveCallee] disambiguates overloads by ARGUMENT expressions; a callable reference has none, only an
     * arity. Overloads that differ only in parameter types are therefore not distinguishable here.
     */
    private fun resolveCalleeByArity(type: TypeInfo, name: String, arity: Int): MethodInfo? {
        val all = mutableListOf<MethodInfo>()
        collectMethods(type, name, arity, mutableSetOf(), all)
        all.removeIf { m -> m.isSynthetic && all.any { !it.isSynthetic && it.typeInfo() === m.typeInfo() } }
        if (all.size > 1) ++ambiguousBindings
        return all.firstOrNull()
    }

    private fun collectMethods(type: TypeInfo, name: String, arity: Int, visited: MutableSet<TypeInfo>,
                               acc: MutableList<MethodInfo>) {
        if (!visited.add(type)) return
        // ⚠ A varargs callee is matched the way the Java front end represents one: the arguments stay
        // WRITTEN OUT, so a call carries more expressions than the method has parameters (hence
        // MethodInfo.typeOfParameterHandleVarargs). Without this, `listOf("a", "b")` — 2 written against 1
        // array parameter — found nothing and became a placeholder.
        members(type).methods().filterTo(acc) {
            it.name() == name
            && (it.parameters().size == arity || (it.isVarargs && arity >= it.parameters().size - 1))
        }
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
        with(memberConverter) { finishAnonMembers(anon, expression.objectDeclaration) }
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
        is KtBreakExpression, is KtContinueExpression, is KtThrowExpression -> false
        is KtAnnotatedExpression -> statement.baseExpression?.let { isLambdaResultExpression(it) } ?: false
        is KtBinaryExpression -> !isAssignment(statement.operationToken)
        else -> true
    }

    /**
     * Build a CST [io.codelaser.maddi.cst.api.expression.MethodCall]. The callee [MethodInfo] is resolved
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
            ?: return placeholder("k2-unsupported-callee", call)
        // `call.valueArguments` already includes a trailing lambda (a KtLambdaArgument IS a KtValueArgument),
        // so it must NOT be appended again from `call.lambdaArguments` (that double-counts the lambda).
        val valueArgs = call.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e, method, locals) } }
        val resolved = call.resolveSymbol()
        val calleeSymbol = resolved as? KaNamedFunctionSymbol
        // named and/or omitted arguments (`f(1, c = 5)`): rebuild the list in declaration order; a call omitting
        // one binds to the callee's `f$default` (see callArguments). Not for a vararg; a trailing lambda fills the
        // last parameter. A constructor needs it as much as a function: `Finding(e, "m")` against a third,
        // defaulted parameter otherwise finds no two-parameter constructor and becomes a placeholder, arguments
        // and all.
        val ordered = (resolved as? KaFunctionSymbol)?.takeIf {
            it.valueParameters.none { p -> p.isVararg } &&
                (call.valueArguments.any { a -> a.getArgumentName() != null } ||
                    call.valueArguments.size < it.valueParameters.size)
        }?.let { callArguments(call, it, method, locals) }
        val defaults = ordered?.defaults
        // a callee with context parameters takes them FIRST, ahead of an extension receiver (KotlinScan.contextParameters);
        // K2 names the value each one is bound to. One this converter cannot express leaves a named placeholder.
        val contexts = contextArguments(call.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.contextArguments,
            method, locals) ?: return placeholder("k2-context-argument-unresolved:$name", call)
        val arguments = contexts + (ordered?.expressions ?: valueArgs)

        // a call of a LOCAL function, `g(x)` or `x.g()` for a local extension: `g.invoke([x,] args)` on its variable
        if ((calleeSymbol?.psi as? KtNamedFunction)?.isLocal == true) {
            val recv = if (calleeSymbol.receiverParameter != null) receiver?.first ?: implicitExtensionReceiver(call, method, locals)
                       else null
            if (calleeSymbol.receiverParameter == null || recv != null) {
                locals[name]?.let { fnValue ->
                    val invokeArgs = listOfNotNull(recv) + arguments
                    fnValue.parameterizedType().typeInfo()?.let { resolveCallee(members(it), "invoke", invokeArgs) }?.let { invoke ->
                        return runtime.newMethodCallBuilder().setObject(variableExpression(fnValue)).setObjectIsImplicit(false)
                            .setMethodInfo(invoke).setParameterExpressions(invokeArgs)
                            .setConcreteReturnType(call.expressionType?.let { mapType(it, method.typeInfo()) } ?: invoke.returnType())
                            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                    }
                }
            }
        }

        // a member of a primitive (`i.toString()`, `b.not()`, `i.toLong()`): no Java type declares it
        if (receiver != null) primitiveMember(name, receiver.first, arguments, call, method)?.let { return it }

        // `arrayOf(a, b)`: an intrinsic with no bytecode of its own -- `new T[]{a, b}`, as the Java front end builds it
        if (receiver == null) arrayLiteral(call, calleeSymbol, arguments, method)?.let { return it }

        // `RuleSet(id, rules)` where RuleSet's COMPANION (or an `object`) declares `operator fun invoke`: not the
        // constructor, but `RuleSet.Companion.invoke(id, rules)` -- the class name used as a value, then invoked
        if (receiver == null && calleeSymbol?.name?.asString() == "invoke") {
            (call.calleeExpression as? KtNameReferenceExpression)?.let { classAsValue(it) }?.let { holder ->
                holder.parameterizedType().typeInfo()?.let { resolveCallee(members(it), "invoke", arguments) }?.let { callee ->
                    return runtime.newMethodCallBuilder().setObject(holder).setObjectIsImplicit(false)
                        .setMethodInfo(callee).setParameterExpressions(arguments)
                        .setConcreteReturnType(call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType())
                        .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                }
            }
        }

        // a SAM constructor, `Runnable { … }`: what it makes IS the lambda, whose anonymous type implements the
        // interface -- so the lambda is the expression, carrying that interface rather than its Kotlin function type.
        // Unhandled, the call fell through to an unresolved placeholder that swallowed the lambda and every
        // declaration inside it (javalin's censuses lost the overrides declared in one).
        if (resolved is KaSamConstructorSymbol) {
            val lambda = call.valueArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression
            if (lambda != null) {
                return convertLambda(lambda, method, locals, call.expressionType?.let { mapType(it, method.typeInfo()) })
            }
        }
        // a constructor call `Foo(args)` -> ConstructorCall (the call resolves to a constructor, not a method)
        if (resolved is KaConstructorSymbol) return convertConstructorCall(call, arguments, method, defaults)

        // an extension call `recv.ext(args)` routes to the facade's static `ext(recv, args)` (receiver as arg 0).
        // The receiver need not be written: `run { … }` inside a member is `this.run { … }`.
        if (calleeSymbol?.receiverParameter != null) {
            (receiver?.first ?: implicitExtensionReceiver(call, method, locals))?.let { recv ->
                val valueArguments = arguments.drop(contexts.size)
                extensionCall(name, recv, contexts, valueArguments, calleeSymbol, call, method, defaults)?.let { return it }
                if (defaults == null) memberExtensionCall(call, name, recv, contexts, valueArguments, method, locals)?.let { return it }
            }
        }
        // a companion call `Outer.member(args)` routes through the singleton: `Outer.Companion.member(args)`
        companionCall(name, calleeSymbol, arguments, call, method, defaults)?.let { return it }
        // a qualified call on a named object `Object.member(args)` routes through `Object.INSTANCE`
        if (receiver != null) objectCall(name, calleeSymbol, arguments, call, method, defaults)?.let { return it }
        // a top-level function `f(args)` called from another type -> the file facade's static `<File>Kt.f(args)`
        if (receiver == null) facadeCall(name, calleeSymbol, arguments, call, method, defaults)?.let { return it }

        // invoking a function-typed value `action()` -> `action.invoke(args)` (Kotlin's invoke-operator
        // sugar): the callee is a variable in scope, not a method. Resolve `invoke` on its functional type.
        if (receiver == null) resolveReference(name, method, locals)?.let { fnValue ->
            fnValue.parameterizedType().typeInfo()?.let { resolveCallee(it, "invoke", arguments) }?.let { invoke ->
                val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: invoke.returnType()
                return runtime.newMethodCallBuilder().setObject(fnValue).setObjectIsImplicit(false)
                    .setMethodInfo(invoke).setParameterExpressions(arguments).setConcreteReturnType(returnType)
                    .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
            }
        }

        // a member called on the receiver of the lambda it is written in -- `append` in `sb.apply { append("x") }`,
        // which Kotlin source never spells a receiver for. convertLambda holds it as the lambda's `$receiver`.
        if (receiver == null) locals["\$receiver"]?.let { lambdaReceiver ->
            lambdaReceiver.parameterizedType().typeInfo()
                ?.let { resolveCallee(it, name, arguments, callReturnFqn(call, method)) }?.let { callee ->
                    return runtime.newMethodCallBuilder()
                        .setObject(variableExpression(lambdaReceiver)).setObjectIsImplicit(true)
                        .setMethodInfo(callee).setParameterExpressions(arguments)
                        .setConcreteReturnType(call.expressionType?.let { mapType(it, method.typeInfo()) }
                            ?: callee.returnType())
                        .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                }
        }

        // a member called on an implicit receiver that is not this class's own `this`: the receiver of the extension
        // function the call is written in (`append(…)` inside `fun Md.h1()` is `$receiver.append(…)`), or an
        // enclosing receiver. K2 names the receiver; the name-based routes above could only guess at it.
        if (receiver == null && defaults == null) implicitDispatchCall(call, name, arguments, method, locals)?.let { return it }

        val ownerType = receiver?.second ?: method.typeInfo()
        val callee = defaults ?: resolveCallee(ownerType, name, arguments, callReturnFqn(call, method))
            ?: return placeholder("k2-unresolved-call:$name", call)
        val obj = receiver?.first
            ?: if (callee.isStatic) runtime.newTypeExpression(callee.typeInfo().asParameterizedType(), runtime.diamondNo())
            else self(method)
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
     * The value an extension call with no written receiver applies to: `run { … }` in a member of `C` is
     * `this.run { … }`, and inside `fun String.f()` it is that function's own `$receiver`. K2 says which, and
     * nothing is guessed: an implicit receiver this converter cannot name returns null, and the call stays a
     * placeholder rather than being given the wrong object.
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaSession.implicitExtensionReceiver(call: KtCallExpression, method: MethodInfo,
                                                    locals: Map<String, Variable>): Expression? =
        implicitReceiverValue(call.resolveToCall()?.singleFunctionCallOrNull()?.extensionReceiver, method, locals)

    /**
     * The CST value an IMPLICIT receiver stands for -- `this` of the class or of one enclosing it, or the `$receiver`
     * of the lambda or extension function K2 says it is -- or null when [value] is not implicit, or names a receiver
     * this converter cannot express (then the caller keeps its placeholder rather than picking the wrong object).
     */
    @OptIn(KaExperimentalApi::class) // KaContextParameterSymbol
    private fun KaSession.implicitReceiverValue(value: KaReceiverValue?, method: MethodInfo,
                                                locals: Map<String, Variable>): Expression? {
        val implicit = generateSequence(value) { (it as? KaSmartCastedReceiverValue)?.original }
            .filterIsInstance<KaImplicitReceiverValue>().firstOrNull() ?: return null
        return when (val symbol = implicit.symbol) {
            // `this` of the class the call is written in, or of one enclosing it
            is KaClassSymbol -> symbol.classId?.asFqNameString()?.let { infoByFqn.getType(it, sourceSet) }?.let {
                if (it == method.typeInfo()) self(method) else variableExpression(runtime.newThis(it.asParameterizedType()))
            }
            // an enclosing receiver: a lambda's, by the function literal K2 says owns it (receivers nest, and inside
            // `with(a) { with(b) { … } }` the innermost is not always the one meant), or the extension function's
            // own `$receiver`. Failing an exact owner, the innermost in scope -- only when its type is the one wanted.
            is KaReceiverParameterSymbol -> {
                (symbol.owningCallableSymbol.psi as? KtFunctionLiteral)?.let { locals[receiverKey(it)] }
                    ?.let { return variableExpression(it) }
                val wanted = mapType(implicit.type, method.typeInfo()).typeInfo()
                listOfNotNull(locals["\$receiver"], method.parameters().firstOrNull { it.name() == "\$receiver" })
                    .firstOrNull { it.parameterizedType().typeInfo() == wanted }?.let { variableExpression(it) }
            }
            // a context parameter passed on as a context argument: the enclosing function's parameter of that name
            is KaContextParameterSymbol -> resolveReference(symbol.name.asString(), method, locals)
            else -> null
        }
    }

    /**
     * The CST values of a call's context arguments, in order, or null when one names a value this converter cannot
     * express. Empty for a callee without context parameters.
     */
    private fun KaSession.contextArguments(values: List<KaReceiverValue>?, method: MethodInfo,
                                           locals: Map<String, Variable>): List<Expression>? =
        values.orEmpty().map { implicitReceiverValue(it, method, locals) ?: return null }

    /** The scope key under which a receiver lambda's `$receiver` stays reachable from lambdas nested in it. */
    private fun receiverKey(literal: KtFunctionLiteral): String = "\$receiver@${literal.textOffset}"

    /**
     * The type to look a member up in, for an implicit receiver [value] standing for [obj]: a SMART-CAST receiver's
     * narrowed type (`is KaClassSymbol -> classId`), as for a written smart-cast receiver, whose expressionType
     * convertQualified uses; otherwise the value's own.
     */
    private fun KaSession.receiverLookupType(value: KaReceiverValue?, obj: Expression, method: MethodInfo): TypeInfo? =
        (value as? KaSmartCastedReceiverValue)?.let { mapType(it.type, method.typeInfo()).typeInfo() }
            ?: obj.parameterizedType().typeInfo()

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.implicitDispatchCall(call: KtCallExpression, name: String, arguments: List<Expression>,
                                               method: MethodInfo, locals: Map<String, Variable>): Expression? {
        val dispatch = call.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.dispatchReceiver
        val obj = implicitReceiverValue(dispatch, method, locals) ?: return null
        val type = receiverLookupType(dispatch, obj, method) ?: return null
        // this class's own `this` is the fallback's business below, unchanged
        if (obj is VariableExpression && obj.variable() is This && type == method.typeInfo()) return null
        val callee = resolveCallee(members(type), name, arguments, callReturnFqn(call, method)) ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(obj).setObjectIsImplicit(true)
            .setMethodInfo(callee).setParameterExpressions(arguments)
            .setConcreteReturnType(call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /**
     * A bare name that is a member of an implicit receiver the name-based lookup cannot see: `length` inside
     * `fun String.f()` is `$receiver.length()`, a METHOD on the JVM, where [resolveReference] only looks for a field
     * of the extension receiver. K2 says which receiver and which member; the member is the field when the type has
     * one of that name, else its accessor, exactly as a qualified `obj.x` is converted.
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaSession.implicitMemberAccess(expression: KtNameReferenceExpression, method: MethodInfo,
                                               locals: Map<String, Variable>): Expression? {
        val access = expression.resolveToCall()?.successfulVariableAccessCall() ?: return null
        val name = expression.getReferencedName()
        // an EXTENSION property on an implicit receiver (`containingClassOrObject` inside `fun KtProperty.f()`): the
        // same getter a written `recv.prop` converts to, top-level or member extension, with that receiver
        if (access.partiallyAppliedSymbol.symbol.receiverParameter != null) {
            val receiver = implicitReceiverValue(access.partiallyAppliedSymbol.extensionReceiver, method, locals)
                ?: return null
            return extensionPropertyAccess(expression, name, receiver, method, locals)
                ?: memberExtensionPropertyAccess(expression, name, receiver, method, locals)
        }
        val dispatch = access.partiallyAppliedSymbol.dispatchReceiver
        val obj = implicitReceiverValue(dispatch, method, locals) ?: return null
        val type = receiverLookupType(dispatch, obj, method)?.let { members(it) } ?: return null
        type.fields().firstOrNull { it.name() == name }?.let { field ->
            return runtime.newVariableExpressionBuilder()
                .setVariable(runtime.newFieldReference(field, obj, field.type())).setSource(runtime.noSource()).build()
        }
        return resolveAccessor(type, name)?.let { accessorCall(obj, it) }
    }

    /**
     * Build an extension-function call as a static call on the file facade with the receiver as argument 0
     * (the JVM shape): `recv.ext(args)` → `<File>Kt.ext(recv, args)`. A library extension has no source facade;
     * its JVM one is loaded from the class path, as a top-level library function's is. Returns null when neither
     * the facade nor the static method can be resolved.
     */
    private fun KaSession.extensionCall(name: String, receiverExpr: Expression, contexts: List<Expression>,
                                        arguments: List<Expression>, symbol: KaNamedFunctionSymbol,
                                        call: KtCallExpression, method: MethodInfo, defaults: MethodInfo?): Expression? {
        val facade = extensionFacade(symbol) ?: with(typeMapper) { loadLibraryFacadeFor(symbol) } ?: return null
        val facadeArgs = contexts + listOf(receiverExpr) + arguments
        val callee = defaults ?: resolveCallee(facade, name, facadeArgs, callReturnFqn(call, method)) ?: return null
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
     * `recv.ext(args)` where `ext` is a MEMBER extension -- declared inside a type, so the call has two receivers:
     * the extension receiver [receiverExpr], and the dispatch receiver, always implicit (detekt's
     * `expression.resolveToCall()` inside `analyze(…) { }` dispatches on the lambda's `KaSession`). On the JVM it is
     * an instance method of the declaring type with the extension receiver as argument 0:
     * `$receiver.resolveToCall(expression)`. Null when K2 names a dispatch receiver this converter cannot express.
     */
    @OptIn(KaExperimentalApi::class)
    private fun KaSession.memberExtensionCall(call: KtCallExpression, name: String, receiverExpr: Expression,
                                              contexts: List<Expression>, arguments: List<Expression>,
                                              method: MethodInfo, locals: Map<String, Variable>): Expression? {
        val dispatch = call.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.dispatchReceiver
            ?: return null
        val obj = implicitReceiverValue(dispatch, method, locals) ?: return null
        val type = receiverLookupType(dispatch, obj, method) ?: return null
        val memberArgs = contexts + listOf(receiverExpr) + arguments
        val callee = resolveCallee(type, name, memberArgs, callReturnFqn(call, method)) ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(obj).setObjectIsImplicit(true)
            .setMethodInfo(callee).setParameterExpressions(memberArgs)
            .setConcreteReturnType(call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /**
     * Build a companion-member call `Outer.member(args)` as `Outer.Companion.member(args)`: the call's
     * object is a field access of the `Companion` singleton on the enclosing class. Returns null when the
     * callee isn't a companion member or its types aren't in this compilation.
     */
    private fun KaSession.companionCall(name: String, calleeSymbol: KaNamedFunctionSymbol?, arguments: List<Expression>,
                                        call: KtCallExpression, method: MethodInfo, defaults: MethodInfo?): Expression? {
        val companionDecl = (calleeSymbol?.psi as? KtNamedFunction)?.containingClassOrObject as? KtObjectDeclaration ?: return null
        if (!companionDecl.isCompanion()) return null
        val enclosingFqn = (companionDecl.containingClassOrObject?.symbol as? KaNamedClassSymbol)?.classId?.asFqNameString()
            ?: return null
        val enclosing = infoByFqn.getType(enclosingFqn, sourceSet) ?: return null
        val companionName = companionDecl.name ?: "Companion"
        val companion = enclosing.subTypes().firstOrNull { it.simpleName() == companionName } ?: return null
        val companionField = enclosing.fields().firstOrNull { it.name() == companionName } ?: return null
        val callee = defaults ?: resolveCallee(companion, name, arguments, callReturnFqn(call, method)) ?: return null
        return singletonMemberCall(enclosing, companionField, callee, arguments, call, method)
    }

    /**
     * Build a call `Object.member(args)` through a singleton: `<holder>.<field>.member(args)`, where the
     * call's object is a field access of the singleton (`Outer.Companion` or `Object.INSTANCE`).
     */
    private fun KaSession.objectCall(name: String, calleeSymbol: KaNamedFunctionSymbol?, arguments: List<Expression>,
                                     call: KtCallExpression, method: MethodInfo, defaults: MethodInfo?): Expression? {
        val objectDecl = (calleeSymbol?.psi as? KtNamedFunction)?.containingClassOrObject as? KtObjectDeclaration ?: return null
        if (objectDecl.isCompanion()) return null // companions go through companionCall
        val fqn = (objectDecl.symbol as? KaNamedClassSymbol)?.classId?.asFqNameString() ?: return null
        val objectType = infoByFqn.getType(fqn, sourceSet) ?: return null
        val instanceField = objectType.fields().firstOrNull { it.name() == "INSTANCE" } ?: return null
        val callee = defaults ?: resolveCallee(objectType, name, arguments, callReturnFqn(call, method)) ?: return null
        return singletonMemberCall(objectType, instanceField, callee, arguments, call, method)
    }

    /** The `MethodCall` of a singleton member: object = a field access of [singletonField] on [holder]. */
    private fun KaSession.singletonMemberCall(holder: TypeInfo, singletonField: FieldInfo, callee: MethodInfo,
                                              arguments: List<Expression>, call: KtCallExpression, method: MethodInfo): Expression {
        val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        // an object's `@JvmStatic` function is static (KotlinScan.isJvmStatic): called on the type, not the instance
        val scope = if (callee.isStatic) runtime.newTypeExpression(callee.typeInfo().asParameterizedType(), runtime.diamondNo())
                    else singletonAccess(holder, singletonField)
        return runtime.newMethodCallBuilder()
            .setObject(scope).setObjectIsImplicit(false).setMethodInfo(callee)
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
    /**
     * A top-level function call `f(args)` made from ANOTHER type resolves to the file facade's static
     * method `<File>Kt.f(args)`. Returns null when the callee isn't a top-level (non-extension) function,
     * when the enclosing method already is that facade (the normal path handles it), or when unresolved.
     */
    private fun KaSession.facadeCall(name: String, calleeSymbol: KaNamedFunctionSymbol?, arguments: List<Expression>,
                                     call: KtCallExpression, method: MethodInfo, defaults: MethodInfo?): Expression? {
        if (calleeSymbol == null || calleeSymbol.receiverParameter != null) return null
        if ((calleeSymbol.psi as? KtNamedFunction)?.containingClassOrObject != null) return null // a member, not top-level
        // a same-compilation top-level function lives on a source facade; a library one (e.g. kotlin.io.println)
        // has no source facade -- load its JVM file-facade class (kotlin.io.ConsoleKt) from the classpath instead.
        val facade = extensionFacade(calleeSymbol)?.takeIf { it != method.typeInfo() }
            ?: with(typeMapper) { loadLibraryFacadeFor(calleeSymbol) }
            ?: return null
        val callee = defaults ?: resolveCallee(facade, name, arguments, callReturnFqn(call, method)) ?: return null
        val returnType = call.expressionType?.let { mapType(it, method.typeInfo()) } ?: callee.returnType()
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(callee).setParameterExpressions(arguments)
            .setConcreteReturnType(returnType).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    private fun extensionFacade(symbol: KaNamedFunctionSymbol): TypeInfo? {
        val ktFile = (symbol.psi as? KtNamedFunction)?.containingKtFile ?: return null
        val pkg = ktFile.packageFqName
        val fqn = (if (pkg.isRoot) "" else pkg.asString() + ".") + facadeSimpleName(ktFile)
        return infoByFqn.getType(fqn, sourceSet)
    }


    /**
     * Prefix/postfix unary: `++`/`--` become an [io.codelaser.maddi.cst.api.expression.Assignment]
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
     * Convert a binary expression to a CST [io.codelaser.maddi.cst.api.expression.BinaryOperator] whose
     * operator is the corresponding `Runtime` operator method (e.g. `plusOperatorInt`). Only built-in
     * operators on primitive/String operands are emitted; overloaded operators (and Kotlin `==` on
     * objects, which is `.equals()`) fall back to a placeholder, to be handled as method calls.
     */
    private fun KaSession.convertBinary(expression: KtBinaryExpression, method: MethodInfo,
                                        locals: Map<String, Variable>): Expression {
        val left = expression.left?.let { convertExpression(it, method, locals) }
        val right = expression.right?.let { convertExpression(it, method, locals) }
        if (left == null || right == null) return placeholder("k2-binary-operand", expression)
        // elvis `a ?: b` -> `if (a == null) b else a`, marked NULL_COALESCING at the `?:` token.
        // The left operand stands in the lowering TWICE, and the CST is a TREE: the same instance in both places
        // makes every consumer that walks it visit its statements twice, and prep then throws "Trying to overwrite
        // a value for property variableData" -- 7 of the 8 methods it isolated on detekt (#32). So it is converted
        // a second time, which is also what the lowering says: `a` is written in the test and in the branch.
        if (expression.operationToken == KtTokens.ELVIS) return runtime.newInlineConditionalBuilder()
            .setCondition(runtime.newEquals(left, runtime.nullConstant()))
            .setIfTrue(right).setIfFalse(expression.left!!.let { convertExpression(it, method, locals) })
            .setSource(runtime.noSource().withDetailedSources(marker(DetailedSources.NULL_COALESCING, expression.operationReference)))
            .build(runtime)
        // `a in coll` -> `coll.contains(a)`; `a !in coll` -> `!coll.contains(a)` (receiver is the RIGHT operand)
        if (expression.operationToken == KtTokens.IN_KEYWORD || expression.operationToken == KtTokens.NOT_IN) {
            val collectionType = receiverTypeInfo(expression.right, method)
            val contains = collectionType?.let { resolveCallee(it, "contains", listOf(left)) }
                ?: return placeholder("k2-in-unresolved", expression)
            val call = runtime.newMethodCallBuilder().setObject(right).setObjectIsImplicit(false).setMethodInfo(contains)
                .setParameterExpressions(listOf(left)).setConcreteReturnType(contains.returnType()).setTypeArguments(listOf())
                .setSource(runtime.noSource()).build()
            return if (expression.operationToken == KtTokens.NOT_IN) runtime.newUnaryOperator(listOf(),
                runtime.noSource(), runtime.logicalNotOperatorBool(), call, runtime.precedenceUnary()) else call
        }
        // range `a..b` -> a constructor call of the range type (`1..10` -> IntRange(1, 10)); the range type
        // comes from the use-site expressionType and its 2-arg constructor (Int/Long/Char ranges)
        if (expression.operationToken == KtTokens.RANGE) {
            val rangeType = expression.expressionType?.let { mapType(it, method.typeInfo()) }
            rangeType?.typeInfo()?.let { members(it) }?.constructors()?.firstOrNull { it.parameters().size == 2 }?.let { ctor ->
                return runtime.newConstructorCallBuilder().setConstructor(ctor).setConcreteReturnType(rangeType)
                    .setParameterExpressions(listOf(left, right)).setDiamond(runtime.diamondNo())
                    .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
            }
        }
        val numeric = left.isNumeric && right.isNumeric
        val stringPlus = left.parameterizedType().isJavaLangString || right.parameterizedType().isJavaLangString
        // comparison on Comparable objects (not primitives): `a < b` -> `a.compareTo(b) < 0`
        val comparisonToZero = if (numeric) null else when (expression.operationToken) {
            KtTokens.LT -> runtime.lessOperatorInt()
            KtTokens.GT -> runtime.greaterOperatorInt()
            KtTokens.LTEQ -> runtime.lessEqualsOperatorInt()
            KtTokens.GTEQ -> runtime.greaterEqualsOperatorInt()
            else -> null
        }
        if (comparisonToZero != null) {
            val leftType = receiverTypeInfo(expression.left, method)
            leftType?.let { resolveCallee(it, "compareTo", listOf(right)) }?.let { compareTo ->
                val cmp = runtime.newMethodCallBuilder().setObject(left).setObjectIsImplicit(false).setMethodInfo(compareTo)
                    .setParameterExpressions(listOf(right)).setConcreteReturnType(runtime.intParameterizedType())
                    .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                return runtime.newBinaryOperatorBuilder().setLhs(cmp).setRhs(runtime.intZero())
                    .setOperator(comparisonToZero).setPrecedence(runtime.precedenceRelational())
                    .setParameterizedType(runtime.booleanParameterizedType()).setSource(runtime.noSource()).build()
            }
        }
        // referential identity `a === b`/`a !== b` -> the CST Equals node (== operator = reference equality)
        if (expression.operationToken == KtTokens.EQEQEQ) return runtime.newEquals(left, right)
        if (expression.operationToken == KtTokens.EXCLEQEQEQ) return logicalNot(runtime.newEquals(left, right))
        // Structural equality on objects: Kotlin `a == b` maps to the value-equality `a.equals(b)` call
        // (`!=` negated) -- the CST Equals node is REFERENCE identity (Kotlin `===`), so it must not be used
        // for `==`. APPROXIMATION (accepted, documented): Kotlin's `==` is null-safe -- really
        // `if (a == null) b == null else a.equals(b)` -- but we emit the plain `a.equals(b)` call. That is
        // exact when the receiver is non-null and drops only the null short-circuit otherwise; adequate for
        // modification/immutability analysis, which cares about the equals call, not the null guard.
        // A literal-null operand (`a == null`) is itself a reference null-check, so it maps to the Equals
        // node instead. Numeric `==` keeps the primitive equalsOperatorInt path (handled below).
        if (!numeric && (expression.operationToken == KtTokens.EQEQ || expression.operationToken == KtTokens.EXCLEQ)) {
            val negate = expression.operationToken == KtTokens.EXCLEQ
            val nullComparison = left is NullConstant || right is NullConstant
            val equality: Expression? = if (nullComparison) runtime.newEquals(left, right) else {
                val leftType = receiverTypeInfo(expression.left, method)
                leftType?.let { resolveCallee(it, "equals", listOf(right)) }?.let { equals ->
                    runtime.newMethodCallBuilder().setObject(left).setObjectIsImplicit(false).setMethodInfo(equals)
                        .setParameterExpressions(listOf(right)).setConcreteReturnType(runtime.booleanParameterizedType())
                        .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                }
            }
            if (equality != null) return if (negate) logicalNot(equality) else equality
        }
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
        } ?: return placeholder("k2-unsupported-operator:${expression.operationToken}", expression)
        val returnType = expression.expressionType?.let { mapType(it, method.typeInfo()) }
        left.parameterizedType().typeInfo()?.let { resolveCallee(it, functionName, listOf(right)) }?.let { callee ->
            return runtime.newMethodCallBuilder()
                .setObject(left).setObjectIsImplicit(false).setMethodInfo(callee)
                .setParameterExpressions(listOf(right))
                .setConcreteReturnType(returnType ?: callee.returnType())
                .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
        }
        // ⛔ An operator or infix function declared as an EXTENSION is not a member of the left operand's
        // type, so the lookup above can never find it: `"a" to 1` failed while `"a".to(1)` resolved, the
        // same call written two ways. It routes to the facade exactly as `extensionCall` routes a written
        // call, with the left operand as argument 0.
        // ⚠ Measured on detekt: `to` was the single biggest placeholder kind (347) once `mapOf(…)` started
        // resolving and stopped swallowing its arguments.
        extensionOperatorCall(expression, functionName, left, right, returnType)?.let { return it }
        return placeholder("k2-unresolved-operator:$functionName", expression)
    }

    /**
     * `a <op> b` where the operator/infix function is a library EXTENSION: `Facade.op(a, b)`.
     * ⚠ The symbol comes from `resolveToCall` on the binary expression — an operator IS a call — rather
     * than from `resolveSymbol` on the operation reference, which is a different experimental overload that
     * this module's opt-in does not cover.
     */
    private fun KaSession.extensionOperatorCall(expression: KtBinaryExpression, functionName: String,
                                                left: Expression, right: Expression,
                                                returnType: ParameterizedType?): Expression? {
        val symbol = expression.resolveToCall()?.singleFunctionCallOrNull()?.symbol
            as? KaNamedFunctionSymbol ?: return null
        if (symbol.receiverParameter == null) return null
        val facade = extensionFacade(symbol) ?: with(typeMapper) { loadLibraryFacadeFor(symbol) } ?: return null
        val facadeArgs = listOf(left, right)
        val callee = resolveCallee(facade, functionName, facadeArgs) ?: return null
        return runtime.newMethodCallBuilder()
            .setObject(runtime.newTypeExpression(facade.asParameterizedType(), runtime.diamondNo()))
            .setObjectIsImplicit(false).setMethodInfo(callee)
            .setParameterExpressions(facadeArgs)
            .setConcreteReturnType(returnType ?: callee.returnType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** The synthetic extension-receiver parameter (`$receiver`) of an extension method, or null. */
    private fun receiverParam(method: MethodInfo): ParameterInfo? =
        method.parameters().firstOrNull()?.takeIf { it.name() == "\$receiver" }

    /** Resolve a bare name: a local, else a parameter, else a field (locals shadow params shadow fields). */
    private fun resolveReference(name: String, method: MethodInfo, locals: Map<String, Variable>): Expression? {
        locals[name]?.let { return variableExpression(it) }
        method.parameters().firstOrNull { it.name() == name }?.let { return variableExpression(it) }
        method.typeInfo().fields().firstOrNull { it.name() == name }?.let { field ->
            return variableExpression(if (field.isStatic || !method.isStatic) runtime.newFieldReference(field)
                                      else runtime.newFieldReference(field, self(method), field.type()))
        }
        // unqualified access to a member of the extension receiver: `name` means `$receiver.name`
        receiverParam(method)?.let { receiver ->
            receiver.parameterizedType().typeInfo()?.fields()?.firstOrNull { it.name() == name }?.let { field ->
                return runtime.newVariableExpressionBuilder()
                    .setVariable(runtime.newFieldReference(field, variableExpression(receiver), field.type()))
                    .setSource(runtime.noSource()).build()
            }
        }
        // a field of an enclosing type accessed from a (non-static) inner class: `label` -> `Outer.this.label`
        var enclosing = method.typeInfo().compilationUnitOrEnclosingType()
        while (enclosing.isRight) {
            val outer = enclosing.right
            outer.fields().firstOrNull { it.name() == name }?.let { field ->
                val outerThis = variableExpression(runtime.newThis(outer.asParameterizedType(), outer, false))
                return variableExpression(runtime.newFieldReference(field, outerThis, field.type()))
            }
            enclosing = outer.compilationUnitOrEnclosingType()
        }
        // a property with no backing field (interface/abstract/computed) accessed unqualified: `name` in a
        // default method means `this.getName()` -- resolve the accessor on the enclosing type via `this`
        if (!method.isStatic || singleton(method.typeInfo()) != null) resolveAccessor(method.typeInfo(), name)?.let { accessor ->
            return accessorCall(self(method), accessor)
        }
        return null
    }

    /**
     * What `this` is in [method]: `this`, but in a static function of an `object` (`@JvmStatic`, see
     * KotlinScan.isJvmStatic) the object's `INSTANCE`, which is how kotlinc compiles it.
     */
    private fun self(method: MethodInfo): Expression {
        val type = method.typeInfo()
        if (method.isStatic) singleton(type)?.let { return singletonAccess(type, it) }
        return variableExpression(runtime.newThis(type.asParameterizedType()))
    }

    /** An `object`'s `INSTANCE` field, or null. */
    private fun singleton(type: TypeInfo): FieldInfo? = type.fields().firstOrNull { it.name() == "INSTANCE" && it.isStatic }

    internal fun variableExpression(variable: Variable): Expression =
        runtime.newVariableExpressionBuilder().setVariable(variable).setSource(runtime.noSource()).build()
}
