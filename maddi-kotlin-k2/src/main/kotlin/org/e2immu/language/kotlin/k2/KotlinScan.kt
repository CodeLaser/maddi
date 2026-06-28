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
import org.e2immu.language.cst.api.type.TypeNature
import org.e2immu.language.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import java.net.URI
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
class KotlinScan(
    private val runtime: Runtime,
    private val sourceSet: SourceSet,
    // The one shared type registry (source + library types), so single-instance-per-(FQN, sourceSet)
    // holds and can be shared (with a CompiledTypesManager receptacle, and later the Java parser). A
    // driver passes a shared instance; standalone callers get a fresh one. One scan per instance.
    private val infoByFqn: InfoByFqn = InfoByFqn(),
) {
    // Loader for library types (JDK, kotlin-stdlib, jars); deposits into infoByFqn by JVM FQN.
    private val symbolScanner = KotlinSymbolScanner(runtime, infoByFqn, sourceSet)

    // One-level guard: while loading a library type's members, types referenced by those member
    // signatures are loaded hierarchy-only (no members), to bound the cascade.
    private var loadingMembers = false

    /** Parse one in-memory Kotlin source file and return its primary CST types (test convenience). */
    fun parse(fileName: String, content: String): List<TypeInfo> = parse(mapOf(fileName to content))

    /** Parse a set of in-memory Kotlin files (name -> content) in one shared session. */
    fun parse(filesByName: Map<String, String>): List<TypeInfo> {
        // Standalone resolves from source roots: lay the files down in a temp directory.
        val srcRoot = Files.createTempDirectory("k2-src")
        filesByName.forEach { (name, content) ->
            val file = srcRoot.resolve(name)
            Files.createDirectories(file.parent ?: srcRoot)
            Files.writeString(file, content)
        }
        val session = buildSession(srcRoot)
        return convert(session.modulesWithFiles.values.flatten().filterIsInstance<KtFile>())
    }

    /**
     * Build a standalone session over [srcRoot], with the running JVM's JDK and this process's classpath
     * (kotlin-stdlib, etc.) as library dependencies, so library types resolve to real symbols.
     */
    private fun buildSession(srcRoot: java.nio.file.Path) = buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            val jvm = JvmPlatforms.defaultJvmPlatform
            platform = jvm
            val jdk = buildKtSdkModule {
                platform = jvm
                addBinaryRootsFromJdkHome(java.nio.file.Paths.get(System.getProperty("java.home")), isJre = false)
                libraryName = "jdk"
            }
            addModule(jdk)
            val classpathRoots = System.getProperty("java.class.path")
                .split(java.io.File.pathSeparator)
                .map { java.nio.file.Paths.get(it) }
                .filter { Files.exists(it) }
            val classpath = buildKtLibraryModule {
                platform = jvm
                addBinaryRoots(classpathRoots)
                libraryName = "classpath"
            }
            addModule(classpath)
            addModule(buildKtSourceModule {
                moduleName = "main"
                platform = jvm
                addSourceRoot(srcRoot)
                addRegularDependency(jdk)
                addRegularDependency(classpath)
            })
        }
    }

    /**
     * Convert resolved Kotlin files into committed CST types, sharing one [InfoByFqn] across all files.
     * A global two-pass — register every file's types first, then convert members — lets references
     * resolve across files, not just within one. The session is owned by the caller (a driver may wire a
     * real classpath before calling this); this is where multi-file projects are handled.
     */
    fun convert(ktFiles: List<KtFile>): List<TypeInfo> {
        // pass A (all files): create + register every type so cross-file references resolve
        val perFile = ktFiles.map { ktFile ->
            val compilationUnit = compilationUnitFor(ktFile)
            val declarations = ktFile.declarations.filterIsInstance<KtClassOrObject>()
            val types = analyze(ktFile) { declarations.map { registerType(compilationUnit, it) } }
            FileConversion(ktFile, compilationUnit, declarations, types)
        }
        // pass B (all files): convert members, now that all sibling types are known
        perFile.forEach { fc ->
            analyze(fc.ktFile) { fc.declarations.zip(fc.types).forEach { (d, ti) -> convertMembers(d, ti) } }
            fc.compilationUnit.setTypes(fc.types)
        }
        return perFile.flatMap { it.types }
    }

    private class FileConversion(
        val ktFile: KtFile,
        val compilationUnit: CompilationUnit,
        val declarations: List<KtClassOrObject>,
        val types: List<TypeInfo>,
    )

    private fun compilationUnitFor(ktFile: KtFile): CompilationUnit =
        runtime.newCompilationUnitBuilder()
            .setPackageName(ktFile.packageFqName.asString())
            .setURI(URI.create(ktFile.virtualFile?.url ?: "memory:/${ktFile.name}"))
            .setSourceSet(sourceSet)
            .build()

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
        infoByFqn.put(typeInfo.fullyQualifiedName(), typeInfo, sourceSet)
        return typeInfo
    }

    /** Pass B: convert members and commit the type. */
    private fun KaSession.convertMembers(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .forEach { function -> typeInfo.builder().addMethod(convertMethod(typeInfo, function)) }

        val builder = typeInfo.builder()
        applyHierarchy(builder, typeInfo, classSymbol)
        builder.setAccess(runtime.accessPublic()).commit()
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
        val kotlinFqn = type.classId.asFqNameString()
        // already known (a sibling source type, or a previously loaded library type), else load it:
        val typeInfo = infoByFqn.getType(kotlinFqn, sourceSet) ?: run {
            val jvmFqn = mapToJvmFqn(type.classId)
            if (jvmFqn != kotlinFqn) symbolScanner.getOrLoad(jvmFqn, type.typeArguments.size) // mapped -> shell
            else loadLibraryType(type.symbol as KaNamedClassSymbol, jvmFqn) // non-mapped -> deepen from symbol
        }
        return parameterize(typeInfo, type, owner)
    }

    /** Build the parameterized type for [typeInfo], converting and boxing the use-site type arguments. */
    private fun KaSession.parameterize(typeInfo: TypeInfo, type: KaClassType, owner: TypeInfo): ParameterizedType {
        val typeArguments = type.typeArguments.map { projection ->
            val arg = projection.type?.let { mapType(it, owner) } ?: runtime.objectParameterizedType() // star
            if (arg.isPrimitiveExcludingVoid) arg.ensureBoxed(runtime) else arg // List<Int> -> List<Integer>
        }
        return runtime.newParameterizedType(typeInfo, typeArguments)
    }

    /**
     * Load a non-mapped library/JDK type from its real resolved symbol (the analogue of openjdk's
     * `ClassSymbolScanner.loadType` in LAZILY mode): type nature, type-parameter arity, and the
     * supertype hierarchy (parent class + interfaces). The type is registered *before* its supertypes
     * are loaded so self/cyclic references terminate. Type-parameter bounds and members are deferred.
     */
    private fun KaSession.loadLibraryType(symbol: KaNamedClassSymbol, jvmFqn: String): TypeInfo {
        infoByFqn.getType(jvmFqn, sourceSet)?.let { return it }
        val typeInfo = runtime.newTypeInfo(
            runtime.newCompilationUnitStub(jvmFqn.substringBeforeLast('.', "")),
            jvmFqn.substringAfterLast('.')
        )
        symbol.typeParameters.forEachIndexed { i, tp ->
            val cstTp = runtime.newTypeParameter(i, tp.name.asString(), typeInfo)
            cstTp.builder().setTypeBounds(listOf()).commit() // bounds deferred
            typeInfo.builder().addOrSetTypeParameter(cstTp)
        }
        infoByFqn.put(jvmFqn, typeInfo, sourceSet) // register before loading supertypes (cycles)

        val builder = typeInfo.builder()
        applyHierarchy(builder, typeInfo, symbol)
        builder.setAccess(runtime.accessPublic())

        // Members, one level deep: skip while already loading members (bounds the cascade — types named
        // only by these signatures are loaded hierarchy-only).
        if (!loadingMembers) {
            loadingMembers = true
            try {
                symbol.declaredMemberScope.declarations
                    .filterIsInstance<KaNamedFunctionSymbol>()
                    .forEach { builder.addMethod(convertLibraryMethod(typeInfo, it)) }
            } finally {
                loadingMembers = false
            }
        }
        builder.commit()
        return typeInfo
    }

    /** A library method: signature only (params + return type), no body (the analogue of a class-file method). */
    private fun KaSession.convertLibraryMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo {
        val method = runtime.newMethod(owner, function.name.asString(), runtime.methodTypeMethod())
        val builder = method.builder()
        function.valueParameters.forEach { p -> builder.addParameter(p.name.asString(), mapType(p.returnType, owner)) }
        builder
            .setReturnType(mapType(function.returnType, owner))
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody()) // no body available (like a class-file method)
            .addMethodModifier(runtime.methodModifierPublic())
            .setAccess(runtime.accessPublic())
            .commitParameters()
            .commit()
        return method
    }

    /** Set type nature and supertypes (parent class + interfaces) from the resolved class symbol. */
    private fun KaSession.applyHierarchy(builder: TypeInfo.Builder, owner: TypeInfo, classSymbol: KaClassSymbol) {
        var parentClass: ParameterizedType? = null
        classSymbol.superTypes.forEach { superType ->
            val pt = mapType(superType, owner)
            if (pt.isJavaLangObject) return@forEach // implicit Any/Object supertype
            val superKind = (superType as? KaClassType)?.symbol?.let { (it as? KaClassSymbol)?.classKind }
            if (superKind == KaClassKind.INTERFACE) builder.addInterfaceImplemented(pt) else parentClass = pt
        }
        builder.setTypeNature(natureFor(classSymbol.classKind))
            .setParentClass(parentClass ?: runtime.objectParameterizedType())
    }

    private fun natureFor(kind: KaClassKind): TypeNature = when (kind) {
        KaClassKind.INTERFACE -> runtime.typeNatureInterface()
        KaClassKind.ANNOTATION_CLASS -> runtime.typeNatureAnnotation()
        KaClassKind.ENUM_CLASS -> runtime.typeNatureEnum()
        else -> runtime.typeNatureClass()
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
