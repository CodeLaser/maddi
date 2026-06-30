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
) {
    private val symbolScanner = KotlinSymbolScanner(runtime, infoByFqn, sourceSet)
    private var memberDepth = 0
    private val maxMemberDepth = 2 // load members this many levels deep; deeper co-loaded types stay shells

    /**
     * Map a resolved Kotlin type to a CST [ParameterizedType], in the context of [owner] (whose type
     * parameters a bare `T` may refer to). Handles: the Kotlin builtins with a JVM/java.lang counterpart;
     * type parameters; and references to other types of the current compilation, with their generic
     * arguments. A nullable `T?` is boxed and tagged [NullableState.NULLABLE]. External/library types
     * (the CompiledTypesManager's job) still fall back to Object.
     */
    internal fun KaSession.mapType(type: KaType, owner: TypeInfo): ParameterizedType {
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
            if (jvmFqn != kotlinFqn) {
                // mapped (kotlin.collections.List -> java.util.List): load the JAVA symbol, so the shared
                // java.* type carries its real JVM surface and matches the Java front-end + AAPI -- not the
                // Kotlin read-only view (which would be order-dependent: List vs MutableList both map here)
                val javaSymbol = findClass(ClassId.topLevel(FqName(jvmFqn))) as? KaNamedClassSymbol
                if (javaSymbol != null) loadLibraryType(javaSymbol, jvmFqn)
                else symbolScanner.getOrLoad(jvmFqn, type.typeArguments.size) // not on classpath -> shell
            } else loadLibraryType(type.symbol as KaNamedClassSymbol, jvmFqn) // non-mapped -> deepen from symbol
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
        builder.computeAccess()

        // Members, flattened: the full member scope (declared + inherited), so calls resolve to inherited
        // methods too (`equals`/`hashCode`/`toString` from Any, interface methods, …) -- the predefined
        // Object carries no such instance methods, so they must sit on each type. Bounded by depth so the
        // cascade terminates: types referenced beyond maxMemberDepth stay hierarchy-only shells. Depth 2 lets
        // a single chained call resolve (e.g. list.iterator().next() -- Iterator gets members too).
        if (memberDepth < maxMemberDepth) {
            memberDepth++
            try {
                // dedup by FQN: flattened overloads can erase to the same signature (e.g. printStackTrace
                // (PrintStream)/(PrintWriter) both map to Object on a shell), which the type map rejects
                val seen = mutableSetOf<String>()
                symbol.memberScope.declarations
                    .filterIsInstance<KaNamedFunctionSymbol>()
                    .map { convertLibraryMethod(typeInfo, it) }
                    .forEach { if (seen.add(it.fullyQualifiedName())) builder.addMethod(it) }
                // properties -> fields, so `obj.size`/`obj.length` resolve (the body resolver reads a
                // property access as a field access, like a source type's backing field)
                val seenFields = mutableSetOf<String>()
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
    private fun KaSession.convertLibraryMethod(owner: TypeInfo, function: KaNamedFunctionSymbol): MethodInfo {
        val method = runtime.newMethod(owner, function.name.asString(), runtime.methodTypeMethod())
        val builder = method.builder()
        function.valueParameters.forEach { p -> builder.addParameter(p.name.asString(), mapType(p.returnType, owner)) }
        builder
            .setReturnType(mapType(function.returnType, owner))
            .setMethodBody(runtime.emptyBlock())
            .setMissingData(runtime.methodMissingMethodBody()) // no body available (like a class-file method)
        addMethodModifiers(builder, function)
        builder.commitParameters().computeAccess().commit()
        return method
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
