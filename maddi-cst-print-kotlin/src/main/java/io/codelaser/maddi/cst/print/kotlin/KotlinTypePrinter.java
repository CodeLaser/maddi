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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.info.TypeModifierEnum;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prints a {@link TypeInfo} as a Kotlin type declaration, mirroring the Java {@code TypePrinterImpl} and its
 * pluggable-printer seam: the method/field/enclosed-type printers are supplied by factories (defaulting to the
 * Kotlin printers), so callers can substitute their own — exactly as in the Java case.
 *
 * <p>Best-effort reconstruction (requires the analyzer's prepwork phase, which populates {@code getSetField}):
 * <ul>
 *   <li>for a type parsed from Kotlin, getter/setter methods (a non-empty {@code getSetField}) are collapsed away —
 *       the backing field prints as a `val`/`var` property, avoiding the Kotlin platform-declaration clash of a
 *       property + its `getX()`. A Java getter stays a method: its callers print as calls;</li>
 *   <li>a single constructor whose parameters all name a field becomes the <b>primary constructor</b>
 *       (`class Foo(val id: Int)`); those fields and that constructor are then omitted from the body.</li>
 * </ul>
 * The class nature maps to `class`/`interface`/`enum class`; supertypes use Kotlin's `:` (a class parent as a
 * constructor call `Super()`). `public`/`final` are omitted (Kotlin defaults); a non-final class is `open`.
 */
public record KotlinTypePrinter(TypeInfo typeInfo, boolean formatter2) implements TypePrinter {

    @Override
    public List<TypeModifier> minimalModifiers(TypeInfo typeInfo) {
        return List.of();
    }

    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration) {
        CompilationUnitPrinterImpl printer = new CompilationUnitPrinterImpl(typeInfo.compilationUnit(), formatter2);
        CompilationUnitPrinter.ImportData importData = printer.computeImportData(importComputer, qualification);
        return print(importData, doTypeDeclaration);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration) {
        return print(importData, doTypeDeclaration, KotlinMethodPrinter::new, KotlinFieldPrinter::new, KotlinTypePrinter::new);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                               MethodPrinterFactory methodPrinterFactory, FieldPrinterFactory fieldPrinterFactory,
                               EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        KotlinContext.pushType(typeInfo);
        Set<String> shadowing = KotlinContext.shadowingParameters();
        KotlinContext.shadowingParameters(Set.of()); // an enclosing type's constructor parameters are not in scope here
        try {
            return printType(importData, doTypeDeclaration, methodPrinterFactory, fieldPrinterFactory,
                    enclosedTypePrinterFactory);
        } finally {
            KotlinContext.shadowingParameters(shadowing);
            KotlinContext.popType();
        }
    }

    private OutputBuilder printType(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                                    MethodPrinterFactory methodPrinterFactory, FieldPrinterFactory fieldPrinterFactory,
                                    EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        Qualification insideType = importData.insideType();
        // Java's static members go to the companion object; a type parsed from Kotlin has its own JVM shape for them
        boolean companion = !fromKotlinSource(typeInfo);

        List<MethodInfo> constructors = typeInfo.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        Map<String, FieldInfo> fieldByName = typeInfo.fields().stream()
                .collect(Collectors.toMap(FieldInfo::name, Function.identity(), (a, b) -> a));
        boolean dataClass = typeInfo.typeNature().isRecord() || hasComponentMethods(typeInfo);

        // a single constructor whose parameters each name a field => the primary constructor `class Foo(val id: Int)`
        MethodInfo primary = null;
        if (constructors.size() == 1) {
            MethodInfo c = constructors.getFirst();
            if (!c.parameters().isEmpty() && c.parameters().stream().allMatch(p -> fieldByName.containsKey(p.name()))
                && (!companion || onlyAssignsParameters(c))) {
                primary = c;
            }
        }
        // Java only: a record's components, or the one constructor as `class Foo(x: Int) { … init { body } }`
        List<FieldInfo> components = companion && typeInfo.typeNature().isRecord()
                ? typeInfo.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList() : List.of();
        MethodInfo canonical = components.isEmpty() ? null : constructors.stream()
                .filter(c -> c.parameters().size() == components.size()).findFirst().orElse(null);
        MethodInfo initConstructor = companion && primary == null && components.isEmpty() ? soleConstructor(constructors)
                : canonical;
        if (!components.isEmpty()) primary = null;
        Set<String> headerFields = !components.isEmpty()
                ? components.stream().map(FieldInfo::name).collect(Collectors.toSet())
                : primary == null ? Set.of() : primary.parameters().stream().map(ParameterInfo::name).collect(Collectors.toSet());
        MethodInfo primaryFinal = primary;
        if (components.isEmpty() && typeInfo.typeNature().isRecord()) dataClass = false; // a data class needs a property

        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            if (!isLocal(typeInfo)) {
                // a private nested type is visible in the whole Java file; Kotlin's private stops at its outer type
                Optional<Keyword> visibility = companion && !typeInfo.isPrimaryType()
                                               && typeInfo.access() != null && typeInfo.access().isPrivate()
                        ? Optional.of(KotlinKeyword.INTERNAL) : KotlinModifiers.visibility(typeInfo.access(), typeInfo);
                visibility.ifPresent(v -> out.add(v).add(SpaceEnum.ONE));
            }
            Set<TypeModifier> mods = typeInfo.typeModifiers();
            if (typeInfo.typeNature().isClass()) {
                if (mods.contains(TypeModifierEnum.ABSTRACT)) out.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
                else if (mods.contains(TypeModifierEnum.SEALED)) out.add(KeywordImpl.SEALED).add(SpaceEnum.ONE);
                else if (!mods.contains(TypeModifierEnum.FINAL)) out.add(KotlinKeyword.OPEN).add(SpaceEnum.ONE);
            }
            if (companion && typeInfo.isInnerClass() && !isLocal(typeInfo) && !typeInfo.isAnonymous()) {
                out.add(new TextImpl("inner")).add(SpaceEnum.ONE); // Java's nested class sees the outer instance
            }
            if (typeInfo.typeNature().isEnum()) {
                out.add(KeywordImpl.ENUM).add(SpaceEnum.ONE).add(KeywordImpl.CLASS);
            } else if (typeInfo.typeNature().isInterface()) {
                // a Kotlin lambda converts to a Kotlin interface only when it is a `fun interface`
                if (companion && typeInfo.isFunctionalInterface() && !typeInfo.typeNature().isAnnotation()) {
                    out.add(KotlinKeyword.FUN).add(SpaceEnum.ONE);
                }
                out.add(KeywordImpl.INTERFACE);
            } else if (dataClass) {
                out.add(KotlinKeyword.DATA).add(SpaceEnum.ONE).add(KeywordImpl.CLASS); // record / Kotlin data class
            } else {
                out.add(KeywordImpl.CLASS);
            }
            out.add(SpaceEnum.ONE).add(new TextImpl(KotlinNames.name(typeInfo.simpleName())));

            if (!typeInfo.typeParameters().isEmpty()) {
                out.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                out.add(typeInfo.typeParameters().stream()
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.typeParameter(tp, insideType))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                out.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
            }
            if (primary != null) {
                out.add(primary.parameters().stream()
                        .map(p -> new OutputBuilderImpl()
                                .add(fieldByName.get(p.name()).isFinal() ? KotlinKeyword.VAL : KotlinKeyword.VAR)
                                .add(SpaceEnum.ONE).add(new TextImpl(KotlinNames.name(p.name())))
                                .add(SymbolEnum.COLON_LABEL)
                                // a property: the field's type, with its verdict, as every use of it reads it
                                .add(new TextImpl(KotlinTypeName.of(KotlinNullability.fieldType(fieldByName.get(p.name())),
                                        insideType))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            } else if (!components.isEmpty()) {
                out.add(components.stream()
                        .map(f -> new OutputBuilderImpl().add(KotlinKeyword.VAL).add(SpaceEnum.ONE)
                                .add(new TextImpl(KotlinNames.name(f.name()))).add(SymbolEnum.COLON_LABEL)
                                .add(new TextImpl(KotlinTypeName.of(KotlinNullability.fieldType(f), insideType))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            } else if (initConstructor != null) {
                out.add(primaryConstructorHeader(initConstructor, insideType));
            }
            OutputBuilder superArguments = primary != null || !components.isEmpty()
                                           || constructors.stream().allMatch(KotlinTypePrinter::isImplicitDefaultConstructor)
                    ? new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                    : initConstructor != null ? superArguments(initConstructor, insideType) : null;
            List<OutputBuilder> supers = superTypes(superArguments, insideType);
            if (!supers.isEmpty()) {
                out.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE)
                        .add(supers.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
        }

        List<OutputBuilder> members = new ArrayList<>();
        // enum constants render as Kotlin entries (`RED, GREEN, BLUE`), not as `val RED = Color()` properties
        if (!enumConstants().isEmpty()) {
            members.add(enumEntries(typeInfo.methods().stream().anyMatch(m -> !m.isSynthetic())
                                    || !typeInfo.subTypes().isEmpty() || !typeInfo.fields().stream().allMatch(this::isEnumConstant)
                                    || constructors.stream().anyMatch(c -> !isImplicitDefaultConstructor(c)), insideType));
        }
        // the primary constructor's parameters are in scope in property initializers and init blocks, where the
        // Java code meant the field of the same name
        Set<String> parameterNames = initConstructor == null ? Set.of()
                : initConstructor.parameters().stream().map(ParameterInfo::name).collect(Collectors.toSet());
        KotlinContext.shadowingParameters(parameterNames);
        try {
            typeInfo.fields().stream()
                    .filter(f -> !f.isSynthetic() && !headerFields.contains(f.name()) && !isEnumConstant(f))
                    .filter(f -> !companion || !f.isStatic())
                    .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(insideType, false)));
            if (initConstructor != null) {
                OutputBuilder init = initBlock(initConstructor, insideType);
                if (init != null) members.add(init);
            }
        } finally {
            KotlinContext.shadowingParameters(Set.of());
        }
        constructors.stream()
                .filter(c -> c != primaryFinal && c != initConstructor && !isImplicitDefaultConstructor(c))
                .forEach(c -> members.add(methodPrinterFactory.create(typeInfo, c, formatter2).print(insideType)));
        typeInfo.methods().stream()
                // data-class componentN/copy are synthetic (front-end). A Kotlin property's accessors are its own; a Java
                // getter stays a method, because its callers print as calls (prepwork marks it all the same)
                .filter(m -> !m.isSynthetic() && (companion || !isAccessor(m)))
                .filter(m -> !companion || !m.isStatic() && !m.isStaticInitializer())
                .filter(m -> !isRecordAccessor(m))
                .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(insideType)));
        typeInfo.subTypes().stream()
                .filter(st -> !st.isSynthetic())
                .forEach(st -> members.add(enclosedTypePrinterFactory.create(st, formatter2).print(importData, true)));
        if (companion) companionObject(fieldPrinterFactory, methodPrinterFactory, insideType).forEach(members::add);

        if (!members.isEmpty()) {
            // NEWLINE between members: Kotlin has no `;`, so members must be newline-separated to stay valid
            out.add(SpaceEnum.ONE).add(members.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                    SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
        }
        return out;
    }

    /** The one explicit constructor of a Java class, which becomes the primary one; null when there are more. */
    private MethodInfo soleConstructor(List<MethodInfo> constructors) {
        if (typeInfo.isAnonymous() || typeInfo.typeNature().isInterface() || constructors.size() != 1) return null;
        MethodInfo c = constructors.getFirst();
        return isImplicitDefaultConstructor(c) ? null : c;
    }

    /** {@code this.x = x; …} and nothing else: the parameters ARE the properties. */
    /**
     * The property a constructor parameter declares, when its constructor is printed as the primary constructor
     * {@code class Foo(val id: Int)} or is a record's canonical one: the field of the same name. Null otherwise. A
     * call's argument for that parameter goes into the property, so it is checked against the field's type.
     */
    static FieldInfo propertyOf(ParameterInfo parameter) {
        MethodInfo c = parameter.methodInfo();
        if (!c.isConstructor()) return null;
        TypeInfo owner = c.typeInfo();
        FieldInfo field = owner.fields().stream().filter(f -> f.name().equals(parameter.name()) && !f.isStatic())
                .findFirst().orElse(null);
        if (field == null) return null;
        boolean companion = !fromKotlinSource(owner);
        if (companion && owner.typeNature().isRecord()) return field;
        List<MethodInfo> constructors = owner.constructors().stream().filter(x -> !x.isSynthetic()).toList();
        if (constructors.size() != 1 || constructors.getFirst() != c) return null;
        boolean allFields = c.parameters().stream()
                .allMatch(p -> owner.fields().stream().anyMatch(f -> f.name().equals(p.name())));
        return allFields && (!companion || onlyAssignsParameters(c)) ? field : null;
    }

    private static boolean onlyAssignsParameters(MethodInfo c) {
        return c.methodBody() != null && c.methodBody().statements().stream().filter(s -> !s.isSynthetic())
                .allMatch(s -> s instanceof io.codelaser.maddi.cst.api.statement.ExpressionAsStatement eas
                               && eas.expression() instanceof io.codelaser.maddi.cst.api.expression.Assignment a
                               && a.value() instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                               && ve.variable() instanceof ParameterInfo);
    }

    /** {@code private constructor(x: Int)}, or just {@code (x: Int)}. */
    private OutputBuilder primaryConstructorHeader(MethodInfo c, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        if (!typeInfo.typeNature().isEnum()) {
            KotlinModifiers.visibility(c.access(), typeInfo, c).ifPresent(v -> b.add(SpaceEnum.ONE).add(v).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.CONSTRUCTOR));
        }
        return b.add(KotlinMethodPrinter.parameters(c, q));
    }

    /** The arguments of the constructor's {@code super(…)}, or {@code ()}. */
    private static OutputBuilder superArguments(MethodInfo c, Qualification q) {
        io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation eci = KotlinMethodPrinter.explicitConstructorInvocation(c);
        if (eci == null || eci.parameterExpressions().isEmpty()) {
            return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        return KotlinExpressionPrinter.arguments(eci.parameterExpressions(), eci.methodInfo(), q);
    }

    /**
     * The constructor body, without its {@code super(…)}, as {@code init { }}; for a record's canonical constructor,
     * also without the component assignments the primary constructor does. Null when nothing is left.
     */
    private OutputBuilder initBlock(MethodInfo c, Qualification q) {
        if (c.methodBody() == null) return null;
        List<io.codelaser.maddi.cst.api.statement.Statement> statements = c.methodBody().statements().stream()
                .filter(st -> !st.isSynthetic())
                .filter(st -> !(st instanceof io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation))
                .filter(st -> !typeInfo.typeNature().isRecord() || !isComponentAssignment(st))
                .toList();
        if (statements.isEmpty()) return null;
        // the constructor's own scope: its locals' verdicts are asked for with it, its pattern variables are its own
        var scope = KotlinContext.enterMethod(c);
        try {
            c.parameters().forEach(p -> KotlinContext.declared(p.name()));
            return new OutputBuilderImpl().add(new TextImpl("init")).add(SpaceEnum.ONE)
                    .add(KotlinStatementPrinter.block(KotlinStatementPrinter.reassignedParameters(c.parameters(),
                            c.methodBody()), statements, q));
        } finally {
            KotlinContext.exitMethod(scope);
        }
    }

    private static boolean isComponentAssignment(io.codelaser.maddi.cst.api.statement.Statement st) {
        return st instanceof io.codelaser.maddi.cst.api.statement.ExpressionAsStatement eas
               && eas.expression() instanceof io.codelaser.maddi.cst.api.expression.Assignment a
               && a.variableTarget() instanceof io.codelaser.maddi.cst.api.variable.FieldReference;
    }

    /** {@code x()} of a record with component {@code x}: the data class's property {@code x} takes its place. */
    static boolean isRecordAccessor(MethodInfo m) {
        TypeInfo owner = m.typeInfo();
        return owner.typeNature().isRecord() && !m.isStatic() && m.parameters().isEmpty() && !fromKotlinSource(owner)
               && owner.fields().stream().anyMatch(f -> !f.isStatic() && f.name().equals(m.name()));
    }

    /** A Java class with more than one constructor has no primary one, so its final fields cannot be vals. */
    static boolean finalFieldsAssignedInSecondaryConstructors(TypeInfo typeInfo) {
        return !fromKotlinSource(typeInfo) && !typeInfo.typeNature().isRecord()
               && typeInfo.constructors().stream().filter(c -> !c.isSynthetic() && !isImplicitDefaultConstructor(c))
                       .count() > 1;
    }

    /**
     * {@code companion object { … }} with the static fields, static methods and static initializers, in that order;
     * nothing when there are none. The enum constants stay entries.
     */
    private Stream<OutputBuilder> companionObject(FieldPrinterFactory fieldPrinterFactory,
                                                  MethodPrinterFactory methodPrinterFactory, Qualification q) {
        List<OutputBuilder> members = new ArrayList<>();
        typeInfo.fields().stream().filter(f -> !f.isSynthetic() && f.isStatic() && !isEnumConstant(f))
                .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
        typeInfo.methods().stream().filter(m -> !m.isSynthetic() && (m.isStatic() || m.isStaticInitializer()))
                .sorted(java.util.Comparator.comparing(MethodInfo::isStaticInitializer))
                .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(q)));
        if (members.isEmpty()) return Stream.of();
        return Stream.of(new OutputBuilderImpl().add(KotlinKeyword.COMPANION).add(SpaceEnum.ONE).add(KotlinKeyword.OBJECT)
                .add(SpaceEnum.ONE).add(members.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                        SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock()))));
    }

    /** Parsed from a {@code .kt} file: printed back in its own shape, not translated from Java's. */
    static boolean fromKotlinSource(TypeInfo typeInfo) {
        java.net.URI uri = typeInfo.compilationUnit().uri();
        return uri != null && (uri.toString().endsWith(".kt") || uri.toString().endsWith(".kts"));
    }

    /** An enum constant: a static final field of the enum's own type (initialized by a constructor call). */
    private boolean isEnumConstant(FieldInfo f) {
        return typeInfo.typeNature().isEnum() && f.isStatic() && f.isFinal()
                && f.type().typeInfo() == typeInfo;
    }

    private List<FieldInfo> enumConstants() {
        return typeInfo.fields().stream().filter(this::isEnumConstant).toList();
    }

    /** `RED, GREEN, BLUE` (with `(args)` where a constant has constructor arguments); `;`-terminated if more follows. */
    private OutputBuilder enumEntries(boolean moreMembersFollow, Qualification q) {
        OutputBuilder entries = enumConstants().stream().map(f -> {
            OutputBuilder e = new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(f.name())));
            if (f.initializer() instanceof ConstructorCall cc && !cc.parameterExpressions().isEmpty()) {
                e.add(KotlinExpressionPrinter.arguments(cc.parameterExpressions(), cc.constructor(), q));
            }
            return e;
        }).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
        if (moreMembersFollow) entries.add(SymbolEnum.SEMICOLON);
        return entries;
    }

    private static boolean isAccessor(MethodInfo methodInfo) {
        return methodInfo.getSetField() != null && methodInfo.getSetField().field() != null;
    }

    /** The implicit no-arg, empty-body constructor a class gets by default (not written in Kotlin). */
    private static boolean isImplicitDefaultConstructor(MethodInfo c) {
        return c.parameters().isEmpty() && (c.methodBody() == null || c.methodBody().isEmpty());
    }

    /** A data class exposes generated destructuring accessors {@code component1()}, {@code component2()}, … */
    private static boolean hasComponentMethods(TypeInfo typeInfo) {
        return typeInfo.methods().stream().anyMatch(m -> m.parameters().isEmpty() && m.name().matches("component\\d+"));
    }

    /**
     * The supertypes after {@code :}. The superclass is called, {@code Super(args)}, only by a primary constructor (or
     * the implicit one); with secondary constructors only, each of those delegates, and the header names the type.
     */
    private List<OutputBuilder> superTypes(OutputBuilder superArguments, Qualification q) {
        List<OutputBuilder> supers = new ArrayList<>();
        if (hasWrittenSuperclass(typeInfo)) {
            OutputBuilder parent = new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(typeInfo.parentClass(), q)));
            if (superArguments != null) parent.add(superArguments);
            supers.add(parent);
        }
        typeInfo.interfacesImplemented().forEach(i ->
                supers.add(new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(i, q)))));
        if (overridesObjectClone(typeInfo) && !cloneable(typeInfo)) {
            // Java overrides Object.clone() anywhere; Kotlin's Any has no clone(), kotlin.Cloneable declares it
            supers.add(new OutputBuilderImpl().add(new TextImpl("Cloneable")));
        }
        return supers;
    }

    private static boolean overridesObjectClone(TypeInfo typeInfo) {
        // Object's directly: a subclass overriding its parent's clone() inherits the parent's Cloneable, and a second
        // one makes `super.clone()` ambiguous ("multiple supertypes available")
        return typeInfo.methods().stream().anyMatch(m -> "clone".equals(m.name()) && m.parameters().isEmpty()
                && !m.isStatic() && !m.overrides().isEmpty() && m.overrides().stream()
                        .allMatch(o -> "java.lang.Object".equals(o.typeInfo().fullyQualifiedName())));
    }

    private static boolean cloneable(TypeInfo typeInfo) {
        if ("java.lang.Cloneable".equals(typeInfo.fullyQualifiedName())) return true;
        if (typeInfo.parentClass() != null && typeInfo.parentClass().typeInfo() != null
            && cloneable(typeInfo.parentClass().typeInfo())) return true;
        return typeInfo.interfacesImplemented().stream()
                .anyMatch(i -> i.typeInfo() != null && cloneable(i.typeInfo()));
    }

    /** A class declared in a method body: not reachable as a member type from its primary type. No visibility in Kotlin. */
    static boolean isLocal(TypeInfo typeInfo) {
        return !typeInfo.isAnonymous() && !typeInfo.isPrimaryType() && !isMemberOf(typeInfo.primaryType(), typeInfo);
    }

    private static boolean isMemberOf(TypeInfo outer, TypeInfo typeInfo) {
        return outer.subTypes().stream().anyMatch(st -> st == typeInfo || isMemberOf(st, typeInfo));
    }

    /** The type extends a class Kotlin writes: not Object, Enum or Record, which are implicit. */
    static boolean hasWrittenSuperclass(TypeInfo typeInfo) {
        ParameterizedType parent = typeInfo.parentClass();
        if (parent == null || parent.isJavaLangObject() || parent.typeInfo() == null) return false;
        String fqn = parent.typeInfo().fullyQualifiedName();
        return !"java.lang.Enum".equals(fqn) && !"java.lang.Record".equals(fqn);
    }

    /** A class that can be extended: printed {@code open} (or abstract/sealed), so its methods may be open too. */
    static boolean isOpen(TypeInfo typeInfo) {
        Set<TypeModifier> mods = typeInfo.typeModifiers();
        return typeInfo.typeNature().isClass() && !mods.contains(TypeModifierEnum.FINAL)
               && !typeInfo.typeNature().isRecord() && !typeInfo.typeNature().isEnum();
    }
}
