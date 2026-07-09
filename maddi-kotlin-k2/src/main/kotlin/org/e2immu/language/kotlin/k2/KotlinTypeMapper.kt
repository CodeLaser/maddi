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
import org.e2immu.language.cst.api.element.RecordPattern
import org.e2immu.language.cst.api.element.SourceSet
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
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.components.packageScope
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
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
 * Maps resolved Kotlin types ([KaType]) to CST [ParameterizedType]s, and lazily loads non-source
 * library/JDK types from their resolved symbols. The bottom layer of the front-end (called by the rest,
 * calls almost nothing back); it owns the shared registry view + the library-symbol loader. Extracted
 * from `KotlinScan`; its functions keep the `KaSession` receiver, so `KotlinScan` delegates via
 * `with(typeMapper) { … }`.
 */
internal class KotlinTypeMapper(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    private val sourceSet: SourceSet,
    // external-library types (JDK / classpath) live in their own external-library source set, so the
    // analyzer (ComputeCallGraph via CompilationUnit.externalLibrary()) skips them; their CompilationUnit
    // must carry a non-null source set (a stub's is null -> AssertionError under doPrimaryType).
    private val librarySourceSet: SourceSet,
    // Phase 1 (shared JDK/library core): when a driver injects the Java front-end's CompiledTypesManager,
    // library (java.* / classpath) types are resolved through it -- one bytecode-authoritative TypeInfo
    // instance shared with the Java parser -- instead of being (re)built from K2 symbols. Null = standalone
    // (the K2-based library loading below is used).
    private val compiledTypesManager: CompiledTypesManager? = null,
) {
    private val symbolScanner = KotlinSymbolScanner(runtime, infoByFqn, librarySourceSet)
    private var memberDepth = 0
    private val maxMemberDepth = 2 // load members this many levels deep; deeper co-loaded types stay shells

    /**
     * Map a resolved Kotlin type to a CST [ParameterizedType], in the context of [owner] (whose type
     * parameters a bare `T` may refer to). Handles: the Kotlin builtins with a JVM/java.lang counterpart;
     * type parameters; and references to other types of the current compilation, with their generic
     * arguments. A nullable `T?` is boxed and tagged [NullableState.NULLABLE]. External/library types
     * (the CompiledTypesManager's job) still fall back to Object.
     */
    internal fun KaSession.mapType(type: KaType, owner: TypeInfo, method: MethodInfo? = null): ParameterizedType {
        // a Java platform type (`PrintStream!`, `String!`) is a flexible type (T..T?); map its non-null lower
        // bound, so Java library member types (e.g. the type of `System.out`) resolve instead of degrading to Object
        if (type is KaFlexibleType) return mapType(type.lowerBound, owner, method)
        val base = when (type) {
            is KaClassType -> mapClassType(type, owner, method)
            is KaTypeParameterType -> {
                val name = type.symbol.name.asString()
                // a method type parameter (`fun <T> …`) shadows a same-named type parameter of the owner
                (method?.typeParameters()?.firstOrNull { it.simpleName() == name }
                    ?: owner.typeParameters().firstOrNull { it.simpleName() == name })
                    ?.let { runtime.newParameterizedType(it, 0, null) }
                    ?: runtime.objectParameterizedType()
            }
            else -> runtime.objectParameterizedType()
        }
        return if (type.nullability == KaTypeNullability.NULLABLE)
            base.ensureBoxed(runtime).withNullable(NullableState.NULLABLE)
        else base
    }

    private fun KaSession.mapClassType(type: KaClassType, owner: TypeInfo, method: MethodInfo? = null): ParameterizedType {
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
        // an anonymous object type (`val r = object : Runnable {}`) has no named symbol: map it to its
        // first declared supertype (the SAM/base type), else Object -- there is no CST type of its own.
        if (type.symbol !is KaNamedClassSymbol) {
            return (type.symbol as? KaClassSymbol)?.superTypes
                ?.firstNotNullOfOrNull { st -> mapType(st, owner, method).takeUnless { it.isJavaLangObject } }
                ?: runtime.objectParameterizedType()
        }
        val kotlinFqn = type.classId.asFqNameString()
        // already known (a sibling source type, or a previously loaded library type), else load it:
        val typeInfo = infoByFqn.getType(kotlinFqn, sourceSet) ?: run {
            val jvmFqn = mapToJvmFqn(type.classId)
            // Phase 1 -- shared JDK/library core: delegate to the injected CompiledTypesManager (its
            // getOrLoad lazily loads from bytecode), so java.* is ONE TypeInfo instance across the Java and
            // Kotlin front-ends. Cache it locally; fall back to the K2-based load when absent (standalone) or
            // when the manager doesn't know the type (a Kotlin-only stdlib type).
            compiledTypesManager?.getOrLoad(jvmFqn, librarySourceSet)?.also {
                // only register if absent: a SHARED registry (mixed setup) already holds this instance under its
                // own (java.base) source set via the openjdk load, and re-putting the same instance trips the
                // InfoByFqn duplicate assertion. Standalone: getType is null on first use, so we still cache.
                if (infoByFqn.getType(jvmFqn, librarySourceSet) == null) infoByFqn.put(jvmFqn, it, librarySourceSet)
            }
                ?: if (jvmFqn != kotlinFqn) {
                // mapped (kotlin.collections.List -> java.util.List): load the JAVA symbol, so the shared
                // java.* type carries its real JVM surface and matches the Java front-end + AAPI -- not the
                // Kotlin read-only view (which would be order-dependent: List vs MutableList both map here)
                val javaSymbol = findClass(ClassId.topLevel(FqName(jvmFqn))) as? KaNamedClassSymbol
                if (javaSymbol != null) loadLibraryType(javaSymbol, jvmFqn)
                else symbolScanner.getOrLoad(jvmFqn, type.typeArguments.size) // not on classpath -> shell
            } else loadLibraryType(type.symbol as KaNamedClassSymbol, jvmFqn) // non-mapped -> deepen from symbol
        }
        return parameterize(typeInfo, type, owner, method)
    }

    /** Build the parameterized type for [typeInfo], converting and boxing the use-site type arguments. */
    private fun KaSession.parameterize(typeInfo: TypeInfo, type: KaClassType, owner: TypeInfo,
                                       method: MethodInfo? = null): ParameterizedType {
        val typeArguments = type.typeArguments.map { projection ->
            val arg = projection.type?.let { mapType(it, owner, method) } ?: runtime.objectParameterizedType() // star
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
    /** A [CompilationUnit] for a library type, carrying the external-library source set (never a null-set stub). */
    internal fun libraryCompilationUnit(packageName: String): CompilationUnit =
        runtime.newCompilationUnitBuilder()
            .setPackageName(packageName)
            .setURI(URI.create("library:/" + packageName.replace('.', '/')))
            .setSourceSet(librarySourceSet)
            .build()

    /**
     * The library file-facade [TypeInfo] that hosts a top-level *library* function ([function], e.g.
     * `kotlin.io.println` → `kotlin.io.ConsoleKt`). A JVM file facade is not a Kotlin classifier (so
     * `findClass` cannot see it); we derive its [ClassId] from the FIR container source, then build it from the
     * package's top-level functions that belong to the same facade — all as `public static` library methods, so
     * calls resolve and their arguments (their reads) are tracked. Null when not derivable/loadable.
     */
    internal fun KaSession.loadLibraryFacadeFor(function: KaNamedFunctionSymbol): TypeInfo? {
        val classId = jvmFacadeClassId(function) ?: return null
        val jvmFqn = classId.asFqNameString()
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { return it }
        val pkg = findPackage(classId.packageFqName) ?: return null
        val functions = pkg.packageScope.callables
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.receiverParameter == null && jvmFacadeClassId(it) == classId }
            .toList()
        if (functions.isEmpty()) return null
        val typeInfo = runtime.newTypeInfo(
            libraryCompilationUnit(classId.packageFqName.asString()), classId.shortClassName.asString())
        infoByFqn.put(jvmFqn, typeInfo, librarySourceSet) // register before members (param types may cycle back)
        val builder = typeInfo.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierFinal())
        val seen = mutableSetOf<String>() // erased overloads can collide on the same signature
        functions.map { convertLibraryMethod(typeInfo, it, static = true) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        builder.computeAccess().commit()
        return typeInfo
    }

    /**
     * The JVM file-facade [ClassId] of a top-level *library* function — read from its FIR `containerSource`
     * (a `JvmPackagePartSource`, e.g. `kotlin/io/ConsoleKt`). A library symbol has no PSI, so this is the only
     * place the facade name lives. Reached reflectively: the Analysis API does not expose the FIR container
     * source in its stable surface, and a soft failure (null) simply skips the resolution.
     */
    private fun jvmFacadeClassId(symbol: KaNamedFunctionSymbol): ClassId? = try {
        val firSymbol = symbol.javaClass.methods.firstOrNull { it.name == "getFirSymbol" }?.invoke(symbol)
        val fir = firSymbol?.javaClass?.methods?.firstOrNull { it.name == "getFir" }?.invoke(firSymbol)
        val cs = fir?.javaClass?.methods?.firstOrNull { it.name == "getContainerSource" }?.invoke(fir)
        cs?.javaClass?.methods?.firstOrNull { it.name == "getClassId" }?.invoke(cs) as? ClassId
    } catch (_: Throwable) {
        null
    }

    /**
     * Load (and register) the library/JDK type for a resolved class [symbol] — e.g. `java.lang.System` behind a
     * `System.out` static access — deepening it from bytecode. Returns null if it can't be located. Mapped types
     * (`kotlin.String` → `java.lang.String`) load their Java symbol, matching the rest of the front-end.
     */
    internal fun KaSession.loadLibraryClass(symbol: KaNamedClassSymbol): TypeInfo? {
        val classId = symbol.classId ?: return null
        val jvmFqn = mapToJvmFqn(classId)
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { return it }
        // an explicit "load this whole type" request (a `Type.staticMember` access): load from a reset depth so
        // the type's own members AND their types load, not shells -- e.g. `System.out`'s PrintStream keeps its
        // `println` overloads, so `System.out.println(...)` resolves. (A deeper co-load still bottoms out.)
        val saved = memberDepth
        memberDepth = 0
        try {
            return if (jvmFqn != classId.asFqNameString())
                (findClass(ClassId.topLevel(FqName(jvmFqn))) as? KaNamedClassSymbol)?.let { loadLibraryType(it, jvmFqn) }
            else loadLibraryType(symbol, jvmFqn)
        } finally {
            memberDepth = saved
        }
    }

    private fun KaSession.loadLibraryType(symbol: KaNamedClassSymbol, jvmFqn: String): TypeInfo {
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { return it }
        val typeInfo = runtime.newTypeInfo(
            libraryCompilationUnit(jvmFqn.substringBeforeLast('.', "")),
            jvmFqn.substringAfterLast('.')
        )
        // register all type parameters first (so a bound can reference any of them, incl. itself), then the
        // type (cycles), then map bounds (which may load other types and cycle back)
        val cstTypeParameters = symbol.typeParameters.mapIndexed { i, tp ->
            runtime.newTypeParameter(i, tp.name.asString(), typeInfo)
                .also { typeInfo.builder().addOrSetTypeParameter(it) } to tp
        }
        infoByFqn.put(jvmFqn, typeInfo, librarySourceSet) // register before loading bounds/supertypes (cycles)
        cstTypeParameters.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, typeInfo) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }

        val builder = typeInfo.builder()
        applyHierarchy(builder, typeInfo, symbol)
        builder.computeAccess()

        // Members, flattened: the full member scope (declared + inherited), so calls resolve to inherited
        // methods too (`equals`/`hashCode`/`toString` from Any, interface methods, …) -- the predefined
        // Object carries no such instance methods, so they must sit on each type. Bounded by depth so the
        // cascade terminates: types referenced beyond maxMemberDepth stay hierarchy-only shells. Depth 2 lets
        // a single chained call resolve (e.g. list.iterator().next() -- Iterator gets members too).
        if (memberDepth < maxMemberDepth) {
            memberDepth++
            try {
                // Static fields FIRST (`System.out`, `Integer.MAX_VALUE`, `Math.PI`, …): they live in the static
                // member scope as KaJavaFieldSymbols (not properties), and are commonly used as call receivers
                // (`System.out.println(...)`). Loading them before the methods means the field's own type
                // (PrintStream) loads WITH its members here, rather than being shelled first by some method's
                // transitive type at a deeper level -- so the chained call resolves.
                val seenFields = mutableSetOf<String>()
                symbol.staticMemberScope.declarations
                    .filterIsInstance<KaJavaFieldSymbol>()
                    .forEach { if (seenFields.add(it.name.asString())) builder.addField(convertLibraryStaticField(typeInfo, it)) }
                // dedup by FQN: flattened overloads can erase to the same signature (e.g. printStackTrace
                // (PrintStream)/(PrintWriter) both map to Object on a shell), which the type map rejects
                val seen = mutableSetOf<String>()
                symbol.memberScope.declarations
                    .filterIsInstance<KaNamedFunctionSymbol>()
                    .map { convertLibraryMethod(typeInfo, it) }
                    .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
                // properties -> fields, so `obj.size`/`obj.length` resolve (the body resolver reads a
                // property access as a field access, like a source type's backing field)
                symbol.memberScope.declarations
                    .filterIsInstance<KaPropertySymbol>()
                    .forEach { if (seenFields.add(it.name.asString())) builder.addField(convertLibraryField(typeInfo, it)) }
                // constructors (declared; not inherited) so `Foo(...)` resolves the called constructor
                val seenCtors = mutableSetOf<String>()
                symbol.declaredMemberScope.declarations
                    .filterIsInstance<KaConstructorSymbol>()
                    .map { convertLibraryConstructor(typeInfo, it) }
                    .forEach { if (seenCtors.add(it.fullyQualifiedName())) builder.addConstructor(it) }
            } finally {
                memberDepth--
            }
        }
        builder.commit()
        return typeInfo
    }

    /** A library method: signature only (params + return type), no body (the analogue of a class-file method). */
    private fun KaSession.convertLibraryMethod(owner: TypeInfo, function: KaNamedFunctionSymbol,
                                               static: Boolean = false): MethodInfo {
        val methodType = if (static) runtime.methodTypeStaticMethod() else runtime.methodTypeMethod()
        val method = runtime.newMethod(owner, function.name.asString(), methodType)
        val builder = method.builder()
        function.valueParameters.forEach { p -> builder.addParameter(p.name.asString(), mapType(p.returnType, owner)) }
        builder
            .setReturnType(mapType(function.returnType, owner))
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody()) // no body available (like a class-file method)
        addMethodModifiers(builder, function)
        if (static) builder.addMethodModifier(runtime.methodModifierStatic())
        builder.commitParameters().computeAccess().commit()
        return method
    }

    /**
     * Populate the predefined `java.lang.Object` with its real members (`equals`/`hashCode`/`toString`/…),
     * once, so source types walking up the hierarchy resolve inherited-from-Object calls. Mirrors the
     * openjdk front-end's bootstrap (ScanCompilationUnits loads Object with LOAD_MEMBERS). No-op if Object
     * is already inspected (openjdk ran first, or we already did this) or isn't on the classpath.
     */
    internal fun KaSession.bootstrapObject() {
        val objectType = runtime.objectTypeInfo()
        if (objectType.hasBeenInspected()) return
        val symbol = findClass(ClassId.topLevel(FqName("java.lang.Object"))) as? KaNamedClassSymbol ?: return
        val builder = objectType.builder()
        val seen = mutableSetOf<String>()
        symbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { convertLibraryMethod(objectType, it) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        symbol.declaredMemberScope.declarations
            .filterIsInstance<KaConstructorSymbol>()
            .forEach { builder.addConstructor(convertLibraryConstructor(objectType, it)) }
        builder.commit()
    }

    /**
     * Populate the predefined `java.lang.String` with its real members (`charAt`/`length`/`substring`/…), once.
     * `kotlin.String` maps to the predefined String shell, which otherwise carries no methods — so calls on a
     * String (including `s[i]`, whose indexed-get intrinsic maps to `charAt`) would not resolve. Mirrors
     * [bootstrapObject]; no-op if String is already inspected or isn't on the classpath.
     */
    internal fun KaSession.bootstrapString() {
        val stringType = runtime.stringTypeInfo()
        if (stringType.hasBeenInspected()) return
        val symbol = findClass(ClassId.topLevel(FqName("java.lang.String"))) as? KaNamedClassSymbol ?: return
        val builder = stringType.builder()
        val seen = mutableSetOf<String>()
        symbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { convertLibraryMethod(stringType, it) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        symbol.declaredMemberScope.declarations
            .filterIsInstance<KaConstructorSymbol>()
            .forEach { builder.addConstructor(convertLibraryConstructor(stringType, it)) }
        builder.commit()
    }

    /** A library constructor: signature only (params), no body (like a class-file constructor). */
    private fun KaSession.convertLibraryConstructor(owner: TypeInfo, ctor: KaConstructorSymbol): MethodInfo {
        val constructor = runtime.newConstructor(owner, runtime.methodTypeConstructor())
        val builder = constructor.builder()
        ctor.valueParameters.forEach { p -> builder.addParameter(p.name.asString(), mapType(p.returnType, owner)) }
        builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody())
        visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
        builder.commitParameters().computeAccess().commit()
        return constructor
    }

    /** A Java static field (`java.lang.System.out`, `Integer.MAX_VALUE`) -> a `public static` field on [owner]. */
    private fun KaSession.convertLibraryStaticField(owner: TypeInfo, field: KaJavaFieldSymbol): FieldInfo {
        val fieldInfo = runtime.newFieldInfo(field.name.asString(), true, mapType(field.returnType, owner), owner)
        val builder = fieldInfo.builder()
            .addFieldModifier(runtime.fieldModifierPublic())
            .addFieldModifier(runtime.fieldModifierStatic())
            .setInitializer(runtime.newEmptyExpression())
        if (field.isVal) builder.addFieldModifier(runtime.fieldModifierFinal()) // final field (`out`, `MAX_VALUE`)
        builder.computeAccess().commit()
        return fieldInfo
    }

    /** A library property -> a field on the type (signature only); the body resolver reads `obj.x` as field access. */
    private fun KaSession.convertLibraryField(owner: TypeInfo, property: KaPropertySymbol): FieldInfo {
        val field = runtime.newFieldInfo(property.name.asString(), false, mapType(property.returnType, owner), owner)
        val builder = field.builder()
            .addFieldModifier(runtime.fieldModifierPublic())
            .setInitializer(runtime.newEmptyExpression())
        if (property.setter == null) builder.addFieldModifier(runtime.fieldModifierFinal()) // read-only (val)
        builder.computeAccess().commit()
        return field
    }

    /** Set type nature and supertypes (parent class + interfaces) from the resolved class symbol. */
    internal fun KaSession.applyHierarchy(builder: TypeInfo.Builder, owner: TypeInfo, classSymbol: KaClassSymbol) {
        var parentClass: ParameterizedType? = null
        classSymbol.superTypes.forEach { superType ->
            val pt = mapType(superType, owner)
            if (pt.isJavaLangObject) return@forEach // implicit Any/Object supertype
            val superKind = (superType as? KaClassType)?.symbol?.let { (it as? KaClassSymbol)?.classKind }
            if (superKind == KaClassKind.INTERFACE) builder.addInterfaceImplemented(pt) else parentClass = pt
        }
        builder.setTypeNature(natureFor(classSymbol.classKind))
            .setParentClass(parentClass ?: runtime.objectParameterizedType())
        when (classSymbol.modality) {
            KaSymbolModality.ABSTRACT -> builder.addTypeModifier(runtime.typeModifierAbstract())
            KaSymbolModality.SEALED -> builder.addTypeModifier(runtime.typeModifierSealed())
            KaSymbolModality.FINAL -> builder.addTypeModifier(runtime.typeModifierFinal())
            else -> {} // OPEN
        }
        // visibility as a modifier; the *eventual* access is computed (computeAccess) from it + the enclosing type
        when (classSymbol.visibility) {
            KaSymbolVisibility.PRIVATE -> builder.addTypeModifier(runtime.typeModifierPrivate())
            KaSymbolVisibility.PROTECTED -> builder.addTypeModifier(runtime.typeModifierProtected())
            KaSymbolVisibility.INTERNAL -> builder.addTypeModifier(runtime.typeModifierInternal()) // Kotlin module visibility
            KaSymbolVisibility.PUBLIC -> builder.addTypeModifier(runtime.typeModifierPublic())
            else -> {}
        }
    }

    private fun natureFor(kind: KaClassKind): TypeNature = when (kind) {
        KaClassKind.INTERFACE -> runtime.typeNatureInterface()
        KaClassKind.ANNOTATION_CLASS -> runtime.typeNatureAnnotation()
        KaClassKind.ENUM_CLASS -> runtime.typeNatureEnum()
        else -> runtime.typeNatureClass()
    }

    internal fun visibilityMethodModifier(symbol: KaDeclarationSymbol): MethodModifier? = when (symbol.visibility) {
        KaSymbolVisibility.PRIVATE -> runtime.methodModifierPrivate()
        KaSymbolVisibility.PROTECTED -> runtime.methodModifierProtected()
        KaSymbolVisibility.INTERNAL -> runtime.methodModifierInternal() // Kotlin module visibility
        KaSymbolVisibility.PUBLIC -> runtime.methodModifierPublic()
        else -> null
    }

    internal fun addMethodModifiers(builder: MethodInfo.Builder, symbol: KaDeclarationSymbol) {
        visibilityMethodModifier(symbol)?.let { builder.addMethodModifier(it) }
        when (symbol.modality) {
            KaSymbolModality.ABSTRACT -> builder.addMethodModifier(runtime.methodModifierAbstract())
            KaSymbolModality.FINAL -> builder.addMethodModifier(runtime.methodModifierFinal())
            else -> {} // OPEN -> no modifier (overridable is the JVM default)
        }
    }

    /** Kotlin's mapped types (List, String, Any, …) -> their JVM FQN; everything else keeps its own FQN. */
    private fun mapToJvmFqn(classId: ClassId): String {
        val mapped = JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe())
        return mapped?.asSingleFqName()?.asString() ?: classId.asFqNameString()
    }

    /** Kotlin declaration-site variance (`out`/`in`) -> CST [Variance]. */
    internal fun mapVariance(variance: KotlinVariance): Variance = when (variance) {
        KotlinVariance.OUT_VARIANCE -> Variance.COVARIANT
        KotlinVariance.IN_VARIANCE -> Variance.CONTRAVARIANT
        KotlinVariance.INVARIANT -> Variance.INVARIANT
    }
}
