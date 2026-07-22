package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

// important: this class should not retain any references to OpenJDK structures
public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompiledTypesManagerImpl.class);

    private final SourceSet javaBase;
    // read and written from parallel analyzer worker threads (getOrLoad during PARALLEL analysis):
    // these must be concurrent, and the lazy-load slow path below must additionally serialize access
    // to the (thread-hostile) javac task behind the loader
    private final Map<String, TypeInfo> typesLoaded = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> packageParts = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // the scan's own registry. This class is a second, flatter cache of the same types, so invalidating or rewiring
    // a type has to reach both: leaving the type in InfoByFqn would have the next scan resolve references to the
    // stale object.
    private final InfoByFqn infoByFqn;
    // a javac-FREE callback (so this class retains no OpenJDK references): the driver injects one that loads a
    // single compiled type by FQN from bytecode on demand (JavaInspectorImpl -> ScanCompilationUnits).
    private java.util.function.Function<String, TypeInfo> lazyLoader;

    // Once the javac AST is dropped (JavaInspectorImpl.invalidateAllSources on a heavy-analysis run, DROP_AST),
    // the lazy loader can no longer serve any compiled type it had not already cached. A getOrLoad miss then
    // means the drop was too aggressive for this analysis. We must NOT silently return null (that corrupts the
    // analysis with missing types); we surface it — log the FQN once (measurement mode), or throw (strict mode).
    // The flag mirrors "no live javac task" and is flipped by JavaInspectorImpl as the task is dropped/re-scanned.
    private volatile boolean lazyLoaderDisabled;
    private volatile boolean strictOnDisabledMiss; // throw instead of log-and-null; a run-level policy, set once
    private final Set<String> distinctMissesAfterDrop = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final LongAdder totalMissesAfterDrop = new LongAdder();

    public CompiledTypesManagerImpl(SourceSet javaBase, InfoByFqn infoByFqn) {
        this.javaBase = javaBase;
        this.infoByFqn = infoByFqn;
    }

    public void setLazyLoader(java.util.function.Function<String, TypeInfo> lazyLoader) {
        this.lazyLoader = lazyLoader;
    }

    /** Flipped by the inspector: {@code true} after the javac AST is dropped, {@code false} once a scan revives it. */
    public void setLazyLoaderDisabled(boolean lazyLoaderDisabled) {
        this.lazyLoaderDisabled = lazyLoaderDisabled;
    }

    /** When the loader is disabled, throw on a miss (fail loud) instead of logging and returning null. */
    public void setStrictOnDisabledMiss(boolean strictOnDisabledMiss) {
        this.strictOnDisabledMiss = strictOnDisabledMiss;
    }

    /** How many distinct FQNs could not be resolved after the AST was dropped (0 == the drop was safe). */
    public int distinctMissesAfterDrop() {
        return distinctMissesAfterDrop.size();
    }

    /** Total getOrLoad misses (with repeats) after the AST was dropped. */
    public long totalMissesAfterDrop() {
        return totalMissesAfterDrop.sum();
    }

    /** A sample of the FQNs that went unresolved after the drop, for a post-run report. */
    public List<String> sampleMissesAfterDrop(int limit) {
        return distinctMissesAfterDrop.stream().sorted().limit(limit).toList();
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
        // ask each type what it belongs to, rather than walk down: primaryType() climbs enclosing types AND enclosing
        // methods, so it also claims the anonymous types a compilation unit registers (a.b.C.$0), which
        // recursiveSubTypeStream() does not list. One left behind makes the re-scan throw "Duplicating type".
        typesLoaded.values().removeIf(ti -> typeInfo.equals(ti.primaryType()));
        infoByFqn.removeType(typeInfo);
    }

    /**
     * Point both registries at one rewired type: same FQN and source set, new object
     * ({@code InvalidationState.REWIRE}). Registers exactly the type given — the caller feeds it every type the
     * rewire produced ({@code InfoMap.rewiredTypes()}), primary types, subtypes and the anonymous/local/lambda types
     * alike. Walking the type here instead would miss the last group: they are not among its {@code subTypes()}.
     */
    @Override
    public void setRewiredType(TypeInfo typeInfo) {
        typesLoaded.put(typeInfo.fullyQualifiedName(), typeInfo);
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
        if (lazyLoader != null) {
            // the loader runs on the scan's live JavacTask, and javac is not thread-safe: unsynchronized
            // concurrent loads from PARALLEL analyzer threads corrupted javac's process-wide state, surfacing
            // as the intermittent StarImportScope NPE / CompilationProblems in LATER parses of the same JVM
            synchronized (this) {
                TypeInfo raced = typesLoaded.get(fullyQualifiedName);
                if (raced != null) return raced;
                TypeInfo loaded = lazyLoader.apply(fullyQualifiedName);
                if (loaded != null) {
                    addTypeInfo(null, loaded); // cache; the loader already registered it in InfoByFqn
                    return loaded;
                }
            }
        }
        // A miss. Normally benign — the type is simply not on the (deliberately partial) classpath, and callers
        // handle null. But once the AST has been dropped the loader is dead, so a miss here means a type the
        // analysis genuinely needed is now unresolvable. Surface it rather than corrupt the analysis silently.
        if (lazyLoaderDisabled) reportMissAfterDrop(fullyQualifiedName);
        return null;
    }

    private void reportMissAfterDrop(String fullyQualifiedName) {
        totalMissesAfterDrop.increment();
        boolean firstTime = distinctMissesAfterDrop.add(fullyQualifiedName);
        if (strictOnDisabledMiss) {
            throw new IllegalStateException("getOrLoad miss after the javac AST was dropped: " + fullyQualifiedName
                    + " — the drop was too aggressive for this analysis (pre-load this type, or don't drop the AST)");
        }
        if (firstTime) {
            LOGGER.warn("getOrLoad miss after javac AST drop: {} (returning null; the type is now unresolvable)",
                    fullyQualifiedName);
        }
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
