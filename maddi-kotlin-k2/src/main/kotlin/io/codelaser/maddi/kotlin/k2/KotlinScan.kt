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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.kotlin.api.ConstructorDelegation
import io.codelaser.maddi.kotlin.api.KotlinSourceScan
import io.codelaser.maddi.kotlin.api.KotlinParseObserver
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import io.codelaser.maddi.cst.api.element.CompilationUnit
import io.codelaser.maddi.cst.api.element.DetailedSources
import io.codelaser.maddi.cst.api.element.RecordPattern
import io.codelaser.maddi.cst.api.element.Source
import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.MethodInfo
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
import io.codelaser.maddi.inspection.api.util.EnumSynthetics
import io.codelaser.maddi.inspection.api.util.RecordSynthetics
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveSymbol
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.contextParameters
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtConstructor
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
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import java.net.URI
import io.codelaser.maddi.inspection.resource.SourceSetImpl
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
    // Phase 1 (shared JDK/library core): a driver may inject the Java front-end's CompiledTypesManager so
    // java.* / classpath types resolve to ONE bytecode-authoritative TypeInfo instance shared with the Java
    // parser. Null = standalone (K2 loads library types itself).
    private val compiledTypesManager: CompiledTypesManager? = null,
) : MemberConverter, KotlinSourceScan {
    // Type mapping (KaType -> ParameterizedType) + lazy library-type loading: the bottom layer.
    // Delegated via thin forwarders at the end of this class (KaSession member extensions -> with(…)).
    // external-library (JDK/classpath) types get their own external-library source set, so the analyzer
    // skips them (CompilationUnit.externalLibrary()); a source set is required (a stub's is null).
    private val librarySourceSet: SourceSet = SourceSetImpl.Builder()
        .setName(sourceSet.name() + "-library").setUri(URI.create("library:/"))
        .setExternalLibrary(true).build()
    private val typeMapper = KotlinTypeMapper(runtime, infoByFqn, sourceSet, librarySourceSet, compiledTypesManager)
    private val kotlinAnnotations = KotlinAnnotations(runtime, typeMapper)

    /** Copy the annotations K2 placed on [symbol] onto the CST element [builder] builds (see [KotlinAnnotations]). */
    private fun KaSession.annotate(builder: Element.Builder<*>, symbol: KaAnnotated?, owner: TypeInfo) =
        with(kotlinAnnotations) { annotate(builder, symbol, owner) }

    /** See [KotlinTypeMapper.javaSourceTypes]. */
    var javaSourceTypes: ((String) -> TypeInfo?)?
        get() = typeMapper.javaSourceTypes
        set(value) {
            typeMapper.javaSourceTypes = value
        }

    // Function-body conversion (statements/expressions/calls/lambdas). It builds anonymous-object members
    // through `this` (KotlinScan is the MemberConverter), breaking the bodies<->declarations cycle.
    private val bodyConverter = KotlinBodyConverter(runtime, infoByFqn, sourceSet, typeMapper)

    /**
     * Calls this scan bound to an overload none of whose parameters the arguments fit — see
     * [KotlinBodyConverter.ambiguousBindings]. Exposed because it is the one failure the placeholder census
     * cannot see: a guessed callee is a RESOLVED call in the tree.
     */
    val ambiguousBindings: Int get() = bodyConverter.ambiguousBindings

    /** See [KotlinBodyConverter.elvisReEvaluations]. */
    val elvisReEvaluations: Int get() = bodyConverter.elvisReEvaluations

    /** See [KotlinBodyConverter.elvisTemporaries]. */
    val elvisTemporaries: Int get() = bodyConverter.elvisTemporaries

    /** See [KotlinBodyConverter.nullSafeTemporaries]. */
    val nullSafeTemporaries: Int get() = bodyConverter.nullSafeTemporaries

    init {
        bodyConverter.memberConverter = this
        bodyConverter.defaultsOf = { references.defaultsOf(it) }
        bodyConverter.localDeclared = { psi, variable -> references.local(psi, variable) }
    }

    // Where each member names a project declaration (see KotlinReferenceRegistry). A project scan shares one
    // across its source sets; a standalone scan has its own.
    internal var references = KotlinReferenceRegistry()

    // > 0 while a body is being converted: a member built then (a local class's, an `object :` expression's)
    // records nothing, what is written in it is recorded on the member it is written in; a member built at 0 is a
    // reference host. Every member's commit waits in deferredCommits until every file's members exist
    // (commitDeferred): a host for its records, and any method for what it overrides.
    private var bodyDepth = 0
    private val deferredCommits = ArrayList<Pair<Info, () -> Unit>>()

    // a `$default` synthetic whose body waits for the members its defaults name (see defaultsMethod), with the
    // declaration it calls and that declaration's parameters; and the constructors among them, committed in finalizeType
    private class PendingDefaults(val target: MethodInfo, val parameters: List<KtParameter?>)
    private val pendingDefaults = java.util.IdentityHashMap<MethodInfo, PendingDefaults>()
    private val defaultsConstructors = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<MethodInfo, Boolean>())
    private val defaultsMethodOf = java.util.IdentityHashMap<MethodInfo, MethodInfo>()

    // ⛔ A BODY IS NOT A DECLARATION, AND IN A MIXED SOURCE SET THE DIFFERENCE IS THE WHOLE PARSE. Between [declare]
    // and [complete] every type, member signature, field and accessor exists, and no body does: bodies wait here,
    // each with the file whose session converts it. A driver runs another front end in the gap -- javac, whose Java
    // sources call into these signatures (through stubs generated from them) and whose members these bodies call.
    // Converting a body before that would resolve a call into Java against a type with no members yet: a
    // `k2-unresolved-call` placeholder, silently. A Kotlin-only parse gains too: no body is converted before
    // another FILE's signatures exist, where B1 used to run file by file.
    // ⛔ A WAITING BODY HOLDS POINTERS, NOT SYMBOLS. A symbol would stay valid (the standalone session's lifetime
    // tokens are always accessible), but it pins the analysis session it came from, and with it every FIR cache K2
    // would otherwise let go of: the kotlin-stdlib parse kept 500 MB live and failed on a 512 MB test heap, where
    // converting each body at once lets the collector bring it back to 224 MB.
    private var declaring = false
    private var declaringFile: KtFile? = null
    private val pendingBodies = ArrayList<Pair<KtFile, KaSession.() -> Unit>>()
    private val methodsWithPendingBody = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<MethodInfo, Boolean>())
    private var declared: List<FileConversion>? = null

    private fun <S : KaSymbol> KaSession.restore(pointer: KaSymbolPointer<S>): S =
        checkNotNull(pointer.restoreSymbol()) { "the symbol of a waiting body no longer resolves: $pointer" }

    /** Marks [method]'s body as waiting in [body], when it does: see [hasOrAwaitsBody]. */
    private fun awaitBody(method: MethodInfo) {
        if (declaring && bodyDepth == 0) methodsWithPendingBody += method
    }

    /** Runs [action] now, or, for a top-level declaration while [declare] runs, in [complete]. */
    private fun KaSession.body(action: KaSession.() -> Unit) {
        val file = declaringFile
        if (declaring && bodyDepth == 0 && file != null) pendingBodies += file to action else action()
    }

    /**
     * Whether [method] has, or will have once [complete] runs, a body: an implementation rather than an abstract
     * declaration. What a stub generated between [declare] and [complete] needs, where `methodBody()` is still null.
     */
    fun hasOrAwaitsBody(method: MethodInfo): Boolean =
        method in methodsWithPendingBody || runCatching { method.methodBody() }.getOrNull() != null

    private inline fun <T> inBody(block: () -> T): T {
        bodyDepth++
        try {
            return block()
        } finally {
            bodyDepth--
        }
    }

    /**
     * Commits [info] once every member exists ([commitDeferred]); a source-level member with a [psi] is a reference
     * host, and gets its records then.
     */
    private fun commitOrDefer(info: Info, psi: PsiElement?, commit: () -> Unit) {
        if (bodyDepth == 0 && psi != null) references.host(psi, info)
        deferredCommits += info to commit
    }

    // MemberConverter: an `object : Super { … }` body builds the anonymous type's members via these.
    override fun KaSession.buildAnonProperty(owner: TypeInfo, property: KaPropertySymbol) =
        convertProperty(owner, property)

    override fun KaSession.buildAnonMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo =
        convertMethod(owner, function)

    override fun KaSession.finishAnonMembers(owner: TypeInfo, declaration: KtObjectDeclaration) {
        convertInitializers(owner)
        convertInitBlocks(declaration, owner)
    }

    override fun KaSession.buildLocalType(enclosingMethod: MethodInfo, declaration: KtClassOrObject,
                                          outerLocals: Map<String, Variable>): TypeInfo =
        buildLocalTypeImpl(enclosingMethod, declaration, outerLocals)

    // Body conversion delegated to KotlinBodyConverter (KaSession member extensions -> with(…)).
    private fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType,
                                      method: MethodInfo, outerLocals: Map<String, Variable> = emptyMap()) =
        inBody { with(bodyConverter) { convertBody(function, returnType, method, outerLocals) } }

    private fun KaSession.convertExpression(expression: KtExpression, method: MethodInfo, locals: Map<String, Variable>) =
        inBody { with(bodyConverter) { convertExpression(expression, method, locals) } }

    private fun KaSession.convertInitBlock(init: KtAnonymousInitializer, method: MethodInfo, index: String) =
        inBody { with(bodyConverter) { convertInitBlock(init, method, index) } }

    private fun KaSession.convertStatement(statement: KtExpression, method: MethodInfo,
                                           locals: MutableMap<String, Variable>, index: String) =
        inBody { with(bodyConverter) { convertStatement(statement, method, locals, index) } }

    /**
     * Build a method-local type (`class C : A { … }` declared inside [enclosingMethod]'s body) as a full source
     * type: created under the method (so its FQN nests under the method, not a bad `<local>` compilation unit),
     * registered so `C()` resolves, hierarchy + members built exactly like a normal class, and its method bodies
     * capturing [outerLocals]. Mirrors the openjdk parser's `parseLocal`.
     */
    private fun KaSession.buildLocalTypeImpl(enclosingMethod: MethodInfo, declaration: KtClassOrObject,
                                             outerLocals: Map<String, Variable>): TypeInfo {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        val index = enclosingMethod.typeInfo().builder().getAndIncrementAnonymousTypes()
        val typeInfo = runtime.newTypeInfo(enclosingMethod, classSymbol.name.asString(), index)
        // declaration-site type parameters first (a bound may reference a sibling / itself), then bounds+variance
        val cstTypeParameters = classSymbol.typeParameters.mapIndexed { i, tp ->
            runtime.newTypeParameter(i, tp.name.asString(), typeInfo)
                .also { typeInfo.builder().addOrSetTypeParameter(it) } to tp
        }
        cstTypeParameters.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, typeInfo) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }
        typeInfo.builder().setEnclosingMethod(enclosingMethod)
        infoByFqn.put(typeInfo.fullyQualifiedName(), typeInfo, sourceSet)
        references.target(declaration, typeInfo)
        // a local type has no ClassId, so register it by PSI too -- a use-site `C()` resolves through this
        classSymbol.psi?.let { typeMapper.registerLocalType(it, typeInfo) }
        prepareType(declaration, typeInfo)
        convertMembers(declaration, typeInfo, outerLocals)
        finalizeType(declaration, typeInfo)
        return typeInfo
    }

    /** Parse one in-memory Kotlin source file and return its primary CST types (test convenience). */
    fun parse(fileName: String, content: String): List<TypeInfo> = parse(mapOf(fileName to content))

    /** Parse a set of in-memory Kotlin files (name -> content) in one shared session. */
    override fun parse(filesByName: Map<String, String>): List<TypeInfo> = parse(filesByName, emptyMap(), emptyList())

    override fun parse(filesByName: Map<String, String>, javaFilesByName: Map<String, String>): List<TypeInfo> =
        parse(filesByName, javaFilesByName, emptyList())

    /**
     * Parse Kotlin files, with optional accompanying Java SOURCE files ([javaFilesByName], path -> content,
     * e.g. `a/b/Foo.java`) laid into the same K2 source root so K2 resolves Java-source types referenced from
     * Kotlin (Phase 2 of the mixed-language integration). Only the Kotlin (`KtFile`) types are converted here;
     * the Java files are for K2 resolution — the referenced Java TypeInfo is reused from the shared registry /
     * CompiledTypesManager (built authoritatively by the Java front-end), not rebuilt from K2.
     */
    fun parse(filesByName: Map<String, String>, javaFilesByName: Map<String, String>,
              observers: List<KotlinParseObserver> = emptyList()): List<TypeInfo> {
        // Standalone resolves from source roots: lay the files down in a temp directory (Kotlin + Java).
        // ⚠ The directory is ours alone and dies with the call: conversion below is eager, so nothing in the
        // returned CST reads it again (only the CompilationUnit URI still names it). Leaving it behind cost
        // us a whole test run — /tmp is a tmpfs with a hard inode cap, and one dir per parse() exhausted it.
        val srcRoot = Files.createTempDirectory("k2-src")
        // disposed with the temporary sources: an undisposed session stays reachable (see KotlinProjectScan.parse)
        val disposable = Disposer.newDisposable("maddi KotlinScan")
        try {
            (filesByName + javaFilesByName).forEach { (name, content) ->
                val file = srcRoot.resolve(name)
                Files.createDirectories(file.parent ?: srcRoot)
                Files.writeString(file, content)
            }
            val session = buildSession(disposable, srcRoot).also { it.registerKDocResolution() }
            val ktFiles = session.modulesWithFiles.values.flatten().filterIsInstance<KtFile>()
            val types = convert(ktFiles)
            observers.filterIsInstance<K2ParseObserver>().forEach { it.observe(runtime, ktFiles, types) { sourceSet.name() } }
            return types
        } finally {
            Disposer.dispose(disposable)
            srcRoot.toFile().deleteRecursively()
        }
    }

    /**
     * Build a standalone session over [srcRoot], with the running JVM's JDK and this process's classpath
     * (kotlin-stdlib, etc.) as library dependencies, so library types resolve to real symbols.
     */
    private fun buildSession(disposable: Disposable, srcRoot: java.nio.file.Path) =
        buildStandaloneAnalysisAPISession(disposable) {
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
        declare(ktFiles)
        return complete()
    }

    /**
     * Passes A and B1 without bodies: every type of [ktFiles] registered, with its hierarchy, constructors, fields,
     * accessors and method signatures, uncommitted. Bodies wait for [complete]; see [body]. Returns the types.
     */
    fun declare(ktFiles: List<KtFile>): List<TypeInfo> {
        check(declared == null) { "declare() twice without complete()" }
        facadeByFqn.clear()
        pendingDelegates.clear()
        pendingDelegatesOf.clear()
        ktFiles.forEach { f -> f.virtualFile?.url?.let { references.projectFiles += it } }
        // before anything resolves: index the `typealias` declarations, so an `expect` type realised by an
        // `actual typealias` maps to its expansion rather than minting a shell for a name no JVM class has
        typeMapper.registerTypeAliases(ktFiles)
        // bootstrap: populate the predefined java.lang.Object with its real members (equals/hashCode/toString/
        // …) once, so source types resolve inherited-from-Object calls (mirrors openjdk's ScanCompilationUnits)
        ktFiles.firstOrNull()?.let { analyze(it) { bootstrapObject(); bootstrapString() } }
        // pass A (all files): create + register every type so cross-file references resolve
        val perFile = ktFiles.map { ktFile ->
            val compilationUnit = compilationUnitFor(ktFile)
            // Kotlin-multiplatform: an `expect` declaration (commonMain) is realised by an `actual` of the same
            // FQN (jvmMain); on the JVM the `actual` is authoritative, so drop the `expect` here to avoid a
            // "Duplicating type" collision (e.g. kotlin.Annotation is expect in common, actual in jvm)
            val declarations = ktFile.declarations.filterIsInstance<KtClassOrObject>()
                .filterNot { it.hasModifier(KtTokens.EXPECT_KEYWORD) }
            // top-level functions/properties live on the JVM file facade `<FileName>Kt` (a static container)
            val topLevelFunctions = ktFile.declarations.filterIsInstance<KtNamedFunction>()
                .filterNot { it.hasModifier(KtTokens.EXPECT_KEYWORD) }
            val topLevelProperties = ktFile.declarations.filterIsInstance<KtProperty>()
                .filterNot { it.hasModifier(KtTokens.EXPECT_KEYWORD) }
            val hasFacade = topLevelFunctions.isNotEmpty() || topLevelProperties.isNotEmpty()
            // flatten top-level + nested classes into parallel (declaration, type) lists for the later passes
            val (pairs, facade) = analyze(ktFile) {
                declarations.flatMap { registerTypeTree(compilationUnit, null, it) } to
                        (if (hasFacade) registerFacade(compilationUnit, ktFile) else null)
            }
            // imports and file annotations are written in the file, outside any member
            (facade ?: pairs.firstOrNull()?.second)?.let { references.host(ktFile, it) }
            FileConversion(ktFile, compilationUnit, pairs.map { it.first }, pairs.map { it.second },
                facade, topLevelFunctions, topLevelProperties)
        }
        // pass B1 (all files): members. B1a (prepareType) adds hierarchy + constructor structures for EVERY
        // type first, then B1b (convertMembers) declares members -- their bodies wait, see [body].
        declaring = true
        try {
            perFile.forEach { fc ->
                declaringFile = fc.ktFile
                analyze(fc.ktFile) {
                    fc.declarations.zip(fc.types).forEach { (d, ti) -> prepareType(d, ti) }
                    // facade SIGNATURES first (so a class method can call a top-level function like `gcd(...)`),
                    // then class members, then facade BODIES (so an extension function body can read its receiver's
                    // members / call a class method -- those class members now exist)
                    val facadeBodies = fc.facade
                        ?.let { convertFacadeSignatures(it, fc.topLevelFunctions, fc.topLevelProperties) } ?: emptyList()
                    fc.declarations.zip(fc.types).forEach { (d, ti) -> convertMembers(d, ti) }
                    facadeBodies.forEach { (method, _) -> awaitBody(method) }
                    val facadePointers = facadeBodies.map { (method, sym) -> method to sym.createPointer() }
                    body {
                        facadePointers.forEach { (method, pointer) -> finishMethodBody(restore(pointer), method) }
                        fc.facade?.let { convertInitializers(it) } // top-level `val x = …`, `val x by lazy { … }`
                    }
                }
            }
        } finally {
            declaring = false
            declaringFile = null
        }
        declared = perFile
        return perFile.flatMap { it.allTypes() }.distinct()
    }

    /** The bodies [declare] left waiting, then references, overrides and commits (pass B2). Returns the types. */
    fun complete(): List<TypeInfo> {
        val perFile = checkNotNull(declared) { "complete() without declare()" }
        declared = null
        val waiting = pendingBodies.toList()
        pendingBodies.clear()
        var i = 0
        while (i < waiting.size) {
            val file = waiting[i].first
            var j = i
            while (j < waiting.size && waiting[j].first === file) j++
            val run = waiting.subList(i, j)
            analyze(file) { run.forEach { (_, action) -> action() } }
            i = j
        }
        methodsWithPendingBody.clear()
        // every type now has its members, so a delegate declared after its user resolves: fill the accessors
        drainDelegatedProperties()
        // ...and every declaration a reference can name exists: record them, then commit the members that waited
        recordReferences(perFile)
        commitDeferred()
        // pass B2 (all files): wire constructor this(...)/super(...) delegations — now every constructor
        // exists, so even super() to another source type resolves — then commit.
        val committedFacades = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<TypeInfo, Boolean>())
        perFile.forEach { fc ->
            analyze(fc.ktFile) { fc.declarations.zip(fc.types).forEach { (d, ti) -> finalizeType(d, ti) } }
            val facade = fc.facade
            // a @JvmMultifileClass facade is shared by several files: commit it once (its first file owns it)
            // and list it only in that file's compilation unit
            val ownsFacade = facade != null && committedFacades.add(facade)
            if (ownsFacade) {
                references.attach(runtime, facade!!)
                facade.builder().commit()
            }
            fc.compilationUnit.setTypes(if (facade != null && !ownsFacade) fc.types else fc.allTypes())
        }
        drainDelegatedProperties() // a delegated property built while B2 converted a body is finished here
        commitDeferred() // ...and a member built there (an `object :` in a constructor argument, say)
        typeMapper.commitShells() // the library types no body reached from a shallower depth
        return perFile.flatMap { it.allTypes() }.distinct()
    }

    /**
     * Records every project reference of these files on its host member (see [KotlinReferenceRegistry]). The hosts
     * pick theirs up as they commit: the members in [commitDeferred], the constructors, classes and facades in
     * pass B2.
     */
    private fun recordReferences(perFile: List<FileConversion>) {
        val walker = KotlinReferenceWalker(references.projectFiles)
        perFile.forEach { fc ->
            val owner = fc.allTypes().firstOrNull()
            analyze(fc.ktFile) {
                with(references) {
                    record(runtime, fc.ktFile, walker) { symbol -> owner?.let { javaSourceTarget(symbol, it) } }
                }
            }
        }
    }

    /**
     * The Java front end's declaration behind a Java-source [symbol] that a Kotlin reference resolves to, in a mixed
     * project: its type, and on it the method or constructor of that name whose parameters map to the same types, or
     * the field. Null when there is none, or more than one. [owner] is where type parameters are looked up.
     */
    private fun KaSession.javaSourceTarget(symbol: KaSymbol, owner: TypeInfo): Info? {
        if (symbol.origin != KaSymbolOrigin.JAVA_SOURCE) return null
        return when (symbol) {
            is KaNamedClassSymbol -> javaSourceType(symbol.classId, owner)
            is KaConstructorSymbol -> javaSourceType(symbol.containingClassId, owner)
                ?.let { matching(it.constructors(), symbol.valueParameters, owner) }
            is KaNamedFunctionSymbol -> javaSourceType(symbol.callableId?.classId, owner)?.let { type ->
                matching(type.methods().filter { it.name() == symbol.name.asString() }, symbol.valueParameters, owner)
            }
            is KaJavaFieldSymbol -> javaSourceType(symbol.callableId?.classId, owner)
                ?.fields()?.firstOrNull { it.name() == symbol.name.asString() }
            else -> null
        }
    }

    private fun KaSession.javaSourceType(classId: ClassId?, owner: TypeInfo): TypeInfo? {
        classId ?: return null
        infoByFqn.getType(classId.asFqNameString(), sourceSet)?.let { return it }
        val classSymbol = findClass(classId) as? KaNamedClassSymbol ?: return null
        return mapType(buildClassType(classSymbol), owner).typeInfo()
    }

    private fun KaSession.matching(candidates: List<MethodInfo>, parameters: List<KaValueParameterSymbol>,
                                   owner: TypeInfo): MethodInfo? {
        val byArity = candidates.filter { it.parameters().size == parameters.size }
        if (byArity.size <= 1) return byArity.singleOrNull()
        return byArity.singleOrNull { m ->
            m.parameters().zip(parameters).all { pair ->
                pair.first.parameterizedType().typeInfo() == mapType(pair.second.returnType, owner).typeInfo()
            }
        }
    }

    /** Commits the members that waited for every member to exist, each with its records and overrides. */
    private fun commitDeferred() {
        val waiting = deferredCommits.toList()
        deferredCommits.clear()
        waiting.forEach { (member, commit) ->
            // every project method exists now, so what a method overrides can be computed -- it never was for
            // Kotlin, which left every override family (and the graph's methodsThatOverride) empty. An `object :`
            // expression's and a local class's methods included: they are how a family reaches into a body.
            if (member is MethodInfo && !member.isConstructor && !member.isStatic) {
                member.builder().addOverrides(runtime.computeMethodOverrides().overrides(member))
            }
            references.attach(runtime, member)
            commit()
        }
    }

    private class FileConversion(
        val ktFile: KtFile,
        val compilationUnit: CompilationUnit,
        val declarations: List<KtClassOrObject>,
        val types: List<TypeInfo>,
        val facade: TypeInfo?,
        val topLevelFunctions: List<KtNamedFunction>,
        val topLevelProperties: List<KtProperty>,
    ) {
        fun allTypes(): List<TypeInfo> = if (facade == null) types else types + facade
    }

    private fun compilationUnitFor(ktFile: KtFile): CompilationUnit {
        val packageName = ktFile.packageFqName.asString()
        val builder = runtime.newCompilationUnitBuilder()
            .setPackageName(packageName)
            .setURI(URI.create(ktFile.virtualFile?.url ?: "memory:/${ktFile.name}"))
            .setSourceSet(sourceSet)
        // package-name detail keyed by the package String (mirroring Java), when there is a package directive
        ktFile.packageDirective?.takeUnless { it.isRoot }?.packageNameExpression?.let { packageExpression ->
            builder.setSource(sourceOf(runtime, ktFile, "-").withDetailedSources(
                runtime.newDetailedSourcesBuilder().put(packageName, sourceOf(runtime, packageExpression, "-")).build()))
        }
        // the import directives, as the Java front end records them (ScanCompilationUnit.parseImportStatement).
        // A reference recorded at an import's NAME is not the same thing: it gives the simple name's position, which
        // is enough to RENAME the imported declaration and not enough to say what the line imports, where it is, or
        // to move it to another package.
        for (directive in ktFile.importDirectives) {
            val fqName = directive.importedFqName ?: continue // a broken import names nothing
            // `import a.b.*` has `a.b` as its fqName; spell the star out, as Java's importString does
            val importString = if (directive.isAllUnder) "${fqName.asString()}.*" else fqName.asString()
            builder.addImportStatement(
                runtime.newImportStatementBuilder()
                    .setSource(sourceOf(runtime, directive, "-"))
                    .setImport(importString)
                    // Kotlin has no `import static`: one import reaches a class, a top-level function and a
                    // companion member alike, and nothing in the syntax distinguishes them
                    .setIsStatic(false)
                    .setAlias(directive.aliasName)
                    .build()
            )
        }
        return builder.build()
    }

    /** Pass A: create the [TypeInfo] and its type parameters, and register it by FQN. No members yet. */
    private fun KaSession.registerType(compilationUnit: CompilationUnit, enclosing: TypeInfo?, declaration: KtClassOrObject): TypeInfo {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        val name = classSymbol.name.asString()
        // a nested class is created under its enclosing type (Outer.Nested) and added as a subtype
        val typeInfo = if (enclosing == null) runtime.newTypeInfo(compilationUnit, name)
        else runtime.newTypeInfo(enclosing, name).also { enclosing.builder().addSubType(it) }

        // declaration-site type parameters: register all first (so a bound can reference any of them, incl.
        // itself -- `T : Comparable<T>`), then set bounds + variance (out T / in T) and commit
        val cstTypeParameters = classSymbol.typeParameters.mapIndexed { index, tp ->
            runtime.newTypeParameter(index, tp.name.asString(), typeInfo)
                .also { typeInfo.builder().addOrSetTypeParameter(it) } to tp
        }
        cstTypeParameters.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, typeInfo) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }
        infoByFqn.put(typeInfo.fullyQualifiedName(), typeInfo, sourceSet)
        references.target(declaration, typeInfo)
        return typeInfo
    }

    /**
     * Register [declaration] and its nested classes recursively, returning flat (declaration, TypeInfo) pairs in
     * outer-before-nested order for the later passes. A companion object is converted with its class
     * ([convertCompanion]: it gets a singleton field, forwarders), so it is not a pair -- but it is REGISTERED here,
     * and the classes nested in it are pairs.
     *
     * ⛔ Registering it only when its class's members were converted left a window, and nothing registered the
     * classes nested in a companion at all. A signature naming one -- javalin's `Endpoint.Companion.EndpointBuilder`
     * -- missed the registry and fell through to the shared compiled-type manager, whose javac found the BUILD's
     * `Endpoint${'$'}Companion.class` in `target/classes` and committed a bytecode copy of a type this scan was about to
     * build from source: "Inspection of io.javalin.router.Endpoint.Companion has already been committed".
     */
    private fun KaSession.registerTypeTree(compilationUnit: CompilationUnit, enclosing: TypeInfo?,
                                           declaration: KtClassOrObject): List<Pair<KtClassOrObject, TypeInfo>> {
        val typeInfo = registerType(compilationUnit, enclosing, declaration)
        val nested = nestedClasses(declaration).flatMap { registerTypeTree(compilationUnit, typeInfo, it) }
        val companionPairs = declaration.declarations.filterIsInstance<KtObjectDeclaration>()
            .firstOrNull { it.isCompanion() }
            ?.let { companion ->
                val name = companion.name ?: "Companion"
                val companionType = runtime.newTypeInfo(typeInfo, name)
                typeInfo.builder().addSubType(companionType)
                infoByFqn.put(companionType.fullyQualifiedName(), companionType, sourceSet)
                registeredCompanions[typeInfo] = companionType
                nestedClasses(companion).flatMap { registerTypeTree(compilationUnit, companionType, it) }
            } ?: emptyList()
        return listOf(declaration to typeInfo) + nested + companionPairs
    }

    // enum entries are KtClassOrObject too, but they're constants (handled with the enum), not classes
    private fun nestedClasses(declaration: KtClassOrObject): List<KtClassOrObject> =
        declaration.declarations.filterIsInstance<KtClassOrObject>()
            .filterNot { it is KtEnumEntry || (it is KtObjectDeclaration && it.isCompanion()) }

    // a companion registered in pass A, by its class; set up in prepareType, converted in convertCompanion
    private val registeredCompanions = java.util.IdentityHashMap<TypeInfo, TypeInfo>()

    // @JvmMultifileClass facades shared across files in this convert() run: FQN -> the single facade TypeInfo
    private val facadeByFqn = HashMap<String, TypeInfo>()

    // delegated properties awaiting their initializer + accessor bodies: see drainDelegatedProperties
    private val pendingDelegates = mutableListOf<PendingDelegate>()
    // ...and the same, by owner, for convertDelegateInitializers
    private val pendingDelegatesOf = java.util.IdentityHashMap<TypeInfo, MutableList<PendingDelegate>>()

    // property initializers awaiting conversion, while their owner is open and its members exist: see
    // convertInitializers. One its owner never reaches keeps the empty initializer it was created with.
    // ⚠ BY OWNER. A flat list scanned per owner was harmless while each type drained its own right after declaring
    // them; with every body waiting for complete() it held all of a source set's initializers at once, and copying
    // and scanning it per type made the kotlin-stdlib parse quadratic: 26 s became 189 s and then a heap failure.
    private val pendingInitializers = java.util.IdentityHashMap<TypeInfo, MutableList<PendingInitializer>>()
    private class PendingInitializer(val owner: TypeInfo, val field: FieldInfo, val expression: KtExpression,
                                     val static: Boolean)

    // the constructor an instance property's initializer and an init block run in, per type: the primary one, else
    // the first that calls super rather than this(...)
    private val runsInitOf = java.util.IdentityHashMap<TypeInfo, MethodInfo>()

    // init blocks converted in pass B1, per constructor, waiting for the rest of its body: see convertInitBlocks
    private val initBlocksOf = java.util.IdentityHashMap<MethodInfo, PlannedInitBlocks>()
    private class PlannedInitBlocks(val prefix: Int, val total: Int, val blocks: List<Statement>)

    /** Pass A: create + register the file-facade [TypeInfo] `<FileName>Kt`. Members are added in pass B1. */
    private fun registerFacade(compilationUnit: CompilationUnit, ktFile: KtFile): TypeInfo {
        val facade = runtime.newTypeInfo(compilationUnit, facadeSimpleName(ktFile))
        val fqn = facade.fullyQualifiedName()
        // @JvmMultifileClass: several files contribute to ONE facade (e.g. kotlin.collections.CollectionsKt
        // spans 10 files). Reuse the facade registered by the first file; the others add their top-level
        // members to it in pass B1, and it is committed once (see convert's pass B2).
        facadeByFqn[fqn]?.let { return it }
        facadeByFqn[fqn] = facade
        infoByFqn.put(fqn, facade, sourceSet)
        return facade
    }

    // facadeSimpleName / jvmNameOverride are top-level functions now (see KotlinNaming.kt).

    /**
     * Pass B1: the file facade is a final public class holding the top-level functions and properties as
     * static members (the JVM emits top-level declarations exactly this way).
     */
    /**
     * Pass B1a: the facade's type setup, its property members, and its function SIGNATURES (no bodies).
     * Returns the (method, symbol) pairs whose bodies are converted later by [finishMethodBody] -- AFTER the
     * class members exist, so a top-level/extension function body can call a class method or read a receiver
     * member. (Class bodies, converted in between, can already see these facade signatures.)
     */
    private fun KaSession.convertFacadeSignatures(facade: TypeInfo, functions: List<KtNamedFunction>,
                                                  properties: List<KtProperty>): List<Pair<MethodInfo, KaNamedFunctionSymbol>> {
        facade.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierFinal())
            .computeAccess() // top-level: eventual access == the public modifier; needed before members
        properties.forEach { p ->
            (p.symbol as? KaPropertySymbol)?.let { convertProperty(facade, it, static = true) }
        }
        return functions.mapNotNull { fn ->
            (fn.symbol as? KaNamedFunctionSymbol)?.let { sym ->
                convertMethodSignature(facade, sym, static = true).also { facade.builder().addMethod(it) } to sym
            }
        }
    }

    /**
     * Pass B1a: hierarchy, access, declaration source, and constructor STRUCTURES. Run for every type
     * BEFORE any method body is converted (in [convertMembers]), so a body can resolve a forward reference
     * to another same-file type -- e.g. `Outer.make()` returning `Inner()`, or a self-`new` like
     * `Counter.plus` returning `Counter(...)`.
     */
    private fun KaSession.prepareType(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        // hierarchy first, so method bodies can resolve inherited callees via parentClass/interfaces
        applyHierarchy(typeInfo.builder(), typeInfo, classSymbol)
        annotate(typeInfo.builder(), classSymbol, typeInfo)
        typeInfo.builder().computeAccess() // eventual type access, needed before members' computeAccess()
        // ...and before its companion's, which a class nested in the companion needs in ITS prepareType, next
        registeredCompanions[typeInfo]?.let { setUpCompanion(it) }
        // declaration source (nature is set now): name keyed by simpleName(), keyword by typeNature(), and
        // each supertype reference keyed by its TypeInfo (the `: Base(), Iface` clause) -- mirroring Java
        val superTypeDetails = declaration.superTypeListEntries.mapNotNull { entry ->
            entry.typeReference?.let { reference -> mapType(reference.type, typeInfo) to reference }
        }
        typeInfo.builder().setSource(declarationSource(declaration) {
            putPsi(runtime, typeInfo.simpleName(), declaration.nameIdentifier)
            putPsi(runtime, typeInfo.typeNature(), declaration.getDeclarationKeyword())
            attachModifiers(runtime, declaration) { typeModifierFor(it) }
            superTypeDetails.forEach { (superType, reference) -> putTypeReference(runtime, superType, reference) }
        })
        if (bodyDepth == 0) references.host(declaration, typeInfo) // attached in finalizeType
        // constructor structures (params); bodies + delegations are wired in pass B2 (finalizeType)
        var runsInit: MethodInfo? = null
        val declared = ArrayList<Pair<KaConstructorSymbol, MethodInfo>>()
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaConstructorSymbol>()
            .forEach { ctor ->
                val constructor = convertConstructorStructure(typeInfo, ctor)
                declared += ctor to constructor
                // an implicit constructor's psi is the CLASS: only a written one is a declaration of its own, so a
                // call to an implicit one records the class, which is what the call spells anyway
                val ctorPsi = ctor.psi as? KtConstructor<*>
                references.target(ctorPsi, constructor)
                if (bodyDepth == 0) references.host(ctorPsi, constructor) // attached in finalizeType
                typeInfo.builder().addConstructor(constructor)
                // the primary constructor; without one, init code runs in each constructor that calls super rather
                // than this(...): the first of those is the one it is converted into (twice would declare twice)
                val callsThis = (ctor.psi as? KtSecondaryConstructor)?.getDelegationCall()?.isCallToThis == true
                if (ctor.isPrimary || runsInit == null && !callsThis) runsInit = constructor
            }
        // after the declared constructors, which finalizeType pairs with their symbols by position
        declared.forEach { (ctor, constructor) ->
            defaultsConstructor(typeInfo, ctor, constructor)?.let { overloadConstructors(typeInfo, ctor, constructor, it) }
        }
        if (bodyDepth == 0) declared.forEach { (ctor, constructor) -> recordDelegation(declaration, ctor, constructor) }
        // an overload's `this(...)` is into its target, and throws what the target's own delegation throws
        if (bodyDepth == 0) overloadTargets.forEach { (overload, target) ->
            if (overload.typeInfo() === typeInfo) delegationOf[overload] = ConstructorDelegation(false,
                target.parameters().map { p -> p.parameterizedType().takeIf { it.typeParameter() == null } },
                delegationOf[target]?.thrown ?: emptyList())
        }
        runsInit?.let { runsInitOf[typeInfo] = it }
        // an `init` block is code of that constructor (convertInitBlocks): what it names is recorded there, where
        // the graph looks for a caller, rather than falling through to the class
        if (bodyDepth == 0) runsInit?.let { ctor -> declaration.getAnonymousInitializers().forEach { references.host(it, ctor) } }
        // so are a superclass constructor call in the header (`: Base(x = 1)`) and a delegation (`: I by d`): kotlinc
        // compiles them into the primary constructor, and the CST converts the call there, as its `super(...)`
        if (bodyDepth == 0) runsInit?.let { ctor ->
            declaration.superTypeListEntries
                .filter { it is KtSuperTypeCallEntry || it is KtDelegatedSuperTypeEntry }
                .forEach { references.host(it, ctor) }
        }
    }

    /**
     * Pass B1b: convert members (properties, methods + bodies, synthetics) and register nested singletons.
     * [outerLocals] is non-empty only for a method-local type, so its method bodies capture the enclosing
     * method's parameters/locals.
     */
    private fun KaSession.convertMembers(declaration: KtClassOrObject, typeInfo: TypeInfo,
                                         outerLocals: Map<String, Variable> = emptyMap()) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        // properties (+ enum entry fields) before methods, so a method body can reference them
        val isObject = classSymbol.classKind == KaClassKind.OBJECT
        recordWrittenSignatures(typeInfo, classSymbol)
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaPropertySymbol>()
            .forEach { property -> convertProperty(typeInfo, property, static = isObject && isJvmStatic(property)) }
        // enum: entry fields + synthetic name()/values()/valueOf() (K2 doesn't surface these). Before the
        // methods, so an enum method body can reference `HIGH` etc.
        if (classSymbol.classKind == KaClassKind.ENUM_CLASS) addEnumMembers(typeInfo, declaration)
        // method SIGNATURES first, then bodies -- so a method body can call a sibling declared later (or itself).
        // `declarations` is a lazy Sequence: without toList() each signature was followed by its body, and a call to
        // a sibling declared later was a placeholder.
        val pendingMethods = classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { function ->
                val static = isObject && function.annotations.contains(JVM_STATIC)
                convertMethodSignature(typeInfo, function, static).also { typeInfo.builder().addMethod(it) } to function
            }
            .toList()
        pendingMethods.forEach { (method, _) -> awaitBody(method) }
        val pendingPointers = pendingMethods.map { (method, function) -> method to function.createPointer() }
        body {
            pendingPointers.forEach { (method, pointer) -> finishMethodBody(restore(pointer), method, outerLocals) }
            // initializers, delegate expressions and init blocks now: the type is still open (a lambda mints an
            // anonymous type on its builder) and its own methods exist. See convertInitializers, convertInitBlocks.
            convertInitializers(typeInfo)
            convertInitBlocks(declaration, typeInfo)
        }
        addDelegatedMembers(declaration, typeInfo)
        // a data class gets synthetic structural equals/hashCode/toString (like a Java record), unless the
        // user declared them; componentN/copy/getters are already provided by K2's member scope
        if (classSymbol.isData) {
            val recordSynthetics = RecordSynthetics(runtime, typeInfo)
            val declared = classSymbol.declaredMemberScope.declarations.filterIsInstance<KaNamedFunctionSymbol>()
                .map { it.name.asString() to it.valueParameters.size }.toSet()
            if ("equals" to 1 !in declared) typeInfo.builder().addMethod(recordSynthetics.createEquals())
            if ("hashCode" to 0 !in declared) typeInfo.builder().addMethod(recordSynthetics.createHashCode())
            if ("toString" to 0 !in declared) typeInfo.builder().addMethod(recordSynthetics.createToString())
        }
        // sealed: record the permitted subclasses (the direct sealed inheritors), for exhaustiveness
        if (classSymbol.modality == KaSymbolModality.SEALED) {
            classSymbol.sealedClassInheritors.forEach { inheritor ->
                infoByFqn.getType(inheritor.classId?.asFqNameString() ?: return@forEach, sourceSet)
                    ?.let { typeInfo.builder().addPermittedType(it) }
            }
        }
        // companion object -> a nested `Companion` type + a static field on the enclosing class
        classSymbol.companionObject?.let { convertCompanion(typeInfo, it) }
        // a named object (singleton) gets a `public static final INSTANCE` field of its own type
        // the `$default` constructors' bodies, last: a default may call a companion's function
        body {
            typeInfo.constructors().forEach { c ->
                pendingDefaults.remove(c)?.let { c.builder().setMethodBody(defaultsBody(c, it.target, it.parameters)) }
            }
        }
        if (classSymbol.classKind == KaClassKind.OBJECT) {
            typeInfo.builder().addField(singletonField(typeInfo, "INSTANCE", typeInfo.asParameterizedType()))
        }
        // access + type commit happen in finalizeType (pass B2), after delegations are wired
    }

    /**
     * Enum synthetics (K2 doesn't surface these): a `public static final <Enum> NAME` field per entry, plus
     * the shared `EnumSynthetics` for `name()`/`values()`/`valueOf()` (same as the Java parser uses).
     */
    private fun addEnumMembers(typeInfo: TypeInfo, declaration: KtClassOrObject) {
        val enumType = typeInfo.asParameterizedType()
        declaration.declarations.filterIsInstance<KtEnumEntry>().forEach { entry ->
            val name = entry.name ?: return@forEach
            val field = runtime.newFieldInfo(name, true, enumType, typeInfo)
            field.builder()
                .addFieldModifier(runtime.fieldModifierPublic())
                .addFieldModifier(runtime.fieldModifierStatic())
                .addFieldModifier(runtime.fieldModifierFinal())
                .setInitializer(runtime.newEmptyExpression())
                // every Info has a source (the Java parsers give even a synthesized one noSource()): consumers such
                // as the text index read it unguarded
                .setSource(declarationSource(entry) { putPsi(runtime, field.name(), entry.nameIdentifier) })
                .computeAccess()
            // a reference to the entry (`Level.LOW`, `LOW` in a `when`) is recorded against this field, and the
            // entry's KDoc (whose links name project declarations) waits, as a property's, for every target to exist
            references.target(entry, field)
            commitOrDefer(field, entry) { field.builder().commit() }
            typeInfo.builder().addField(field)
        }
        EnumSynthetics(runtime, typeInfo, typeInfo.builder()).create()
    }

    /**
     * A `public static [final] <type> <name>` singleton field (the `INSTANCE`/`Companion` handle, and the
     * enclosing-class surface of a companion's `const val`/`@JvmField`). A `@JvmField var` is not final.
     */
    private fun singletonField(holder: TypeInfo, name: String, type: ParameterizedType,
                               final: Boolean = true): FieldInfo {
        val field = runtime.newFieldInfo(name, true, type, holder)
        val builder = field.builder()
            .addFieldModifier(runtime.fieldModifierPublic())
            .addFieldModifier(runtime.fieldModifierStatic())
        if (final) builder.addFieldModifier(runtime.fieldModifierFinal())
        builder.setInitializer(runtime.newEmptyExpression())
            .setSource(runtime.noSource()) // compiler-made: no text of its own
            .computeAccess().commit()
        return field
    }

    /**
     * Convert a Kotlin `companion object` to its JVM shape: a nested type `Outer.Companion` holding the
     * companion's members (as instance members of the singleton), plus a `public static final Companion`
     * field on the enclosing class. (`@JvmStatic`/`const` forwarders onto the enclosing class, and
     * `Outer.member()` call routing, are later refinements.)
     */
    private fun KaSession.convertCompanion(enclosing: TypeInfo, companionSymbol: KaNamedClassSymbol) {
        val name = companionSymbol.name?.asString() ?: "Companion"
        // registered in pass A (registerTypeTree) for a class of a file; a local class's is made here
        val companion = registeredCompanions.remove(enclosing) ?: runtime.newTypeInfo(enclosing, name).also {
            enclosing.builder().addSubType(it)
            infoByFqn.put(it.fullyQualifiedName(), it, sourceSet)
            setUpCompanion(it)
        }
        references.target(companionSymbol.psi, companion)
        // the private constructor kotlinc gives it, called once, from the enclosing class's static initializer. The
        // companion's properties are instance fields of it, so their initializers run there, as a class's run in its
        // primary constructor. Built as the Java parser builds a class's default constructor.
        val constructor = runtime.newConstructor(companion, runtime.methodTypeSyntheticConstructor())
        constructor.builder()
            .setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
            .setMethodBody(runtime.emptyBlock())
            .addMethodModifier(runtime.methodModifierPrivate())
            .setSynthetic(true).setSource(runtime.noSource()).commitParameters().computeAccess()
        companion.builder().addConstructor(constructor)
        runsInitOf[companion] = constructor
        commitOrDefer(constructor, null) { constructor.builder().commit() }

        recordWrittenSignatures(companion, companionSymbol)
        companionSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaPropertySymbol>()
            .forEach { property -> convertProperty(companion, property) }
        // signatures first, then bodies, as for a class (convertMembers)
        val pendingMethods = companionSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { function -> convertMethodSignature(companion, function).also { companion.builder().addMethod(it) } to function }
            .toList()
        pendingMethods.forEach { (method, _) -> awaitBody(method) }
        val companionPsi = companionSymbol.psi as? KtObjectDeclaration
        companionPsi?.let { declaration ->
            declaration.getAnonymousInitializers().forEach { references.host(it, constructor) }
        }
        val pendingPointers = pendingMethods.map { (method, function) -> method to function.createPointer() }
        body {
            pendingPointers.forEach { (method, pointer) -> finishMethodBody(restore(pointer), method) }
            convertInitializers(companion)
            companionPsi?.let { declaration ->
                convertInitBlocks(declaration, companion)
                initBlocksOf.remove(constructor)?.let { constructor.builder().setMethodBody(constructorBody(listOf(), it)) }
            }
            companion.builder().commit()
        }

        // the singleton handle: `public static final <Companion> Companion` on the enclosing class
        val companionField = singletonField(enclosing, name, companion.asParameterizedType())
        enclosing.builder().addField(companionField)
        addCompanionStatics(enclosing, companion, companionField, companionSymbol)
    }

    private fun setUpCompanion(companion: TypeInfo) {
        companion.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierStatic()) // a nested object is a static nested class on the JVM
            .addTypeModifier(runtime.typeModifierFinal())
            .computeAccess() // nested: combines with the (already-computed) enclosing access
    }

    /**
     * Surface companion members that the JVM also emits on the enclosing class, which is where Java names them
     * (javalin's `import static io.javalin.testtools.TestTool.TestLogsKey`, a `companion object { @JvmField val }`):
     *
     * - `const val` and `@JvmField val/var` → a `public static` field on the enclosing class. A `@JvmField var`
     *   is not final. The field is the JVM surface of a `@JvmField`, which has no accessors at all.
     * - `@JvmStatic` property → its ACCESSORS, as static forwarders; the field stays on the companion.
     * - `@JvmStatic fun` → a static forwarder method.
     *
     * A plain companion member gets nothing: Java must write `Outer.Companion.member`. Each member also keeps its
     * copy on the companion, which is what Kotlin resolves `Outer.Companion.member` against.
     */
    private fun KaSession.addCompanionStatics(enclosing: TypeInfo, companion: TypeInfo, companionField: FieldInfo,
                                              companionSymbol: KaNamedClassSymbol) {
        val properties = companionSymbol.declaredMemberScope.declarations.filterIsInstance<KaPropertySymbol>().toList()
        properties.filter { (it as? KaKotlinPropertySymbol)?.isConst == true || isJvmField(it) }
            .forEach { property ->
                enclosing.builder().addField(
                    singletonField(enclosing, property.name.asString(), mapType(property.returnType, enclosing),
                        final = property.isVal))
            }
        // a @JvmField has no accessors to forward; `const` is inlined at the call site, so neither has one either
        properties.filter { isJvmStaticProperty(it) }
            .forEach { property ->
                listOf(accessorName(property, false), accessorName(property, true)).forEach { name ->
                    companion.methods().firstOrNull { it.name() == name }
                        ?.let { staticForwarder(enclosing, companionField, it) }
                }
            }

        companionSymbol.declaredMemberScope.declarations.filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.annotations.contains(JVM_STATIC) }
            .forEach { function ->
                companionTarget(companion, enclosing, function)?.let { staticForwarder(enclosing, companionField, it) }
            }
    }

    /**
     * The companion's own method that a `@JvmStatic` [function] forwards to. ⛔ MATCHED ON PARAMETER TYPES, NOT
     * ARITY: javalin's `Validation` writes two one-argument `@JvmStatic collectErrors` overloads (a `vararg` and an
     * `Iterable`), and an arity match hands both the same target -- two forwarders of one signature, which is an
     * assertion in MethodMapImpl ("Two methods with the same FQN and return type?").
     */
    private fun KaSession.companionTarget(companion: TypeInfo, enclosing: TypeInfo,
                                          function: KaNamedFunctionSymbol): MethodInfo? {
        val name = (function.psi as? KtNamedFunction)?.let { jvmNameOverride(it) } ?: function.name.asString()
        val candidates = companion.methods()
            .filter { it.name() == name && it.parameters().size == function.valueParameters.size }
        if (candidates.size <= 1) return candidates.firstOrNull()
        // only to tell overloads apart: a type parameter maps by simple name, which need not agree across owners
        val parameters = function.valueParameters.map { p ->
            // a vararg's K2 returnType is the element type; the JVM/CST parameter is an array of it
            val elementType = mapType(p.returnType, enclosing)
            erasedName(if (p.isVararg) elementType.copyWithArrays(elementType.arrays() + 1) else elementType)
        }
        return candidates.firstOrNull { method ->
            method.parameters().map { erasedName(it.parameterizedType()) } == parameters
        }
    }

    /** A `@JvmField`: the field is the JVM surface, on the enclosing class, and there are no accessors. */
    private fun isJvmField(property: KaPropertySymbol): Boolean =
        property.backingFieldSymbol?.annotations?.contains(JVM_FIELD) == true
                || property.annotations.contains(JVM_FIELD)

    /** A `@JvmStatic` property: its ACCESSORS are also emitted on the enclosing class. A `@JvmField` has none. */
    private fun isJvmStaticProperty(property: KaPropertySymbol): Boolean =
        !isJvmField(property) && (property as? KaKotlinPropertySymbol)?.isConst != true
                && (property.annotations.contains(JVM_STATIC)
                || property.getter?.annotations?.contains(JVM_STATIC) == true)

    /**
     * A `public static` method on [enclosing] with [target]'s signature, delegating to `Companion.target(...)`:
     * what the JVM emits for a `@JvmStatic` member of a companion object.
     */
    private fun staticForwarder(enclosing: TypeInfo, companionField: FieldInfo, target: MethodInfo) {
        val forwarder = runtime.newMethod(enclosing, target.name(), runtime.methodTypeStaticMethod())
        val builder = forwarder.builder()
        val params = target.parameters().map { builder.addParameter(it.name(), it.parameterizedType()) }
        val returnType = target.returnType()
        builder.setReturnType(returnType)
            .addMethodModifier(runtime.methodModifierPublic())
            .addMethodModifier(runtime.methodModifierStatic())
            .commitParameters()
        val delegate = runtime.newMethodCallBuilder()
            .setObject(bodyConverter.singletonAccess(enclosing, companionField)).setObjectIsImplicit(false)
            .setMethodInfo(target).setParameterExpressions(params.map { bodyConverter.variableExpression(it) })
            .setConcreteReturnType(returnType).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
        val statement = if (returnType == runtime.voidParameterizedType())
            runtime.newExpressionAsStatement(delegate) else runtime.newReturnStatement(delegate)
        builder.setMethodBody(runtime.newBlockBuilder().addStatement(statement).build()).computeAccess().commit()
        enclosing.builder().addMethod(forwarder)
    }

    /** Pass B1: a constructor's structure — parameters only. Body/delegation come in [finalizeType]. */
    private fun KaSession.convertConstructorStructure(owner: TypeInfo, ctor: KaConstructorSymbol): MethodInfo {
        val constructor = runtime.newConstructor(owner, runtime.methodTypeConstructor())
        val builder = constructor.builder()
        ctor.valueParameters.forEach { p ->
            val type = mapType(p.returnType, owner)
            val parameterInfo = builder.addParameter(p.name.asString(), type)
            parameter(parameterInfo, p.psi as? KtParameter, type)
            annotate(parameterInfo.builder(), p, owner)
        }
        annotate(builder, ctor, owner)
        builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
        visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
        builder.commitParameters().computeAccess()
        return constructor
    }

    /**
     * Pass B2: wire each constructor's body — an [io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation]
     * for an explicit `this(...)`/`super(...)` (resolved against the now-complete set of constructors),
     * followed by the property-field assignments — then commit the constructors and the type.
     */
    private fun KaSession.finalizeType(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        val ctorSymbols = classSymbol.declaredMemberScope.declarations.filterIsInstance<KaConstructorSymbol>().toList()
        ctorSymbols.zip(typeInfo.constructors()).forEach { (sym, cst) ->
            val statements = mutableListOf<Statement>()
            explicitConstructorInvocation(typeInfo, declaration, sym, cst)?.let { statements.add(it) } // index "0"
            cst.parameters().forEach { param ->
                typeInfo.fields().firstOrNull { it.name() == param.name() }
                    ?.let { statements.add(assignFieldFromParam(typeInfo, it, param, false)) }
            }
            cst.builder().setMethodBody(constructorBody(statements, initBlocksOf.remove(cst)))
            references.attach(runtime, cst)
            cst.builder().commit()
        }
        typeInfo.constructors().filter { defaultsConstructors.remove(it) }.forEach { it.builder().commit() }
        references.attach(runtime, typeInfo)
        typeInfo.builder().commit() // access already computed in convertMembers (B1)
    }

    /**
     * A constructor's body: [prefix] (the `this(...)`/`super(...)` invocation, the parameter-property assignments),
     * then the `init` blocks [planned] for it in pass B1, whose statement indices were fixed then. The prefix is
     * indexed into the slots planned for it, right-aligned: a planned invocation that did not resolve leaves its slot
     * empty rather than shifting indices the blocks already carry.
     */
    private fun constructorBody(prefix: List<Statement>, planned: PlannedInitBlocks?): Block {
        val body = runtime.newBlockBuilder()
        if (planned == null) {
            prefix.forEachIndexed { i, s -> body.addStatement(bodyConverter.indexed(s, bodyConverter.pad(i, prefix.size))) }
        } else {
            val shift = planned.prefix - prefix.size
            check(shift >= 0) { "constructor body: ${prefix.size} statements before the init blocks, ${planned.prefix} planned" }
            prefix.forEachIndexed { i, s -> body.addStatement(bodyConverter.indexed(s, bodyConverter.pad(i + shift, planned.total))) }
            planned.blocks.forEach { body.addStatement(it) }
        }
        return body.build()
    }

    /**
     * Convert [declaration]'s `init` blocks, in pass B1, so that what they declare (an `object :` expression, its
     * overrides) exists before references are recorded. They are code of the constructor [runsInitOf] names -- which
     * is where kotlinc compiles them -- appended to its body after the invocation and the parameter-property
     * assignments, each a nested block with its own scope. The body itself is assembled in pass B2 (finalizeType),
     * so the prefix is counted here as finalizeType will build it. A type without a constructor (an `object :`
     * expression) gets them as the body of its instance initializer.
     */
    private fun KaSession.convertInitBlocks(declaration: KtClassOrObject, owner: TypeInfo) {
        val inits = declaration.getAnonymousInitializers()
        if (inits.isEmpty()) return
        val ctor = runsInitOf[owner]
        if (ctor == null) {
            val initializer = initializerMethod(owner, runtime.methodTypeInstanceInitializer(), "<init_0>") {
                it.isInstanceInitializer
            }
            val body = runtime.newBlockBuilder()
            inits.forEachIndexed { j, init -> body.addStatement(convertInitBlock(init, initializer, bodyConverter.pad(j, inits.size))) }
            initializer.builder().setMethodBody(body.build())
            return
        }
        val symbol = (declaration.symbol as? KaNamedClassSymbol)?.declaredMemberScope?.declarations
            ?.filterIsInstance<KaConstructorSymbol>()?.toList()?.getOrNull(owner.constructors().indexOf(ctor))
        val invocation = if (symbol != null && hasExplicitInvocation(declaration, symbol)) 1 else 0
        val prefix = invocation + ctor.parameters().count { p -> owner.fields().any { it.name() == p.name() } }
        val total = prefix + inits.size
        val blocks = inits.mapIndexed { j, init -> convertInitBlock(init, ctor, bodyConverter.pad(prefix + j, total)) }
        initBlocksOf[ctor] = PlannedInitBlocks(prefix, total, blocks)
    }

    /** Whether [ctor] is written with a `this(...)`/`super(...)` call, resolved or not: see [explicitConstructorInvocation]. */
    private fun hasExplicitInvocation(declaration: KtClassOrObject, ctor: KaConstructorSymbol): Boolean =
        when (val psi = ctor.psi) {
            is KtSecondaryConstructor -> !psi.getDelegationCall().isImplicit
            else -> declaration.superTypeListEntries.any { it is KtSuperTypeCallEntry }
        }

    /**
     * Build the `this(...)`/`super(...)` invocation for a constructor, or null if there is none (or it is unresolved).
     * Its arguments are ordered and completed as at any call: one omitting an argument invokes the target's `$default`
     * constructor.
     */
    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtCallElement)
    private fun KaSession.explicitConstructorInvocation(
        owner: TypeInfo, declaration: KtClassOrObject, ctor: KaConstructorSymbol, constructor: MethodInfo,
    ): Statement? {
        val (isSuper, call) = when (val psi = ctor.psi) {
            is KtSecondaryConstructor -> {
                val delegation = psi.getDelegationCall()
                if (delegation.isImplicit) return null // implicit super() — not represented
                !delegation.isCallToThis to (delegation as KtCallElement)
            }
            else -> { // primary constructor: an explicit super-type call `class Sub : Base(args)`
                val superCall = declaration.superTypeListEntries.filterIsInstance<KtSuperTypeCallEntry>().firstOrNull()
                    ?: return null
                true to (superCall as KtCallElement)
            }
        }
        // ⛔ never `return null` past this point: a dropped call takes its arguments' reads with it and leaves no
        // placeholder, so the census reports the constructor clean. An unbindable call is a NAMED placeholder instead.
        val targetType = (if (isSuper) owner.parentClass()?.typeInfo() else owner)
            ?.let { typeMapper.withMembers(it) } // a class-file parent may be a shell: see KotlinTypeMapper.withMembers
            ?: return unboundInvocation("k2-super-call-no-parent")
        val ordered = (call.resolveSymbol() as? KaConstructorSymbol)?.takeIf { s -> s.valueParameters.none { it.isVararg } }
            ?.let { s -> inBody { with(bodyConverter) { callArguments(call, s, constructor, emptyMap()) } } }
        val argExpressions = ordered?.expressions ?: call.valueArguments
            .mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e, constructor, emptyMap()) } }
        // else resolve the target constructor by arity (refine to full overload resolution later)
        val target = ordered?.defaults
            // not kotlinc's overloads (synthetic, and only Java's to call) -- but a Java class's GENERATED default
            // constructor is synthetic too, and it is the one `class D : V()` calls when V declares none
            ?: targetType.constructors().firstOrNull {
                (!it.isSynthetic || it.isSyntheticConstructor) && it.parameters().size == argExpressions.size
            }
            ?: return unboundInvocation("k2-super-call-unresolved:${targetType.simpleName()}")
        return runtime.newExplicitConstructorInvocationBuilder()
            .setIsSuper(isSuper)
            .setMethodInfo(target)
            .setParameterExpressions(argExpressions)
            .setSource(runtime.newParserSource("0", 0, 0, 0, 0)) // must be the first statement (index "0")
            .build()
    }

    /** A `this(...)`/`super(...)` that cannot be bound, as a placeholder statement in its place (index "0"). */
    private fun unboundInvocation(kind: String): Statement = runtime.newExpressionAsStatementBuilder()
        .setExpression(runtime.newEmptyExpression(kind))
        .setSource(runtime.newParserSource("0", 0, 0, 0, 0))
        .build()

    /**
     * What a constructor's `this(...)`/`super(...)` calls, for a Java stub made before any body exists: the call
     * itself is converted in pass B2 ([explicitConstructorInvocation]). A stub constructor without it calls the
     * parent's no-argument constructor, which javalin's `JavalinServletContext` -- and 90 more -- do not have.
     * A parameter typed by a type parameter is null: its substitution is the use site's, and a stub passes a bare
     * `null` for it. [thrown]: the checked exceptions of a Java or library parent constructor, which kotlinc never
     * has to declare and javac does (javalin's `LeveledBrotli4jStream` calls `BrotliOutputStream`'s, which throws
     * `IOException`).
     */

    private val delegationOf = java.util.IdentityHashMap<MethodInfo, ConstructorDelegation>()

    fun delegationOf(constructor: MethodInfo): ConstructorDelegation? = delegationOf[constructor]

    @OptIn(KaExperimentalApi::class) // resolveSymbol(KtCallElement)
    private fun KaSession.recordDelegation(declaration: KtClassOrObject, ctor: KaConstructorSymbol, constructor: MethodInfo) {
        val (isSuper, call) = when (val psi = ctor.psi) {
            is KtSecondaryConstructor -> {
                val delegation = psi.getDelegationCall()
                if (delegation.isImplicit) return
                !delegation.isCallToThis to (delegation as KtCallElement)
            }
            else -> true to (declaration.superTypeListEntries.filterIsInstance<KtSuperTypeCallEntry>().firstOrNull()
                ?: return)
        }
        val target = call.resolveSymbol() as? KaConstructorSymbol ?: return
        val owner = constructor.typeInfo()
        val parameterTypes = target.valueParameters.map { p ->
            if (p.returnType is KaTypeParameterType) null
            else mapType(p.returnType, owner).let { if (p.isVararg) it.copyWithArrays(it.arrays() + 1) else it }
        }
        // a bytecode parent is loaded with its members; a source parent (Kotlin, or Java not yet parsed) throws nothing
        // javac would ask about
        val parentConstructors = if (!isSuper) emptyList() else owner.parentClass()?.typeInfo()
            ?.takeIf { it.compilationUnit().externalLibrary() }
            ?.let { parent -> runCatching { parent.constructors() }.getOrDefault(emptyList()) }
            ?.filter { it.parameters().size == parameterTypes.size } ?: emptyList()
        // the one whose parameter types match; failing that -- a nested or generic parameter type can map to
        // another instance than the one the parent carries -- what EVERY parent constructor of that arity throws.
        // Intersecting, never adding: a `throws` the real constructor does not have would force a Java caller of
        // this Kotlin constructor to catch what it cannot throw (javalin's `LeveledBrotli4jStream`, whose parent
        // `BrotliOutputStream(OutputStream, Encoder.Parameters)` throws IOException).
        val matched = parentConstructors.firstOrNull { c ->
            c.parameters().zip(parameterTypes).all { (p, t) -> t == null || p.parameterizedType().typeInfo() == t.typeInfo() }
        }
        val exceptions = { c: MethodInfo -> runCatching { c.exceptionTypes() }.getOrDefault(emptyList()) }
        val thrown = when {
            matched != null -> exceptions(matched)
            parentConstructors.isEmpty() -> emptyList()
            else -> parentConstructors.map { exceptions(it).toSet() }
                .reduce { a, b -> a.intersect(b) }.toList()
        }
        delegationOf[constructor] = ConstructorDelegation(isSuper, parameterTypes, thrown)
    }

    /**
     * The JVM overloads kotlinc adds to a function or constructor with default values, as the parameter indices each
     * keeps: one per defaulted parameter under `@JvmOverloads`, dropping them from the last; and the no-argument
     * constructor of a primary constructor whose parameters all have defaults. Java calls them (javalin's
     * `new CompressionStrategy()`, `addWsHandler` without its roles). [offset]: the parameters before the value
     * parameters, an extension's receiver.
     */
    private fun overloadParameters(offset: Int, defaults: List<Boolean>, jvmOverloads: Boolean,
                                   primaryConstructor: Boolean): List<List<Int>> {
        val all = (0 until offset + defaults.size).toList()
        val defaulted = defaults.indices.filter { defaults[it] }.map { it + offset }
        return when {
            jvmOverloads -> (1..defaulted.size).map { drop -> all - defaulted.takeLast(drop).toSet() }
            primaryConstructor && defaults.isNotEmpty() && defaults.all { it } -> listOf(emptyList())
            else -> emptyList()
        }
    }

    /** Each overload constructor ([overloadConstructors]) and the constructor it stands for. */
    private val overloadTargets = java.util.IdentityHashMap<MethodInfo, MethodInfo>()

    /**
     * kotlinc's overloads of [target] ([overloadParameters]) as members of [owner]: synthetic, as a data class's
     * `copy()` is, since they spell nothing; each passes its parameters on to [defaults], [target]'s `$default`, with
     * the zero value and the mask bit of every one it leaves out -- the call kotlinc compiles into them. A Java call
     * that leaves the defaults out binds to one; a Kotlin call never does (it binds to [defaults] itself).
     */
    @OptIn(KaExperimentalApi::class) // contextParameters
    private fun KaSession.overloadMethods(owner: TypeInfo, function: KaNamedFunctionSymbol, target: MethodInfo,
                                          static: Boolean) {
        val defaults = defaultsMethodOf[target] ?: return
        val contexts = function.contextParameters
        val offset = contexts.size + if (function.receiverParameter != null) 1 else 0
        overloadParameters(offset, function.valueParameters.map { it.hasDefaultValue },
            function.annotations.contains(JVM_OVERLOADS), false).forEach { kept ->
            val method = runtime.newMethod(owner, target.name(), target.methodType())
            val builder = method.builder().setSynthetic(true)
            addTypeParameters(builder, function, owner, method)
            kept.forEach { i ->
                if (i < contexts.size) contexts[i].let { c -> syntheticParameter(builder, c.name.asString(), mapType(c.returnType, owner, method)) }
                else if (i < offset) syntheticParameter(builder, "\$receiver", mapType(function.receiverParameter!!.returnType, owner, method))
                else function.valueParameters[i - offset].let { p ->
                    syntheticParameter(builder, p.name.asString(), mapType(p.returnType, owner, method))
                }
            }
            builder.commitParameters()
                .setReturnType(mapType(function.returnType, owner, method))
                .setSource(runtime.noSource())
            visibilityMethodModifier(function)?.let { builder.addMethodModifier(it) }
            builder.addMethodModifier(when {
                static -> runtime.methodModifierStatic()
                owner.isInterface -> runtime.methodModifierDefault()
                else -> runtime.methodModifierFinal()
            })
            builder.computeAccess()
            builder.setMethodBody(overloadBody(method, defaults, kept, offset, function.valueParameters.size))
            owner.builder().addMethod(method)
            commitOrDefer(method, null) { method.builder().commit() }
        }
    }

    /** [overloadMethods], for a constructor: the body is `this(...)` into [defaults], the `$default` constructor. */
    private fun overloadConstructors(owner: TypeInfo, ctor: KaConstructorSymbol, target: MethodInfo, defaults: MethodInfo) {
        overloadParameters(0, ctor.valueParameters.map { it.hasDefaultValue }, ctor.annotations.contains(JVM_OVERLOADS),
            ctor.isPrimary).forEach { kept ->
            // a no-argument constructor written by hand is the one kotlinc keeps
            if (kept.isEmpty() && owner.constructors().any { !it.isSynthetic && it.parameters().isEmpty() }) return@forEach
            val constructor = runtime.newConstructor(owner, runtime.methodTypeConstructor())
            val builder = constructor.builder().setSynthetic(true)
            kept.forEach { i -> target.parameters()[i].let { syntheticParameter(builder, it.name(), it.parameterizedType()) } }
            builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor()).setSource(runtime.noSource())
            visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
            builder.commitParameters().computeAccess()
            builder.setMethodBody(overloadBody(constructor, defaults, kept, 0, ctor.valueParameters.size))
            owner.builder().addConstructor(constructor)
            overloadTargets[constructor] = target
            defaultsConstructors += constructor // committed with the `$default` constructors, after the declared ones
        }
    }

    /**
     * An overload's body: [defaults] called with the [overload]'s parameters where [kept] keeps them, the zero value of
     * the rest, their bits in the masks, and for a constructor the `null` marker.
     */
    private fun overloadBody(overload: MethodInfo, defaults: MethodInfo, kept: List<Int>, offset: Int,
                             valueParameters: Int): Block {
        val passed = defaults.parameters().subList(0, offset + valueParameters)
        val masks = IntArray(masks(valueParameters).size)
        val arguments = passed.mapIndexed { i, p ->
            val k = kept.indexOf(i)
            if (k >= 0) bodyConverter.variableExpression(overload.parameters()[k])
            else {
                masks[(i - offset) / 32] = masks[(i - offset) / 32] or (1 shl ((i - offset) % 32))
                runtime.nullValue(p.parameterizedType())
            }
        } + masks.map { runtime.newInt(it) } + if (overload.isConstructor) listOf(runtime.nullConstant()) else listOf()
        val statement = if (overload.isConstructor) {
            runtime.newExplicitConstructorInvocationBuilder().setIsSuper(false).setMethodInfo(defaults)
                .setParameterExpressions(arguments).setSource(runtime.noSource().withIndex("0")).build()
        } else {
            val owner = defaults.typeInfo()
            val call = runtime.newMethodCallBuilder()
                .setObject(if (defaults.isStatic) runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo())
                           else bodyConverter.variableExpression(runtime.newThis(owner.asParameterizedType())))
                .setObjectIsImplicit(!defaults.isStatic).setMethodInfo(defaults).setParameterExpressions(arguments)
                .setConcreteReturnType(defaults.returnType()).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
            bodyConverter.indexed(if (defaults.returnType() == runtime.voidParameterizedType())
                runtime.newExpressionAsStatement(call) else runtime.newReturnStatement(call), "0")
        }
        return runtime.newBlockBuilder().addStatement(statement).build()
    }

    /**
     * Whether [property], of an `object`, is static on the JVM, as the members Java names on the type are
     * (javalin's `Header.AUTHORIZATION`, `RouteOverviewUtil.getMetaInfo(handler)`): a `const val`, a `@JvmField`, and a
     * `@JvmStatic` property with its accessors. So are the object's `@JvmStatic` functions. The rest of an object's
     * members belong to its `INSTANCE`.
     */
    /**
     * The JVM signatures of the functions a type writes, by type: an accessor maddi would synthesize for one of its
     * properties is not synthesized when the type declares that signature itself. kotlinc gives a PRIVATE property no
     * accessors at all, so javalin's `private var routeRoles` plus its written `fun setRouteRoles(Set<RouteRole>)` are
     * one JVM method, and minting the setter too put two methods of one signature in the type (an assertion in
     * MethodMapImpl, and the stub had to dedupe them).
     */
    private val writtenSignatures = java.util.IdentityHashMap<TypeInfo, Set<String>>()

    private fun KaSession.recordWrittenSignatures(owner: TypeInfo, classSymbol: KaNamedClassSymbol) {
        writtenSignatures[owner] = classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .map { function ->
                val name = (function.psi as? KtNamedFunction)?.let { jvmNameOverride(it) } ?: function.name.asString()
                signature(name, function.valueParameters.map { mapType(it.returnType, owner) },
                    mapType(function.returnType, owner))
            }.toSet()
    }

    /**
     * A JVM signature: name, parameter types, AND return type -- the JVM keys a method by all three, and Kotlin uses
     * that. detekt's `YML` has `open val indent: Int` next to a `private fun getIndent(): String`, two methods
     * (MemberTest.anAccessorOverridesTheAccessorNotAFunctionOfTheSameJvmName); javalin's written
     * `setRouteRoles(Set<RouteRole>)` returns Unit, exactly as the setter maddi would synthesize.
     */
    private fun signature(name: String, parameterTypes: List<ParameterizedType>, returnType: ParameterizedType): String =
        name + parameterTypes.joinToString(",", "(", ")") { erasedName(it) } + ":" + erasedName(returnType)

    private fun erasedName(pt: ParameterizedType): String =
        (pt.typeInfo()?.fullyQualifiedName() ?: pt.typeParameter()?.simpleName() ?: "?") + "[]".repeat(pt.arrays())

    /** Whether [owner] writes a function with this accessor's JVM signature ([writtenSignatures]). */
    private fun isWritten(owner: TypeInfo, name: String, parameterTypes: List<ParameterizedType>,
                          returnType: ParameterizedType): Boolean =
        writtenSignatures[owner]?.contains(signature(name, parameterTypes, returnType)) == true

    private fun isJvmStatic(property: KaPropertySymbol): Boolean =
        (property as? KaKotlinPropertySymbol)?.isConst == true
                || property.backingFieldSymbol?.annotations?.contains(JVM_FIELD) == true
                || property.annotations.contains(JVM_FIELD)
                || property.annotations.contains(JVM_STATIC)
                || property.getter?.annotations?.contains(JVM_STATIC) == true

    /**
     * Convert a Kotlin property (`val`/`var`, incl. primary-constructor `val x: Int`) into a backing
     * [FieldInfo] plus accessor methods whose bodies maddi already recognises as getters/setters:
     * `getX() { return this.x; }` and (for `var`) `setX(v) { this.x = v; }`. Each accessor is tagged via
     * `runtime.setGetSetField`, so the analyzer's getter/setter normalisation treats Kotlin property
     * access identically to a Java field access. Two kinds have no backing field of their own and are handled
     * before that: a **delegated** property (`by`, see [convertDelegatedProperty]) and a **computed** one
     * (a custom `get()`).
     */
    private fun KaSession.convertProperty(owner: TypeInfo, property: KaPropertySymbol, static: Boolean = false) {
        val name = property.name.asString()
        val type = mapType(property.returnType, owner)
        val isVal = property.isVal

        // a delegated property (`by`) has no backing field either, but it is not computed: the state is real
        // and lives in the delegate object. Modelled before the computed branch, which would otherwise take it.
        if (property.isDelegatedProperty) {
            convertDelegatedProperty(owner, property, type, static)
            return
        }

        // a computed property (custom getter, no backing field, e.g. `val sum get() = x + y`) becomes just
        // a getter with its real body — no field, no getter/setter field tagging.
        if ((property as? KaKotlinPropertySymbol)?.hasBackingField == false) {
            owner.builder().addMethod(buildComputedGetter(owner, property, type, static))
            // a `var` has a setter too, written or abstract (#36)
            if (!isVal) owner.builder().addMethod(buildComputedSetter(owner, property, type, static))
            return
        }

        val field = runtime.newFieldInfo(name, static, type, owner)
        val fieldBuilder = field.builder()
            .addFieldModifier(runtime.fieldModifierPrivate())
            .setInitializer(runtime.newEmptyExpression()) // replaced by the converted one, see convertInitializers
        (property.psi as? KtProperty)?.initializer?.let {
            pendingInitializers.getOrPut(owner) { mutableListOf() } += PendingInitializer(owner, field, it, static)
        }
        if (isVal) fieldBuilder.addFieldModifier(runtime.fieldModifierFinal())
        if (static) fieldBuilder.addFieldModifier(runtime.fieldModifierStatic())
        annotate(fieldBuilder, property.backingFieldSymbol, owner)
        // name keyed by field.name(), type reference keyed by its TypeInfo -- mirroring the Java parser
        fieldBuilder.setSource(declarationSource(property.psi) {
            putPsi(runtime, field.name(), (property.psi as? KtNamedDeclaration)?.nameIdentifier)
            (property.psi as? KtModifierListOwner)?.let { attachModifiers(runtime, it) { t -> fieldModifierFor(t) } }
            (property.psi as? KtModifierListOwner)?.let { attachOverride(runtime, it) }
            putTypeReference(runtime, type, (property.psi as? KtCallableDeclaration)?.typeReference)
        })
        fieldBuilder.computeAccess()
        references.target(property.psi, field)
        commitOrDefer(field, property.psi) { field.builder().commit() }
        owner.builder().addField(field)

        // Getter: a `const val` (inlined static-final field) and a `private` property have no getter method on
        // the JVM -- only the backing field -- and a synthetic one clashes with an explicitly-declared getX()
        // of the same signature (DoubleCompanionObject.getMIN_VALUE; ParameterizedTypeImpl.getOwnerType, a
        // private val overriding a Java interface method). Setter: kept for every `var` (incl. private) -- the
        // finality analysis models a var's field-demoting setter (see TestFinalFieldBranchAssignment).
        // @JvmField: the field is the JVM surface, with no accessor either. One made anyway was named what K2 reports
        // for it -- the property's own name -- and collided with a written `fun sessionId()` (javalin's WsContext)
        val isConst = (property as? KaKotlinPropertySymbol)?.isConst == true
                || property.backingFieldSymbol?.annotations?.contains(JVM_FIELD) == true
                || property.annotations.contains(JVM_FIELD)
        val isPrivate = property.visibility == KaSymbolVisibility.PRIVATE
        // a written accessor body (`get() = field.trim()`, `set(v) { field = v.coerceAtLeast(0) }`) is converted:
        // kotlinc compiles it into the accessor, even for a private property. Only a default one is synthesized.
        val customGetter = (property.psi as? KtProperty)?.getter?.takeIf { it.hasBody() }
        val customSetter = (property.psi as? KtProperty)?.setter?.takeIf { it.hasBody() }
        // ... nor one whose JVM signature the type writes itself (see writtenSignatures)
        val hasGetter = !isConst && !isWritten(owner, accessorName(property, false), listOf(), type)
        val hasSetter = !isConst && !isVal
                && !isWritten(owner, accessorName(property, true), listOf(type), runtime.voidParameterizedType())
        if (hasGetter && customGetter != null) {
            owner.builder().addMethod(buildCustomAccessor(owner, field, type, property, static, customGetter))
        } else if (hasGetter && !isPrivate) {
            owner.builder().addMethod(buildGetter(owner, field, type, property, static))
        }
        if (hasSetter && customSetter != null) {
            owner.builder().addMethod(buildCustomAccessor(owner, field, type, property, static, customSetter))
        } else if (hasSetter) {
            owner.builder().addMethod(buildSetter(owner, field, type, property, static))
        }
    }

    /**
     * The PROPERTY's own name, at its name identifier, on an accessor of a property that has NO backing field:
     * an interface's `val x: T`, an abstract one, or a computed one.
     *
     * A backed property carries this on its [FieldInfo] (see the field's `declarationSource` above). An unbacked
     * one has no field at all, so its accessor is the only `Info` a rename of the property could edit -- and
     * without this detail that accessor has no recorded name position whatsoever, which is why renaming such a
     * property was refused outright ("a family mixing those with backed properties is not supported yet": 68
     * families on detekt, 4 on javalin).
     *
     * ⛔ Keyed by [DetailedSources.PROPERTY_NAME], not by the name itself: lookup is by object IDENTITY, and
     * `property.name.asString()` is a runtime String no caller could hold the same instance of -- where a
     * `FieldInfo`'s `name()` returns the very instance that was stored. And NEVER by the accessor's JVM name:
     * `getDisplayName` is spelled nowhere in Kotlin source, and `KotlinRenameMethod.spelledByItsName` decides
     * what counts as an overload from exactly that absence.
     */
    private fun DetailedSources.Builder.putPropertyName(property: KaPropertySymbol) {
        putPsi(runtime, DetailedSources.PROPERTY_NAME, (property.psi as? KtNamedDeclaration)?.nameIdentifier)
    }

    /**
     * A property's written getter or setter: its real body, where `field` is the backing field, and the setter's
     * parameter named as written. Not tagged as a getter/setter of the field: the analyzer's normalisation of
     * `getX() { return x; }` to a field read would hide what the body does. What its text names is recorded on it.
     */
    private fun KaSession.buildCustomAccessor(owner: TypeInfo, field: FieldInfo?, type: ParameterizedType,
                                              property: KaPropertySymbol, static: Boolean,
                                              accessor: KtPropertyAccessor): MethodInfo {
        val setter = accessor.isSetter
        // [field] is null for a property that has none (a computed `var`'s written setter, #36): the accessor is
        // named after the property, and `field` is not in scope -- Kotlin forbids it where there is no backing field
        val method = runtime.newMethod(owner, accessorName(property, setter), methodType(static, property, owner))
        val builder = method.builder()
        builder.setReturnType(if (setter) runtime.voidParameterizedType() else type)
        // an extension property's accessor is static, with the receiver first (the JVM model), as the getter has it
        if (field == null) contextParameters(builder, property, owner, null, synthetic = false)
        if (field == null) property.receiverParameter?.let {
            builder.addParameter("\$receiver", mapType(it.returnType, owner))
        }
        if (setter) {
            val psi = accessor.parameter
            val parameterInfo = builder.addParameter(psi?.name ?: "value", type)
            parameter(parameterInfo, psi, type)
            annotate(parameterInfo.builder(), property.setter?.parameter, owner)
        }
        annotate(builder, if (setter) property.setter else property.getter, owner)
        addMethodModifiers(builder, property)
        builder.commitParameters().computeAccess()
        // the whole-declaration source stays the accessor's own text; only the property's name is added, and only
        // where there is no field to carry it
        builder.setSource(declarationSource(accessor) { if (field == null) putPropertyName(property) })
        awaitBody(method)
        body {
            val scope = mutableMapOf<String, Variable>()
            field?.let { scope["field"] = runtime.newFieldReference(it, fieldAccessScope(owner, static), it.type()) }
            val block = runtime.newBlockBuilder()
            val expressionBody = accessor.bodyExpression.takeIf { accessor.bodyBlockExpression == null }
            if (expressionBody != null) {
                val value = convertExpression(expressionBody, method, scope)
                block.addStatement(bodyConverter.indexed(if (setter) runtime.newExpressionAsStatement(value)
                    else runtime.newReturnStatement(value), "0"))
            } else {
                val statements = accessor.bodyBlockExpression?.statements.orEmpty()
                statements.forEachIndexed { i, st -> block.addStatement(convertStatement(st, method, scope, bodyConverter.pad(i, statements.size))) }
            }
            builder.setMethodBody(block.build())
            commitOrDefer(method, accessor) { method.builder().commit() }
        }
        return method
    }

    /**
     * A delegated property (`val x: T by lazy { … }`, `var x: T by Delegates.observable(…)`), as the JVM has
     * it: a private final `x$delegate` field holding the **delegate object**, plus accessors that route every
     * read (and write) through it. The delegate's type is the field's type, so the state the property really
     * has is state the analyzer can see — where dropping the property made its owner come out one immutability
     * level ABOVE the same value written explicitly, the holder now matches the explicit form.
     *
     * The delegate **expression** becomes the field's initializer — `x$delegate = lazy { … }` — which is where
     * a Java `private final Lazy<T> slot = new Lazy<>(…)` puts it too, and what makes the initializer's calls
     * (the lambda body included) visible instead of dropped. It is converted in [drainDelegatedProperties], not
     * here: like the accessor bodies it has to see every type's members.
     */
    private fun KaSession.convertDelegatedProperty(owner: TypeInfo, property: KaPropertySymbol,
                                                   type: ParameterizedType, static: Boolean) {
        val name = property.name.asString()
        val psi = property.psi as? KtProperty
        // the delegate's own type (`kotlin.Lazy<T>` for `by lazy`), not the property's
        val delegateType = psi?.delegateExpression?.expressionType?.let { mapType(it, owner) }
            ?: runtime.objectParameterizedType()
        // committed in the drain, once its initializer is converted
        val field = runtime.newFieldInfo("$name\$delegate", static, delegateType, owner)
        field.builder()
            .addFieldModifier(runtime.fieldModifierPrivate())
            .addFieldModifier(runtime.fieldModifierFinal()) // final on the JVM, for a `var` property too
            .setSource(runtime.noSource()) // compiler-made; attach adds what the `by` expression names
        if (static) field.builder().addFieldModifier(runtime.fieldModifierStatic())
        owner.builder().addField(field)
        // the field is the host of what the `by` expression names: it is where that expression ends up
        commitOrDefer(field, psi) { field.builder().commit() }

        // an accessor at every visibility, `private` included — unlike a backing-field property, the delegate
        // field is the only route to the value, so a read has nothing else to resolve against
        val getter = buildDelegateAccessor(owner, field, type, property, static, write = false)
        owner.builder().addMethod(getter)
        val setter = if (property.isVal) null
        else buildDelegateAccessor(owner, field, type, property, static, write = true).also { owner.builder().addMethod(it) }
        val pending = PendingDelegate(owner, field, type, static, getter, setter, psi?.delegateExpression)
        pendingDelegates.add(pending)
        pendingDelegatesOf.getOrPut(owner) { mutableListOf() } += pending
    }

    /**
     * `getX()`/`setX(value)` for a delegated property: signature now, **body later**. The body has to find
     * `getValue`/`setValue` on the delegate's type, and a delegate declared in source is only converted when
     * its own turn in pass B1 comes — which, for a delegate declared after its user, is after this. So the
     * body is filled in [drainDelegatedProperties], once every type of the compilation has its members. No
     * `setGetSetField` tagging: this is a call into the delegate, not a field access.
     */
    private fun KaSession.buildDelegateAccessor(owner: TypeInfo, field: FieldInfo, type: ParameterizedType,
                                      property: KaPropertySymbol, static: Boolean, write: Boolean): MethodInfo {
        val accessor = runtime.newMethod(owner, accessorName(property, write), methodType(static))
        if (write) annotate(accessor.builder().addParameter("value", type).builder(), property.setter?.parameter, owner)
        annotate(accessor.builder(), if (write) property.setter else property.getter, owner)
        accessor.builder().setReturnType(if (write) runtime.voidParameterizedType() else type)
        addMethodModifiers(accessor.builder(), property)
        accessor.builder().commitParameters().computeAccess()
        return accessor
    }

    /**
     * A delegated property whose field initializer and accessor bodies wait for every type's members. The field
     * itself commits with the other members ([commitDeferred]); [initialized] says its initializer is set.
     */
    private class PendingDelegate(val owner: TypeInfo, val field: FieldInfo, val type: ParameterizedType,
                                  val static: Boolean, val getter: MethodInfo, val setter: MethodInfo?,
                                  val delegateExpression: KtExpression?) {
        var initialized = false
    }

    /**
     * Fill in the delegate accessors' bodies and commit them, plus any field whose initializer was never
     * converted (see [finishDelegate]). Runs after pass B1 — every type has its members, so a delegate
     * declared after its user resolves — and again at the very end, so a delegated property built while pass
     * B2 converted a body is committed too: a field's and a method's own inspection are independent of their
     * type's, so committing after the type is fine. Needs no `KaSession`: nothing here converts source.
     */
    private fun drainDelegatedProperties() {
        val drained = pendingDelegates.toList()
        pendingDelegates.clear()
        drained.forEach { finishDelegate(it) }
    }

    /**
     * Convert [owner]'s property initializers (see [initializerContext]) and delegate expressions (see
     * [convertDelegateInitializers]) into their fields' initializers, while [owner] is open and its members exist.
     */
    private fun KaSession.convertInitializers(owner: TypeInfo) {
        convertDelegateInitializers(owner)
        // an `object :` expression in an initializer queues its own properties under its own type, which its
        // conversion finishes (finishAnonMembers)
        pendingInitializers.remove(owner)?.forEach { p ->
            p.field.builder().setInitializer(convertExpression(p.expression, initializerContext(owner, p.static), emptyMap()))
        }
    }

    /**
     * The member a property initializer is code of, which is where kotlinc compiles it: an instance property's into
     * the primary constructor (of a class, an `object`, a companion); a top-level property's into the file facade's
     * static initializer; an `object :` expression's, which has no constructor in the CST (nor in Java's), into an
     * instance initializer. An `object :` expression or a lambda in the initializer is enclosed by that member, and a
     * primary-constructor parameter the initializer reads resolves as the constructor's parameter.
     */
    private fun initializerContext(owner: TypeInfo, static: Boolean): MethodInfo =
        if (static) initializerMethod(owner, runtime.methodTypeStaticInitializer(), "<static_0>") { it.isStaticInitializer }
        else runsInitOf[owner] ?: initializerMethod(owner, runtime.methodTypeInstanceInitializer(), "<init_0>") {
            it.isInstanceInitializer
        }

    /** The synthetic initializer block of [owner], made on first use: empty, private, as the Java parser names one. */
    private fun initializerMethod(owner: TypeInfo, type: MethodInfo.MethodType, name: String,
                                  existing: (MethodInfo) -> Boolean): MethodInfo {
        owner.methods().firstOrNull(existing)?.let { return it }
        val method = runtime.newMethod(owner, name, type)
        method.builder()
            .setReturnType(runtime.voidParameterizedType())
            .setMethodBody(runtime.emptyBlock())
            .setAccess(runtime.accessPrivate())
            .setSynthetic(true).setSource(runtime.noSource()).commitParameters()
        owner.builder().addMethod(method)
        commitOrDefer(method, null) { method.builder().commit() }
        return method
    }

    /**
     * Convert the delegate expressions of [owner]'s delegated properties into their fields' initializers.
     * Called while [owner] is still **open**, at the end of its member conversion: a delegate expression is
     * usually a call with a lambda (`lazy { … }`), and a lambda mints an anonymous type on its enclosing
     * type's builder — which a committed type refuses ("Inspection of … has already been committed", seen on
     * `kotlin.text.CharDirectionality.Companion` when this ran in the later drain instead). Running here also
     * means the owner's own methods already exist, so a delegate calling one of them resolves; a delegate
     * calling a type declared later does not, exactly as for every other body converted in this pass.
     */
    private fun KaSession.convertDelegateInitializers(owner: TypeInfo) {
        // converting a delegate expression can build an anonymous type, whose own delegated properties are queued
        // under that type
        pendingDelegatesOf.remove(owner)?.forEach { p ->
            if (p.initialized) return@forEach
            val initializer = p.delegateExpression?.let { convertExpression(it, p.getter, emptyMap()) }
                ?: runtime.newEmptyExpression("k2-delegate-initializer:${p.field.name()}")
            p.field.builder().setInitializer(initializer).computeAccess()
            p.initialized = true
        }
    }

    private fun finishDelegate(p: PendingDelegate) {
        if (!p.initialized) {
            // not converted while the owner was open (no convertInitializers reached it): mark rather than
            // convert -- a lambda in the expression would need the owner's builder, and it is committed now
            val placeholder = runtime.newEmptyExpression("k2-delegate-initializer:${p.field.name()}")
            p.field.builder().setInitializer(p.delegateExpression?.let { placeholder.withSource(sourceOf(runtime, it, "-")) }
                ?: placeholder).computeAccess()
            p.initialized = true
        }
        if (!p.getter.hasBeenInspected()) {
            val read = runtime.newReturnBuilder()
                .setExpression(delegateRead(p.owner, p.field, p.type, p.static)).setSource(runtime.noSource()).build()
            p.getter.builder().setMethodBody(runtime.newBlockBuilder().addStatement(read).build()).commit()
        }
        val setter = p.setter ?: return
        if (!setter.hasBeenInspected()) {
            val value = setter.parameters().first()
            val write = runtime.newExpressionAsStatement(delegateWrite(p.owner, p.field, value, p.static))
            setter.builder().setMethodBody(runtime.newBlockBuilder().addStatement(write).build()).commit()
        }
    }

    /**
     * The delegate read. Kotlin's convention is the `getValue(thisRef, property)` operator, which is what a
     * hand-written delegate declares; `kotlin.Lazy` — the `by lazy` case — declares `val value` instead, and
     * once loaded from bytecode that is a FIELD, so `this.x$delegate.value` is the spelling there. That is the
     * same shape the explicit form (`private val slot: Lazy<T> = lazy { … }; fun get() = slot.value`) already
     * produces. The `KProperty` argument of the operator form is not modelled; `null` stands in for it.
     */
    private fun delegateRead(owner: TypeInfo, field: FieldInfo, type: ParameterizedType, static: Boolean): Expression {
        val delegate = fieldReadExpression(owner, field, static)
        val delegateType = field.type().typeInfo()
        delegateType?.methods()?.firstOrNull { it.name() == "getValue" && it.parameters().size == 2 }?.let { getValue ->
            return runtime.newMethodCallBuilder()
                .setObject(delegate).setObjectIsImplicit(false)
                .setMethodInfo(getValue)
                .setParameterExpressions(listOf(thisRef(owner, static), runtime.nullConstant()))
                .setConcreteReturnType(type).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
        }
        delegateType?.fields()?.firstOrNull { it.name() == "value" }?.let { valueField ->
            return runtime.newVariableExpressionBuilder()
                .setVariable(runtime.newFieldReference(valueField, delegate, type))
                .setSource(runtime.noSource()).build()
        }
        return runtime.newEmptyExpression("k2-delegate-read:${field.name()}")
    }

    /** The delegate write, `this.x$delegate.setValue(this, null, value)` — `var` properties only. */
    private fun delegateWrite(owner: TypeInfo, field: FieldInfo, value: ParameterInfo, static: Boolean): Expression {
        val setValue = field.type().typeInfo()?.methods()
            ?.firstOrNull { it.name() == "setValue" && it.parameters().size == 3 }
            ?: return runtime.newEmptyExpression("k2-delegate-write:${field.name()}")
        return runtime.newMethodCallBuilder()
            .setObject(fieldReadExpression(owner, field, static)).setObjectIsImplicit(false)
            .setMethodInfo(setValue)
            .setParameterExpressions(listOf(thisRef(owner, static), runtime.nullConstant(),
                bodyConverter.variableExpression(value)))
            .setConcreteReturnType(runtime.voidParameterizedType())
            .setTypeArguments(listOf()).setSource(runtime.noSource()).build()
    }

    /** The `thisRef` a delegate operator takes: `this`, or `null` for a delegated property on a facade/companion. */
    private fun thisRef(owner: TypeInfo, static: Boolean): Expression =
        if (static) runtime.nullConstant() else fieldAccessScope(owner, false)

    /**
     * The method type, mirroring java-openjdk's `FlagHelper.methodType`: static first, then a declaration without a
     * body (Kotlin's `abstract` modality, which an interface member without a body has), then a member of an
     * interface that does have one -- what Java calls a `default` method, and what kotlinc compiles it to.
     *
     * Abstractness used to survive only as the `abstract` MODIFIER, so `MethodInfo.isAbstract()` was false for every
     * Kotlin interface member, and prep registered no implementation for any of them (#35).
     */
    private fun methodType(static: Boolean, symbol: KaDeclarationSymbol? = null, owner: TypeInfo? = null) = when {
        static -> runtime.methodTypeStaticMethod()
        symbol?.modality == KaSymbolModality.ABSTRACT -> runtime.methodTypeAbstractMethod()
        owner?.isInterface == true -> runtime.methodTypeDefaultMethod()
        else -> runtime.methodTypeMethod()
    }

    /** Scope for a backing-field access: `this` for an instance member, the owning type for a static one. */
    private fun fieldAccessScope(owner: TypeInfo, static: Boolean): Expression =
        if (static) runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo())
        else runtime.newVariableExpressionBuilder()
            .setVariable(runtime.newThis(owner.asParameterizedType())).setSource(runtime.noSource()).build()

    private fun fieldReadExpression(owner: TypeInfo, field: FieldInfo, static: Boolean): Expression =
        runtime.newVariableExpressionBuilder()
            .setVariable(runtime.newFieldReference(field, fieldAccessScope(owner, static), field.type()))
            .setSource(runtime.noSource()).build()

    /** A computed property's getter: its real (custom) body, no field-access tagging. */
    private fun KaSession.buildComputedGetter(owner: TypeInfo, property: KaPropertySymbol,
                                              type: ParameterizedType, static: Boolean): MethodInfo {
        val getter = runtime.newMethod(owner, accessorName(property, false),
                methodType(static, property, owner))
        getter.builder().setReturnType(type)
        // an extension property (`val Int.doubled get() = this * 2`) becomes a static getter whose first
        // parameter is the `$receiver` -- so `this` in the body resolves to it (the JVM model)
        contextParameters(getter.builder(), property, owner, null, synthetic = false)
        property.receiverParameter?.let { getter.builder().addParameter("\$receiver", mapType(it.returnType, owner)) }
        annotate(getter.builder(), property.getter, owner)
        addMethodModifiers(getter.builder(), property)
        getter.builder().commitParameters().computeAccess()
        val accessor = (property.psi as? KtProperty)?.getter
        // a computed property has no field: its getter is what a reference names, and where its body's are recorded
        val propertyPsi = property.psi
        // ... and the only place the property's own NAME position can be recorded; the whole-declaration source is
        // the `val`/`var`, not the `get()`, since that is what the name sits in
        getter.builder().setSource(declarationSource(propertyPsi) { putPropertyName(property) })
        references.target(propertyPsi, getter)
        awaitBody(getter)
        body {
            val block = runtime.newBlockBuilder()
            val expressionBody = accessor?.bodyExpression
            if (expressionBody != null) {
                block.addStatement(bodyConverter.indexed(runtime.newReturnStatement(convertExpression(expressionBody, getter, emptyMap())), "0"))
            } else {
                val statements = accessor?.bodyBlockExpression?.statements.orEmpty()
                val scope = mutableMapOf<String, Variable>()
                statements.forEachIndexed { i, s -> block.addStatement(convertStatement(s, getter, scope, bodyConverter.pad(i, statements.size))) }
            }
            getter.builder().setMethodBody(block.build())
            commitOrDefer(getter, propertyPsi) { getter.builder().commit() }
        }
        return getter
    }

    /**
     * The setter of a `var` that has no backing field (#36): a computed one, whose written `set(value) { … }` is
     * converted as any accessor body is, and an abstract one -- an interface's `var v: Int`, where the class that
     * implements it overrides this declaration. Without it, an assignment to such a property had no target, and a
     * written setter's body was code nothing saw.
     */
    private fun KaSession.buildComputedSetter(owner: TypeInfo, property: KaPropertySymbol,
                                              type: ParameterizedType, static: Boolean): MethodInfo {
        val accessor = (property.psi as? KtProperty)?.setter
        if (accessor != null && accessor.hasBody()) {
            return buildCustomAccessor(owner, null, type, property, static, accessor)
        }
        val setter = runtime.newMethod(owner, accessorName(property, true),
                methodType(static, property, owner))
        val builder = setter.builder()
        builder.setReturnType(runtime.voidParameterizedType())
        contextParameters(builder, property, owner, null, synthetic = false)
        property.receiverParameter?.let { builder.addParameter("\$receiver", mapType(it.returnType, owner)) }
        val valueParameter = builder.addParameter(accessor?.parameter?.name ?: "value", type)
        parameter(valueParameter, accessor?.parameter, type)
        annotate(valueParameter.builder(), property.setter?.parameter, owner)
        annotate(builder, property.setter, owner)
        addMethodModifiers(builder, property)
        builder.commitParameters().computeAccess()
        // an abstract `var` has no accessor text at all: the `val`/`var` declaration is where its name is written
        builder.setSource(declarationSource(accessor ?: property.psi) { putPropertyName(property) })
        builder.setMethodBody(runtime.emptyBlock())
        commitOrDefer(setter, accessor) { setter.builder().commit() }
        return setter
    }

    private fun KaSession.buildGetter(owner: TypeInfo, field: FieldInfo, type: ParameterizedType,
                                      property: KaPropertySymbol, static: Boolean): MethodInfo {
        val getter = runtime.newMethod(owner, accessorName(property, false), methodType(static))
        val source = runtime.noSource()
        val returnField = runtime.newReturnBuilder()
            .setExpression(fieldReadExpression(owner, field, static))
            .setSource(source).build()
        getter.builder()
            .setReturnType(type)
            .setMethodBody(runtime.newBlockBuilder().addStatement(returnField).build())
        annotate(getter.builder(), property.getter, owner)
        addMethodModifiers(getter.builder(), property)
        getter.builder().commitParameters().computeAccess()
        runtime.setGetSetField(getter, field, false, -1, false)
        // deferred, like every other member, FOR ITS OVERRIDES: they are computed in commitDeferred, once every
        // member exists. Committed on the spot, an `override val`'s getter overrode nothing (#37).
        commitOrDefer(getter, null) { getter.builder().commit() }
        return getter
    }

    private fun KaSession.buildSetter(owner: TypeInfo, field: FieldInfo, type: ParameterizedType,
                                      property: KaPropertySymbol, static: Boolean): MethodInfo {
        val setter = runtime.newMethod(owner, accessorName(property, true), methodType(static))
        val value = setter.builder().addParameter("value", type)
        annotate(value.builder(), property.setter?.parameter, owner)
        annotate(setter.builder(), property.setter, owner)
        setter.builder()
            .setReturnType(runtime.voidParameterizedType())
            .setMethodBody(runtime.newBlockBuilder().addStatement(assignFieldFromParam(owner, field, value, static)).build())
        addMethodModifiers(setter.builder(), property)
        setter.builder().commitParameters().computeAccess()
        runtime.setGetSetField(setter, field, true, -1, false)
        commitOrDefer(setter, null) { setter.builder().commit() } // for its overrides, as the getter (#37)
        return setter
    }

    /** Build the statement `this.field = param` (or `Type.field = param` for a static backing field). */
    private fun assignFieldFromParam(owner: TypeInfo, field: FieldInfo, param: ParameterInfo, static: Boolean): Statement {
        val source = runtime.noSource()
        val assignment = runtime.newAssignmentBuilder()
            .setTarget(runtime.newVariableExpressionBuilder()
                .setVariable(runtime.newFieldReference(field, fieldAccessScope(owner, static), field.type()))
                .setSource(source).build())
            .setValue(runtime.newVariableExpressionBuilder().setVariable(param).setSource(source).build())
            .setSource(source).build()
        return runtime.newExpressionAsStatementBuilder().setExpression(assignment).setSource(source).build()
    }


    /** JavaBean accessor name, matching the JVM names Kotlin generates (and that maddi recognises). */
    /** Compiler-generated: a `by`-delegation [forwarder], or a source class's generated member (componentN, copy). */
    private fun isGenerated(function: KaNamedFunctionSymbol, forwarder: Boolean): Boolean =
        forwarder || function.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED

    /**
     * The JVM name kotlinc gives [property]'s getter or setter: `getName`/`setName`, but `isEnabled`/`setEnabled` for
     * `isEnabled`, and whatever `@get:JvmName`/`@set:JvmName` says. A Java caller spells that name, and a class file
     * of the same code carries it.
     */
    @OptIn(KaExperimentalApi::class) // javaGetterName, javaSetterName
    private fun KaSession.accessorName(property: KaPropertySymbol, setter: Boolean): String {
        val jvmName = if (setter) property.javaSetterName else property.javaGetterName
        return jvmName?.asString()
            ?: ((if (setter) "set" else "get") + property.name.asString().replaceFirstChar { it.uppercaseChar() })
    }

    /**
     * Convert one declared function symbol into a committed CST method (with parameters and body).
     * [static] marks top-level functions, which the JVM emits as static members of the file facade.
     */
    private fun KaSession.convertMethod(owner: TypeInfo, function: KaNamedFunctionSymbol,
                                        static: Boolean = false): MethodInfo {
        val method = convertMethodSignature(owner, function, static)
        finishMethodBody(function, method)
        return method
    }

    /** The method's body, then commit; call after the signature exists (so forward/self calls resolve). */
    private fun KaSession.finishMethodBody(function: KaNamedFunctionSymbol, method: MethodInfo,
                                           outerLocals: Map<String, Variable> = emptyMap()) {
        method.builder().setMethodBody(convertBody(function, method.returnType(), method, outerLocals))
        // nor does it host that PSI: references written there are the property's or the class's, not copy()'s (#38)
        commitOrDefer(method, if (isGenerated(function, false)) null else function.psi) { method.builder().commit() }
        defaultsMethodOf.remove(method)?.let { defaults ->
            val pending = pendingDefaults.remove(defaults)!!
            defaults.builder().setMethodBody(defaultsBody(defaults, method, pending.parameters))
            commitOrDefer(defaults, null) { defaults.builder().commit() }
        }
    }

    /**
     * The method's signature only (parameters, type parameters, return type, modifiers, access) -- NO body,
     * NOT committed. Registering all signatures before converting any body lets a body resolve a forward or
     * self reference to a sibling (e.g. a `tailrec` function's recursive call, or a top-level function
     * calling another one on the file facade).
     */
    /**
     * Kotlin **interface delegation**: `class C(private val d: I) : I by d` makes kotlinc generate an override
     * of every member of `I` on `C`, forwarding to `d`. K2's `declaredMemberScope` does not surface those, and
     * they have no PSI, so nothing else here sees them — `C` came out with **no members at all**, which is a
     * modelling hole (the analysis sees a type that implements an interface yet has none of its methods) as
     * well as the reason a generated Java stub would not compile ("C is not abstract and does not override
     * abstract method timeout() in Sink", coil's `FaultHidingSink : Sink by delegate`).
     *
     * The **abstract** members are the ones that must exist for `C` to be concrete, so those are what we
     * materialise, with an empty body (the delegation target is a field; the forwarding call itself carries no
     * information the modification analysis does not already get from the field). Inherited abstract members
     * count too — `okio.Sink` extends `Closeable`/`Flushable` — hence `memberScope` rather than the declared
     * one; `Any`'s members are excluded because Kotlin delegation never forwards them.
     */
    private fun KaSession.addDelegatedMembers(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val delegations = declaration.superTypeListEntries.filterIsInstance<KtDelegatedSuperTypeEntry>()
        if (delegations.isEmpty()) return
        val present = typeInfo.methods().map { it.name() to it.parameters().size }.toMutableSet()
        present += listOf("equals" to 1, "hashCode" to 0, "toString" to 0)
        delegations.forEach { entry ->
            val superType = entry.typeReference?.type as? KaClassType ?: return@forEach
            val superSymbol = superType.symbol as? KaClassSymbol ?: return@forEach
            superSymbol.memberScope.declarations
                .filterIsInstance<KaNamedFunctionSymbol>()
                .filter { it.modality == KaSymbolModality.ABSTRACT }
                .forEach { function ->
                    if (!present.add(function.name.asString() to function.valueParameters.size)) return@forEach
                    val method = convertMethodSignature(typeInfo, function, forwarder = true)
                    typeInfo.builder().addMethod(method)
                    method.builder().setMethodBody(runtime.emptyBlock())
                    commitOrDefer(method, null) { method.builder().commit() } // for its overrides

                }
        }
    }

    /**
     * @param forwarder a `by`-delegation forwarder: [function] is the delegated interface's member, whose PSI is
     * that member's declaration, in another file. The forwarder is compiler-generated: it spells nothing, and it is
     * not what that PSI declares.
     */
    private fun KaSession.convertMethodSignature(owner: TypeInfo, function: KaNamedFunctionSymbol,
                                                 static: Boolean = false, forwarder: Boolean = false): MethodInfo {
        // a `by`-delegation forwarder is built from the interface's ABSTRACT symbol but has a body of its own
        val methodType = methodType(static, if (forwarder) null else function, owner)
        // honour @JvmName on the function (overloads that erase to the same JVM signature are disambiguated by it)
        val jvmName = (function.psi as? KtNamedFunction)?.let { jvmNameOverride(it) }
        val method = runtime.newMethod(owner, jvmName ?: function.name.asString(), methodType)
        // a GENERATED member's PSI is what it was generated from -- a data class's componentN() has the constructor
        // parameter's, copy() the class's -- so as that PSI's target it overwrote the property and the constructor,
        // and every reference to them named componentN()/copy() (#38). It spells nothing, as a forwarder does not
        if (!isGenerated(function, forwarder)) references.target(function.psi, method)
        val builder = method.builder()
        // compiler-generated members of a source class (a data class's componentN()/copy(), …) are synthetic
        if (isGenerated(function, forwarder)) builder.setSynthetic(true)
        // method type parameters (`fun <T : Comparable<T>> …`): create them first (a bound may reference a
        // sibling, `T : Comparable<T>`), then set bounds -- resolved with `method` so a bare `T` binds here
        val cstTypeParameters = function.typeParameters.mapIndexed { index, tp ->
            runtime.newTypeParameter(index, tp.name.asString(), method)
                .also { builder.addTypeParameter(it) } to tp
        }
        cstTypeParameters.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, owner, method) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }
        val returnType = mapType(function.returnType, owner, method)
        // context parameters come first, then an extension function's receiver, then the value parameters (the JVM model)
        contextParameters(builder, function, owner, method, synthetic = false)
        function.receiverParameter?.let { receiver ->
            val parameterInfo = builder.addParameter("\$receiver", mapType(receiver.returnType, owner, method))
            if (!forwarder) annotate(parameterInfo.builder(), receiver, owner)
        }
        function.valueParameters.forEach { p ->
            // a vararg's K2 returnType is the element type; the JVM/CST parameter is an array of it
            val elementType = mapType(p.returnType, owner, method)
            val parameterType = if (p.isVararg) elementType.copyWithArrays(elementType.arrays() + 1) else elementType
            val parameterInfo = builder.addParameter(p.name.asString(), parameterType)
            parameterInfo.builder().setVarArgs(p.isVararg)
            parameter(parameterInfo, if (forwarder) null else p.psi as? KtParameter, elementType)
            if (!forwarder) annotate(parameterInfo.builder(), p, owner)
        }
        // a `by`-delegation forwarder is kotlinc's, and carries none of the interface method's annotations
        if (!forwarder) annotate(builder, function, owner)
        builder.commitParameters() // so method.parameters() is available while converting the body
        val psi = if (forwarder) null else function.psi as? KtNamedFunction
        builder
            .setReturnType(returnType)
            // name keyed by method.name(), return-type reference keyed by its TypeInfo -- mirroring the Java parser
            .setSource(declarationSource(psi) {
                putPsi(runtime, method.name(), psi?.nameIdentifier)
                psi?.let { attachModifiers(runtime, it) { t -> methodModifierFor(t) } }
                psi?.let { attachOverride(runtime, it) }
                putTypeReference(runtime, returnType, psi?.typeReference)
            })
        addMethodModifiers(builder, function)
        if (static) builder.addMethodModifier(runtime.methodModifierStatic())
        builder.computeAccess() // eventual access from the visibility modifier + owner type; commit after the body
        if (psi != null) defaultsMethod(owner, function, method, static, psi)
        if (psi != null) overloadMethods(owner, function, method, static)
        return method
    }

    /**
     * kotlinc's `f$default`, for a function [psi] that declares a default value: [target]'s parameters, then a bit mask
     * of the omitted ones (`$mask`, one per 32 parameters). Its body ([defaultsBody]) evaluates each omitted
     * parameter's default where kotlinc does, in the function's own scope, then calls [target]; a call that omits an
     * argument calls it instead of [target] (KotlinBodyConverter.callArguments). Where kotlinc makes a member's
     * `f$default` static, with the receiver as its first parameter, this one is an instance method, so that the
     * defaults read `this` as written. Not for a vararg function: a call with a vararg is not ordered.
     */
    private fun KaSession.defaultsMethod(owner: TypeInfo, function: KaNamedFunctionSymbol, target: MethodInfo,
                                         static: Boolean, psi: KtNamedFunction) {
        if (psi.valueParameters.none { it.defaultValue != null } || function.valueParameters.any { it.isVararg }) return
        val method = runtime.newMethod(owner, target.name() + "\$default",
            if (static) runtime.methodTypeStaticMethod() else runtime.methodTypeMethod())
        val builder = method.builder().setSynthetic(true)
        addTypeParameters(builder, function, owner, method)
        contextParameters(builder, function, owner, method, synthetic = true)
        function.receiverParameter?.let { syntheticParameter(builder, "\$receiver", mapType(it.returnType, owner, method)) }
        function.valueParameters.forEach { p -> syntheticParameter(builder, p.name.asString(), mapType(p.returnType, owner, method)) }
        masks(function.valueParameters.size).forEach { syntheticParameter(builder, it, runtime.intParameterizedType()) }
        builder.commitParameters()
            .setReturnType(mapType(function.returnType, owner, method))
            .setSource(runtime.noSource())
        visibilityMethodModifier(function)?.let { builder.addMethodModifier(it) }
        builder.addMethodModifier(when {
            static -> runtime.methodModifierStatic()
            owner.isInterface -> runtime.methodModifierDefault()
            else -> runtime.methodModifierFinal()
        })
        builder.computeAccess()
        owner.builder().addMethod(method)
        references.defaults(psi, method)
        defaultsMethodOf[target] = method
        pendingDefaults[method] = PendingDefaults(target, psi.valueParameters)
    }

    /**
     * The `$default` constructor, for a constructor [ctor] declaring a default value: [target]'s parameters, the masks,
     * and kotlinc's `DefaultConstructorMarker`, which keeps it from colliding with a declared constructor. See
     * [defaultsMethod]; its body, [defaultsBody], ends in `this(...)`.
     */
    private fun KaSession.defaultsConstructor(owner: TypeInfo, ctor: KaConstructorSymbol, target: MethodInfo): MethodInfo? {
        val declaration = ctor.psi ?: return null
        val parameters = ctor.valueParameters.map { it.psi as? KtParameter }
        if (parameters.none { it?.defaultValue != null } || ctor.valueParameters.any { it.isVararg }) return null
        val constructor = runtime.newConstructor(owner, runtime.methodTypeConstructor())
        val builder = constructor.builder().setSynthetic(true)
        target.parameters().forEach { syntheticParameter(builder, it.name(), it.parameterizedType()) }
        masks(parameters.size).forEach { syntheticParameter(builder, it, runtime.intParameterizedType()) }
        syntheticParameter(builder, "\$marker", defaultConstructorMarker())
        builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor()).setSource(runtime.noSource())
        visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
        builder.commitParameters().computeAccess()
        owner.builder().addConstructor(constructor)
        references.defaults(declaration, constructor)
        pendingDefaults[constructor] = PendingDefaults(target, parameters)
        defaultsConstructors += constructor
        return constructor
    }

    /** [function]'s type parameters, as [method]'s own: a synthetic member made from [function]'s declaration. */
    private fun KaSession.addTypeParameters(builder: MethodInfo.Builder, function: KaNamedFunctionSymbol, owner: TypeInfo,
                                            method: MethodInfo) {
        function.typeParameters.mapIndexed { index, tp ->
            runtime.newTypeParameter(index, tp.name.asString(), method).also { builder.addTypeParameter(it) } to tp
        }.forEach { (cstTp, tp) ->
            cstTp.builder()
                .setTypeBounds(tp.upperBounds.map { mapType(it, owner, method) }.filterNot { it.isJavaLangObject })
                .setVariance(mapVariance(tp.variance))
                .commit()
        }
    }

    /**
     * [callable]'s context parameters (`context(session: KaSession)`), which kotlinc compiles as the LEADING parameters,
     * ahead of an extension receiver: `context(s: S) fun T.f(x: X)` is `f(S, T, X)`, and a context property's getter
     * `getP(S, T)` (measured with javap on kotlinc 2.4.0). They are ordinary parameters in the body -- Kotlin makes
     * them no implicit receiver; `with(session) { … }` does that.
     */
    @OptIn(KaExperimentalApi::class) // contextParameters
    private fun KaSession.contextParameters(builder: MethodInfo.Builder, callable: KaCallableSymbol, owner: TypeInfo,
                                            method: MethodInfo?, synthetic: Boolean) {
        callable.contextParameters.forEach { c ->
            val type = mapType(c.returnType, owner, method)
            if (synthetic) syntheticParameter(builder, c.name.asString(), type)
            else builder.addParameter(c.name.asString(), type)
        }
    }

    private fun syntheticParameter(builder: MethodInfo.Builder, name: String, type: ParameterizedType) {
        builder.addParameter(name, type).builder().setSource(runtime.noSource())
    }

    /** The names of the masks of a `$default` for [parameters] parameters: one int per 32. */
    private fun masks(parameters: Int): List<String> =
        (0 until (parameters + 31) / 32).map { if (it == 0) "\$mask" else "\$mask$it" }

    private fun KaSession.defaultConstructorMarker(): ParameterizedType =
        (findClass(ClassId.fromString("kotlin/jvm/internal/DefaultConstructorMarker")) as? KaNamedClassSymbol)
            ?.let { with(typeMapper) { loadLibraryClass(it) } }?.asParameterizedType()
            ?: runtime.objectParameterizedType()

    /**
     * The body of [defaults], the `$default` of [target] ([defaultsMethod], [defaultsConstructor]): for each of
     * [parameters] with a default value, `if (($mask & bit) != 0) p = <default>`, in order, so a default reads the
     * parameters before it as completed; then `return target(...)`, or `this(...)`. A default is converted in
     * [defaults]' scope: [target]'s parameters by name, its `this`, its receiver.
     */
    private fun KaSession.defaultsBody(defaults: MethodInfo, target: MethodInfo, parameters: List<KtParameter?>): Block {
        val passed = defaults.parameters().subList(0, target.parameters().size)
        val masks = defaults.parameters().subList(passed.size, passed.size + masks(parameters.size).size)
        val offset = passed.size - parameters.size // an extension's receiver comes first
        val withDefault = parameters.withIndex().filter { it.value?.defaultValue != null }
        val count = withDefault.size + 1
        val body = runtime.newBlockBuilder()
        withDefault.forEachIndexed { j, (i, psi) ->
            val index = bodyConverter.pad(j, count)
            val value = convertExpression(psi!!.defaultValue!!, defaults, emptyMap())
            val assignment = runtime.newAssignmentBuilder()
                .setTarget(runtime.newVariableExpressionBuilder().setVariable(passed[offset + i]).setSource(runtime.noSource()).build())
                .setValue(value).setSource(runtime.noSource()).build()
            body.addStatement(runtime.newIfElseBuilder()
                .setExpression(maskTest(masks[i / 32], 1 shl (i % 32)))
                .setIfBlock(runtime.newBlockBuilder().setSource(runtime.noSource().withIndex("$index.0"))
                    .addStatement(bodyConverter.indexed(runtime.newExpressionAsStatement(assignment), "$index.0.0")).build())
                .setElseBlock(runtime.newBlockBuilder().setSource(runtime.noSource().withIndex("$index.1")).build())
                .setSource(runtime.noSource().withIndex(index)).build())
        }
        val index = bodyConverter.pad(count - 1, count)
        val arguments = passed.map { bodyConverter.variableExpression(it) }
        body.addStatement(if (target.isConstructor) {
            runtime.newExplicitConstructorInvocationBuilder().setIsSuper(false).setMethodInfo(target)
                .setParameterExpressions(arguments).setSource(runtime.noSource().withIndex(index)).build()
        } else {
            val owner = target.typeInfo()
            val call = runtime.newMethodCallBuilder()
                .setObject(if (target.isStatic) runtime.newTypeExpression(owner.asParameterizedType(), runtime.diamondNo())
                           else bodyConverter.variableExpression(runtime.newThis(owner.asParameterizedType())))
                .setObjectIsImplicit(!target.isStatic).setMethodInfo(target).setParameterExpressions(arguments)
                .setConcreteReturnType(target.returnType()).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
            bodyConverter.indexed(if (target.returnType() == runtime.voidParameterizedType())
                runtime.newExpressionAsStatement(call) else runtime.newReturnStatement(call), index)
        })
        return body.build()
    }

    /** `($mask & bit) != 0`: whether the parameter with [bit] was omitted. */
    private fun maskTest(mask: ParameterInfo, bit: Int): Expression {
        val and = runtime.newBinaryOperatorBuilder().setLhs(bodyConverter.variableExpression(mask)).setRhs(runtime.newInt(bit))
            .setOperator(runtime.andOperatorInt()).setPrecedence(runtime.precedenceBitwiseAnd())
            .setParameterizedType(runtime.intParameterizedType()).setSource(runtime.noSource()).build()
        return runtime.newBinaryOperatorBuilder().setLhs(and).setRhs(runtime.intZero())
            .setOperator(runtime.notEqualsOperatorInt()).setPrecedence(runtime.precedenceEquality())
            .setParameterizedType(runtime.booleanParameterizedType()).setSource(runtime.noSource()).build()
    }

    /**
     * A parameter's declaration source (its name keyed by `parameterInfo.name()`, like the Java parser; its type
     * reference; where its default value is written, [DetailedSources.DEFAULT_VALUE]), and the parameter as the target
     * of the references to it. Not a constructor's `val`/`var` parameter: its declaration is the property's, and a
     * reference to it names the property ([convertProperty]).
     */
    private fun parameter(parameterInfo: ParameterInfo, psi: KtParameter?, type: ParameterizedType) {
        parameterInfo.builder().setSource(declarationSource(psi) {
            putPsi(runtime, parameterInfo.name(), psi?.nameIdentifier)
            putTypeReference(runtime, type, psi?.typeReference)
            psi?.defaultValue?.let { put(DetailedSources.DEFAULT_VALUE, sourceOf(runtime, it, "-")) }
        })
        if (psi != null && !psi.hasValOrVar()) references.target(psi, parameterInfo)
    }

    /**
     * The whole-declaration source of [declaration] with a `DetailedSources` built by [populate]. Mirrors
     * java-openjdk's keys so a refactoring engine stays language-unaware: declaration names by the Info's own
     * name String (`typeInfo.simpleName()` / `method.name()`), the type-nature keyword by the shared
     * `typeNature()` object, type references by their `TypeInfo`/`TypeParameter`. `DetailedSources` is
     * identity-keyed, so a consumer looks up via the same instance. `noSource()` if synthetic.
     */
    private fun declarationSource(declaration: PsiElement?, populate: DetailedSources.Builder.() -> Unit): Source {
        if (declaration == null) return runtime.noSource()
        val dsb = runtime.newDetailedSourcesBuilder().apply(populate)
        return sourceOf(runtime, declaration, "-").withDetailedSources(dsb.build())
    }


    // --- Type mapping + library loading: delegated to KotlinTypeMapper (a clean bottom layer). The
    // KaSession-receiver forwarders use `with(typeMapper) { … }`; the plain ones call it directly. ---

    private fun KaSession.mapType(type: KaType, owner: TypeInfo, method: MethodInfo? = null): ParameterizedType =
        with(typeMapper) { mapType(type, owner, method) }

    private fun KaSession.bootstrapObject() = with(typeMapper) { bootstrapObject() }

    private fun KaSession.bootstrapString() = with(typeMapper) { bootstrapString() }

    private fun KaSession.applyHierarchy(builder: TypeInfo.Builder, owner: TypeInfo, classSymbol: KaClassSymbol) =
        with(typeMapper) { applyHierarchy(builder, owner, classSymbol) }

    private fun addMethodModifiers(builder: MethodInfo.Builder, symbol: KaDeclarationSymbol) =
        typeMapper.addMethodModifiers(builder, symbol)

    private fun mapVariance(variance: KotlinVariance): Variance = typeMapper.mapVariance(variance)

    private fun visibilityMethodModifier(symbol: KaDeclarationSymbol): MethodModifier? =
        typeMapper.visibilityMethodModifier(symbol)
}

private val JVM_STATIC = ClassId.fromString("kotlin/jvm/JvmStatic")
private val JVM_FIELD = ClassId.fromString("kotlin/jvm/JvmField")
private val JVM_OVERLOADS = ClassId.fromString("kotlin/jvm/JvmOverloads")
