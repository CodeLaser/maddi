package org.e2immu.language.cst.api.info;


import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.support.Either;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface TypeInfo extends NamedType, Info {

    // for java, that will be packageName == "java.lang"
    boolean doesNotRequirePackage();

    default CompilationUnit compilationUnit() {
        return compilationUnitOrEnclosingType().isLeft() ? compilationUnitOrEnclosingType().getLeft()
                : compilationUnitOrEnclosingType().getRight().compilationUnit();
    }

    boolean hasBeenInspected();

    boolean isAnnotation();

    boolean isAnonymous();

    boolean isEnclosedIn(TypeInfo typeInfo);

    default boolean isPrimaryType() {
        return compilationUnitOrEnclosingType().isLeft();
    }

    default boolean isInnerClass() {
        return typeNature().isClass() && compilationUnitOrEnclosingType().isRight() && !isStatic();
    }

    boolean isSealed();

    String packageName();

    // chain of type names Primary.Sub.Sub2
    String fromPrimaryTypeDownwards();

    Either<CompilationUnit, TypeInfo> compilationUnitOrEnclosingType();

    default Iterable<ParameterizedType> parentAndInterfacesImplemented() {
        Stream<ParameterizedType> stream = Stream.concat(Stream.ofNullable(parentClass()),
                interfacesImplemented().stream());
        return stream::iterator;
    }

    // from inspection
    Set<TypeInfo> superTypesExcludingJavaLangObject();

    Set<TypeModifier> typeModifiers();

    List<TypeParameter> typeParameters();

    MethodInfo findUniqueMethod(String methodName, int n);

    MethodInfo findConstructor(int i);

    MethodInfo findConstructor(TypeInfo typeOfFirstParameter);

    TypeInfo findSubType(String simpleName);

    TypeInfo findSubType(String simpleName, boolean complain);

    FieldInfo getFieldByName(String name, boolean complain);

    MethodInfo findUniqueMethod(String name, TypeInfo typeInfoOfFirstParameter);

    ParameterizedType parentClass();

    List<ParameterizedType> interfacesImplemented();

    default List<MethodInfo> methods() {
        return methodStream().toList();
    }

    Stream<MethodInfo> recursiveMethodStream();

    boolean isPrimitiveExcludingVoid();

    boolean isJavaIoSerializable();

    boolean isJavaLangObject();

    /**
     * @return null when the type is not a functional interface
     */
    MethodInfo singleAbstractMethod();

    boolean isNumeric();

    boolean isBoxedExcludingVoid();

    boolean isFunctionalInterface();

    boolean isJavaLangString();

    boolean isJavaLangClass();

    boolean isVoid();

    boolean isJavaLangVoid();

    boolean isBoxedBoolean();

    List<TypeInfo> subTypes();

    boolean isPrivate();

    boolean isAbstract();

    boolean isCharacter();

    boolean isBoxedLong();

    boolean isInteger();

    boolean isBoxedShort();

    boolean isBoxedByte();

    boolean isBoxedDouble();

    boolean isBoxedFloat();

    Stream<MethodInfo> methodStream();

    List<MethodInfo> constructors();

    List<FieldInfo> fields();

    boolean isStatic();

    boolean isInterface();

    TypeInfo primaryType();

    boolean isBoolean();

    boolean isInt();

    boolean isLong();

    boolean isShort();

    boolean isByte();

    boolean isFloat();

    boolean isDouble();

    boolean isChar();

    boolean isPublic();

    boolean isPubliclyAccessible();

    void setOnDemandInspection(Consumer<TypeInfo> inspector);

    boolean haveOnDemandInspection();

    Builder builder();

    TypeNature typeNature();

    int anonymousTypes();

    default boolean hierarchyNotYetDone() {
        return !hasBeenInspected() && builder().hierarchyNotYetDone();
    }

    List<TypeInfo> permittedWhenSealed();

    boolean isFinal();

    boolean isNonSealed();

    interface Builder extends Info.Builder<Builder> {
        Builder addPermittedType(TypeInfo typeInfo);

        List<MethodInfo> constructors();

        List<FieldInfo> fields();

        int getAndIncrementAnonymousTypes();

        boolean isAbstract();

        List<MethodInfo> methods();

        @Fluent
        Builder setAnonymousTypes(int anonymousTypes);

        @Fluent
        Builder setEnclosingMethod(MethodInfo methodInfo);

        @Fluent
        Builder addSubType(TypeInfo subType);

        @Fluent
        Builder addTypeModifier(TypeModifier typeModifier);

        @Fluent
        Builder addMethod(MethodInfo methodInfo);

        @Fluent
        Builder addField(FieldInfo field);

        @Fluent
        Builder addConstructor(MethodInfo constructor);

        @Fluent
        Builder setTypeNature(TypeNature typeNature);

        @Fluent
        Builder setParentClass(ParameterizedType parentClass);

        @Fluent
        Builder addInterfaceImplemented(ParameterizedType interfaceImplemented);

        @Fluent
        Builder addOrSetTypeParameter(TypeParameter typeParameter);

        @Fluent
        Builder setSingleAbstractMethod(MethodInfo singleAbstractMethod);

        Source source();

        TypeNature typeNature();

        boolean hierarchyNotYetDone();

        @Fluent
        Builder hierarchyIsDone();
    }

    boolean isAtLeastImmutableHC();

    OutputBuilder print(Qualification qualification, boolean doTypeDeclaration);

    // as part of type resolution
    boolean fieldsAccessedInRestOfPrimaryType();


    default Stream<TypeInfo> recursiveSubTypeStream() {
        return Stream.concat(Stream.of(this), subTypes().stream().flatMap(TypeInfo::recursiveSubTypeStream));
    }

    default Stream<TypeInfo> recursiveSuperTypeStream() {
        Stream<TypeInfo> s1;
        if (compilationUnitOrEnclosingType().isRight() && !isStatic()) {
            TypeInfo right = compilationUnitOrEnclosingType().getRight();
            s1 = Stream.concat(Stream.of(right), right.recursiveSuperTypeStream());
        } else {
            s1 = Stream.of();
        }
        Stream<TypeInfo> s2;
        if (parentClass() != null) {
            TypeInfo parent = parentClass().bestTypeInfo();
            s2 = Stream.concat(Stream.of(parent), parent.recursiveSuperTypeStream());
        } else {
            s2 = Stream.of();
        }
        Stream<TypeInfo> s3 = interfacesImplemented().stream().map(ParameterizedType::bestTypeInfo)
                .filter(Objects::nonNull)
                .flatMap(ti -> Stream.concat(Stream.of(ti), ti.recursiveSuperTypeStream()));
        return Stream.concat(s1, Stream.concat(s2, s3));
    }

    default Stream<TypeInfo> typeHierarchyExcludingJLOStream() {
        Stream<TypeInfo> s2;
        if (parentClass() != null && !parentClass().typeInfo().isJavaLangObject()) {
            TypeInfo parent = parentClass().bestTypeInfo();
            s2 = Stream.concat(Stream.of(parent), parent.recursiveSuperTypeStream());
        } else {
            s2 = Stream.of();
        }
        Stream<TypeInfo> s3 = interfacesImplemented().stream().map(ParameterizedType::bestTypeInfo)
                .filter(Objects::nonNull)
                .flatMap(ti -> Stream.concat(Stream.of(ti), ti.recursiveSuperTypeStream()));
        return Stream.concat(s2, s3);
    }

    default Stream<TypeInfo> enclosingTypeStream() {
        Stream<TypeInfo> enclosingStream;
        if (compilationUnitOrEnclosingType().isRight()) {
            TypeInfo right = compilationUnitOrEnclosingType().getRight();
            enclosingStream = right.enclosingTypeStream();
        } else {
            enclosingStream = Stream.of();
        }
        return Stream.concat(Stream.of(this), enclosingStream);
    }

    default Stream<MethodInfo> constructorAndMethodStream() {
        return Stream.concat(constructors().stream(), methodStream());
    }

    default Iterable<MethodInfo> constructorsAndMethods() {
        return () -> constructorAndMethodStream().iterator();
    }

    boolean isExtensible();

    MethodInfo enclosingMethod();

    default TypeInfo owningTypeStartFromAnonymous() {
        if (enclosingMethod() != null) return enclosingMethod().typeInfo().owningTypeStartFromAnonymous();
        return this;
    }

    TypeInfo rewirePhase0(InfoMap infoMap, TypeInfo newEnclosingType);

    TypeInfo rewirePhase1(InfoMap infoMap);

    void rewirePhase2(InfoMap infoMap);

    void rewirePhase3(InfoMap infoMap);

    default List<TypeInfo> translate(TranslationMap translationMap) {
        return List.of(this);
    }

    default Stream<TypeInfo> innerClassEnclosingStream() {
        if (!isStatic()) {
            assert compilationUnitOrEnclosingType().isRight();
            return Stream.concat(Stream.of(this),
                    compilationUnitOrEnclosingType().getRight().innerClassEnclosingStream());
        }
        return Stream.of(this);
    }

    default boolean isDescendantOf(TypeInfo ancestor) {
        if (equals(ancestor)) return true;
        if (parentClass() == null) return false;
        return parentClass().typeInfo().isDescendantOf(ancestor);
    }

    /*
    Always true for typeNature() != CLASS.
    True for classes when the type's declaration does not contain "extends".
     */
    boolean hasImplicitParent();
}
