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

import io.codelaser.maddi.cst.impl.analysis.NullAnnotations;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.element.Element;
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

    /**
     * {@code boolean equals(Object)}, {@code int hashCode()} or {@code String toString()} redeclared abstract in an
     * interface, for its documentation (guava's Predicate, Function, Equivalence): Java's classes keep Object's
     * implementation, but in Kotlin the abstract override in the interface wins, and every class implementing it
     * would have to implement the member again. Left out: the interface means the same without it.
     */
    private static boolean abstractObjectMethod(MethodInfo m) {
        if (!m.typeInfo().isInterface() || !m.isAbstract() || m.isStatic()) return false;
        return "equals".equals(m.name()) && m.parameters().size() == 1
               && m.parameters().getFirst().parameterizedType().isJavaLangObject()
               || ("hashCode".equals(m.name()) || "toString".equals(m.name())) && m.parameters().isEmpty();
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
            if (!c.parameters().isEmpty() && c.parameters().stream().allMatch(p -> fieldByName.containsKey(p.name())
                    && !KotlinFieldPrinter.renamed(fieldByName.get(p.name())))
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

        // Java's @interface: Kotlin's annotation class, its elements the constructor's properties
        boolean annotationType = companion && typeInfo.typeNature().isAnnotation();
        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            // outside the class's scope, as its header: nacos's AbstractHealthChecker is annotated
            // @JsonTypeInfo(defaultImpl = None.class), None one of its nested classes
            out.add(KotlinAnnotations.print(typeInfo.annotations(), new KotlinHeaderQualification(insideType, typeInfo)));
            if (dataClass && typeInfo.typeNature().isRecord() && KotlinContext.translatingJava()
                && !accessorsAsFunctions(typeInfo, components)) {
                // a JVM record, as Java's: Java code calls item.varExprent(), not item.getVarExprent()
                out.add(new TextImpl("@JvmRecord")).add(SpaceEnum.ONE);
            }
            if (!isLocal(typeInfo)) {
                // a private nested type is visible in the whole Java file; Kotlin's private stops at its outer type
                // ... and public where a class that is not private names it in its signatures (guava's
                // MapMakerInternalMap: Java lets a package-private Segment take a private Entry, Kotlin exposes nothing)
                boolean privateNested = companion && !typeInfo.isPrimaryType()
                                        && typeInfo.access() != null && typeInfo.access().isPrivate();
                Optional<Keyword> visibility = privateNested
                        ? exposedBySignature(typeInfo) ? Optional.empty() : Optional.of(KotlinKeyword.INTERNAL)
                        : KotlinModifiers.visibility(typeInfo.access(), typeInfo);
                visibility.ifPresent(v -> out.add(v).add(SpaceEnum.ONE));
            }
            Set<TypeModifier> mods = typeInfo.typeModifiers();
            if (typeInfo.typeNature().isClass()) {
                if (mods.contains(TypeModifierEnum.ABSTRACT)) out.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
                else if (mods.contains(TypeModifierEnum.SEALED)) out.add(KeywordImpl.SEALED).add(SpaceEnum.ONE);
                else if (!mods.contains(TypeModifierEnum.FINAL)) out.add(KotlinKeyword.OPEN).add(SpaceEnum.ONE);
            }
            if (companion && inner(typeInfo) && !typeInfo.isAnonymous()
                && (!isLocal(typeInfo) || memberOfAnonymous(typeInfo))) {
                // Java's nested class sees the outer instance; an object expression takes only an inner class
                // (guava's HashBiMap: class MapEntry inside the anonymous BiIterator)
                out.add(new TextImpl("inner")).add(SpaceEnum.ONE);
            }
            if (annotationType) {
                out.add(new TextImpl("annotation")).add(SpaceEnum.ONE).add(KeywordImpl.CLASS);
            } else if (typeInfo.typeNature().isEnum()) {
                out.add(KeywordImpl.ENUM).add(SpaceEnum.ONE).add(KeywordImpl.CLASS);
            } else if (typeInfo.typeNature().isInterface()) {
                // a Kotlin lambda converts to a Kotlin interface only when it is a `fun interface`; a generic method
                // cannot be one's, nor is it a lambda's target in Java
                // nor one whose method is a property, String name() (nacos's RpcClientConfig)
                if (companion && typeInfo.isFunctionalInterface() && !typeInfo.typeNature().isAnnotation()
                    && (typeInfo.singleAbstractMethod() == null
                        || typeInfo.singleAbstractMethod().typeParameters().isEmpty()
                           && !KotlinMappedMembers.enumProperty(typeInfo.singleAbstractMethod()))) {
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
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.typeParameter(tp,
                                new KotlinHeaderQualification(insideType, typeInfo))
                                + (KotlinTypeName.nonNullClassTypeParameter(tp) ? " : Any" : ""))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                out.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
            }
            if (primary != null) {
                // guava's ClosingFuture.DeferredCloser: private DeferredCloser(CloseableList list) into a private field,
                // CloseableList a private nested class (internal): the header keeps both visibilities, or its public
                // constructor and property expose the internal type
                Optional<Keyword> constructorVisibility = typeInfo.typeNature().isEnum() ? Optional.empty()
                        : KotlinModifiers.visibility(primary.access(), typeInfo, primary);
                constructorVisibility.ifPresent(v -> out.add(SpaceEnum.ONE).add(v).add(SpaceEnum.ONE)
                        .add(KotlinKeyword.CONSTRUCTOR));
                out.add(primary.parameters().stream()
                        .map(p -> new OutputBuilderImpl()
                                .add(KotlinFieldPrinter.jvmModifiers(fieldByName.get(p.name())))
                                .add(propertyVisibility(fieldByName.get(p.name())))
                                // a record's component is read through its accessor, x(): a data class property. A
                                // private property has no accessors, and @JvmField none to replace
                                .add(!privateProperty(fieldByName.get(p.name())) && !typeInfo.typeNature().isRecord()
                                     && KotlinFieldPrinter.accessorClash(fieldByName.get(p.name()), fieldByName.get(p.name()).isFinal())
                                     || !privateProperty(fieldByName.get(p.name())) && !typeInfo.typeNature().isRecord()
                                        && !hasComponentMethods(typeInfo)
                                        && KotlinFieldPrinter.javaField(fieldByName.get(p.name()), KotlinModifiers.visibility(
                                                fieldByName.get(p.name()).access(), fieldByName.get(p.name()).owner()))
                                        ? new OutputBuilderImpl().add(new TextImpl("@JvmField")).add(SpaceEnum.ONE)
                                        : new OutputBuilderImpl())
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
            } else if (annotationType && typeInfo.methods().stream().anyMatch(KotlinTypePrinter::isAnnotationElement)) {
                out.add(typeInfo.methods().stream().filter(KotlinTypePrinter::isAnnotationElement)
                        .map(m -> annotationElement(m, insideType))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            }
            OutputBuilder superArguments = primary != null || !components.isEmpty()
                                           || constructors.stream().allMatch(KotlinTypePrinter::isImplicitDefaultConstructor)
                    ? new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                    : initConstructor != null ? superArguments(initConstructor, insideType) : null;
            List<OutputBuilder> supers = annotationType ? List.of()
                    : superTypes(superArguments, new KotlinHeaderQualification(insideType, typeInfo));
            if (!supers.isEmpty()) {
                out.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE)
                        .add(supers.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
            String where = KotlinTypeName.whereClause(typeInfo.typeParameters(),
                    new KotlinHeaderQualification(insideType, typeInfo));
            if (!where.isEmpty()) out.add(SpaceEnum.ONE).add(new TextImpl(where));
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
            if (accessorsAsFunctions(typeInfo, components)) {
                components.forEach(f -> members.add(componentAccessor(typeInfo, f, insideType)));
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
                .filter(m -> !isRecordAccessor(m) && !isAnnotationElement(m) && !abstractObjectMethod(m))
                .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(insideType)));
        if (KotlinContext.translatingJava()) members.addAll(KotlinInheritedDefaults.overrides(typeInfo, insideType));
        if (KotlinContext.translatingJava() && !typeInfo.isAbstract() && extendsNumber(typeInfo)) {
            // Java's Number implements byteValue() and shortValue() from intValue(); Kotlin's declares toByte() and
            // toShort() abstract (guava's UnsignedInteger, UnsignedLong, AtomicDouble)
            if (!declaresInChain(typeInfo, "byteValue")) {
                members.add(new OutputBuilderImpl().add(new TextImpl("override fun toByte(): Byte = toInt().toByte()")));
            }
            if (!declaresInChain(typeInfo, "shortValue")) {
                members.add(new OutputBuilderImpl().add(new TextImpl("override fun toShort(): Short = toInt().toShort()")));
            }
        }
        if (typeInfo.isAnonymous() && typeInfo.interfacesImplemented().stream().anyMatch(this::readOnlyIterator)) {
            // new Iterator<B>() {…} without remove(): the object is a MutableIterator, as where it goes
            // (MutableIterable.iterator()) wants one, and takes Java's default remove
            members.add(new OutputBuilderImpl().add(new TextImpl(
                    "override fun remove() { throw UnsupportedOperationException(\"remove\") }")));
        }
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
        if (typeInfo.typeNature().isInterface() || constructors.size() != 1) return null;
        MethodInfo c = constructors.getFirst();
        // an anonymous class's instance initializer, its one constructor: double-brace initialization, nacos's
        // new HashMap<String, String>(8) {{ put(…); }}; an object expression has an init block, and no constructor
        if (typeInfo.isAnonymous() && !c.parameters().isEmpty()) return null;
        return isImplicitDefaultConstructor(c) || returnsEarly(c) ? null : c;
    }

    /** A {@code return} in a constructor body, outside lambdas: an init block cannot return, a secondary constructor can. */
    static boolean returnsEarly(MethodInfo constructor) {
        if (constructor.methodBody() == null) return false;
        boolean[] found = {false};
        ((Element) constructor.methodBody()).visit((Element e) -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.Lambda) return false;
            if (e instanceof io.codelaser.maddi.cst.api.statement.ReturnStatement) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    /**
     * A type of the same file that is not private names {@code nested} in a supertype, or in a member that is not
     * private: Kotlin refuses a public declaration exposing an internal type.
     */
    private static boolean exposedBySignature(TypeInfo nested) {
        return exposedIn(nested.primaryType(), nested);
    }

    private static boolean exposedIn(TypeInfo t, TypeInfo nested) {
        if (t != nested && (t.access() == null || !t.access().isPrivate())) {
            java.util.stream.Stream<ParameterizedType> supertypes = java.util.stream.Stream.concat(
                    t.parentClass() == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(t.parentClass()),
                    t.interfacesImplemented().stream());
            if (supertypes.anyMatch(st -> names(st, nested))) return true;
            for (MethodInfo m : t.constructorAndMethodStream().toList()) {
                if (m.access() != null && m.access().isPrivate() || m.isSynthetic()) continue;
                if (m.returnType() != null && names(m.returnType(), nested)) return true;
                if (m.parameters().stream().anyMatch(p -> names(p.parameterizedType(), nested))) return true;
            }
            for (FieldInfo f : t.fields()) {
                if ((f.access() == null || !f.access().isPrivate()) && !f.isSynthetic() && names(f.type(), nested)) return true;
            }
        }
        return t.subTypes().stream().anyMatch(st -> st != nested && exposedIn(st, nested));
    }

    private static boolean names(ParameterizedType type, TypeInfo nested) {
        if (type == null) return false;
        if (type.typeInfo() == nested) return true;
        if (type.wildcard() != null && type.typeInfo() == null) return false;
        return type.parameters().stream().anyMatch(p -> names(p, nested));
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
        if (field == null || KotlinFieldPrinter.renamed(field)) return null;
        boolean companion = !fromKotlinSource(owner);
        if (companion && owner.typeNature().isRecord()) return field;
        List<MethodInfo> constructors = owner.constructors().stream().filter(x -> !x.isSynthetic()).toList();
        if (constructors.size() != 1 || constructors.getFirst() != c) return null;
        boolean allFields = c.parameters().stream()
                .allMatch(p -> owner.fields().stream().anyMatch(f -> f.name().equals(p.name())));
        return allFields && (!companion || onlyAssignsParameters(c)) ? field : null;
    }

    /**
     * Each statement assigns a parameter to the field of its name. Not to another field: guava's Invokable assigns
     * its member to accessibleObject and member, and the header property took the second, the first assignment lost.
     */
    private static boolean onlyAssignsParameters(MethodInfo c) {
        return c.methodBody() != null && c.methodBody().statements().stream().filter(s -> !s.isSynthetic())
                .allMatch(s -> s instanceof io.codelaser.maddi.cst.api.statement.ExpressionAsStatement eas
                               && eas.expression() instanceof io.codelaser.maddi.cst.api.expression.Assignment a
                               && a.value() instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                               && ve.variable() instanceof ParameterInfo pi
                               && a.variableTarget() != null && pi.name().equals(a.variableTarget().simpleName()));
    }

    private static boolean privateProperty(FieldInfo field) {
        return KotlinModifiers.visibility(field.access(), field.owner(), null)
                .equals(Optional.of(io.codelaser.maddi.cst.impl.output.KeywordImpl.PRIVATE));
    }

    /**
     * A header property's visibility: its field's, nothing for a public one. Parsed from Kotlin, the property's own,
     * which is its getter's: the front end models {@code class Foo(val id: Int)} as a private field and a public
     * {@code getId()}, and the field's would print the public property private.
     */
    private static OutputBuilder propertyVisibility(FieldInfo field) {
        OutputBuilder b = new OutputBuilderImpl();
        MethodInfo getter = fromKotlinSource(field.owner()) ? getter(field) : null;
        (getter != null ? KotlinModifiers.visibility(getter.access(), field.owner(), getter)
                : KotlinModifiers.visibility(field.access(), field.owner(), field)).ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
        return b;
    }

    /** The accessor that reads {@code field}, as the front end links it ({@code getSetField}); null when none. */
    private static MethodInfo getter(FieldInfo field) {
        return field.owner().methods().stream()
                .filter(m -> isAccessor(m) && m.getSetField().field() == field && m.parameters().isEmpty())
                .findFirst().orElse(null);
    }

    /** {@code private constructor(x: Int)}, or just {@code (x: Int)}. */
    private OutputBuilder primaryConstructorHeader(MethodInfo c, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        // class TextContentMixin @JsonCreator constructor(@JsonProperty("text") text: String): annotated, the primary
        // constructor needs its keyword
        OutputBuilder annotations = KotlinAnnotations.print(c.annotations(), q);
        java.util.Optional<io.codelaser.maddi.cst.api.output.element.Keyword> visibility = typeInfo.typeNature().isEnum()
                ? java.util.Optional.empty() : KotlinModifiers.visibility(c.access(), typeInfo, c);
        if (!annotations.isEmpty() || visibility.isPresent()) {
            b.add(SpaceEnum.ONE).add(annotations);
            visibility.ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
            b.add(KotlinKeyword.CONSTRUCTOR);
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
                            c.methodBody(), q), statements, q));
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
    /** An element of a Java annotation type, {@code String value() default ""}: a property in Kotlin. */
    static boolean isAnnotationElement(MethodInfo m) {
        return m.typeInfo().typeNature().isAnnotation() && !m.isStatic() && !m.isSynthetic()
               && m.parameters().isEmpty() && !fromKotlinSource(m.typeInfo());
    }

    /**
     * {@code val value: Array<String> = [""]}: a class is a {@code KClass}, and a single default for an array element
     * is that array. Java keeps a default as the element's body, a single return.
     */
    private static OutputBuilder annotationElement(MethodInfo m, Qualification q) {
        ParameterizedType type = m.returnType();
        OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.VAL).add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinNames.name(m.name()))).add(SymbolEnum.COLON_LABEL)
                .add(new TextImpl(KotlinAnnotations.elementType(type, q)));
        if (m.methodBody() != null && m.methodBody().statements().size() == 1
            && m.methodBody().statements().getFirst() instanceof io.codelaser.maddi.cst.api.statement.ReturnStatement rs
            && rs.expression() != null) {
            b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                    .add(KotlinAnnotations.elementValue(rs.expression(), type.arrays() > 0, type, q));
        }
        return b;
    }

    /**
     * A record whose component accessor implements a supertype's method, {@code T response()}: a Kotlin property
     * cannot override a function, and a {@code @JvmRecord}'s getter has the function's JVM signature. Not a JVM record
     * then; each component gets its accessor as a function, for the Java code that calls it.
     */
    private static boolean accessorsAsFunctions(TypeInfo record, List<FieldInfo> components) {
        return KotlinContext.translatingJava()
               && components.stream().anyMatch(f -> supertypeMethod(record, f.name(), new java.util.HashSet<>()));
    }

    private static boolean supertypeMethod(TypeInfo typeInfo, String name, Set<TypeInfo> visited) {
        List<ParameterizedType> supertypes = new ArrayList<>(typeInfo.interfacesImplemented());
        if (typeInfo.parentClass() != null) supertypes.add(typeInfo.parentClass());
        for (ParameterizedType st : supertypes) {
            TypeInfo t = st.typeInfo();
            if (t == null || !visited.add(t)) continue;
            if (t.methods().stream().anyMatch(m -> !m.isStatic() && m.parameters().isEmpty() && name.equals(m.name()))
                || supertypeMethod(t, name, visited)) return true;
        }
        return false;
    }

    /** {@code [override] fun response(): T = response}, beside the property. */
    private static OutputBuilder componentAccessor(TypeInfo record, FieldInfo component, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        if (supertypeMethod(record, component.name(), new java.util.HashSet<>())) {
            b.add(KotlinKeyword.OVERRIDE).add(SpaceEnum.ONE);
        }
        String name = KotlinNames.name(component.name());
        return b.add(KotlinKeyword.FUN).add(SpaceEnum.ONE).add(new TextImpl(name))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.COLON_LABEL)
                .add(new TextImpl(KotlinTypeName.of(KotlinNullability.fieldType(component), q)))
                .add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE).add(new TextImpl(name));
    }

    static boolean isRecordAccessor(MethodInfo m) {
        return !fromKotlinSource(m.typeInfo()) && CstQueries.isRecordAccessor(m);
    }

    /**
     * {@code companion object { … }} with the static fields, static methods and static initializers, in that order;
     * nothing when there are none. The enum constants stay entries.
     */
    private Stream<OutputBuilder> companionObject(FieldPrinterFactory fieldPrinterFactory,
                                                  MethodPrinterFactory methodPrinterFactory, Qualification q) {
        List<OutputBuilder> members = new ArrayList<>();
        // an inner, local or anonymous class can have no companion: its serialVersionUID (all guava's statics there)
        // goes, the class's stream id is then computed
        boolean noCompanion = inner(typeInfo) || isLocal(typeInfo) || typeInfo.isAnonymous();
        typeInfo.fields().stream().filter(f -> !f.isSynthetic() && f.isStatic() && !isEnumConstant(f))
                .filter(f -> !noCompanion || !"serialVersionUID".equals(f.name()))
                .filter(f -> !hoisted(f))
                .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
        typeInfo.subTypes().stream().flatMap(t -> t.fields().stream()).filter(KotlinTypePrinter::hoisted)
                .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
        typeInfo.methods().stream().filter(m -> !m.isSynthetic() && (m.isStatic() || m.isStaticInitializer()))
                .sorted(java.util.Comparator.comparing(MethodInfo::isStaticInitializer))
                .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(q)));
        if (members.isEmpty()) return Stream.of();
        return Stream.of(new OutputBuilderImpl().add(KotlinKeyword.COMPANION).add(SpaceEnum.ONE).add(KotlinKeyword.OBJECT)
                .add(SpaceEnum.ONE).add(members.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                        SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock()))));
    }

    /**
     * A static final field of an inner class, printed in the enclosing class's companion object: Kotlin allows no
     * companion in an inner class, whose members find the enclosing class's by their simple names. nacos's
     * ServerMemberManager.MemberInfoReportTask: {@code public static final long REPORT_INTERVAL}. Not where the
     * enclosing class has a static field of that name, nor another of its inner classes.
     */
    static boolean hoisted(FieldInfo f) {
        TypeInfo owner = f.owner();
        if (!KotlinContext.translatingJava() || f.isSynthetic() || !f.isStatic() || !f.isFinal() || owner == null
            || "serialVersionUID".equals(f.name()) || !inner(owner) || owner.isAnonymous() || isLocal(owner)) {
            return false;
        }
        TypeInfo enclosing = CstQueries.enclosing(owner);
        if (enclosing == null || enclosing == owner || inner(enclosing) || isLocal(enclosing) || enclosing.isAnonymous()
            || enclosing.typeNature().isInterface()) {
            return false;
        }
        return enclosing.fields().stream().noneMatch(g -> g.name().equals(f.name()))
               && enclosing.subTypes().stream().filter(t -> t != owner && inner(t))
                       .flatMap(t -> t.fields().stream()).noneMatch(g -> g.isStatic() && g.name().equals(f.name()));
    }

    /**
     * Java's inner class is Kotlin's: it sees the outer instance. Not a member class holding constants only, nacos's
     * RpcConstants.ClientSuffix (public class, static finals): nothing of it uses the outer instance, and Kotlin
     * allows no companion object in an inner class.
     */
    static boolean inner(TypeInfo typeInfo) {
        if (!typeInfo.isInnerClass()) return false;
        if (!KotlinContext.translatingJava() || typeInfo.isAnonymous() || isLocal(typeInfo)) return true;
        boolean staticOnly = (typeInfo.parentClass() == null || typeInfo.parentClass().isJavaLangObject())
                && typeInfo.interfacesImplemented().isEmpty()
                && typeInfo.fields().stream().allMatch(f -> f.isSynthetic() || f.isStatic())
                && typeInfo.fields().stream().anyMatch(f -> !f.isSynthetic() && f.isStatic())
                && typeInfo.methods().stream().allMatch(m -> m.isSynthetic() || m.isStatic() || m.isStaticInitializer())
                && typeInfo.constructors().stream().allMatch(c -> c.isSynthetic() || c.parameters().isEmpty()
                        && (c.methodBody() == null || c.methodBody().statements().stream().allMatch(st -> st.isSynthetic())))
                && typeInfo.subTypes().isEmpty();
        return !staticOnly;
    }

    /** Parsed from a {@code .kt} file: printed back in its own shape, not translated from Java's. */
    static boolean fromKotlinSource(TypeInfo typeInfo) {
        java.net.URI uri = typeInfo.compilationUnit().uri();
        return uri != null && (uri.toString().endsWith(".kt") || uri.toString().endsWith(".kts"));
    }

    /**
     * An enum constant: a static final field of the enum's own type, initialized by a constructor call. Not guava's
     * Types.JavaVersion.CURRENT, a static final field of the enum's type that a static block assigns one of the
     * constants; nor LocalCache.EntryFactory's {@code static final EntryFactory[] factories = {…}}.
     */
    private boolean isEnumConstant(FieldInfo f) {
        return typeInfo.typeNature().isEnum() && f.isStatic() && f.isFinal()
                && f.type().typeInfo() == typeInfo && f.type().arrays() == 0 && f.initializer() instanceof ConstructorCall;
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
            if (f.initializer() instanceof ConstructorCall cc && cc.anonymousClass() != null) {
                // JAVAC("javac") { override fun compile(…) }: an entry with members of its own
                OutputBuilder body = new KotlinTypePrinter(cc.anonymousClass(), true).print(
                        new CompilationUnitPrinterImpl.ImportDataImpl(List.of(), q, q), false);
                if (!body.isEmpty()) e.add(SpaceEnum.ONE).add(body);
            }
            return e;
        }).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
        if (moreMembersFollow) entries.add(SymbolEnum.SEMICOLON);
        return entries;
    }

    private static boolean isAccessor(MethodInfo methodInfo) {
        return methodInfo.getSetField() != null && methodInfo.getSetField().field() != null;
    }

    /**
     * A varargs constructor whose parameters become the class header's properties: a record's canonical one, or the
     * sole constructor of {@link #print}'s primary-constructor rule. Its last parameter is an {@code Array<T>} there, not
     * a vararg: a data class's primary constructor takes none (jenkins's FilePath.Archive, FilePath.Read).
     */
    static boolean varargsAsArray(MethodInfo c) {
        if (c == null || !c.isConstructor() || c.parameters().isEmpty() || !c.parameters().getLast().isVarArgs()
            || !KotlinContext.translatingJava()) {
            return false;
        }
        TypeInfo t = c.typeInfo();
        // a library's constructor keeps Java's varargs: jenkins's Messages, new Localizable(holder, key, arg0, arg1)
        if (fromKotlinSource(t) || !KotlinNullability.translated(t)) return false;
        if (t.typeNature().isRecord()) {
            return c.parameters().size() == t.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).count();
        }
        List<MethodInfo> constructors = t.constructors().stream().filter(x -> !x.isSynthetic()).toList();
        if (constructors.size() != 1 || constructors.getFirst() != c) return false;
        Map<String, FieldInfo> fieldByName = t.fields().stream()
                .collect(Collectors.toMap(FieldInfo::name, Function.identity(), (a, b) -> a));
        return c.parameters().stream().allMatch(p -> fieldByName.containsKey(p.name())
                && !KotlinFieldPrinter.renamed(fieldByName.get(p.name()))) && onlyAssignsParameters(c);
    }

    /** The implicit no-arg, empty-body constructor a class gets by default (not written in Kotlin). */
    private static boolean isImplicitDefaultConstructor(MethodInfo c) {
        return c.parameters().isEmpty() && (c.methodBody() == null || c.methodBody().isEmpty());
    }

    private static boolean extendsNumber(TypeInfo t) {
        for (ParameterizedType p = t.parentClass(); p != null && p.typeInfo() != null; p = p.typeInfo().parentClass()) {
            if ("java.lang.Number".equals(p.typeInfo().fullyQualifiedName())) return true;
        }
        return false;
    }

    /** A zero-parameter method of that name in the class or a translated superclass (not java.lang.Number's own). */
    private static boolean declaresInChain(TypeInfo t, String name) {
        for (TypeInfo c = t; c != null && !"java.lang.Number".equals(c.fullyQualifiedName());
             c = c.parentClass() == null ? null : c.parentClass().typeInfo()) {
            if (c.methods().stream().anyMatch(m -> name.equals(m.name()) && m.parameters().isEmpty())) return true;
        }
        return false;
    }

    /** A data class exposes generated destructuring accessors {@code component1()}, {@code component2()}, … */
    private static boolean hasComponentMethods(TypeInfo typeInfo) {
        return typeInfo.methods().stream().anyMatch(m -> m.parameters().isEmpty() && m.name().matches("component\\d+"));
    }

    /**
     * The supertypes after {@code :}. The superclass is called, {@code Super(args)}, only by a primary constructor (or
     * the implicit one); with secondary constructors only, each of those delegates, and the header names the type.
     */
    /**
     * {@code implements Iterator<E>} without a {@code remove()} of its own (in the class or a superclass): Java's
     * default remove throws UnsupportedOperationException. Kotlin's MutableIterator declares remove abstract; its
     * read-only Iterator does not, and Kotlin gives the class a remove that throws the same exception.
     */
    private boolean readOnlyIterator(ParameterizedType implemented) {
        if (implemented.typeInfo() == null || !"java.util.Iterator".equals(implemented.typeInfo().fullyQualifiedName())
            || !KotlinContext.translatingJava()) {
            return false;
        }
        for (TypeInfo t = typeInfo; t != null && !t.isJavaLangObject();
             t = t.parentClass() == null ? null : t.parentClass().typeInfo()) {
            if (t.methods().stream().anyMatch(m -> "remove".equals(m.name()) && m.parameters().isEmpty())) return false;
        }
        return true;
    }

    /**
     * A supertype with the nullability its type arguments declare by a type-use annotation: guava's
     * {@code Equivalence<T> implements BiPredicate<@Nullable T, @Nullable T>} is a {@code BiPredicate<T?, T?>}, and
     * the overrides of {@code test} take the {@code T?} the clause says.
     */
    /** As {@link #declaredArguments}, the type itself included: {@code <S extends @Nullable T>}'s bound is {@code T?}. */
    static ParameterizedType declared(ParameterizedType type) {
        ParameterizedType withArguments = declaredArguments(type);
        return type != null && type.wildcard() == null && type.annotations().stream()
                .anyMatch(ae -> NullAnnotations.NULLABLE.contains(ae.typeInfo().simpleName()))
                ? withArguments.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE) : withArguments;
    }

    static ParameterizedType declaredArguments(ParameterizedType supertype) {
        if (supertype == null || supertype.parameters().isEmpty()) return supertype;
        boolean changed = false;
        List<ParameterizedType> arguments = new ArrayList<>(supertype.parameters().size());
        for (ParameterizedType a : supertype.parameters()) {
            ParameterizedType inner = declaredArguments(a);
            // a bounded wildcard's too: ? super @Nullable V (guava's RangeMap.merge)
            if ((a.wildcard() == null || !a.isUnboundWildcard()) && a.arrays() == 0 && inner.annotations().stream()
                    .anyMatch(ae -> NullAnnotations.NULLABLE.contains(ae.typeInfo().simpleName()))) {
                inner = inner.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE);
            }
            changed |= inner != a;
            arguments.add(inner);
        }
        return changed ? supertype.withParameters(arguments) : supertype;
    }

    /**
     * A raw supertype, nacos's {@code AbstractRequestCallBack implements RequestCallBack}: Kotlin forbids the star
     * projections a raw type is elsewhere; Java's erasure, each type parameter's bound, is
     * {@code RequestCallBack<Response>}. Null when the supertype is not raw.
     */
    /**
     * The superclass as the header writes it: the verdicts' where they type its arguments (jenkins's
     * DiskSpaceMonitorDescriptor, whose own code puts null into its superclass's slot, extends
     * AbstractAsyncNodeMonitorDescriptor<DiskSpace?>), the declared one otherwise.
     */
    static ParameterizedType parentClass(TypeInfo typeInfo) {
        ParameterizedType verdict = KotlinContext.translatingJava()
                ? KotlinNullability.verdicts().parentClass(typeInfo) : null;
        return verdict != null ? verdict : typeInfo.parentClass();
    }

    static String rawSupertype(ParameterizedType supertype, Qualification q) {
        if (!KotlinContext.translatingJava() || supertype == null || supertype.typeInfo() == null) return null;
        return KotlinTypeName.erased(supertype, q);
    }

    private List<OutputBuilder> superTypes(OutputBuilder superArguments, Qualification q) {
        List<OutputBuilder> supers = new ArrayList<>();
        if (hasWrittenSuperclass(typeInfo)) {
            String sameName = sameNamedInnerSuperclass(q);
            if (sameName == null) sameName = innerOfAGenericOuter(q);
            String raw = sameName == null ? rawSupertype(typeInfo.parentClass(), q) : null;
            OutputBuilder parent = new OutputBuilderImpl().add(new TextImpl(sameName != null ? sameName
                    : raw != null ? raw : KotlinTypeName.of(declaredArguments(parentClass(typeInfo)), q)));
            if (superArguments != null) parent.add(superArguments);
            supers.add(parent);
        }
        typeInfo.interfacesImplemented().forEach(i -> {
            String raw = rawSupertype(i, q);
            supers.add(new OutputBuilderImpl().add(new TextImpl(raw != null ? raw : readOnlyIterator(i)
                    ? KotlinTypeName.of(declaredArguments(i), q).replaceFirst("^MutableIterator", "Iterator")
                    : KotlinTypeName.of(declaredArguments(i), q))));
        });
        if (overridesObjectClone(typeInfo) && !cloneable(typeInfo)) {
            // Java overrides Object.clone() anywhere; Kotlin's Any has no clone(), kotlin.Cloneable declares it
            supers.add(new OutputBuilderImpl().add(new TextImpl("Cloneable")));
        }
        return supers;
    }

    /**
     * guava's ConcurrentHashMultiset.EntrySet extends AbstractMultiset<E>.EntrySet: the superclass, an inner class of
     * the outer class's superclass, has the class's own simple name, which inside it means the class itself ("cycle in
     * supertypes"). Qualified by its outer class, with the type arguments the class's outer gives it. Null otherwise.
     */
    /**
     * The superclass with only its own type arguments: an inner class named through its parameterized outer,
     * {@code Base<E>.EntrySet}, has the outer's arguments first in {@code parameters()} (the Java front end and the
     * bytecode reader alike). The callers write the outer classes themselves, and then the class's own arguments.
     */
    private static ParameterizedType ownArgumentsOnly(ParameterizedType parent) {
        int own = parent.typeInfo().typeParameters().size();
        int all = parent.parameters().size();
        return all > own ? parent.withParameters(new ArrayList<>(parent.parameters().subList(all - own, all))) : parent;
    }

    private String sameNamedInnerSuperclass(Qualification q) {
        ParameterizedType parent = typeInfo.parentClass();
        TypeInfo parentType = parent.typeInfo();
        if (!parentType.simpleName().equals(typeInfo.simpleName()) || parentType.isPrimaryType()) return null;
        TypeInfo parentOuter = CstQueries.enclosing(parentType);
        TypeInfo outer = CstQueries.enclosing(typeInfo);
        if (parentOuter == null || outer == null) return null;
        String outerName;
        if (parentType.isInnerClass() && !parentOuter.typeParameters().isEmpty()) {
            ParameterizedType seen;
            try {
                seen = outer.asParameterizedType().concreteSuperType(parentOuter.asParameterizedType());
            } catch (RuntimeException | AssertionError e) {
                return null;
            }
            if (seen == null) return null;
            outerName = KotlinTypeName.of(seen, q);
        } else {
            outerName = KotlinTypeName.name(parentOuter, q);
        }
        String own = KotlinTypeName.of(ownArgumentsOnly(declaredArguments(parent)), q);
        int lt = own.indexOf('<');
        return outerName + "." + parentType.simpleName() + (lt < 0 ? "" : own.substring(lt));
    }

    /**
     * guava's AbstractMapBasedMultimap.WrappedList.WrappedListIterator extends WrappedIterator, an inner class of
     * WrappedCollection, itself an inner class of the generic AbstractMapBasedMultimap<K, V>: Kotlin wants the outer
     * classes' type arguments ("type arguments must be specified for outer class"), the same K and V the class sees
     * as a member of that outer. Null when the superclass is no inner class of an outer that is not the class's own.
     */
    private String innerOfAGenericOuter(Qualification q) {
        ParameterizedType parent = typeInfo.parentClass();
        TypeInfo parentType = parent.typeInfo();
        if (!parentType.isInnerClass() || CstQueries.enclosing(parentType) == CstQueries.enclosing(typeInfo)) return null;
        List<TypeInfo> chain = new ArrayList<>();
        for (TypeInfo t = CstQueries.enclosing(parentType); t != null; t = CstQueries.enclosing(t)) chain.addFirst(t);
        if (chain.isEmpty() || chain.stream().allMatch(t -> t.typeParameters().isEmpty())) return null;
        // only where the generic outers enclose this class too: their type parameters are in scope as they are
        java.util.Set<TypeInfo> ownOuters = new java.util.HashSet<>();
        for (TypeInfo t = CstQueries.enclosing(typeInfo); t != null; t = CstQueries.enclosing(t)) ownOuters.add(t);
        if (chain.stream().anyMatch(t -> !t.typeParameters().isEmpty() && !ownOuters.contains(t))) return null;
        StringBuilder sb = new StringBuilder();
        for (TypeInfo t : chain) {
            if (!sb.isEmpty()) sb.append('.');
            sb.append(KotlinNames.name(t.simpleName()));
            if (!t.typeParameters().isEmpty()) {
                sb.append(t.typeParameters().stream().map(tp -> KotlinNames.name(tp.simpleName()))
                        .collect(Collectors.joining(", ", "<", ">")));
            }
        }
        String own = KotlinTypeName.of(ownArgumentsOnly(declaredArguments(parent)), q);
        int lt = own.indexOf('<');
        return sb + "." + KotlinNames.name(parentType.simpleName()) + (lt < 0 ? "" : own.substring(lt));
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
    private static boolean memberOfAnonymous(TypeInfo typeInfo) {
        TypeInfo enclosing = CstQueries.enclosing(typeInfo);
        return enclosing != null && enclosing.isAnonymous() && typeInfo.enclosingMethod() == null;
    }

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
        return CstQueries.isExtensible(typeInfo);
    }
}
