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

    private static class Candidate {
        private final SourceFile sourceFile;
        private TypeInfo typeInfo;

        Candidate(SourceFile sourceFile) {
            this.sourceFile = sourceFile;
        }

        public Candidate(SourceFile sourceFile, TypeInfo typeInfo) {
            this.sourceFile = sourceFile;
            this.typeInfo = typeInfo;
        }

        boolean isCompiled() {
            return sourceFile.sourceSet().externalLibrary();
        }
    }

    private final Resources classPath;
    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
    private final Map<String, TypeInfo> mapSingleTypeForFQN = new HashMap<>();
    private final ReentrantReadWriteLock trieLock = new ReentrantReadWriteLock();
    private final Trie<Candidate> typeTrie = new Trie<>();

    public CompiledTypesManagerImpl(Resources classPath) {
        this.classPath = classPath;
        addToTrie(classPath, false);
    }


    // we must also call this for the sources, after the classpath
    // clearExisting == true when you want the sources to override any same FQN type in the class path
    public void addToTrie(Resources resources, boolean removeCompiled) {
        resources.visit(new String[0], (parts, sourceFiles) -> {
            List<Candidate> candidates = sourceFiles.stream()
                    .filter(sf -> {
                        String uriString = sf.uri().toString();
                        return uriString.endsWith(".class") || uriString.endsWith(".java");
                    })
                    .map(Candidate::new).toList();
            if (!candidates.isEmpty()) {
                int last = parts.length - 1;
                int dot = parts[last].lastIndexOf('.');
                assert dot > 0;
                parts[last] = parts[last].substring(0, dot);
                List<Candidate> all = typeTrie.addAll(parts, candidates);
                if (removeCompiled) all.removeIf(Candidate::isCompiled);
            }
        });
    }

    @Override
    public Resources classPath() {
        return classPath;
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {
        trieLock.writeLock().lock();
        try {
            String fullyQualifiedName = typeInfo.fullyQualifiedName();
            String[] parts = fullyQualifiedName.split("\\.");
            if (!typeInfo.isPrimaryType() && !typeInfo.compilationUnit().externalLibrary()) {
                // we cannot know beforehand the subtypes of a source type. those we should add now
                List<Candidate> subTypes = typeTrie.add(parts, new Candidate(sourceFile, typeInfo));
                if (subTypes.size() == 1) {
                    mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
                } else if (subTypes.size() == 2) {
                    mapSingleTypeForFQN.remove(fullyQualifiedName); // multiple
                }
                return;
            }
            List<Candidate> types = typeTrie.get(parts);
            assert types != null : "We should know about " + fullyQualifiedName;
            if (types.size() == 1) {
                mapSingleTypeForFQN.put(fullyQualifiedName, typeInfo);
            }
            Candidate candidate = types.stream().filter(c -> c.sourceFile.equals(sourceFile)).findFirst().orElseThrow();
            candidate.typeInfo = typeInfo;
        } finally {
            trieLock.writeLock().unlock();
        }
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null) return single;
            Candidate candidate = candidateOrNull(fullyQualifiedName, sourceSetOfRequest);
            return candidate == null ? null : candidate.typeInfo;
        } finally {
            trieLock.readLock().unlock();
        }
    }

    private Candidate candidateOrNull(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        String[] parts = fullyQualifiedName.split("\\.");
        List<Candidate> candidates = typeTrie.get(parts);
        if (candidates == null || candidates.isEmpty()) return null;
       // assert candidates.size() > 1 : "Otherwise, would have been in mapSingleTypeForFQN";
        Set<SourceSet> sourceSets = sourceSetOfRequest == null ? null : sourceSetOfRequest.recursiveDependenciesSameExternal();
        return candidates.stream()
                .filter(candidate -> sourceSetOfRequest == null || sourceSets.contains(candidate.sourceFile.sourceSet()))
                .findFirst().orElse(null);
    }

    private Candidate candidateExactSourceSet(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        String[] parts = fullyQualifiedName.split("\\.");
        List<Candidate> candidates = typeTrie.get(parts);
        Candidate candidate;
        if (candidates != null) {
            candidate = candidates.stream()
                    .filter(c -> c.sourceFile.sourceSet().equals(sourceSetOfRequest))
                    .findFirst().orElse(null);
        } else {
            candidate = null;
        }
        if (candidate == null) {
            throw new UnsupportedOperationException("Cannot find .class file for " + fullyQualifiedName + " in "
                                                    + sourceSetOfRequest);
        }
        return candidate;
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        trieLock.readLock().lock();
        Candidate candidate;
        try {
            TypeInfo single = mapSingleTypeForFQN.get(fullyQualifiedName);
            if (single != null) return single;
            candidate = candidateOrNull(fullyQualifiedName, sourceSetOfRequest);
            if (candidate == null) return null;
            if (candidate.typeInfo != null) return candidate.typeInfo;
        } finally {
            trieLock.readLock().unlock();
        }
        synchronized (byteCodeInspector) {
            return byteCodeInspector.get().load(candidate.sourceFile);
        }
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {
        assert typeInfo.compilationUnit().externalLibrary();
        if (!typeInfo.hasBeenInspected()) {
            Candidate candidate = candidateExactSourceSet(typeInfo.fullyQualifiedName(),
                    typeInfo.compilationUnit().sourceSet());
            synchronized (byteCodeInspector) {
                byteCodeInspector.get().load(candidate.sourceFile, typeInfo);
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
            List<Candidate> candidates = typeTrie.get(parts);
            Candidate candidate = candidates.stream()
                    .filter(c -> c.typeInfo == typeInfo)
                    .findFirst().orElse(null);
            if (candidate != null) {
                candidate.typeInfo = null;
                mapSingleTypeForFQN.remove(fullyQualifiedName);
            }
        } finally {
            trieLock.writeLock().unlock();
        }
    }

    @Override
    public TypeInfo load(SourceFile path) {
        TypeInfo typeInfo;
        synchronized (byteCodeInspector) {
            typeInfo = byteCodeInspector.get().load(path);
        }
        if (typeInfo != null) {
            addTypeInfo(path, typeInfo);
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
                    .filter(candidate -> candidate.typeInfo != null
                                         && (compiled == null || compiled == candidate.isCompiled()))
                    .forEach(candidate -> result.add(candidate.typeInfo)));
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
            Set<SourceSet> sourceSets = sourceSetOfRequest == null ? null
                    : sourceSetOfRequest.recursiveDependenciesSameExternal();
            String[] parts = packageName.split("\\.");
            typeTrie.visitLeaves(parts, (_, candidates) -> {
                Candidate candidate;
                if (candidates.isEmpty()) {
                    candidate = null;
                } else if (sourceSetOfRequest == null) {
                    candidate = candidates.getFirst();// we cannot know
                } else {
                    candidate = candidates.stream().filter(c -> sourceSets.contains(c.sourceFile.sourceSet()))
                            .findFirst().orElse(null);
                }
                if (candidate != null) {
                    TypeInfo typeInfo;
                    if (candidate.typeInfo != null) {
                        typeInfo = candidate.typeInfo;
                    } else if (candidate.isCompiled()) {
                        // All the primary source types should be known; only compiled types can be loaded
                        typeInfo = load(candidate.sourceFile);
                    } else {
                        typeInfo = null;
                        assert candidate.sourceFile.uri().toString().endsWith("package-info.java");
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
}
