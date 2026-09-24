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
import io.codelaser.maddi.cst.api.element.CompilationUnit
import io.codelaser.maddi.cst.api.element.RecordPattern
import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.FieldModifier
import io.codelaser.maddi.cst.api.info.MethodModifier
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.info.Variance
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.api.statement.Block
import io.codelaser.maddi.cst.api.statement.Statement
import io.codelaser.maddi.cst.api.statement.SwitchEntry
import io.codelaser.maddi.cst.api.variable.LocalVariable
import io.codelaser.maddi.cst.api.variable.Variable
import io.codelaser.maddi.cst.api.type.NullableState
import io.codelaser.maddi.cst.api.type.ParameterizedType
import io.codelaser.maddi.cst.api.type.TypeNature
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.components.packageScope
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
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
import com.intellij.psi.PsiElement
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
import org.jetbrains.kotlin.psi.KtTypeAlias
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
    /**
     * Mixed-language, a source set holding Java and Kotlin that reference each other: the Java front end's
     * [io.codelaser.maddi.cst.api.info.TypeInfo] of one of the set's Java SOURCE types, by FQN, made on request before
     * javac has attributed it (see `SourceSetInterleave`). Without it, a Kotlin signature naming such a type built a
     * committed library copy from K2's view, and the Java front end then built the real one beside it: two instances,
     * and every Kotlin reference on the wrong one.
     */
    var javaSourceTypes: ((String) -> TypeInfo?)? = null

    private val symbolScanner = KotlinSymbolScanner(runtime, infoByFqn, librarySourceSet)
    private var memberDepth = 0
    private val maxMemberDepth = 2 // load members this many levels deep; deeper co-loaded types stay shells

    /**
     * The library types first reached too deep to load their members, each with its symbol: left open, so that a later
     * visit from a shallower depth can still give it members ([deepen]). Which visit came first used to decide for
     * good: loading `java.lang.String` reaches `java.util.Iterator` at depth 2, through `Charset`, so a later
     * `list.iterator().next()` found no `next()` -- unless something else happened to load `Iterator` sooner, as
     * `kotlin.ByteArray`'s `ByteIterator` did while that was a type of its own. Committed by [commitShells].
     */
    private val shells = java.util.IdentityHashMap<TypeInfo, KaSymbolPointer<KaNamedClassSymbol>>()

    /** Give a [shells] type its members, when reached from where its first visit could not. */
    private fun KaSession.deepen(typeInfo: TypeInfo) {
        if (memberDepth >= maxMemberDepth) return
        val pointer = shells.remove(typeInfo) ?: return
        if (typeInfo.hasBeenInspected()) return // shared with a CompiledTypesManager, which completed it
        val symbol = pointer.restoreSymbol() ?: return typeInfo.builder().commit()
        loadLibraryMembers(typeInfo, symbol)
        typeInfo.builder().commit()
    }

    /**
     * Commit every library type still waiting in [shells], memberless: its visits are over. Not one the Java front
     * end's `CompiledTypesManager` has completed in the meantime, from its class file.
     */
    internal fun commitShells() {
        shells.keys.filterNot { it.hasBeenInspected() }.forEach { it.builder().commit() }
        shells.clear()
    }

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
        // `T & Any`, a definitely-non-null type parameter, is `T` on the JVM (javalin's `Validator<T>.get(): T & Any`);
        // falling through to Object, Java saw `ctx.queryParamAsClass("from", Instant.class).get()` return an Object
        if (type is KaDefinitelyNotNullType) return mapType(type.original, owner, method)
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

    // method-local types (`class C {…}` in a body) have no stable ClassId, so they can't be keyed by FQN in
    // infoByFqn; the body converter registers each here by its PSI, and a use-site type resolves via its symbol.
    private val localTypesByPsi = mutableMapOf<PsiElement, TypeInfo>()

    internal fun registerLocalType(psi: PsiElement, typeInfo: TypeInfo) { localTypesByPsi[psi] = typeInfo }

    // Top-level `typealias` declarations by FQN, taken from the PSI of the files being converted.
    //
    // The symbol provider cannot supply these when it matters: an `expect class Handle` and an
    // `actual typealias Handle` are, to a plain JVM module, two declarations of one FQN, and the CLASS wins —
    // `findTypeAlias(classId)` returns null and `findClassLike(classId)` returns the class. So the alias is
    // only reachable through the declaration itself. Populated by KotlinScan before conversion.
    private val typeAliasByFqn = mutableMapOf<String, KtTypeAlias>()

    /** Register every top-level `typealias` in [ktFiles], so an `expect` type can resolve to its expansion. */
    internal fun registerTypeAliases(ktFiles: List<KtFile>) {
        ktFiles.forEach { ktFile ->
            val packageName = ktFile.packageFqName.asString()
            ktFile.declarations.filterIsInstance<KtTypeAlias>().forEach { alias ->
                alias.name?.let { typeAliasByFqn[if (packageName.isEmpty()) it else "$packageName.$it"] = alias }
            }
        }
    }

    private fun KaSession.mapClassType(type: KaClassType, owner: TypeInfo, method: MethodInfo? = null): ParameterizedType {
        // a method-local type: resolve to the type we built for its declaration (no ClassId to look up by)
        (type.symbol as? KaNamedClassSymbol)?.psi?.let { psi ->
            localTypesByPsi[psi]?.let { return parameterize(it, type, owner, method) }
        }
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
            "kotlin.ByteArray" -> return runtime.byteParameterizedType().copyWithArrays(1)
            "kotlin.ShortArray" -> return runtime.shortParameterizedType().copyWithArrays(1)
            "kotlin.IntArray" -> return runtime.intParameterizedType().copyWithArrays(1)
            "kotlin.LongArray" -> return runtime.longParameterizedType().copyWithArrays(1)
            "kotlin.CharArray" -> return runtime.charParameterizedType().copyWithArrays(1)
            "kotlin.FloatArray" -> return runtime.floatParameterizedType().copyWithArrays(1)
            "kotlin.DoubleArray" -> return runtime.doubleParameterizedType().copyWithArrays(1)
            "kotlin.BooleanArray" -> return runtime.booleanParameterizedType().copyWithArrays(1)
            "kotlin.Array" -> {
                // Kotlin's Array<T> is the JVM reified array T[] (boxed element): model it as an array so that
                // overloads differing only in the element type -- Array<Double>.max vs Array<Float>.max, both
                // @JvmName("maxOrThrow") -- keep distinct erased signatures, matching JVM overload rules
                val elementRaw = type.typeArguments.firstOrNull()?.type?.let { mapType(it, owner, method) }
                    ?: runtime.objectParameterizedType()
                val element = if (elementRaw.isPrimitiveExcludingVoid) elementRaw.ensureBoxed(runtime) else elementRaw
                return element.copyWithArrays(element.arrays() + 1)
            }
        }
        // an anonymous object type (`val r = object : Runnable {}`) has no named symbol: map it to its
        // first declared supertype (the SAM/base type), else Object -- there is no CST type of its own.
        if (type.symbol !is KaNamedClassSymbol) {
            return (type.symbol as? KaClassSymbol)?.superTypes
                ?.firstNotNullOfOrNull { st -> mapType(st, owner, method).takeUnless { it.isJavaLangObject } }
                ?: runtime.objectParameterizedType()
        }
        // Kotlin-multiplatform: an `expect class` may be realised not by an `actual class` but by an
        // `actual typealias` -- coil declares `expect class Bitmap` in commonMain and
        // `actual typealias Bitmap = org.jetbrains.skia.Bitmap` in nonAndroidMain. KotlinScan drops `expect`
        // declarations (the `actual` is authoritative on the JVM), so when the realisation is an alias NOTHING
        // builds a type for that FQN and the lookup below would mint a shell named `coil3.Bitmap` -- a name no
        // JVM class has. Resolve through the alias to what it expands to, which is the type that really exists.
        // Guarded by isExpect, so the overwhelmingly common path does not pay for the lookup.
        if ((type.symbol as? KaNamedClassSymbol)?.isExpect == true) {
            val expanded = typeAliasByFqn[type.classId.asFqNameString()]?.symbol?.expandedType as? KaClassType
            if (expanded != null && expanded.classId != type.classId) {
                val target = mapClassType(expanded, owner, method)
                // Re-apply the USE-SITE type arguments. The expansion's own arguments name the ALIAS's type
                // parameters (`actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>`), which are
                // not in scope here and would degrade to Object; `Bitmap`, having none, is unaffected either way.
                val targetTypeInfo = target.typeInfo()
                return if (targetTypeInfo != null && type.typeArguments.isNotEmpty())
                    parameterize(targetTypeInfo, type, owner, method) else target
            }
        }
        val kotlinFqn = type.classId.asFqNameString()
        val jvmFqn = mapToJvmFqn(type.classId)
        val mapped = jvmFqn != kotlinFqn
        // A mapped builtin (kotlin.Enum -> java.lang.Enum, kotlin.String -> java.lang.String, …) must always
        // resolve to its JVM type, even while parsing kotlin-stdlib itself -- where kotlin.Enum is ALSO a source
        // type, so the sibling-source lookup below would otherwise return it and break the JVM model (and the
        // enum/annotation parent invariants asserted at commit). So skip that lookup for mapped builtins.
        // already known (a sibling source type, or a previously loaded library type), else load it:
        val typeInfo = (if (mapped) null else infoByFqn.getType(kotlinFqn, sourceSet))
            ?: (if (!mapped && type.symbol.origin == KaSymbolOrigin.JAVA_SOURCE) javaSourceTypes?.invoke(kotlinFqn) else null)
            ?: run {
            // Phase 1 -- shared JDK/library core: delegate to the injected CompiledTypesManager (its
            // getOrLoad lazily loads from bytecode), so java.* is ONE TypeInfo instance across the Java and
            // Kotlin front-ends. Cache it locally; fall back to the K2-based load when absent (standalone) or
            // when the manager doesn't know the type (a Kotlin-only stdlib type).
            compiledTypesManager?.type(jvmFqn, librarySourceSet)?.also {
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
        deepen(typeInfo)
        return parameterize(typeInfo, type, owner, method)
    }

    /**
     * Build the parameterized type for [typeInfo], converting and boxing the use-site type arguments. A use-site
     * projection is the wildcard kotlinc writes into the JVM signature: `*` is `?`, `out T` is `? extends T`, `in T` is
     * `? super T`. As `Object` and `T`, javac read `Class<*>` as `Class<Object>`, which no `Instant.class` converts to.
     */
    private fun KaSession.parameterize(typeInfo: TypeInfo, type: KaClassType, owner: TypeInfo,
                                       method: MethodInfo? = null): ParameterizedType {
        val typeArguments = type.typeArguments.map { projection ->
            if (projection is KaStarTypeProjection) return@map runtime.parameterizedTypeWildcard()
            val mapped = projection.type?.let { mapType(it, owner, method) } ?: runtime.objectParameterizedType()
            val arg = if (mapped.isPrimitiveExcludingVoid) mapped.ensureBoxed(runtime) else mapped // List<Int> -> List<Integer>
            when ((projection as? KaTypeArgumentWithVariance)?.variance) {
                KotlinVariance.OUT_VARIANCE -> arg.withWildcard(runtime.wildcardExtends())
                KotlinVariance.IN_VARIANCE -> arg.withWildcard(runtime.wildcardSuper())
                else -> arg
            }
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
     * Register a library type this front-end just minted, in **both** shared registries.
     *
     * [InfoByFqn] alone is not enough, and the gap is invisible until something asks by name. `Runtime.
     * getFullyQualified` — the only way to reach a type from a name, and what `LoadAnalysisResults` uses to
     * resolve an annotated-API entry — goes through the `CompiledTypesManager`, whose openjdk implementation
     * answers from its own flat map and consults `InfoByFqn` only to CHOOSE between same-named entries it
     * already holds (deliberately: a name unknown there must stay unknown, or a stub would start displacing
     * real types). So a type only this front-end ever built was in the CST, reachable by navigating to it,
     * and not findable by name — and every hint for `kotlin.*` was parsed and then dropped with "type not on
     * the classpath". This is the openjdk front-end's own `addTypeInfo` at commit time, done at the point
     * where the type becomes known instead.
     *
     * Inert standalone: with no manager injected there is no second registry to fall out of step with.
     */
    private fun registerLibraryType(jvmFqn: String, typeInfo: TypeInfo) {
        infoByFqn.put(jvmFqn, typeInfo, librarySourceSet)
        compiledTypesManager?.addTypeInfo(null, typeInfo)
    }

    /**
     * The library file-facade [TypeInfo] that hosts a top-level *library* function ([function], e.g.
     * `kotlin.io.println` → `kotlin.io.ConsoleKt`). A JVM file facade is not a Kotlin classifier (so
     * `findClass` cannot see it); we derive its [ClassId] from the FIR container source, then build it from the
     * package's top-level functions that belong to the same facade — all as `public static` library methods, so
     * calls resolve and their arguments (their reads) are tracked. Null when not derivable/loadable.
     *
     * ⛔ EXTENSIONS INCLUDED, and while they were not, `x.let { … }`, `x.first()` and `run { … }` in a member were
     * all `k2-unresolved-call` placeholders — which swallow their arguments, a lambda and every declaration in it
     * among them (maddi#43). An extension is a static of the facade whose first parameter is the receiver, which
     * is exactly what kotlinc compiles it to, so nothing else here has to know the difference.
     */
    internal fun KaSession.loadLibraryFacadeFor(function: KaNamedFunctionSymbol): TypeInfo? =
        loadLibraryFacade(jvmFacadeClassId(function))

    /**
     * The facade holding a top-level library PROPERTY — `Class<T>.java`, `CharSequence.lastIndex`. Kotlin
     * compiles an extension property into a static getter on the same kind of facade class its functions go
     * to, so this is the same load; it exists as its own entry point because a property is not a
     * [KaNamedFunctionSymbol].
     */
    internal fun KaSession.loadLibraryFacadeForProperty(property: KaPropertySymbol): TypeInfo? =
        loadLibraryFacade(jvmFacadeClassId(property))

    private fun KaSession.loadLibraryFacade(classId: ClassId?): TypeInfo? {
        if (classId == null) return null
        val jvmFqn = classId.asFqNameString()
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { return it }
        val pkg = findPackage(classId.packageFqName) ?: return null
        val functions = pkg.packageScope.callables
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { jvmFacadeClassId(it) == classId }
            .toList()
        // ⭐ and its PROPERTIES, as static getters: an extension property compiles to `getX(receiver)` on the
        // same facade, so a facade built from functions alone cannot resolve `x.lastIndex` or `c.java`.
        val properties = pkg.packageScope.callables
            .filterIsInstance<KaPropertySymbol>()
            .filter { jvmFacadeClassId(it) == classId }
            .toList()
        if (functions.isEmpty() && properties.isEmpty()) return null
        val typeInfo = runtime.newTypeInfo(
            libraryCompilationUnit(classId.packageFqName.asString()), classId.shortClassName.asString())
        registerLibraryType(jvmFqn, typeInfo) // register before members (param types may cycle back)
        val builder = typeInfo.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierFinal())
        val seen = mutableSetOf<String>() // erased overloads can collide on the same signature
        functions.map { convertLibraryMethod(typeInfo, it, static = true) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        properties.mapNotNull { convertLibraryPropertyGetter(typeInfo, it) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        builder.computeAccess().commit()
        return typeInfo
    }

    /**
     * A top-level extension property as the static getter kotlinc compiles it to: `val C.x: T` becomes
     * `getX(C): T` on the file facade. Null when the property has no extension receiver — a plain top-level
     * `val` is a field on the facade, which is a different shape and not this path's business.
     */
    private fun KaSession.convertLibraryPropertyGetter(owner: TypeInfo, property: KaPropertySymbol): MethodInfo? {
        val receiver = property.receiverParameter ?: return null
        val name = property.name.asString()
        val getterName = "get" + name.replaceFirstChar { it.uppercaseChar() }
        val method = runtime.newMethod(owner, getterName, runtime.methodTypeStaticMethod())
        val builder = method.builder()
        builder.addParameter("\$receiver", mapType(receiver.returnType, owner))
        builder.setReturnType(mapType(property.returnType, owner))
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody())
            .addMethodModifier(runtime.methodModifierPublic())
            .addMethodModifier(runtime.methodModifierStatic())
        builder.commitParameters().computeAccess().commit()
        return method
    }

    /**
     * The JVM file-facade [ClassId] of a top-level *library* function — read from its FIR `containerSource`
     * (a `JvmPackagePartSource`, e.g. `kotlin/io/ConsoleKt`). A library symbol has no PSI, so this is the only
     * place the facade name lives. Reached reflectively: the Analysis API does not expose the FIR container
     * source in its stable surface, and a soft failure (null) simply skips the resolution.
     */
    private fun jvmFacadeClassId(symbol: KaCallableSymbol): ClassId? = try {
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
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { deepen(it); return it }
        // Shared-core delegation, exactly as in mapClassType: when a driver injected the Java front-end's
        // CompiledTypesManager, the library type may already have been built (and COMMITTED) from bytecode under
        // its own source set, which the registry lookup above — keyed by librarySourceSet — does not see.
        // Rebuilding it here then commits a second time: "Trying to overwrite final value". This path was
        // reachable only once the manager actually had contents; while its lazy loader was dead in Kotlin-only
        // runs it always missed, which is why a `Type.staticMember` access never tripped it before.
        compiledTypesManager?.type(jvmFqn, librarySourceSet)?.let {
            if (infoByFqn.getType(jvmFqn, librarySourceSet) == null) infoByFqn.put(jvmFqn, it, librarySourceSet)
            return it
        }
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
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { deepen(it); return it }
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
        registerLibraryType(jvmFqn, typeInfo) // register before loading bounds/supertypes (cycles)
        cstTypeParameters.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, typeInfo) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }

        val builder = typeInfo.builder()
        applyHierarchy(builder, typeInfo, symbol)
        builder.computeAccess()

        if (memberDepth < maxMemberDepth) {
            loadLibraryMembers(typeInfo, symbol)
            builder.commit()
        } else {
            shells[typeInfo] = symbol.createPointer()
        }
        return typeInfo
    }

    // Members, flattened: the full member scope (declared + inherited), so calls resolve to inherited
    // methods too (`equals`/`hashCode`/`toString` from Any, interface methods, …) -- the predefined
    // Object carries no such instance methods, so they must sit on each type. Bounded by depth so the
    // cascade terminates: types referenced beyond maxMemberDepth wait in [shells]. Depth 2 lets
    // a single chained call resolve (e.g. list.iterator().next() -- Iterator gets members too).
    private fun KaSession.loadLibraryMembers(typeInfo: TypeInfo, symbol: KaNamedClassSymbol) {
        val builder = typeInfo.builder()
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

    /** A library method: signature only (params + return type), no body (the analogue of a class-file method). */
    private fun KaSession.convertLibraryMethod(owner: TypeInfo, function: KaNamedFunctionSymbol,
                                               static: Boolean = false): MethodInfo {
        val methodType = if (static) runtime.methodTypeStaticMethod() else runtime.methodTypeMethod()
        val method = runtime.newMethod(owner, function.name.asString(), methodType)
        val builder = method.builder()
        // an extension's receiver is its first JVM parameter, named as KotlinScan names a source extension's
        function.receiverParameter?.let { builder.addParameter("\$receiver", mapType(it.returnType, owner)) }
        function.valueParameters.forEach { p ->
            // ⛔ A VARARG'S K2 returnType IS THE ELEMENT TYPE; the JVM parameter is an array of it. Without this
            // `mapOf(vararg Pair)` is modelled as `mapOf(Pair)` -- the signature of the OTHER, single-pair overload,
            // so the two collide and `seen` drops one of them (KotlinScan.convertMethodSignature does the same).
            val elementType = mapType(p.returnType, owner)
            val parameterType = if (p.isVararg) elementType.copyWithArrays(elementType.arrays() + 1) else elementType
            builder.addParameter(p.name.asString(), parameterType).builder().setVarArgs(p.isVararg)
        }
        builder
            .setReturnType(mapType(function.returnType, owner))
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody()) // no body available (like a class-file method)
        declaredExceptions(function).forEach { builder.addExceptionType(it) }
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
        // ⛔ STATIC members: `String.format(…)`, `String.valueOf(…)`, `String.join(…)` live in their own
        // scope, which this bootstrap never read.
        //
        // ⚠ Its PROPERTIES are deliberately NOT turned into fields here, and the measurement is why.
        // `loadLibraryMembers` does that for every other library type, so doing it looked like the missing
        // half — but this bootstrap only runs when nothing has inspected `java.lang.String` yet, i.e. in a
        // Kotlin-only parse with no JDK. Every real run loads the JDK, where `length` is a METHOD and
        // `s.length` resolves through `resolveAccessor` as `length()` — which is why the corpora show ZERO
        // `k2-unresolved-access:length`. Adding a field made the FIXTURE path model a property read that the
        // real path models as a call, and a ported prepwork test caught it: `java.lang.String.length#s`
        // appeared in a VariableData its Java original does not have.
        val seenFields = mutableSetOf<String>()
        symbol.staticMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { convertLibraryMethod(stringType, it, static = true) }
            .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
        symbol.staticMemberScope.declarations
            .filterIsInstance<KaJavaFieldSymbol>()
            .forEach { if (seenFields.add(it.name.asString())) builder.addField(convertLibraryStaticField(stringType, it)) }
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
        declaredExceptions(ctor).forEach { builder.addExceptionType(it) }
        visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
        builder.commitParameters().computeAccess().commit()
        return constructor
    }

    /**
     * The checked exceptions a library method or constructor declares. A Kotlin symbol has no `throws` of its own --
     * Kotlin has no checked exceptions -- so they come from the PSI of the class file, which is what javac reads too.
     * Without them a Java stub calling such a constructor through `super(...)` does not compile: javalin's
     * `LeveledBrotli4jStream` extends brotli4j's `BrotliOutputStream(OutputStream, Encoder.Parameters)`, which throws
     * IOException. (The Java front end reads them from the symbol; a type this front end loads had none.)
     */
    private fun KaSession.declaredExceptions(symbol: KaFunctionSymbol): List<ParameterizedType> {
        val throwsList = (symbol.psi as? PsiMethod)?.throwsList ?: return listOf()
        return throwsList.referencedTypes.mapNotNull { type ->
            val fqn = type.resolve()?.qualifiedName ?: return@mapNotNull null
            // by name only: an exception type is named in a `throws`, never called through, and loading it here
            // would decide the members of whatever it reaches first (see [shells])
            (infoByFqn.getType(fqn, librarySourceSet) ?: symbolScanner.getOrLoad(fqn, 0))?.asParameterizedType()
        }
    }

    /** A Java static field (`java.lang.System.out`, `Integer.MAX_VALUE`) -> a static field on [owner], as visible as declared. */
    private fun KaSession.convertLibraryStaticField(owner: TypeInfo, field: KaJavaFieldSymbol): FieldInfo {
        val fieldInfo = runtime.newFieldInfo(field.name.asString(), true, mapType(field.returnType, owner), owner)
        val builder = fieldInfo.builder()
            .addFieldModifier(runtime.fieldModifierStatic())
            .setInitializer(runtime.newEmptyExpression())
        visibilityFieldModifier(field)?.let { builder.addFieldModifier(it) }
        if (field.isVal) builder.addFieldModifier(runtime.fieldModifierFinal()) // final field (`out`, `MAX_VALUE`)
        builder.computeAccess().commit()
        return fieldInfo
    }

    /** A library property -> a field on the type (signature only); the body resolver reads `obj.x` as field access. */
    private fun KaSession.convertLibraryField(owner: TypeInfo, property: KaPropertySymbol): FieldInfo {
        val field = runtime.newFieldInfo(property.name.asString(), false, mapType(property.returnType, owner), owner)
        val builder = field.builder()
            .setInitializer(runtime.newEmptyExpression())
        visibilityFieldModifier(property)?.let { builder.addFieldModifier(it) }
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
        // a class nested in another is static on the JVM unless it is `inner`: without the modifier every Kotlin
        // nested class was an inner class of the CST (TypeInfo.isInnerClass), capturing an enclosing instance it
        // does not have. A local class is not nested in a type, and an object/interface/enum is static by nature.
        if (owner.compilationUnitOrEnclosingType().isRight && classSymbol.classKind == KaClassKind.CLASS
            && (classSymbol as? KaNamedClassSymbol)?.isInner != true && classSymbol.classId != null) { // a local class: no ClassId
            builder.addTypeModifier(runtime.typeModifierStatic())
        }
        when (classSymbol.modality) {
            KaSymbolModality.ABSTRACT -> builder.addTypeModifier(runtime.typeModifierAbstract())
            KaSymbolModality.SEALED -> builder.addTypeModifier(runtime.typeModifierSealed())
            KaSymbolModality.FINAL -> builder.addTypeModifier(runtime.typeModifierFinal())
            else -> {} // OPEN
        }
        // visibility as a modifier; the *eventual* access is computed (computeAccess) from it + the enclosing type
        when (classSymbol.visibility) {
            KaSymbolVisibility.PRIVATE -> builder.addTypeModifier(runtime.typeModifierPrivate())
            // Java's `protected` (package access too) is its own value, PACKAGE_PROTECTED; see [visibilityMethodModifier]
            KaSymbolVisibility.PROTECTED, KaSymbolVisibility.PACKAGE_PROTECTED ->
                builder.addTypeModifier(runtime.typeModifierProtected())
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

    /**
     * The visibility of a symbol as a modifier; null for a Java package-private one (PACKAGE_PRIVATE), which the
     * CST writes as no modifier at all.
     *
     * ⛔ A JAVA `protected` IS NOT `PROTECTED`. K2 gives it PACKAGE_PROTECTED, protected plus package access, and
     * until 2026-09-24 that fell through to null: every protected member of a library class loaded here came out
     * PACKAGE -- AssertJ's `AbstractAssert.failureWithActualExpected` among them, which a refactoring's access
     * check then saw as merely package-private.
     */
    internal fun visibilityMethodModifier(symbol: KaDeclarationSymbol): MethodModifier? = when (symbol.visibility) {
        KaSymbolVisibility.PRIVATE -> runtime.methodModifierPrivate()
        KaSymbolVisibility.PROTECTED, KaSymbolVisibility.PACKAGE_PROTECTED -> runtime.methodModifierProtected()
        KaSymbolVisibility.INTERNAL -> runtime.methodModifierInternal() // Kotlin module visibility
        KaSymbolVisibility.PUBLIC -> runtime.methodModifierPublic()
        else -> null
    }

    /** As [visibilityMethodModifier], for a field. */
    private fun visibilityFieldModifier(symbol: KaDeclarationSymbol): FieldModifier? = when (symbol.visibility) {
        KaSymbolVisibility.PRIVATE -> runtime.fieldModifierPrivate()
        KaSymbolVisibility.PROTECTED, KaSymbolVisibility.PACKAGE_PROTECTED -> runtime.fieldModifierProtected()
        KaSymbolVisibility.INTERNAL -> runtime.fieldModifierInternal()
        KaSymbolVisibility.PUBLIC -> runtime.fieldModifierPublic()
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
