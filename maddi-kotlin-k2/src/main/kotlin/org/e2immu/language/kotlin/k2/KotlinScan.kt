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

import com.intellij.psi.PsiElement
import org.e2immu.language.cst.api.element.CompilationUnit
import org.e2immu.language.cst.api.element.DetailedSources
import org.e2immu.language.cst.api.element.RecordPattern
import org.e2immu.language.cst.api.element.Source
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
import org.e2immu.language.inspection.api.util.RecordSynthetics
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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
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
) : MemberConverter {
    // Type mapping (KaType -> ParameterizedType) + lazy library-type loading: the bottom layer.
    // Delegated via thin forwarders at the end of this class (KaSession member extensions -> with(…)).
    private val typeMapper = KotlinTypeMapper(runtime, infoByFqn, sourceSet)

    // Function-body conversion (statements/expressions/calls/lambdas). It builds anonymous-object members
    // through `this` (KotlinScan is the MemberConverter), breaking the bodies<->declarations cycle.
    private val bodyConverter = KotlinBodyConverter(runtime, infoByFqn, sourceSet, typeMapper)

    init {
        bodyConverter.memberConverter = this
    }

    // MemberConverter: an `object : Super { … }` body builds the anonymous type's members via these.
    override fun KaSession.buildAnonProperty(owner: TypeInfo, property: KaPropertySymbol) =
        convertProperty(owner, property)

    override fun KaSession.buildAnonMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo =
        convertMethod(owner, function)

    // Body conversion delegated to KotlinBodyConverter (KaSession member extensions -> with(…)).
    private fun KaSession.convertBody(function: KaNamedFunctionSymbol, returnType: ParameterizedType, method: MethodInfo) =
        with(bodyConverter) { convertBody(function, returnType, method) }

    private fun KaSession.convertExpression(expression: KtExpression, method: MethodInfo, locals: Map<String, Variable>) =
        with(bodyConverter) { convertExpression(expression, method, locals) }

    private fun KaSession.convertStatement(statement: KtExpression, method: MethodInfo,
                                           locals: MutableMap<String, Variable>, index: String) =
        with(bodyConverter) { convertStatement(statement, method, locals, index) }

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
        // bootstrap: populate the predefined java.lang.Object with its real members (equals/hashCode/toString/
        // …) once, so source types resolve inherited-from-Object calls (mirrors openjdk's ScanCompilationUnits)
        ktFiles.firstOrNull()?.let { analyze(it) { bootstrapObject() } }
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
        return builder.build()
    }

    /** Pass A: create the [TypeInfo] and its type parameters, and register it by FQN. No members yet. */
    private fun KaSession.registerType(compilationUnit: CompilationUnit, declaration: KtClassOrObject): TypeInfo {
        val classSymbol = declaration.symbol as KaNamedClassSymbol
        val typeInfo = runtime.newTypeInfo(compilationUnit, classSymbol.name.asString())

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
        return typeInfo
    }

    /** Pass A: create + register the file-facade [TypeInfo] `<FileName>Kt`. Members are added in pass B1. */
    private fun registerFacade(compilationUnit: CompilationUnit, ktFile: KtFile): TypeInfo {
        val facade = runtime.newTypeInfo(compilationUnit, facadeSimpleName(ktFile))
        infoByFqn.put(facade.fullyQualifiedName(), facade, sourceSet)
        return facade
    }

    // facadeSimpleName / jvmNameOverride are top-level functions now (see KotlinNaming.kt).

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
        // declaration source (nature is set now): name keyed by simpleName(), keyword by typeNature(), and
        // each supertype reference keyed by its TypeInfo (the `: Base(), Iface` clause) -- mirroring Java
        val superTypeDetails = declaration.superTypeListEntries.mapNotNull { entry ->
            entry.typeReference?.let { reference -> mapType(reference.type, typeInfo) to reference }
        }
        typeInfo.builder().setSource(declarationSource(declaration) {
            putPsi(runtime, typeInfo.simpleName(), declaration.nameIdentifier)
            putPsi(runtime, typeInfo.typeNature(), declaration.getDeclarationKeyword())
            superTypeDetails.forEach { (superType, reference) -> putTypeReference(runtime, superType, reference) }
        })
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
                    .setObject(bodyConverter.singletonAccess(enclosing, companionField)).setObjectIsImplicit(false)
                    .setMethodInfo(target).setParameterExpressions(params.map { bodyConverter.variableExpression(it) })
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
            statements.forEachIndexed { i, s -> body.addStatement(bodyConverter.indexed(s, bodyConverter.pad(i, statements.size))) }
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
        // name keyed by field.name(), type reference keyed by its TypeInfo -- mirroring the Java parser
        fieldBuilder.setSource(declarationSource(property.psi) {
            putPsi(runtime, field.name(), (property.psi as? KtNamedDeclaration)?.nameIdentifier)
            putTypeReference(runtime, type, (property.psi as? KtCallableDeclaration)?.typeReference)
        })
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
            body.addStatement(bodyConverter.indexed(runtime.newReturnStatement(convertExpression(expressionBody, getter, emptyMap())), "0"))
        } else {
            val statements = accessor?.bodyBlockExpression?.statements.orEmpty()
            val scope = mutableMapOf<String, Variable>()
            statements.forEachIndexed { i, s -> body.addStatement(convertStatement(s, getter, scope, bodyConverter.pad(i, statements.size))) }
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
            val parameterType = mapType(p.returnType, owner)
            val parameterInfo = builder.addParameter(p.name.asString(), parameterType)
            // name (keyed by parameterInfo.name(), like the Java parser) + type-reference detail (into generics)
            val parameterPsi = p.psi as? KtParameter
            parameterInfo.builder().setSource(declarationSource(parameterPsi) {
                putPsi(runtime, parameterInfo.name(), parameterPsi?.nameIdentifier)
                putTypeReference(runtime, parameterType, parameterPsi?.typeReference)
            })
        }
        builder.commitParameters() // so method.parameters() is available while converting the body
        val psi = function.psi as? KtNamedFunction
        builder
            .setReturnType(returnType)
            // name keyed by method.name(), return-type reference keyed by its TypeInfo -- mirroring the Java parser
            .setSource(declarationSource(psi) {
                putPsi(runtime, method.name(), psi?.nameIdentifier)
                putTypeReference(runtime, returnType, psi?.typeReference)
            })
            .setMethodBody(convertBody(function, returnType, method))
        addMethodModifiers(builder, function)
        if (static) builder.addMethodModifier(runtime.methodModifierStatic())
        builder.computeAccess().commit() // eventual access from the visibility modifier + owner type
        return method
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

    private fun KaSession.mapType(type: KaType, owner: TypeInfo): ParameterizedType =
        with(typeMapper) { mapType(type, owner) }

    private fun KaSession.bootstrapObject() = with(typeMapper) { bootstrapObject() }

    private fun KaSession.applyHierarchy(builder: TypeInfo.Builder, owner: TypeInfo, classSymbol: KaClassSymbol) =
        with(typeMapper) { applyHierarchy(builder, owner, classSymbol) }

    private fun addMethodModifiers(builder: MethodInfo.Builder, symbol: KaDeclarationSymbol) =
        typeMapper.addMethodModifiers(builder, symbol)

    private fun mapVariance(variance: KotlinVariance): Variance = typeMapper.mapVariance(variance)

    private fun visibilityMethodModifier(symbol: KaDeclarationSymbol): MethodModifier? =
        typeMapper.visibilityMethodModifier(symbol)
}
