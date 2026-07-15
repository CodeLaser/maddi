package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.util.*;

// important: this class should not retain any references to OpenJDK structures
public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final SourceSet javaBase;
    private final Map<String, TypeInfo> typesLoaded = new HashMap<>();
    private final Set<String> packageParts = new HashSet<>();
    // the scan's own registry. This class is a second, flatter cache of the same types, so invalidating or rewiring
    // a type has to reach both: leaving the type in InfoByFqn would have the next scan resolve references to the
    // stale object.
    private final InfoByFqn infoByFqn;
    // a javac-FREE callback (so this class retains no OpenJDK references): the driver injects one that loads a
    // single compiled type by FQN from bytecode on demand (JavaInspectorImpl -> ScanCompilationUnits).
    private java.util.function.Function<String, TypeInfo> lazyLoader;

    public CompiledTypesManagerImpl(SourceSet javaBase, InfoByFqn infoByFqn) {
        this.javaBase = javaBase;
        this.infoByFqn = infoByFqn;
    }

    public void setLazyLoader(java.util.function.Function<String, TypeInfo> lazyLoader) {
        this.lazyLoader = lazyLoader;
    }

    @Override
    public SourceSet javaBase() {
        return javaBase;
    }

    @Override
    public TypeData typeDataOrNull(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, boolean complainSingle) {
        return null;
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {
        typesLoaded.put(typeInfo.fullyQualifiedName(), typeInfo);
        packageParts.addAll(Arrays.asList(typeInfo.packageName().split("\\.")));
    }

    /**
     * Forget a changed source primary type and all its subtypes, so the next parse builds them afresh from source
     * ({@code InvalidationState.INVALID}). Both registries are cleared; see the {@code infoByFqn} field.
     */
    @Override
    public void invalidate(TypeInfo typeInfo) {
        assert !typeInfo.compilationUnit().externalLibrary() : "Cannot invalidate a library type: " + typeInfo;
        assert typeInfo.isPrimaryType() : "Can only invalidate a primary type: " + typeInfo;
        typeInfo.recursiveSubTypeStream().forEach(ti -> typesLoaded.remove(ti.fullyQualifiedName()));
        infoByFqn.removeType(typeInfo);
    }

    /**
     * Point both registries at the rewired copy of a primary type and its subtypes: same FQN and source set, new
     * object ({@code InvalidationState.REWIRE}). Called after {@code InfoMap.rewireAll()}.
     */
    @Override
    public void setRewiredType(TypeInfo typeInfo) {
        assert typeInfo.isPrimaryType() : "Can only rewire a primary type: " + typeInfo;
        typeInfo.recursiveSubTypeStream().forEach(ti -> typesLoaded.put(ti.fullyQualifiedName(), ti));
        infoByFqn.replaceType(typeInfo);
    }

    @Override
    public List<TypeInfo> typesLoaded(Boolean compiled) {
        return typesLoaded.values().stream()
                .filter(ti -> compiled == null ||
                              ti.compilationUnit().sourceSet() != null &&
                              ti.compilationUnit().sourceSet().externalLibrary() == compiled)
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName))
                .toList();
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return typesLoaded.get(fullyQualifiedName);
    }

    // lazily load a compiled type from bytecode on a miss (via the injected loader), so a type first requested
    // by another front-end resolves to the same bytecode-authoritative TypeInfo the Java scan would build.
    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        TypeInfo typeInfo = typesLoaded.get(fullyQualifiedName);
        if (typeInfo != null) return typeInfo;
        if (lazyLoader == null) return null;
        TypeInfo loaded = lazyLoader.apply(fullyQualifiedName);
        if (loaded != null) addTypeInfo(null, loaded); // cache; the loader already registered it in InfoByFqn
        return loaded;
    }

    @Override
    public void preload(String thePackage) {
        // TODO
    }

    @Override
    public boolean isPackagePart(String string) {
        return packageParts.contains(string);
    }

    // FIXME direct dependencies here!
    @Override
    public List<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, SourceSet sourceSetOfRequest) {
        return typesLoaded.values().stream()
                .filter(ti -> ti.isPrimaryType() && ti.packageName().equals(packageName)
                              && sourceSetOfRequest.dependencies().contains(ti.compilationUnit().sourceSet()))
                .toList();
    }

    private final SourceSet ANY = new SourceSetImpl.Builder()
            .setName("any source set will do")
            .setUri(URI.create("file:/"))
            .build();

    @Override
    public boolean packageContainsTypes(String packageName) {
        return !primaryTypesInPackageEnsureLoaded(packageName, ANY).isEmpty();
    }
}
