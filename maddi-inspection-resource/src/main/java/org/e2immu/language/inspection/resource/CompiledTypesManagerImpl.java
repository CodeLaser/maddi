package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.support.SetOnce;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final Logger LOGGER = LoggerFactory.getLogger(CompiledTypesManagerImpl.class);


    private static class TypeDataImpl implements CompiledTypesManager.TypeData {
        private final SourceFile sourceFile;
        private TypeInfo typeInfo;

        private ByteCodeInspector.Data byteCodeInspectorData;

        TypeDataImpl(SourceFile sourceFile) {
            this.sourceFile = sourceFile;
        }

        public TypeDataImpl(SourceFile sourceFile, TypeInfo typeInfo) {
            this.sourceFile = sourceFile;
            this.typeInfo = typeInfo;
        }

        @Override
        public boolean isCompiled() {
            return sourceFile.sourceSet().externalLibrary();
        }

        @Override
        public SourceFile sourceFile() {
            return sourceFile;
        }

        @Override
        public TypeInfo typeInfo() {
            return typeInfo;
        }

        @Override
        public ByteCodeInspector.Data byteCodeInspectorData() {
            return byteCodeInspectorData;
        }

        @Override
        public void setTypeInfo(TypeInfo typeInfo) {
            this.typeInfo = typeInfo;
        }

        @Override
        public void updateByteCodeInspectorData(ByteCodeInspector.Data data) {
            this.byteCodeInspectorData = data;
        }
    }

    private final Resources classPath;
    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
    private final Map<String, TypeInfo> mapSingleTypeForFQN = new HashMap<>();
    private final ReentrantReadWriteLock trieLock = new ReentrantReadWriteLock();
    private final Trie<TypeData> typeTrie = new Trie<>();

    public CompiledTypesManagerImpl(Resources classPath) {
        this.classPath = classPath;
    }


    // we must also call this for the sources, after the classpath
    // clearExisting == true when you want the sources to override any same FQN type in the class path
    public void addToTrie(Resources resources, boolean compiled) {
        resources.visit(new String[0], (parts, sourceFiles) -> {
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile.path().endsWith(".class") || sourceFile.path().endsWith(".java")) {
                    TypeDataImpl typeData = new TypeDataImpl(sourceFile);
                    if (compiled) {
                        typeData.byteCodeInspectorData = byteCodeInspector.get().defaultData();
                    }
                    String fqn = sourceFile.fullyQualifiedNameFromPath();
                    String[] newParts = fqn.split("\\.");
                    typeTrie.add(newParts, typeData);

                }
            }
        });
    }

    @Override
    public Resources classPath() {
        return classPath;
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {
        String fullyQualifiedName = typeInfo.fullyQualifiedName();
        String[] parts = fullyQualifiedName.split("\\.");
        if (!typeInfo.isPrimaryType() && !typeInfo.compilationUnit().externalLibrary()) {
            // we cannot know beforehand the subtypes of a source type. those we should add now
            List<TypeData> subTypes = typeTrie.add(parts, new TypeDataImpl(sourceFile, typeInfo));
            if (subTypes.size() == 1) {
                mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
            } else if (subTypes.size() == 2) {
                mapSingleTypeForFQN.remove(fullyQualifiedName); // multiple
            }
            return;
        }
        List<TypeData> types = typeTrie.get(parts);
        if (types == null) {
            // FIXME this is not an elegant solution for "additional sources to be tested"
            LOGGER.warn("Unknown source file: {}", sourceFile);
            types = typeTrie.add(parts, new TypeDataImpl(sourceFile, typeInfo));
        }
        assert types != null : "We should know about " + fullyQualifiedName;
        if (types.size() == 1) {
            mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
        }
        TypeData typeData = types.stream().filter(c -> c.sourceFile().equals(sourceFile)).findFirst().orElseThrow();
        typeData.setTypeInfo(typeInfo);
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null) return single;
            TypeData typeData = typeDataOrNull(fullyQualifiedName, sourceSetOfRequest, true);
            return typeData == null ? null : typeData.typeInfo();
        } finally {
            trieLock.readLock().unlock();
        }
    }

    @Override
    public TypeData typeDataOrNull(String fullyQualifiedName, SourceSet sourceSetOfRequest, boolean complainSingle) {
        String[] parts = fullyQualifiedName.split("\\.");
        List<TypeData> typeDataList = typeTrie.get(parts);
        if (typeDataList == null || typeDataList.isEmpty()) return null;
        assert !complainSingle || !(typeDataList.size() == 1 && typeDataList.getFirst().typeInfo() != null)
                : "Otherwise, would have been in mapSingleTypeForFQN: " + fullyQualifiedName;
        boolean ignoreRequest = sourceSetOfRequest == null || sourceSetOfRequest.inTestSetup();
        Set<SourceSet> sourceSets = ignoreRequest ? null : sourceSetOfRequest.recursiveDependenciesSameExternal();
        return typeDataList.stream()
                .filter(typeData -> ignoreRequest || sourceSets.contains(typeData.sourceFile().sourceSet()))
                .findFirst().orElse(null);
    }

    private TypeData typeDataExactSourceSet(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        String[] parts = fullyQualifiedName.split("\\.");
        List<TypeData> typeDataList = typeTrie.get(parts);
        TypeData typeData;
        if (typeDataList != null) {
            typeData = typeDataList.stream()
                    .filter(c -> c.sourceFile().sourceSet().equals(sourceSetOfRequest))
                    .findFirst().orElse(null);
        } else {
            typeData = null;
        }
        if (typeData == null) {
            throw new UnsupportedOperationException("Cannot find .class file for " + fullyQualifiedName + " in "
                                                    + sourceSetOfRequest);
        }
        return typeData;
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        TypeData typeData;
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null) return single;
            typeData = typeDataOrNull(fullyQualifiedName, sourceSetOfRequest, true);
            if (typeData == null) return null;
            if (typeData.typeInfo() != null) return typeData.typeInfo();
        } finally {
            trieLock.readLock().unlock();
        }
        return load(typeData);
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {
        assert typeInfo.compilationUnit().externalLibrary();
        if (!typeInfo.hasBeenInspected()) {
            TypeData typeData = typeDataExactSourceSet(typeInfo.fullyQualifiedName(),
                    typeInfo.compilationUnit().sourceSet());
            synchronized (byteCodeInspector) {
                byteCodeInspector.get().load(typeData);
            }
        }
    }

    @Override
    public void invalidate(TypeInfo typeInfo) {
        assert !typeInfo.compilationUnit().externalLibrary();
        trieLock.writeLock().lock();
        try {
            String fullyQualifiedName = typeInfo.fullyQualifiedName();
            String[] parts = fullyQualifiedName.split("\\.");
            List<TypeData> typeDataList = typeTrie.get(parts);
            TypeData typeData = typeDataList.stream()
                    .filter(c -> c.typeInfo() == typeInfo)
                    .findFirst().orElse(null);
            if (typeData != null) {
                typeData.setTypeInfo(null);
                mapSingleTypeForFQN.remove(fullyQualifiedName);
            }
        } finally {
            trieLock.writeLock().unlock();
        }
    }

    private TypeInfo load(TypeData typeData) {
        TypeInfo typeInfo;
        synchronized (byteCodeInspector) {
            typeInfo = byteCodeInspector.get().load(typeData);
        }
        if (typeInfo != null) {
            // TODO can be optimized
            addTypeInfo(typeData.sourceFile(), typeInfo);
        }
        return typeInfo;
    }

    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
        this.byteCodeInspector.set(byteCodeInspector);
    }

    @Override
    public void preload(String thePackage) {
        LOGGER.info("Start pre-loading {}", thePackage);
        List<TypeInfo> types = primaryTypesInPackageEnsureLoaded(thePackage, null);
        LOGGER.info("... loaded {} types", types.size());
    }

    @Override
    public List<TypeInfo> typesLoaded(Boolean compiled) {
        List<TypeInfo> result = new ArrayList<>();
        trieLock.readLock().lock();
        try {
            typeTrie.visit(new String[]{}, (_, list) -> list.stream()
                    .filter(typeData -> typeData.typeInfo() != null
                                        && (compiled == null || compiled == typeData.isCompiled()))
                    .forEach(typeData -> result.add(typeData.typeInfo())));
            return result.stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
        } finally {
            trieLock.readLock().unlock();
        }
    }

    @Override
    public List<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, SourceSet sourceSetOfRequest) {
        List<TypeInfo> result = new ArrayList<>();
        trieLock.readLock().lock();
        try {
            // we ignore "test" situations where there are no actual dependencies (simplified test setup in
            // inspection/integration)
            boolean ignoreRequest = sourceSetOfRequest == null || sourceSetOfRequest.inTestSetup();
            Set<SourceSet> sourceSets = ignoreRequest ? null : sourceSetOfRequest.recursiveDependenciesSameExternal();
            String[] parts = packageName.split("\\.");
            typeTrie.visitDoNotRecurse(parts, typeDataList -> {
                TypeData typeData;
                if (typeDataList == null || typeDataList.isEmpty()) {
                    typeData = null;
                } else if (ignoreRequest) {
                    typeData = typeDataList.getFirst();// we cannot know
                } else {
                    typeData = typeDataList.stream()
                            .filter(c -> sourceSets.contains(c.sourceFile().sourceSet()))
                            .findFirst().orElse(null);
                }
                if (typeData != null) {
                    TypeInfo typeInfo;
                    if (typeData.typeInfo() != null) {
                        typeInfo = typeData.typeInfo();
                    } else if (typeData.isCompiled()) {
                        // All the primary source types should be known; only compiled types can be loaded
                        typeInfo = load(typeData);
                    } else {
                        typeInfo = null;
                        assert typeData.sourceFile().uri().toString().endsWith("package-info.java");
                    }
                    if (typeInfo != null) {
                        result.add(typeInfo);
                    }
                }
            });
        } finally {
            trieLock.readLock().unlock();
        }
        return result;
    }

    @Override
    public boolean packageContainsTypes(String packageName) {
        return !primaryTypesInPackageEnsureLoaded(packageName, null).isEmpty();
    }

    public List<SourceFile> sourceFiles(String path) {
        String[] parts = path.split("/");
        List<TypeData> typeDataList = typeTrie.get(parts);
        return typeDataList == null ? null : typeDataList.stream().map(TypeData::sourceFile).toList();
    }
}
