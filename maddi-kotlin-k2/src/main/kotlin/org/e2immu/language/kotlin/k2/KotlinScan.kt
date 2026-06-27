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
import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.expression.Expression
import org.e2immu.language.cst.api.info.MethodInfo
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.info.Variance
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.Block
import org.e2immu.language.cst.api.statement.Statement
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.api.type.ParameterizedType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance as KotlinVariance
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import java.nio.file.Files

/**
 * Turn Kotlin source into the shared CST, using the K2 Analysis API as the resolved source of truth
 * (the analogue of `maddi-java-openjdk`'s use of javac's Trees/Types).
 *
 * Scope today: top-level classes; declared functions with parameters, return types, nullability and a
 * body (block/expression form, returns, literal constants); declaration-site type-parameter variance.
 * Generics/class-type resolution, operators/references/calls in bodies, and properties arrive in later
 * milestones (see kotlin-parser-plan.md).
 */
class KotlinScan(private val runtime: Runtime, private val sourceSet: SourceSet) {

    // Internal registry of the types in the current compilation, keyed by FQN, so references between them
    // resolve. Library/external types are minted by the symbol scanner (loader) below.
    private val sourceTypes = mutableMapOf<String, TypeInfo>()

    // Loader/receptacle for library types (JDK, kotlin-stdlib, jars), keyed by JVM FQN.
    private val symbolScanner = KotlinSymbolScanner(runtime)

    /** Parse one in-memory Kotlin source file and return its primary CST types. */
    fun parse(fileName: String, content: String): List<TypeInfo> {
        // Standalone resolves from source roots: lay the content down in a temp directory.
        val srcRoot = Files.createTempDirectory("k2-src")
        val srcFile = srcRoot.resolve(fileName)
        Files.writeString(srcFile, content)

        val session = buildStandaloneAnalysisAPISession {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                addModule(
                    buildKtSourceModule {
                        moduleName = "main"
                        platform = JvmPlatforms.defaultJvmPlatform
                        addSourceRoot(srcRoot)
                    }
                )
            }
        }

        sourceTypes.clear()
        val result = mutableListOf<TypeInfo>()
        session.modulesWithFiles.values.flatten().filterIsInstance<KtFile>().forEach { ktFile ->
            val compilationUnit = runtime.newCompilationUnitBuilder()
                .setPackageName(ktFile.packageFqName.asString())
                .setURI(srcFile.toUri())
                .setSourceSet(sourceSet)
                .build()
            val declarations = ktFile.declarations.filterIsInstance<KtClassOrObject>()

            analyze(ktFile) {
                // pass A: create + register every type (and its type parameters) so later references resolve
                val types = declarations.map { registerType(compilationUnit, it) }
                // pass B: convert members now that all sibling types are known, and commit
                declarations.zip(types).forEach { (declaration, typeInfo) -> convertMembers(declaration, typeInfo) }
                compilationUnit.setTypes(types)
                result.addAll(types)
            }
        }
        return result
    }

    /** Pass A: create the [TypeInfo] and its type parameters, and register it by FQN. No members yet. */
    private fun KaSession.registerType(compilationUnit: CompilationUnit, declaration: KtClassOrObject): TypeInfo {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        val typeInfo = runtime.newTypeInfo(compilationUnit, classSymbol.name.asString())

        // declaration-site type parameters, with their variance (out T / in T)
        classSymbol.typeParameters.forEachIndexed { index, tp ->
            val cstTp = runtime.newTypeParameter(index, tp.name.asString(), typeInfo)
            cstTp.builder()
                .setTypeBounds(listOf()) // bounds resolution is a later increment; variance is the point here
                .setVariance(mapVariance(tp.variance))
                .commit()
            typeInfo.builder().addOrSetTypeParameter(cstTp)
        }
        sourceTypes[typeInfo.fullyQualifiedName()] = typeInfo
        return typeInfo
    }

    /** Pass B: convert members and commit the type. */
    private fun KaSession.convertMembers(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .forEach { function -> typeInfo.builder().addMethod(convertMethod(typeInfo, function)) }

        typeInfo.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .setAccess(runtime.accessPublic())
            .commit()
    }

    /** Convert one declared function symbol into a committed CST method (with parameters and body). */
    private fun KaSession.convertMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo {
        val method = runtime.newMethod(owner, function.name.asString(), runtime.methodTypeMethod())
        val returnType = mapType(function.returnType, owner)
        val builder = method.builder()
        function.valueParameters.forEach { p ->
            builder.addParameter(p.name.asString(), mapType(p.returnType, owner))
        }
        builder
            .setReturnType(returnType)
            .setMethodBody(convertBody(function, returnType))
            .addMethodModifier(runtime.methodModifierPublic())
            .setAccess(runtime.accessPublic())
            .commitParameters()
            .commit()
        return method
    }

    /**
     * Convert a function body into a CST [Block]. Handles the two Kotlin body forms — a block body
     * `{ … }` and an expression body `= expr` (which becomes a single `return expr`, or an expression
     * statement when the function returns Unit). M3 scope: literal constants and returns; expressions
     * not yet understood become an explicit [Runtime.newEmptyExpression] placeholder rather than failing.
     */
    private fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType): Block {
        val psi = function.psi as? KtNamedFunction ?: return runtime.newBlockBuilder().build()
        val block = runtime.newBlockBuilder()
        if (psi.hasBlockBody()) {
            psi.bodyBlockExpression?.statements?.forEach { block.addStatement(convertStatement(it)) }
        } else {
            psi.bodyExpression?.let { body ->
                val expr = convertExpression(body)
                block.addStatement(
                    if (returnType == runtime.voidParameterizedType()) runtime.newExpressionAsStatement(expr)
                    else runtime.newReturnStatement(expr)
                )
            }
        }
        return block.build()
    }

    /** Convert one statement (Kotlin statements are expressions). */
    private fun KaSession.convertStatement(statement: KtExpression): Statement =
        if (statement is KtReturnExpression) {
            val returned = statement.returnedExpression
            runtime.newReturnStatement(if (returned == null) runtime.newEmptyExpression() else convertExpression(returned))
        } else {
            runtime.newExpressionAsStatement(convertExpression(statement))
        }

    /**
     * Convert one expression. M3 handles compile-time constants (resolved via [evaluate]); anything else
     * (calls, references, operators) becomes a labelled placeholder, to be filled in incrementally.
     */
    private fun KaSession.convertExpression(expression: KtExpression): Expression {
        val constant = expression.evaluate()
        if (constant != null) {
            return when (val value = constant.value) {
                is Int -> runtime.newInt(value)
                is Boolean -> runtime.newBoolean(value)
                is String -> runtime.newStringConstant(value)
                null -> runtime.nullConstant()
                else -> runtime.newEmptyExpression("k2-unsupported-constant:${value::class.simpleName}")
            }
        }
        return runtime.newEmptyExpression("k2-unsupported-expr:${expression::class.simpleName}")
    }

    /**
     * Map a resolved Kotlin type to a CST [ParameterizedType], in the context of [owner] (whose type
     * parameters a bare `T` may refer to). Handles: the Kotlin builtins with a JVM/java.lang counterpart;
     * type parameters; and references to other types of the current compilation, with their generic
     * arguments. A nullable `T?` is boxed and tagged [NullableState.NULLABLE]. External/library types
     * (the CompiledTypesManager's job) still fall back to Object.
     */
    private fun KaSession.mapType(type: KaType, owner: TypeInfo): ParameterizedType {
        val base = when (type) {
            is KaClassType -> mapClassType(type, owner)
            is KaTypeParameterType ->
                owner.typeParameters().firstOrNull { it.simpleName() == type.symbol.name.asString() }
                    ?.let { runtime.newParameterizedType(it, 0, null) }
                    ?: runtime.objectParameterizedType()
            else -> runtime.objectParameterizedType()
        }
        return if (type.nullability == KaTypeNullability.NULLABLE)
            base.ensureBoxed(runtime).withNullable(NullableState.NULLABLE)
        else base
    }

    private fun KaSession.mapClassType(type: KaClassType, owner: TypeInfo): ParameterizedType {
        // primitives / void / the predefined java.lang singletons: predefined PTs (exact instances)
        when (type.classId.asFqNameString()) {
            "kotlin.Int" -> return runtime.intParameterizedType()
            "kotlin.Long" -> return runtime.longParameterizedType()
            "kotlin.Short" -> return runtime.shortParameterizedType()
            "kotlin.Byte" -> return runtime.byteParameterizedType()
            "kotlin.Boolean" -> return runtime.booleanParameterizedType()
            "kotlin.Char" -> return runtime.charParameterizedType()
            "kotlin.Float" -> return runtime.floatParameterizedType()
            "kotlin.Double" -> return runtime.doubleParameterizedType()
            "kotlin.Unit" -> return runtime.voidParameterizedType()
            "kotlin.String" -> return runtime.stringParameterizedType()
            "kotlin.Any" -> return runtime.objectParameterizedType()
        }
        // a sibling source type, or a library type loaded under its mapped JVM identity
        val typeInfo = sourceTypes[type.classId.asFqNameString()]
            ?: symbolScanner.getOrLoad(mapToJvmFqn(type.classId), type.typeArguments.size)
        val typeArguments = type.typeArguments.map { projection ->
            val arg = projection.type?.let { mapType(it, owner) } ?: runtime.objectParameterizedType() // star
            if (arg.isPrimitiveExcludingVoid) arg.ensureBoxed(runtime) else arg // List<Int> -> List<Integer>
        }
        return runtime.newParameterizedType(typeInfo, typeArguments)
    }

    /** Kotlin's mapped types (List, String, Any, …) -> their JVM FQN; everything else keeps its own FQN. */
    private fun mapToJvmFqn(classId: ClassId): String {
        val mapped = JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe())
        return mapped?.asSingleFqName()?.asString() ?: classId.asFqNameString()
    }

    /** Kotlin declaration-site variance (`out`/`in`) -> CST [Variance]. */
    private fun mapVariance(variance: KotlinVariance): Variance = when (variance) {
        KotlinVariance.OUT_VARIANCE -> Variance.COVARIANT
        KotlinVariance.IN_VARIANCE -> Variance.CONTRAVARIANT
        KotlinVariance.INVARIANT -> Variance.INVARIANT
    }
}
