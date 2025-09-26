package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.TypeMap;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.support.SetOnce;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final Logger LOGGER = LoggerFactory.getLogger(CompiledTypesManagerImpl.class);

    private final Resources classPath;
    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
    private final TypeMap typeMap;
    private final ReentrantReadWriteLock trieLock = new ReentrantReadWriteLock();
    private final Trie<TypeInfo> typeTrie = new Trie<>();
    private final Set<String> allTypesInThisPackageHaveBeenLoaded = new HashSet<>();
    private final ReentrantReadWriteLock allTypesLock = new ReentrantReadWriteLock();

    public CompiledTypesManagerImpl(Resources classPath, TypeMap typeMap) {
        this.classPath = classPath;
        this.typeMap = typeMap;
    }

    @Override
    public Resources classPath() {
        return classPath;
    }

    @Override
    public void add(TypeInfo typeInfo) {
        typeMap.put(typeInfo);
        trieLock.writeLock().lock();
        try {
            String[] parts = typeInfo.fullyQualifiedName().split("\\.");
            typeTrie.add(parts, typeInfo);
        } finally {
            trieLock.writeLock().unlock();
        }
    }

    @Override
    public SourceFile fqnToPath(String fqn, String suffix) {
        return classPath.fqnToPath(fqn, suffix);
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return typeMap.get(fullyQualifiedName, sourceSetOfRequest);
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        TypeInfo typeInfo = typeMap.get(fullyQualifiedName, sourceSetOfRequest);
        if (typeInfo != null) return typeInfo;

        SourceFile path = fqnToPath(fullyQualifiedName, ".class");
        if (path == null) return null;
        synchronized (byteCodeInspector) {
            return byteCodeInspector.get().load(path);
        }
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {
        if (!typeInfo.hasBeenInspected()) {
            SourceFile sourceFile = fqnToPath(typeInfo.fullyQualifiedName(), ".class");
            if (sourceFile == null) {
                throw new UnsupportedOperationException("Cannot find .class file for " + typeInfo);
            }
            synchronized (byteCodeInspector) {
                byteCodeInspector.get().load(typeInfo);
            }
        }
    }

    @Override
    public TypeInfo load(SourceFile path) {
        // only to be used when the type does not yet exist!
        synchronized (byteCodeInspector) {
            return byteCodeInspector.get().load(path);
        }
    }

    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
        this.byteCodeInspector.set(byteCodeInspector);
    }

    @Override
    public void preload(String thePackage) {
        LOGGER.info("Start pre-loading {}", thePackage);
        int inspected = loadAllTypesInPackage(thePackage, null);
        LOGGER.info("... inspected {} paths", inspected);
    }

    private int loadAllTypesInPackage(String thePackage, SourceSet sourceSetOfRequest) {
        AtomicInteger inspected = new AtomicInteger();
        classPath.expandLeaves(thePackage, ".class", (expansion, _) -> {
            // we'll loop over the primary types only
            if (!expansion[expansion.length - 1].contains("$")) {
                String fqn = fqnOfClassFile(thePackage, expansion);
                assert acceptFQN(fqn);
                trieLock.readLock().lock();
                TypeInfo typeInfo;
                try {
                    typeInfo = typeMap.get(fqn, sourceSetOfRequest);
                } finally {
                    trieLock.readLock().unlock();
                }
                if (typeInfo == null) {
                    SourceFile path = fqnToPath(fqn, ".class");
                    if (path != null) {
                        synchronized (byteCodeInspector) {
                            byteCodeInspector.get().load(path);
                        }
                        inspected.incrementAndGet();
                    }
                }
            }
        });
        return inspected.get();
    }

    private String fqnOfClassFile(String prefix, String[] suffixes) {
        String combined = prefix + "." + String.join(".", suffixes).replaceAll("\\$", ".");
        if (combined.endsWith(".class")) {
            return combined.substring(0, combined.length() - 6);
        }
        throw new UnsupportedOperationException("Expected .class or .java file, but got " + combined);
    }

    @Override
    public List<TypeInfo> typesLoaded() {
        return typeMap.typeStream()
                .filter(ti -> ti.compilationUnit().externalLibrary())
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
    }

    @Override
    public List<TypeInfo> primaryTypesInPackage(String fullyQualified) {
        return typeMap.primaryTypesInPackage(fullyQualified);
    }

    @Override
    public List<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, SourceSet sourceSetOfRequest) {
        ensureAllTypesInThisPackageHaveBeenLoaded(packageName, sourceSetOfRequest);
        String[] packages = packageName.split("\\.");
        List<TypeInfo> result = new ArrayList<>();
        trieLock.readLock().lock();
        try {
            typeTrie.visit(packages, (_, list) -> list.stream()
                    .filter(ti -> ti.isPrimaryType() && packageName.equals(ti.packageName()))
                    .forEach(result::add));
            return List.copyOf(result);
        } finally {
            trieLock.readLock().unlock();
        }
    }

    private void ensureAllTypesInThisPackageHaveBeenLoaded(String packageName, SourceSet sourceSetOfRequest) {
        allTypesLock.readLock().lock();
        try {
            if (!allTypesInThisPackageHaveBeenLoaded.contains(packageName)) {
                allTypesLock.readLock().unlock();
                allTypesLock.writeLock().lock();
                try {
                    try {
                        if (allTypesInThisPackageHaveBeenLoaded.add(packageName)) {
                            loadAllTypesInPackage(packageName, sourceSetOfRequest);
                        }
                    } finally {
                        allTypesLock.readLock().lock();
                    }
                } finally {
                    allTypesLock.writeLock().unlock();
                }
            }
        } finally {
            allTypesLock.readLock().unlock();
        }
    }

    @Override
    public boolean packageContainsTypes(String packageName) {
        AtomicBoolean found = new AtomicBoolean();
        classPath.expandLeaves(packageName, ".class", (_, l) -> {
            if (!l.isEmpty()) found.set(true);
        });
        return found.get();
    }
}
