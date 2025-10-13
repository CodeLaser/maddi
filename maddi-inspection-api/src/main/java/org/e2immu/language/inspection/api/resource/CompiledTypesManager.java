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

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName(), javaBase());
    }

    void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo);

    default void setRewiredType(TypeInfo typeInfo) { throw new UnsupportedOperationException(); }

    TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest);

    default TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return get(fullyQualifiedName, sourceSetOfRequest);
    }

    default TypeInfo getOrLoad(Class<?> clazz, SourceSet sourceSetOfRequest) {
        return getOrLoad(clazz.getCanonicalName(), sourceSetOfRequest);
    }

    default TypeInfo getOrLoad(Class<?> clazz) {
        return getOrLoad(clazz.getCanonicalName(), javaBase());
    }

    default void invalidate(TypeInfo typeInfo) {
        throw new UnsupportedOperationException();
    }

    default boolean packageContainsTypes(String packageName) {
        throw new UnsupportedOperationException();
    }

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
