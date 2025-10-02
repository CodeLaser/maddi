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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompiledTypesManagerImpl.class);
    private final SourceSet ANY = new SourceSetImpl("any source set will do", List.of(), URI.create("file:/"),
            StandardCharsets.UTF_8, false, false, false, false, false,
            Set.of(), Set.of());

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

        public void clearTypeInfo() {
            this.typeInfo = null;
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


        public void setTypeInfo(TypeInfo typeInfo) {
            //assert typeInfo != null && this.typeInfo == null;
            // FIXME spring-core issues with some $$serializer magic type
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
    private final SourceSet javaBase;

    public CompiledTypesManagerImpl(SourceSet javaBase, Resources classPath) {
        this.classPath = classPath;
        this.javaBase = javaBase;
    }

    @Override
    public SourceSet javaBase() {
        return javaBase;
    }

    // we must also call this for the sources, after the classpath
    // clearExisting == true when you want the sources to override any same FQN type in the class path
    public void addToTrie(Resources resources, boolean compiled) {
        resources.visit(new String[0], (parts, sourceFiles) -> {
            for (SourceFile sourceFile : sourceFiles) {
                boolean dotClass = sourceFile.path().endsWith(".class");
                boolean dotJava = sourceFile.path().endsWith(".java");
                if (dotClass && compiled || dotJava && !compiled) {
                    TypeDataImpl typeData = new TypeDataImpl(sourceFile);
                    if (compiled) {
                        typeData.byteCodeInspectorData = byteCodeInspector.get().defaultData();
                    }
                    String fqn = sourceFile.fullyQualifiedNameFromPath();
                    String[] newParts = fqn.split("\\.");
                    typeTrie.add(newParts, typeData);
                    // remove all compiled types if we add a source type here
                    // not that this has an effect on missing dependencies, see correction in 'bestCandidate()'
                    if (!compiled) {
                        typeTrie.visit(newParts, (_, list) -> {
                            list.removeIf(TypeData::isCompiled);
                        });
                    }
                }
            }
        });
    }

    public void addPredefinedTypeInfoObjects(List<TypeInfo> predefinedTypes, SourceSet javaBase) {
        for (TypeInfo predefined : predefinedTypes) {
            TypeData typeData = typeDataOrNull(predefined.fullyQualifiedName(), ANY, null, true);
            ((TypeDataImpl) typeData).setTypeInfo(predefined);
            mapSingleTypeForFQN.put(predefined.fullyQualifiedName(), predefined);
        }
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
        TypeData typeData = types == null ? null
                : types.stream().filter(c -> c.sourceFile().equals(sourceFile)).findFirst().orElse(null);
        if (typeData == null) {
            typeTrie.add(parts, new TypeDataImpl(sourceFile, typeInfo));
            mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
            return;
        }
        if (types.size() == 1) {
            mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
        }
        ((TypeDataImpl) typeData).setTypeInfo(typeInfo);
    }

    @Override
    public void setRewiredType(TypeInfo typeInfo) {
        String fullyQualifiedName = typeInfo.fullyQualifiedName();
        String[] parts = fullyQualifiedName.split("\\.");
        SourceSet sourceSet = typeInfo.compilationUnit().sourceSet();
        List<TypeData> typeDataList = typeTrie.get(parts);
        TypeData typeData = typeDataList == null ? null : typeDataList.stream()
                .filter(td -> td.sourceFile().sourceSet().equals(sourceSet))
                .findFirst().orElse(null);
        if (typeData != null) {
            ((TypeDataImpl) typeData).typeInfo = typeInfo;
            if (mapSingleTypeForFQN.containsKey(fullyQualifiedName)) {
                mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
            }
        } else {
            LOGGER.warn("Rewiring: no type data for {}", fullyQualifiedName);
        }
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null) {
                return single.hasBeenInspected() || !single.compilationUnit().externalLibrary() ? single : null; // needs loading
            }
            TypeData typeData = typeDataOrNull(fullyQualifiedName, sourceSetOfRequest, null, true);
            if (typeData != null && typeData.typeInfo() != null
                && (!typeData.typeInfo().compilationUnit().externalLibrary()
                    || typeData.typeInfo().hasBeenInspected())) return typeData.typeInfo();
            return null;
        } finally {
            trieLock.readLock().unlock();
        }
    }

    @Override
    public TypeData typeDataOrNull(String fullyQualifiedName, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet,
                                   boolean complainSingle) {
        List<TypeData> typeDataList = typeDataList(fullyQualifiedName);
        if (typeDataList == null || typeDataList.isEmpty()) return null;
        return bestCandidate(sourceSetOfRequest, typeDataList, nearestSourceSet);
    }

    // public for testing
    public List<TypeData> typeDataList(String fullyQualifiedName) {
        String[] parts = fullyQualifiedName.split("\\.");
        return typeTrie.get(parts);
    }

    private TypeData bestCandidate(SourceSet sourceSetOfRequest, List<TypeData> typeDataList, SourceSet nearestSourceSet) {
        if (sourceSetOfRequest == ANY) {
            // only for the call packageContainsTypes()
            return typeDataList.stream().findFirst().orElse(null);
        }
        Map<SourceSet, Integer> priorityMap = sourceSetOfRequest.priorityDependencies();
        return typeDataList.stream()
                .map(td -> {
                    Integer priority;
                    if (td.sourceFile().sourceSet().equals(sourceSetOfRequest)) {
                        priority = -2; // top prio
                    } else if (td.sourceFile().sourceSet().equals(nearestSourceSet)) {
                        priority = -1; // 2nd highest prio
                    } else if (td.sourceFile().sourceSet().partOfJdk()) {
                        priority = 1_000; // bottom prio
                    } else {
                        Integer inMap = priorityMap.get(td.sourceFile().sourceSet());
                        if (inMap != null) {
                            priority = inMap;
                        } else if (!td.isCompiled()) {
                            priority = 10; // low; we have removed entries from classes with a corresponding source file
                        } else {
                            priority = null;
                        }
                    }
                    return priority == null ? null : new AbstractMap.SimpleEntry<>(td, priority);
                })
                .filter(Objects::nonNull)
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        TypeData typeData;
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null && (!single.compilationUnit().sourceSet().externalLibrary() || single.hasBeenInspected()))
                return single;
            typeData = typeDataOrNull(fullyQualifiedName, sourceSetOfRequest, null, true);
            if (typeData == null) return null;
            if (typeData.typeInfo() != null
                && (!typeData.typeInfo().compilationUnit().externalLibrary() || typeData.typeInfo().hasBeenInspected()))
                return typeData.typeInfo();
        } finally {
            trieLock.readLock().unlock();
        }
        return load(typeData, sourceSetOfRequest);
    }

    @Override
    public void invalidate(TypeInfo typeInfo) {
        assert !typeInfo.compilationUnit().externalLibrary();
        assert typeInfo.isPrimaryType();
        trieLock.writeLock().lock();
        try {
            String fullyQualifiedName = typeInfo.fullyQualifiedName();
            String[] parts = fullyQualifiedName.split("\\.");
            typeTrie.visit(parts, (_, list) -> {
                // this is the primary type and all its subtypes
                for (TypeData typeData : list) {
                    ((TypeDataImpl) typeData).clearTypeInfo();
                    mapSingleTypeForFQN.remove(fullyQualifiedName);
                }
            });
        } finally {
            trieLock.writeLock().unlock();
        }
    }

    private TypeInfo load(TypeData typeData, SourceSet sourceSetOfRequest) {
        TypeInfo typeInfo;
        synchronized (byteCodeInspector) {
            typeInfo = byteCodeInspector.get().load(typeData, sourceSetOfRequest);
        }
        return typeInfo;
    }

    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
        this.byteCodeInspector.set(byteCodeInspector);
    }

    @Override
    public void preload(String thePackage) {
        LOGGER.info("Start pre-loading {}", thePackage);
        List<TypeInfo> types = primaryTypesInPackageEnsureLoaded(thePackage, ANY);
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
            String[] parts = packageName.split("\\.");
            typeTrie.visitDoNotRecurse(parts, typeDataList -> {
                TypeData typeData;
                if (typeDataList == null || typeDataList.isEmpty()) {
                    typeData = null;
                } else {
                    typeData = bestCandidate(sourceSetOfRequest, typeDataList, null);
                }
                if (typeData != null) {
                    TypeInfo typeInfo;
                    if (typeData.typeInfo() != null && (!typeData.isCompiled() || typeData.typeInfo().hasBeenInspected())) {
                        typeInfo = typeData.typeInfo();
                    } else if (typeData.isCompiled()) {
                        // All the primary source types should be known; only compiled types can be loaded
                        typeInfo = load(typeData, sourceSetOfRequest);
                    } else {
                        typeInfo = null;
                        // is either a package-info.java, an annotated API file, ...
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
        return !primaryTypesInPackageEnsureLoaded(packageName, ANY).isEmpty();
    }

    public List<SourceFile> sourceFiles(String path) {
        String[] parts = path.split("/");
        List<TypeData> typeDataList = typeTrie.get(parts);
        return typeDataList == null ? null : typeDataList.stream().map(TypeData::sourceFile).toList();
    }
}
