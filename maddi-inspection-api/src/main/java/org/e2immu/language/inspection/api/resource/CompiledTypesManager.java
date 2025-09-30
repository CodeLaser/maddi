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

    TypeData typeDataOrNull(String fqn, SourceSet sourceSet, boolean complainSingle);

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

    default void preload(String thePackage, SourceSet sourceSetOfRequest) {
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
