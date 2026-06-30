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
import org.e2immu.language.inspection.resource.InfoByFqn
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
    // Type mapping (KaType -> ParameterizedType) + lazy library-type loading: the bottom layer.
    // KotlinScan delegates to it via thin forwarders (see the end of this class).
    private val typeMapper = KotlinTypeMapper(runtime, infoByFqn, sourceSet)

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
            // top-level functions/properties live on the JVM file facade `<FileName>Kt` (a static container)
            val topLevelFunctions = ktFile.declarations.filterIsInstance<KtNamedFunction>()
            val topLevelProperties = ktFile.declarations.filterIsInstance<KtProperty>()
            val hasFacade = topLevelFunctions.isNotEmpty() || topLevelProperties.isNotEmpty()
            val (types, facade) = analyze(ktFile) {
                declarations.map { registerType(compilationUnit, it) } to
                        (if (hasFacade) registerFacade(compilationUnit, ktFile) else null)
            }
            FileConversion(ktFile, compilationUnit, declarations, types, facade, topLevelFunctions, topLevelProperties)
        }
        // pass B1 (all files): members + constructor structures (no body), now that all types are known
        perFile.forEach { fc ->
            analyze(fc.ktFile) {
                fc.declarations.zip(fc.types).forEach { (d, ti) -> convertMembers(d, ti) }
                fc.facade?.let { convertFacadeMembers(it, fc.topLevelFunctions, fc.topLevelProperties) }
            }
        }
        // pass B2 (all files): wire constructor this(...)/super(...) delegations — now every constructor
        // exists, so even super() to another source type resolves — then commit.
        perFile.forEach { fc ->
            analyze(fc.ktFile) { fc.declarations.zip(fc.types).forEach { (d, ti) -> finalizeType(d, ti) } }
            fc.facade?.builder()?.commit()
            fc.compilationUnit.setTypes(fc.allTypes())
        }
        return perFile.flatMap { it.allTypes() }
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

    /** Pass A: create + register the file-facade [TypeInfo] `<FileName>Kt`. Members are added in pass B1. */
    private fun registerFacade(compilationUnit: CompilationUnit, ktFile: KtFile): TypeInfo {
        val facade = runtime.newTypeInfo(compilationUnit, facadeSimpleName(ktFile))
        infoByFqn.put(facade.fullyQualifiedName(), facade, sourceSet)
        return facade
    }

    /**
     * Kotlin's JVM file-facade class name: `@file:JvmName("X")` wins; otherwise the file name (sans
     * extension), first letter upper-cased, + "Kt".
     */
    private fun facadeSimpleName(ktFile: KtFile): String {
        jvmNameOverride(ktFile)?.let { return it }
        val base = ktFile.name.substringAfterLast('/').removeSuffix(".kts").removeSuffix(".kt")
        return base.replaceFirstChar { it.uppercaseChar() } + "Kt"
    }

    /** The string in a `@file:JvmName("…")` annotation, or null. */
    private fun jvmNameOverride(ktFile: KtFile): String? {
        val jvmName = ktFile.fileAnnotationList?.annotationEntries
            ?.firstOrNull { it.shortName?.asString() == "JvmName" } ?: return null
        val literal = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
        return (literal?.entries?.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
    }

    /**
     * Pass B1: the file facade is a final public class holding the top-level functions and properties as
     * static members (the JVM emits top-level declarations exactly this way).
     */
    private fun KaSession.convertFacadeMembers(facade: TypeInfo, functions: List<KtNamedFunction>,
                                               properties: List<KtProperty>) {
        facade.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierFinal())
            .computeAccess() // top-level: eventual access == the public modifier; needed before members
        properties.forEach { p ->
            (p.symbol as? KaPropertySymbol)?.let { convertProperty(facade, it, static = true) }
        }
        functions.forEach { fn ->
            (fn.symbol as? KaNamedFunctionSymbol)?.let { facade.builder().addMethod(convertMethod(facade, it, static = true)) }
        }
    }

    /** Pass B: convert members and commit the type. */
    private fun KaSession.convertMembers(declaration: KtClassOrObject, typeInfo: TypeInfo) {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        // hierarchy first, so method bodies can resolve inherited callees via parentClass/interfaces
        applyHierarchy(typeInfo.builder(), typeInfo, classSymbol)
        typeInfo.builder().computeAccess() // eventual type access, needed before members' computeAccess()
        // properties before methods, so a method body can reference the backing fields
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaPropertySymbol>()
            .forEach { property -> convertProperty(typeInfo, property) }
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .forEach { function -> typeInfo.builder().addMethod(convertMethod(typeInfo, function)) }
        // constructor structures (params); bodies + delegations are wired in pass B2 (finalizeType)
        classSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaConstructorSymbol>()
            .forEach { ctor -> typeInfo.builder().addConstructor(convertConstructorStructure(typeInfo, ctor)) }
        // companion object -> a nested `Companion` type + a static field on the enclosing class
        classSymbol.companionObject?.let { convertCompanion(typeInfo, it) }
        // a named object (singleton) gets a `public static final INSTANCE` field of its own type
        if (classSymbol.classKind == KaClassKind.OBJECT) {
            typeInfo.builder().addField(singletonField(typeInfo, "INSTANCE", typeInfo.asParameterizedType()))
        }
        // access + type commit happen in finalizeType (pass B2), after delegations are wired
    }

    /** A `public static final <type> <name>` singleton field (the `INSTANCE`/`Companion` handle). */
    private fun singletonField(holder: TypeInfo, name: String, type: ParameterizedType): FieldInfo {
        val field = runtime.newFieldInfo(name, true, type, holder)
        field.builder()
            .addFieldModifier(runtime.fieldModifierPublic())
            .addFieldModifier(runtime.fieldModifierStatic())
            .addFieldModifier(runtime.fieldModifierFinal())
            .setInitializer(runtime.newEmptyExpression())
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
        val companion = runtime.newTypeInfo(enclosing, name)
        companion.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .addTypeModifier(runtime.typeModifierPublic())
            .addTypeModifier(runtime.typeModifierStatic()) // a nested object is a static nested class on the JVM
            .addTypeModifier(runtime.typeModifierFinal())
            .computeAccess() // nested: combines with the (already-computed) enclosing access
        enclosing.builder().addSubType(companion)
        infoByFqn.put(companion.fullyQualifiedName(), companion, sourceSet)

        companionSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaPropertySymbol>()
            .forEach { property -> convertProperty(companion, property) }
        companionSymbol.declaredMemberScope.declarations
            .filterIsInstance<KaNamedFunctionSymbol>()
            .forEach { function -> companion.builder().addMethod(convertMethod(companion, function)) }
        companion.builder().commit()

        // the singleton handle: `public static final <Companion> Companion` on the enclosing class
        val companionField = singletonField(enclosing, name, companion.asParameterizedType())
        enclosing.builder().addField(companionField)
        addCompanionStatics(enclosing, companion, companionField, companionSymbol)
    }

    /**
     * Surface companion members that the JVM also emits on the enclosing class: `const val` → a
     * `public static final` field on the enclosing class; `@JvmStatic fun` → a static forwarder method on
     * the enclosing class delegating to `Companion.member(...)`.
     */
    private fun KaSession.addCompanionStatics(enclosing: TypeInfo, companion: TypeInfo, companionField: FieldInfo,
                                              companionSymbol: KaNamedClassSymbol) {
        companionSymbol.declaredMemberScope.declarations.filterIsInstance<KaPropertySymbol>()
            .filter { (it as? KaKotlinPropertySymbol)?.isConst == true }
            .forEach { property ->
                enclosing.builder().addField(
                    singletonField(enclosing, property.name.asString(), mapType(property.returnType, enclosing)))
            }

        val jvmStatic = ClassId.fromString("kotlin/jvm/JvmStatic")
        companionSymbol.declaredMemberScope.declarations.filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.annotations.contains(jvmStatic) }
            .forEach { function ->
                val target = companion.methods().firstOrNull {
                    it.name() == function.name.asString() && it.parameters().size == function.valueParameters.size
                } ?: return@forEach
                val forwarder = runtime.newMethod(enclosing, function.name.asString(), runtime.methodTypeStaticMethod())
                val builder = forwarder.builder()
                val params = function.valueParameters.map { builder.addParameter(it.name.asString(), mapType(it.returnType, enclosing)) }
                val returnType = mapType(function.returnType, enclosing)
                builder.setReturnType(returnType)
                    .addMethodModifier(runtime.methodModifierPublic())
                    .addMethodModifier(runtime.methodModifierStatic())
                    .commitParameters()
                val delegate = runtime.newMethodCallBuilder()
                    .setObject(singletonAccess(enclosing, companionField)).setObjectIsImplicit(false)
                    .setMethodInfo(target).setParameterExpressions(params.map { variableExpression(it) })
                    .setConcreteReturnType(returnType).setTypeArguments(listOf()).setSource(runtime.noSource()).build()
                val statement = if (returnType == runtime.voidParameterizedType())
                    runtime.newExpressionAsStatement(delegate) else runtime.newReturnStatement(delegate)
                builder.setMethodBody(runtime.newBlockBuilder().addStatement(statement).build()).computeAccess().commit()
                enclosing.builder().addMethod(forwarder)
            }
    }

    /** Pass B1: a constructor's structure — parameters only. Body/delegation come in [finalizeType]. */
    private fun KaSession.convertConstructorStructure(owner: TypeInfo, ctor: KaConstructorSymbol): MethodInfo {
        val constructor = runtime.newConstructor(owner, runtime.methodTypeConstructor())
        val builder = constructor.builder()
        ctor.valueParameters.forEach { p -> builder.addParameter(p.name.asString(), mapType(p.returnType, owner)) }
        builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
        visibilityMethodModifier(ctor)?.let { builder.addMethodModifier(it) }
        builder.commitParameters().computeAccess()
        return constructor
    }

    /**
     * Pass B2: wire each constructor's body — an [org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation]
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
            val body = runtime.newBlockBuilder()
            statements.forEachIndexed { i, s -> body.addStatement(indexed(s, pad(i, statements.size))) }
            cst.builder().setMethodBody(body.build()).commit()
        }
        typeInfo.builder().commit() // access already computed in convertMembers (B1)
    }

    /** Build the `this(...)`/`super(...)` invocation for a constructor, or null if there is none (or it is unresolved). */
    private fun KaSession.explicitConstructorInvocation(
        owner: TypeInfo, declaration: KtClassOrObject, ctor: KaConstructorSymbol, constructor: MethodInfo,
    ): Statement? {
        val (isSuper, arguments) = when (val psi = ctor.psi) {
            is KtSecondaryConstructor -> {
                val delegation = psi.getDelegationCall()
                if (delegation.isImplicit) return null // implicit super() — not represented
                !delegation.isCallToThis to delegation.valueArguments
            }
            else -> { // primary constructor: an explicit super-type call `class Sub : Base(args)`
                val superCall = declaration.superTypeListEntries.filterIsInstance<KtSuperTypeCallEntry>().firstOrNull()
                    ?: return null
                true to superCall.valueArguments
            }
        }
        val targetType = (if (isSuper) owner.parentClass()?.typeInfo() else owner) ?: return null
        // resolve the target constructor by arity (refine to full overload resolution later)
        val target = targetType.constructors().firstOrNull { it.parameters().size == arguments.size } ?: return null
        val argExpressions = arguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e, constructor, emptyMap()) } }
        return runtime.newExplicitConstructorInvocationBuilder()
            .setIsSuper(isSuper)
            .setMethodInfo(target)
            .setParameterExpressions(argExpressions)
            .setSource(runtime.newParserSource("0", 0, 0, 0, 0)) // must be the first statement (index "0")
            .build()
    }

    /**
     * Convert a Kotlin property (`val`/`var`, incl. primary-constructor `val x: Int`) into a backing
     * [FieldInfo] plus accessor methods whose bodies maddi already recognises as getters/setters:
     * `getX() { return this.x; }` and (for `var`) `setX(v) { this.x = v; }`. Each accessor is tagged via
     * `runtime.setGetSetField`, so the analyzer's getter/setter normalisation treats Kotlin property
     * access identically to a Java field access. (Computed properties without a backing field are a
     * later refinement; here they also get a backing field.)
     */
    private fun KaSession.convertProperty(owner: TypeInfo, property: KaPropertySymbol, static: Boolean = false) {
        val name = property.name.asString()
        val type = mapType(property.returnType, owner)
        val isVal = property.isVal

        // a computed property (custom getter, no backing field, e.g. `val sum get() = x + y`) becomes just
        // a getter with its real body — no field, no getter/setter field tagging.
        if ((property as? KaKotlinPropertySymbol)?.hasBackingField == false) {
            owner.builder().addMethod(buildComputedGetter(owner, property, type, static))
            return
        }

        val field = runtime.newFieldInfo(name, static, type, owner)
        val fieldBuilder = field.builder()
            .addFieldModifier(runtime.fieldModifierPrivate())
            .setInitializer(runtime.newEmptyExpression())
        if (isVal) fieldBuilder.addFieldModifier(runtime.fieldModifierFinal())
        if (static) fieldBuilder.addFieldModifier(runtime.fieldModifierStatic())
        fieldBuilder.computeAccess().commit()
        owner.builder().addField(field)

        owner.builder().addMethod(buildGetter(owner, field, type, property, static))
        if (!isVal) owner.builder().addMethod(buildSetter(owner, field, type, property, static))
    }

    private fun methodType(static: Boolean) =
        if (static) runtime.methodTypeStaticMethod() else runtime.methodTypeMethod()

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
        val getter = runtime.newMethod(owner, accessorName("get", property.name.asString()), methodType(static))
        getter.builder().setReturnType(type)
        addMethodModifiers(getter.builder(), property)
        getter.builder().commitParameters().computeAccess()
        val accessor = (property.psi as? KtProperty)?.getter
        val body = runtime.newBlockBuilder()
        val expressionBody = accessor?.bodyExpression
        if (expressionBody != null) {
            body.addStatement(indexed(runtime.newReturnStatement(convertExpression(expressionBody, getter, emptyMap())), "0"))
        } else {
            val statements = accessor?.bodyBlockExpression?.statements.orEmpty()
            val scope = mutableMapOf<String, Variable>()
            statements.forEachIndexed { i, s -> body.addStatement(convertStatement(s, getter, scope, pad(i, statements.size))) }
        }
        getter.builder().setMethodBody(body.build()).commit()
        return getter
    }

    private fun KaSession.buildGetter(owner: TypeInfo, field: FieldInfo, type: ParameterizedType,
                                      property: KaPropertySymbol, static: Boolean): MethodInfo {
        val getter = runtime.newMethod(owner, accessorName("get", field.name()), methodType(static))
        val source = runtime.noSource()
        val returnField = runtime.newReturnBuilder()
            .setExpression(fieldReadExpression(owner, field, static))
            .setSource(source).build()
        getter.builder()
            .setReturnType(type)
            .setMethodBody(runtime.newBlockBuilder().addStatement(returnField).build())
        addMethodModifiers(getter.builder(), property)
        getter.builder().commitParameters().computeAccess()
        runtime.setGetSetField(getter, field, false, -1, false)
        getter.builder().commit()
        return getter
    }

    private fun KaSession.buildSetter(owner: TypeInfo, field: FieldInfo, type: ParameterizedType,
                                      property: KaPropertySymbol, static: Boolean): MethodInfo {
        val setter = runtime.newMethod(owner, accessorName("set", field.name()), methodType(static))
        val value = setter.builder().addParameter("value", type)
        setter.builder()
            .setReturnType(runtime.voidParameterizedType())
            .setMethodBody(runtime.newBlockBuilder().addStatement(assignFieldFromParam(owner, field, value, static)).build())
        addMethodModifiers(setter.builder(), property)
        setter.builder().commitParameters().computeAccess()
        runtime.setGetSetField(setter, field, true, -1, false)
        setter.builder().commit()
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
    private fun accessorName(prefix: String, fieldName: String): String =
        prefix + fieldName.replaceFirstChar { it.uppercaseChar() }

    /**
     * Convert one declared function symbol into a committed CST method (with parameters and body).
     * [static] marks top-level functions, which the JVM emits as static members of the file facade.
     */
    private fun KaSession.convertMethod(owner: TypeInfo, function: KaNamedFunctionSymbol,
                                        static: Boolean = false): MethodInfo {
        val methodType = if (static) runtime.methodTypeStaticMethod() else runtime.methodTypeMethod()
        val method = runtime.newMethod(owner, function.name.asString(), methodType)
        val returnType = mapType(function.returnType, owner)
        val builder = method.builder()
        // an extension function's receiver becomes the synthetic first parameter (the JVM model)
        function.receiverParameter?.let { builder.addParameter("\$receiver", mapType(it.returnType, owner)) }
        function.valueParameters.forEach { p ->
            builder.addParameter(p.name.asString(), mapType(p.returnType, owner))
        }
        builder.commitParameters() // so method.parameters() is available while converting the body
        builder
            .setReturnType(returnType)
            .setMethodBody(convertBody(function, returnType, method))
        addMethodModifiers(builder, function)
        if (static) builder.addMethodModifier(runtime.methodModifierStatic())
        builder.computeAccess().commit() // eventual access from the visibility modifier + owner type
        return method
    }

    /**
     * Convert a function body into a CST [Block]. Handles the two Kotlin body forms — a block body
     * `{ … }` and an expression body `= expr` (which becomes a single `return expr`, or an expression
     * statement when the function returns Unit). M3 scope: literal constants and returns; expressions
     * not yet understood become an explicit [Runtime.newEmptyExpression] placeholder rather than failing.
     */
    private fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType,
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
    private fun indexed(statement: Statement, index: String): Statement =
        statement.withSource(runtime.noSource().withIndex(index))

    /** Zero-pad [i] to the width of the largest index in a block of [n] statements (so they sort in order). */
    private fun pad(i: Int, n: Int): String =
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
    private fun KaSession.convertStatement(statement: KtExpression, method: MethodInfo,
                                           locals: MutableMap<String, Variable>, index: String): Statement =
        indexed(rawStatement(statement, method, locals, index), index)

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
            runtime.newLocalVariableCreation(local)
        }
        statement is KtBinaryExpression && isAssignment(statement.operationToken) -> {
            val target = statement.left?.let { convertExpression(it, method, locals) } as? VariableExpression
            val value = statement.right?.let { convertExpression(it, method, locals) } ?: runtime.newEmptyExpression()
            if (target == null) runtime.newExpressionAsStatement(runtime.newEmptyExpression("k2-assign-target"))
            else {
                val builder = runtime.newAssignmentBuilder().setTarget(target).setValue(value).setSource(runtime.noSource())
                augmentedOperator(statement.operationToken)?.let { builder.setAssignmentOperator(it) } // x += y
                runtime.newExpressionAsStatement(builder.build())
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
        else -> runtime.newExpressionAsStatement(convertExpression(statement, method, locals))
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
    private fun KaSession.convertExpression(expression: KtExpression, method: MethodInfo,
                                            locals: Map<String, Variable>): Expression {
        expression.evaluate()?.let { constant ->
            return when (val value = constant.value) {
                is Int -> runtime.newInt(value)
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
            is KtCallExpression -> convertCall(expression, null, true, method, locals) // f(...) on implicit this
            is KtDotQualifiedExpression -> convertQualified(expression, method, locals) // obj.f(...) or obj.x
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
    private fun KaSession.convertQualified(expression: KtDotQualifiedExpression, method: MethodInfo,
                                           locals: Map<String, Variable>): Expression {
        val receiver = convertExpression(expression.receiverExpression, method, locals)
        val receiverType = expression.receiverExpression.expressionType?.let { mapType(it, method.typeInfo()).typeInfo() }
        return when (val selector = expression.selectorExpression) {
            is KtCallExpression -> convertCall(selector, receiver to receiverType, false, method, locals)
            is KtNameReferenceExpression -> {
                val field = receiverType?.fields()?.firstOrNull { it.name() == selector.getReferencedName() }
                    ?: return runtime.newEmptyExpression("k2-unresolved-access:${selector.getReferencedName()}")
                variableExpression(runtime.newFieldReference(field, receiver, field.type())) // obj.x -> field access
            }
            else -> runtime.newEmptyExpression("k2-unsupported-selector")
        }
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
            ?.forEach { property -> convertProperty(anon, property) }
        symbol?.declaredMemberScope?.declarations?.filterIsInstance<KaNamedFunctionSymbol>()
            ?.forEach { function -> anon.builder().addMethod(convertMethod(anon, function)) }
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
        return runtime.newMethodCallBuilder()
            .setObject(obj)
            .setObjectIsImplicit(implicitThis)
            .setMethodInfo(callee)
            .setParameterExpressions(arguments)
            .setConcreteReturnType(returnType)
            .setTypeArguments(listOf())
            .setSource(runtime.noSource())
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
    private fun singletonAccess(holder: TypeInfo, field: FieldInfo): Expression =
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

    private fun variableExpression(variable: Variable): Expression =
        runtime.newVariableExpressionBuilder().setVariable(variable).setSource(runtime.noSource()).build()

    // --- Type mapping + library loading: delegated to KotlinTypeMapper (a clean bottom layer). The
    // KaSession-receiver forwarders use `with(typeMapper) { … }`; the plain ones call it directly. ---

    private fun KaSession.mapType(type: KaType, owner: TypeInfo): ParameterizedType =
        with(typeMapper) { mapType(type, owner) }

    private fun KaSession.applyHierarchy(builder: TypeInfo.Builder, owner: TypeInfo, classSymbol: KaClassSymbol) =
        with(typeMapper) { applyHierarchy(builder, owner, classSymbol) }

    private fun addMethodModifiers(builder: MethodInfo.Builder, symbol: KaDeclarationSymbol) =
        typeMapper.addMethodModifiers(builder, symbol)

    private fun mapVariance(variance: KotlinVariance): Variance = typeMapper.mapVariance(variance)

    private fun visibilityMethodModifier(symbol: KaDeclarationSymbol): MethodModifier? =
        typeMapper.visibilityMethodModifier(symbol)
}
