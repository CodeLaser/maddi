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

package org.e2immu.language.cst.api.info;


import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.support.Either;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represents a type declaration in the CST: a class, interface, enum, record, or annotation type.
 * <p>
 * A {@code TypeInfo} is either a <em>primary type</em> (top-level, owned by a
 * {@link CompilationUnit}) or an <em>enclosed type</em> (nested, owned by another
 * {@code TypeInfo}). The {@link #compilationUnitOrEnclosingType()} discriminator exposes this.
 * <p>
 * Structural data (members, modifiers, hierarchy) is populated during inspection;
 * analysis properties (immutability, nullability) are added afterwards.
 */
public interface TypeInfo extends NamedType, Info {

    /**
     * Returns {@code true} when the type does not need its package to be imported,
     * e.g. because it lives in {@code java.lang}.
     */
    boolean doesNotRequirePackage();

    /** Returns the compilation unit that owns this type, following the enclosing chain if necessary. */
    default CompilationUnit compilationUnit() {
        return compilationUnitOrEnclosingType().isLeft() ? compilationUnitOrEnclosingType().getLeft()
                : compilationUnitOrEnclosingType().getRight().compilationUnit();
    }

    /** Returns {@code true} if this type is an annotation type ({@code @interface}). */
    boolean isAnnotation();

    /** Returns {@code true} if this type is an anonymous class (a lambda, or created with {@code new T() {…}}). */
    boolean isAnonymous();

    /**
     * Returns {@code true} if this type is the same as {@code typeInfo} or is nested inside it
     * at any depth.
     */
    boolean isEnclosedIn(TypeInfo typeInfo);

    /** Returns {@code true} if this type is a JVM primitive ({@code int}, {@code boolean}, …). */
    boolean isPrimitive();

    /** Returns {@code true} if this type is strictly (not equal) nested inside {@code typeInfo}. */
    boolean isStrictlyEnclosedIn(TypeInfo typeInfo);

    /**
     * Returns {@code true} if this type equals {@code typeInfo} or is directly or transitively
     * enclosed in it.
     */
    boolean isEqualToOrInnerClassOf(TypeInfo typeInfo);

    /** Returns {@code true} if this type is owned directly by a {@link CompilationUnit}. */
    default boolean isPrimaryType() {
        return compilationUnitOrEnclosingType().isLeft();
    }

    /**
     * Returns {@code true} if this is a non-static nested class (an inner class in the Java
     * language sense — it captures a reference to the enclosing instance).
     */
    default boolean isInnerClass() {
        return typeNature().isClass() && compilationUnitOrEnclosingType().isRight() && !isStatic();
    }

    /** Returns {@code true} if this type is declared {@code sealed}. */
    boolean isSealed();

    /** Returns the package name of this type, e.g. {@code "java.util"}. */
    String packageName();

    /**
     * Returns the dot-separated chain of simple names from the primary type down to this one,
     * e.g. {@code "Outer.Inner.Leaf"}.
     */
    String fromPrimaryTypeDownwards();

    /**
     * Returns a left {@link CompilationUnit} for primary types, or a right {@link TypeInfo}
     * (the direct enclosing type) for nested types.
     */
    Either<CompilationUnit, TypeInfo> compilationUnitOrEnclosingType();

    /**
     * Returns an iterable over {@link #parentClass()} (if present) followed by all
     * {@link #interfacesImplemented()}, for use in hierarchy traversal.
     */
    default Iterable<ParameterizedType> parentAndInterfacesImplemented() {
        Stream<ParameterizedType> stream = Stream.concat(Stream.ofNullable(parentClass()),
                interfacesImplemented().stream());
        return stream::iterator;
    }

    /**
     * Returns the flat set of all supertypes (transitively) excluding {@code java.lang.Object},
     * computed during inspection.
     */
    Set<TypeInfo> superTypesExcludingJavaLangObject();

    /** Returns the set of modifiers declared on this type ({@code public}, {@code final}, …). */
    Set<TypeModifier> typeModifiers();

    /** Returns the type parameters declared on this type, in declaration order. */
    List<TypeParameter> typeParameters();

    /** Returns any comments that trail the last member in the type body. */
    List<Comment> trailingComments();

    /**
     * Finds the unique method with the given name and parameter count.
     *
     * @throws NoSuchElementException if there is no match or more than one match
     */
    MethodInfo findUniqueMethod(String methodName, int numParams);

    /**
     * Finds the unique method with the given name and parameter count, using the comma-separated
     * FQN list supplied by {@code paramFqnCsv} to disambiguate when multiple overloads exist.
     */
    MethodInfo findUniqueMethod(String methodName, int numParams, Supplier<String> paramFqnCsv);

    /** Returns {@code true} if the internal method lookup map has been built for this type. */
    boolean hasMethodMap();

    /**
     * Finds the constructor with the given number of parameters.
     *
     * @throws NoSuchElementException if there is no match or more than one match
     */
    MethodInfo findConstructor(int i);

    /**
     * Finds the constructor whose first parameter has the given type.
     *
     * @throws NoSuchElementException if there is no match or more than one match
     */
    MethodInfo findConstructor(TypeInfo typeOfFirstParameter);

    /**
     * Finds the directly enclosed subtype with the given simple name.
     *
     * @throws NoSuchElementException if not found
     */
    TypeInfo findSubType(String simpleName);

    /**
     * Finds the directly enclosed subtype with the given simple name.
     *
     * @param complain if {@code true}, throws if not found; otherwise returns {@code null}
     * @throws NoSuchElementException if not found and asked to complain
     */
    TypeInfo findSubType(String simpleName, boolean complain);

    /**
     * Returns the field with the given name.
     *
     * @param complain if {@code true}, throws if not found; otherwise returns {@code null}
     * @throws NoSuchElementException if not found and asked to complain
     */
    FieldInfo getFieldByName(String name, boolean complain);

    /**
     * Finds the unique method with the given name whose first parameter has the given type.
     *
     * @throws NoSuchElementException if there is no match or more than one match
     */
    MethodInfo findUniqueMethod(String name, TypeInfo typeInfoOfFirstParameter);

    /**
     * Returns the superclass of this type, or {@code null} if this type is {@code java.lang.Object}.
     */
    ParameterizedType parentClass();

    /** Returns the interfaces directly implemented or extended by this type, in declaration order. */
    List<ParameterizedType> interfacesImplemented();

    /** Returns all non-constructor methods declared directly on this type. */
    default List<MethodInfo> methods() {
        return methodStream().toList();
    }

    /**
     * Streams all non-constructor methods declared on this type and every enclosed subtype,
     * depth-first.
     */
    Stream<MethodInfo> recursiveMethodStream();

    /** Returns {@code true} if this type is a JVM primitive other than {@code void}. */
    boolean isPrimitiveExcludingVoid();

    /** Returns {@code true} if this type is {@code java.io.Serializable}. */
    boolean isJavaIoSerializable();

    /** Returns {@code true} if this type is {@code java.lang.Object}. */
    boolean isJavaLangObject();

    /**
     * Returns the single abstract method of this functional interface,
     * or {@code null} if this type is not a functional interface.
     */
    MethodInfo singleAbstractMethod();

    /** Returns {@code true} if this type represents a numeric primitive or boxed numeric. */
    boolean isNumeric();

    /** Returns {@code true} if this type is a boxed numeric type (excluding {@code Void}). */
    boolean isBoxedExcludingVoid();

    /** Returns {@code true} if this type is annotated with {@code @FunctionalInterface} or is structurally a SAM type. */
    boolean isFunctionalInterface();

    /** Returns {@code true} if this type is {@code java.lang.String}. */
    boolean isJavaLangString();

    /** Returns {@code true} if this type is {@code java.lang.Class}. */
    boolean isJavaLangClass();

    /** Returns {@code true} if this type is the {@code void} primitive. */
    boolean isVoid();

    /** Returns {@code true} if this type is {@code java.lang.Void}. */
    boolean isJavaLangVoid();

    /** Returns {@code true} if this type is {@code java.lang.Boolean}. */
    boolean isBoxedBoolean();

    /** Returns the types directly enclosed in this type's body. */
    List<TypeInfo> subTypes();

    /** Returns {@code true} if this type has {@code private} access. */
    boolean isPrivate();

    /** Returns {@code true} if this type is declared {@code abstract}. */
    boolean isAbstract();

    /** Returns {@code true} if this type is the {@code char} primitive. */
    boolean isCharacter();

    /** Returns {@code true} if this type is {@code java.lang.Long}. */
    boolean isBoxedLong();

    /** Returns {@code true} if this type is the {@code int} primitive or {@code java.lang.Integer}. */
    boolean isInteger();

    /** Returns {@code true} if this type is {@code java.lang.Short}. */
    boolean isBoxedShort();

    /** Returns {@code true} if this type is {@code java.lang.Byte}. */
    boolean isBoxedByte();

    /** Returns {@code true} if this type is {@code java.lang.Double}. */
    boolean isBoxedDouble();

    /** Returns {@code true} if this type is {@code java.lang.Float}. */
    boolean isBoxedFloat();

    /** Streams the non-constructor methods declared directly on this type. */
    Stream<MethodInfo> methodStream();

    /** Returns the constructors declared directly on this type, in declaration order. */
    List<MethodInfo> constructors();

    /** Returns the fields declared directly on this type, in declaration order. */
    List<FieldInfo> fields();

    /** Returns {@code true} if this type is declared {@code static} (applies to nested types). */
    boolean isStatic();

    /** Returns {@code true} if this type is an interface. */
    boolean isInterface();

    /** Returns the outermost enclosing type, or {@code this} if already a primary type. */
    TypeInfo primaryType();

    /** Returns {@code true} if this type is the {@code boolean} primitive. */
    boolean isBoolean();

    /** Returns {@code true} if this type is the {@code int} primitive. */
    boolean isInt();

    /** Returns {@code true} if this type is the {@code long} primitive. */
    boolean isLong();

    /** Returns {@code true} if this type is the {@code short} primitive. */
    boolean isShort();

    /** Returns {@code true} if this type is the {@code byte} primitive. */
    boolean isByte();

    /** Returns {@code true} if this type is the {@code float} primitive. */
    boolean isFloat();

    /** Returns {@code true} if this type is the {@code double} primitive. */
    boolean isDouble();

    /** Returns {@code true} if this type is the {@code char} primitive. */
    boolean isChar();

    /** Returns {@code true} if this type has {@code public} access. */
    boolean isPublic();

    /**
     * Returns {@code true} if this type is accessible from outside its compilation unit,
     * taking nesting into account.
     */
    boolean isPubliclyAccessible();

    /**
     * Registers a callback that will be invoked the first time this type's inspection data
     * is needed but has not yet been populated (on-demand / lazy inspection).
     */
    void setOnDemandInspection(Consumer<TypeInfo> inspector);

    /** Returns {@code true} if an on-demand inspection callback has been registered. */
    boolean haveOnDemandInspection();

    /** Returns the mutable builder for this type (available until inspection is committed). */
    Builder builder();

    /** Returns the nature of this type ({@code class}, {@code interface}, {@code enum}, …). */
    TypeNature typeNature();

    /** Returns the number of anonymous class declarations inside this type's body. */
    int anonymousTypes();

    /**
     * Returns {@code true} if the type's supertype hierarchy has not yet been resolved.
     * Only valid before inspection is committed.
     */
    default boolean hierarchyNotYetDone() {
        return !hasBeenInspected() && builder().hierarchyNotYetDone();
    }

    /**
     * Returns the list of types that are permitted to extend this sealed type,
     * as declared in the {@code permits} clause.
     */
    List<TypeInfo> permittedWhenSealed();

    /** Returns {@code true} if this type is declared {@code final}. */
    boolean isFinal();

    /** Returns {@code true} if this type is declared {@code non-sealed}. */
    boolean isNonSealed();

    /** Builder for constructing a {@link TypeInfo} during the inspection phase. */
    interface Builder extends Info.Builder<Builder> {
        /** Adds a type that is permitted to extend this sealed type. */
        Builder addPermittedType(TypeInfo typeInfo);

        /** Appends trailing comments (those after the last member in the body). */
        @Fluent
        Builder addTrailingComments(List<Comment> comments);

        /** Removes all interfaces from the implemented-interfaces list. */
        void clearInterfacesImplemented();

        /** Returns the constructors accumulated in the builder so far. */
        List<MethodInfo> constructors();

        /** Returns the fields accumulated in the builder so far. */
        List<FieldInfo> fields();

        /**
         * Returns the current anonymous-type counter and increments it.
         * Used to generate stable names for anonymous classes.
         */
        int getAndIncrementAnonymousTypes();

        /** Returns {@code true} if the type being built is abstract. */
        boolean isAbstract();

        /** Returns the non-constructor methods accumulated in the builder so far. */
        List<MethodInfo> methods();

        /** Sets the running count of anonymous types (used when restoring a previously counted state). */
        @Fluent
        Builder setAnonymousTypes(int anonymousTypes);

        /** Records the method that syntactically encloses this type (for local and anonymous classes). */
        @Fluent
        Builder setEnclosingMethod(MethodInfo methodInfo);

        /** Adds a directly enclosed subtype. */
        @Fluent
        Builder addSubType(TypeInfo subType);

        /** Adds a modifier (e.g. {@code public}, {@code final}) to this type. */
        @Fluent
        Builder addTypeModifier(TypeModifier typeModifier);

        /** Adds a non-constructor method to this type. */
        @Fluent
        Builder addMethod(MethodInfo methodInfo);

        /** Adds a field to this type. */
        @Fluent
        Builder addField(FieldInfo field);

        /** Adds a constructor to this type. */
        @Fluent
        Builder addConstructor(MethodInfo constructor);

        /** Sets the nature ({@code class}, {@code interface}, {@code enum}, …) of this type. */
        @Fluent
        Builder setTypeNature(TypeNature typeNature);

        /** Sets the superclass (the {@code extends} clause when explicit, the root of the object hierarchy otherwise). */
        @Fluent
        Builder setParentClass(ParameterizedType parentClass);

        /** Adds an interface to the {@code implements} or {@code extends} list. */
        @Fluent
        Builder addInterfaceImplemented(ParameterizedType interfaceImplemented);

        /** Adds or replaces a type parameter at the position given by its index. */
        @Fluent
        Builder addOrSetTypeParameter(TypeParameter typeParameter);

        /** Records the pre-resolved single abstract method for a functional interface. */
        @Fluent
        Builder setSingleAbstractMethod(MethodInfo singleAbstractMethod);

        /** Returns the source position recorded in the builder. */
        Source source();

        /** Returns the type nature set so far in the builder. */
        TypeNature typeNature();

        /**
         * Returns {@code true} if the supertype hierarchy has not yet been resolved
         * ({@link #hierarchyIsDone()} has not been called).
         */
        boolean hierarchyNotYetDone();

        /** Marks the supertype hierarchy as fully resolved. */
        @Fluent
        Builder hierarchyIsDone();

        /**
         * Commits all methods (constructors and non-constructors) to their final immutable state
         * without fully committing the type itself.
         * Used when the method bodies are available but the type is not yet complete.
         */
        @Fluent
        Builder commitMethods();
    }

    /**
     * Returns {@code true} if the analyser has determined this type is at least
     * immutable with hidden content.
     */
    boolean isAtLeastImmutableHC();

    /**
     * Renders this type to an {@link OutputBuilder}.
     *
     * @param doTypeDeclaration if {@code true}, emits the full declaration (header + body);
     *                          if {@code false}, emits only the type name
     */
    OutputBuilder print(Qualification qualification, boolean doTypeDeclaration);

    /** Streams this type and every subtype recursively, depth-first. */
    default Stream<TypeInfo> recursiveSubTypeStream() {
        return Stream.concat(Stream.of(this), subTypes().stream().flatMap(TypeInfo::recursiveSubTypeStream));
    }

    /** Streams this type followed by every type in its supertype hierarchy, depth-first. */
    default Stream<TypeInfo> meAndMyRecursiveSuperTypeStream() {
        return Stream.concat(Stream.of(typeInfo()), typeInfo().recursiveSuperTypeStream());
    }

    /**
     * Streams all supertypes of this type (enclosing types for inner classes, parent class,
     * and interfaces), recursively, depth-first.
     */
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

    /** Streams the type hierarchy (parent class and interfaces, recursively) excluding {@code java.lang.Object}. */
    default Stream<TypeInfo> typeHierarchyExcludingJLOStream() {
        Stream<TypeInfo> s2;
        if (parentClass() != null && !parentClass().typeInfo().isJavaLangObject()) {
            TypeInfo parent = parentClass().typeInfo();
            s2 = Stream.concat(Stream.of(parent), parent.typeHierarchyExcludingJLOStream());
        } else {
            s2 = Stream.of();
        }
        Stream<TypeInfo> s3 = interfacesImplemented().stream().map(ParameterizedType::bestTypeInfo)
                .filter(Objects::nonNull)
                .flatMap(ti -> Stream.concat(Stream.of(ti), ti.typeHierarchyExcludingJLOStream()));
        return Stream.concat(s2, s3);
    }

    /**
     * Streams this type followed by each enclosing type, from innermost to outermost.
     * Only non-static nested types contribute their enclosing type to the chain.
     */
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

    /** Streams all constructors followed by all non-constructor methods declared on this type. */
    default Stream<MethodInfo> constructorAndMethodStream() {
        return Stream.concat(constructors().stream(), methodStream());
    }

    /** Returns an iterable over all constructors and non-constructor methods declared on this type. */
    default Iterable<MethodInfo> constructorsAndMethods() {
        return () -> constructorAndMethodStream().iterator();
    }

    /**
     * Returns {@code true} if this type can be subclassed — i.e. it is a non-final,
     * non-sealed class or an interface.
     */
    boolean isExtensible();

    /**
     * Returns the method whose body syntactically contains this type declaration,
     * or {@code null} if this type is not local or anonymous.
     */
    MethodInfo enclosingMethod();

    /**
     * Walks up the chain of anonymous/local types to find the first non-anonymous owning type.
     * For a non-anonymous type this simply returns {@code this}.
     */
    default TypeInfo owningTypeStartFromAnonymous() {
        if (enclosingMethod() != null) return enclosingMethod().typeInfo().owningTypeStartFromAnonymous();
        return this;
    }

    /**
     * Phase 0 of the rewiring protocol: creates a fresh {@code TypeInfo} shell (and shells for
     * all directly enclosed subtypes) registered in {@code infoMap}, with the new enclosing type
     * set to {@code newEnclosingType}.
     *
     * @see InfoMap
     */
    TypeInfo rewirePhase0(InfoMap infoMap, TypeInfo newEnclosingType);

    /**
     * Phase 1 of the rewiring protocol: copies type parameters, modifiers, and the inspection
     * skeleton (without member bodies) into the rewired shell.
     */
    TypeInfo rewirePhase1(InfoMap infoMap);

    /**
     * Phase 2 of the rewiring protocol: copies constructors, methods, and fields (their
     * signatures, without bodies) into the rewired shell.
     */
    void rewirePhase2(InfoMap infoMap);

    /**
     * Phase 3 of the rewiring protocol: copies expressions and statement bodies, rewriting all
     * cross-references through {@code infoMap}.
     */
    void rewirePhase3(InfoMap infoMap);

    /** Returns a singleton list containing {@code this} (types are never dropped by translation). */
    default List<TypeInfo> translate(TranslationMap translationMap) {
        return List.of(this);
    }

    /**
     * Streams this type followed by each enclosing non-static type, from innermost outward,
     * stopping once a static type is reached.
     */
    default Stream<TypeInfo> innerClassEnclosingStream() {
        if (!isStatic()) {
            assert compilationUnitOrEnclosingType().isRight();
            return Stream.concat(Stream.of(this),
                    compilationUnitOrEnclosingType().getRight().innerClassEnclosingStream());
        }
        return Stream.of(this);
    }

    /**
     * Returns {@code true} if this type is the same as {@code ancestor} or extends it
     * (following the parent-class chain only, not interfaces).
     */
    default boolean isDescendantOf(TypeInfo ancestor) {
        if (equals(ancestor)) return true;
        if (parentClass() == null) return false;
        return parentClass().typeInfo().isDescendantOf(ancestor);
    }

    /**
     * Returns {@code true} if this type does not have an explicit {@code extends} clause.
     * Always {@code true} for non-class types (interfaces, enums, records).
     */
    boolean hasImplicitParent();

    /**
     * Returns {@code true} if this type equals {@code typeInfo} or appears anywhere
     * in {@code typeInfo}'s type hierarchy (excluding {@code java.lang.Object}).
     */
    default boolean inHierarchyOf(TypeInfo typeInfo) {
        return equals(typeInfo) || typeInfo.typeHierarchyExcludingJLOStream().anyMatch(this::equals);
    }
}
