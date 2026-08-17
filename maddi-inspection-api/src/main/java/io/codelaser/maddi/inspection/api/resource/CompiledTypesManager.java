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

package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

/**
 * manages all types that come in byte-code form.
 * also deals with the bootstrapping of types (Object, String, etc.)
 * <p>
 * lots of defaults because we make stubs.
 */
public interface CompiledTypesManager {

    default SourceSet javaBase() {
        throw new UnsupportedOperationException();
    }

    TypeData typeDataOrNull(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, boolean complainSingle);

    interface TypeData {

        boolean isCompiled();

        SourceFile sourceFile();

        TypeInfo typeInfo();

        ByteCodeInspector.Data byteCodeInspectorData();

        void updateByteCodeInspectorData(ByteCodeInspector.Data data);
    }

    default Resources classPath() {
        throw new UnsupportedOperationException();
    }

    /** @deprecated renamed to {@link #typeIfLoaded(Class)} — note it is the PEEK, not {@link #type(Class)}. */
    @Deprecated
    default TypeInfo get(Class<?> clazz) {
        return typeIfLoaded(clazz.getCanonicalName(), javaBase());
    }

    void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo);

    default void setRewiredType(TypeInfo typeInfo) { throw new UnsupportedOperationException(); }

    /**
     * The type only if it has already been loaded, without loading anything. ⛔ A null means "not registered
     * (yet)", NOT "no such type" — which is rarely the question a caller wants answered, so prefer
     * {@link #type(String, SourceSet)}. This is the method an implementation provides; everything else here is
     * expressed in terms of it.
     * <p>
     * {@code sourceSetOfRequest} is the set that is asking, and an implementation is expected to honour it: when
     * one fully-qualified name is held by more than one source set, the answer is the one nearest the requester.
     */
    TypeInfo typeIfLoaded(String fullyQualifiedName, SourceSet sourceSetOfRequest);

    /** The peek by class literal; {@link #javaBase()} is the right source set for the JDK types it is used on. */
    default TypeInfo typeIfLoaded(Class<?> clazz) {
        return typeIfLoaded(clazz.getCanonicalName(), javaBase());
    }

    /**
     * @deprecated the name reads like the general accessor and is not: it answers only from what has already been
     * registered, and says nothing for everything else. Call {@link #type(String, SourceSet)} to resolve a type,
     * or {@link #typeIfLoaded(String, SourceSet)} when the question really is "is it already here?".
     */
    @Deprecated
    default TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return typeIfLoaded(fullyQualifiedName, sourceSetOfRequest);
    }

    /**
     * The type this fully-qualified name denotes for the given source set, or null: the accessor to reach for.
     * Answers from the registry when the type is already there and loads it from bytecode otherwise, so a caller
     * does not have to know which of the two applies — that is an implementation's business, and in the openjdk
     * front end the load is a rare tail (single digits over a whole test module; zero over most).
     * <p>
     * {@code sourceSetOfRequest} is the set that is asking. It decides which class path the name is resolved
     * against, and which of two same-named types is the nearer one — see
     * {@code JavaInspectorImpl.loadCompiledTypeOrNull}.
     */
    default TypeInfo type(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        // no loading of its own: an implementation that cannot read bytecode (the stubs this interface is full of)
        // answers from the registry, exactly as the old getOrLoad default did. Overridden where a load is possible.
        return typeIfLoaded(fullyQualifiedName, sourceSetOfRequest);
    }

    default TypeInfo type(Class<?> clazz, SourceSet sourceSetOfRequest) {
        return type(clazz.getCanonicalName(), sourceSetOfRequest);
    }

    /** The JDK types a caller reaches for by class literal; {@link #javaBase()} is the right source set for those. */
    default TypeInfo type(Class<?> clazz) {
        return type(clazz.getCanonicalName(), javaBase());
    }

    /**
     * @deprecated renamed to {@link #type(String, SourceSet)}: "OrLoad" names a rare tail (the bytecode load) in
     * what is the primary accessor, which is why it reads wrong at every call site.
     */
    @Deprecated
    default TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return type(fullyQualifiedName, sourceSetOfRequest);
    }

    /** @deprecated renamed to {@link #type(Class, SourceSet)}. */
    @Deprecated
    default TypeInfo getOrLoad(Class<?> clazz, SourceSet sourceSetOfRequest) {
        return getOrLoad(clazz.getCanonicalName(), sourceSetOfRequest);
    }

    /** @deprecated renamed to {@link #type(Class)}. */
    @Deprecated
    default TypeInfo getOrLoad(Class<?> clazz) {
        return getOrLoad(clazz.getCanonicalName(), javaBase());
    }

    default void invalidate(TypeInfo typeInfo) {
        throw new UnsupportedOperationException();
    }

    default boolean packageContainsTypes(String packageName) {
        throw new UnsupportedOperationException();
    }

    default boolean isPackagePart(String string) { throw new UnsupportedOperationException(); }

    default void preload(String thePackage) {
        throw new UnsupportedOperationException();
    }

    default List<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, SourceSet sourceSetOfRequest) {
        throw new UnsupportedOperationException();
    }

    default boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }

    default List<TypeInfo> typesLoaded(Boolean compiled) {
        throw new UnsupportedOperationException();
    }
}
