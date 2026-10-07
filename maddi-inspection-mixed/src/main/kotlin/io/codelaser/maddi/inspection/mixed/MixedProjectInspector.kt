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

package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.integration.JavaInspector
import io.codelaser.maddi.inspection.api.parser.Summary
import io.codelaser.maddi.inspection.api.resource.InputConfiguration
import io.codelaser.maddi.inspection.kotlin.JavaStubGenerator
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds
import io.codelaser.maddi.kotlin.api.KotlinParseObserver
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl
import io.codelaser.maddi.java.openjdk.SourceSetInterleave
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.StandardLocation
import javax.tools.ToolProvider

/**
 * The **multi-source-set** mixed driver: parse a mixed Java+Kotlin [InputConfiguration] so every type lands in
 * its own CST source set (not flattened into one, as [MixedInspector.parseFromConfiguration] does), over one
 * shared core. Each source set is classified by its on-disk contents (`.kt` ⇒ Kotlin, else `.java` ⇒ Java).
 *
 * The openjdk [JavaInspectorImpl] owns the shared `Runtime`/`InfoByFqn`/`CompiledTypesManager` and is
 * initialised with the Java source sets (+ the generated-stub directory on their classpath). The parse order
 * follows the cross-language dependency direction:
 * - **Java→Kotlin** (or independent): Kotlin runs first via [KotlinFrontEnd.projectScan] (one module per set,
 *   dependency order, sharing the core); a stub is generated and compiled for every Kotlin type; then the Java
 *   sets parse from disk, resolving Kotlin references against the stubs and reusing the shared Kotlin `TypeInfo`.
 * - **Kotlin→Java** (only): the Java sets parse first (their source types commit to the shared CTM), then
 *   Kotlin resolves those references to the same instances (the project scan gets the Java dirs as source
 *   roots so K2 resolves the symbols; the `TypeInfo` comes from the shared CTM).
 *
 * - **Both directions** across sets, or a set holding both languages: neither can go first, so the sets are
 *   interleaved in one dependency order ([parseInterleaved]).
 *
 * A Java set's Kotlin-set dependencies are satisfied via the stubs (dropped from the Java inspector's view). An
 * intra-module Kotlin↔Java cycle within ONE set's declarations (the skeleton-pre-pass case) is a follow-up.
 * Single-threaded (javac); needs the openjdk `--add-exports`.
 */
class MixedProjectInspector @JvmOverloads constructor(private val settings: Settings = Settings()) {

    /**
     * What a host can vary about the Java half of the parse. The defaults are the batch runner's; the IDE daemon
     * is the host that needs the others.
     *
     * @param parseOptions handed to every Java parse (detailed sources are what inline hints are placed by)
     * @param tolerateParseErrors keep the Java types that DID parse when some did not, as an editor on a tree
     *        mid-edit needs. The default refuses, as [Summary.parseResult] does: a batch run over a configuration
     *        that does not compile should say so, not analyse part of it.
     * @param beforeInitialize called on the Java inspector just before `initialize`, the only point at which a
     *        `preload(...)` registration still takes effect (the daemon's hint loader needs the JDK packages it
     *        decodes parsed first)
     * @param newJavaInspector the Java inspector that owns the shared core: a host that reparses incrementally
     *        needs one that computes fingerprints (the refactor server's `JavaInspectorImpl(true, true)`)
     */
    data class Settings @JvmOverloads constructor(
        val parseOptions: JavaInspector.ParseOptions = JavaInspector.ParseOptions.Builder().build(),
        val tolerateParseErrors: Boolean = false,
        val beforeInitialize: java.util.function.Consumer<JavaInspector>? = null,
        val newJavaInspector: java.util.function.Supplier<JavaInspectorImpl> =
            java.util.function.Supplier { JavaInspectorImpl() },
    )

    /** The Kotlin front end: the contract only. Its implementation may live in a realm of its own. */
    private val frontEnd get() = KotlinFrontEnds.get()


    /**
     * ⚠ The stub directory must outlive every call -- [sourceSet] names it as an external-library URI -- so it
     * cannot be deleted eagerly and this class has no close(). A shutdown hook is therefore the only correct
     * point: the directory lives as long as the JVM might read it, and does not survive the run. Left behind,
     * one per instance, these accumulated in /tmp -- a tmpfs with a hard INODE cap -- until createTempDirectory
     * itself started failing with "No space left on device" on a filesystem 93% empty by bytes.
     */
    private val stubDir = Files.createTempDirectory("mixed-proj-stubs").also { dir ->
        java.lang.Runtime.getRuntime().addShutdownHook(Thread {
            runCatching { dir.toFile().deleteRecursively() }
        })
    }

    /** [kotlinBySourceSet] keeps the per-source-set placement; [javaTypes] are the primary Java types;
     *  [runtime] is the shared core both front-ends populated (needed by downstream analysis);
     *  [javaInspector] is the owner of that core — the modification analyzer
     *  (`IteratingAnalyzerImpl`) takes a `JavaInspector`, and this is the only one in a mixed run, so a
     *  Kotlin-only project can still be analyzed through it. */
    data class Result(
        val kotlinBySourceSet: Map<SourceSet, List<TypeInfo>>,
        val javaTypes: List<TypeInfo>,
        val runtime: Runtime,
        val javaInspector: JavaInspector,
        /** The Java half's parse summary: its parse errors and warnings, and whether the run is partial. */
        val javaSummary: Summary? = null,
    ) {
        val kotlinTypes: List<TypeInfo> get() = kotlinBySourceSet.values.flatten()
    }

    /**
     * @param observers read the Kotlin parse through its K2 session before it closes (e.g. a
     * [io.codelaser.maddi.kotlin.api.KotlinReferenceIndex], the oracle a rename census compares against). As
     * `KotlinInspector.parseFromConfiguration(observers)`, which a mixed project cannot use.
     */
    @JvmOverloads
    fun parse(config: InputConfiguration, observers: List<KotlinParseObserver> = emptyList()): Result {
        val sourceSets = config.sourceSets().filter { !it.externalLibrary() }
        // one module, both languages, referencing each other: neither front end can go first (see parseInterleaved)
        if (sourceSets.any { hasExtension(it, ".kt") && hasExtension(it, ".java") }) return parseInterleaved(config, observers)
        val kotlinSets = sourceSets.filter { hasExtension(it, ".kt") }
        val javaSets = sourceSets.filter { !hasExtension(it, ".kt") && hasExtension(it, ".java") }
        val javaSetIdentity = javaSets.toSet()
        val kotlinSetIdentity = kotlinSets.toSet()
        val kotlinDependsOnJava = kotlinSets.any { it.dependencies().any { d -> d in javaSetIdentity } }
        val javaDependsOnKotlin = javaSets.any { it.dependencies().any { d -> d in kotlinSetIdentity } }
        // ⛔ BOTH DIRECTIONS, ACROSS SETS: neither language can go first either. Kotlin first, K2 met the upstream Java
        // sets' types before javac had parsed them -- unresolved, or read off their build output, after which the
        // source commit and a downstream set's class-file read disagreed and the parse was refused (82 errors on the
        // jfocus workspace: 173 Kotlin->Java and 34 Java->Kotlin set dependencies). The interleave runs the sets in
        // one dependency order, converting a Kotlin set just before the first Java set that needs it.
        if (kotlinDependsOnJava && javaDependsOnKotlin) return parseInterleaved(config, observers)

        val stubSet: SourceSet = SourceSetImpl.Builder().setName("mixed-stubs")
            .setUri(stubDir.toUri()).setExternalLibrary(true).build()

        // the Java front-end owns the shared core; a Java set's Kotlin-set deps are satisfied by the stubs (so
        // dropped), its library + Java-set deps are kept, plus the stub directory. withDependencies() mints new
        // SourceSet instances, so rebuild in dependency order and remap a Java-set dep to its rebuilt instance —
        // otherwise a dependent would point at the original (not-in-config) set and the linearization misses it.
        val javaInspector = settings.newJavaInspector.get()
        // The Java half needs the SAME class path as the Kotlin half, because it is the half that loads library
        // types from bytecode for both. It used to get `jmod:java.base` and nothing else, which was survivable
        // only while nothing ever asked it to load anything: with the loader alive (below), K2 delegates real
        // library types to it, and every Kotlin class file carries `@kotlin.Metadata`, so loading one type off
        // kotlin-stdlib or kotlinx-coroutines drags in names javac cannot see — ClassSymbolScanner.classType
        // NPEs on the unresolved reference (`kotlin.Metadata`, `kotlin.coroutines.CoroutineContext`,
        // `org.jetbrains.annotations.NotNull`). `jmod:java.se` is added only when the configuration carries no
        // JDK parts of its own: a compile-log-derived one already has the closure, a hand-assembled Kotlin one
        // need not, the Kotlin front end taking its JDK from java.home instead.
        val javaBase = SourceSetImpl.javaBase()
        val projectClassPath = config.classPathParts()
        val javaConfig = InputConfigurationImpl.Builder().addClassPathParts(stubSet)
        projectClassPath.forEach { javaConfig.addClassPathParts(it) }
        if (projectClassPath.none { it.partOfJdk() }) {
            javaConfig.addClassPathParts(javaBase).addClassPath("jmod:java.se")
        }
        val rebuiltJavaSet = LinkedHashMap<SourceSet, SourceSet>()
        dependencyOrder(javaSets).forEach { js ->
            val keptDeps = js.dependencies().mapNotNull { d ->
                when {
                    d.externalLibrary() -> d
                    d in javaSetIdentity -> rebuiltJavaSet[d] ?: d // the rebuilt upstream Java set
                    else -> null // a Kotlin-set dependency: satisfied by the stubs
                }
            }
            val rebuilt = js.withDependencies(keptDeps + stubSet)
            rebuiltJavaSet[js] = rebuilt
            javaConfig.addSourceSets(rebuilt)
        }
        // A Kotlin-ONLY project has no Java source set, and `onlyPreload` scans the *configured* source sets — so
        // with none, no scan runs, `lastScanUnits` is never set, and the shared CompiledTypesManager's lazy
        // bytecode loader has no live javac task behind it. The effect was silent and total: `getOrLoad` returned
        // null for EVERY library type (java.util.List included), so the Kotlin front end fell back to K2's own
        // view and the "bytecode is the authority for library shape" invariant of
        // maddi-inspection-kotlin/mixed-language-integration.md §9 did not hold. It also aborted the modification
        // analysis, `VirtualFieldComputer`'s constructor doing `getOrLoad(AtomicBoolean.class)`.
        //
        // A protocol source set gives the warmup something real to scan. Its DEPENDENCIES are the point: a task's
        // class path comes from them, so without java.base javac cannot resolve `java.lang.Object`'s own members
        // and the scan dies in ClassSymbolScanner.classType.
        if (javaSets.isEmpty()) {
            javaConfig.addSourceSets(SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL + "warmup")
                .setUri(URI.create("file:/"))
                .setDependencies(projectClassPath + javaBase).build())
        }
        settings.beforeInitialize?.accept(javaInspector)
        javaInspector.initialize(javaConfig.build())
        javaInspector.onlyPreload() // warm the CTM lazy loader before Kotlin delegates java.* to it
        // KNOWN GAP (Kotlin-only projects). `onlyPreload` scans the *configured* source sets, so with no Java
        // source set `scanSourceSet` never runs, `lastScanUnits` is never set, and the shared
        // CompiledTypesManager's lazy bytecode loader has no live javac task behind it. The effect is silent
        // and total: `getOrLoad` returns null for EVERY library type — java.util.List included — so K2 falls
        // back to its own view and the "bytecode is the authority for library shape" invariant documented in
        // maddi-inspection-kotlin/mixed-language-integration.md §9 does not hold here. It is also what stops
        // the modification analysis: `VirtualFieldComputer`'s constructor does `getOrLoad(AtomicBoolean.class)`
        // and NPEs on the null. Adding a TEST_PROTOCOL source set to make the warmup a real scan (what the
        // inspector's own warning suggests) was tried and pushes the failure elsewhere — the fix belongs in
        // JavaInspectorImpl's preload path, not here.

        val runtime = javaInspector.runtime()
        val infoByFqn = javaInspector.infoByFqn()
        val ctm = javaInspector.compiledTypesManager()
        val libraryRoots = config.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }.filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        val orderedKotlin = dependencyOrder(kotlinSets)

        if (kotlinDependsOnJava) {
            // Kotlin→Java only: parse Java first (its source types commit to the shared CTM), then Kotlin
            // resolves those references to the same instances (K2 sees the Java dirs as source roots).
            val (javaTypes, javaSummary) = parseJava(javaInspector)
            val javaSourceRoots = javaSets.flatMap { it.sourceDirectories() }
            val kotlinBySourceSet = frontEnd.projectScan(runtime, infoByFqn, ctm)
                .parse(orderedKotlin, libraryRoots, jdkHome, javaSourceRoots, observers)
            return Result(kotlinBySourceSet, javaTypes, runtime, javaInspector, javaSummary)
        }

        // Java→Kotlin (or independent): Kotlin first, generate stubs, then Java resolves Kotlin via the stubs.
        val kotlinBySourceSet = frontEnd.projectScan(runtime, infoByFqn, ctm)
            .parse(orderedKotlin, libraryRoots, jdkHome, emptyList(), observers)
        val kotlinTypes = kotlinBySourceSet.values.flatten()
        // PRIMARY types only: JavaStubGenerator already recurses into subTypes(), so stubbing a nested type
        // as well emits it twice — once nested inside its parent's stub, once as a top-level class in the
        // parent's package ("duplicate class: coil3.Builder", for `coil3.Extras.Builder` and 13 others).
        val primaryKotlinTypes = kotlinTypes.filter { it.primaryType() === it }
        // ... and only when there is Java to resolve them FROM. A stub exists for exactly one reason: javac
        // cannot read Kotlin, so a Java source referencing a Kotlin type needs something to resolve against.
        // A project with no Java source sets — which is the common case for a Kotlin corpus, and true of both
        // detekt and coil — has nothing for javac to parse, so generating and compiling stubs is pure cost,
        // and any gap in JavaStubGenerator's fidelity becomes a spurious hard failure on a parse that is
        // otherwise complete. detekt is where that bit: all 31 source sets parsed, then the run aborted on
        // stub errors for a compilation that had no consumer.
        if (primaryKotlinTypes.isNotEmpty() && javaSets.isNotEmpty()) {
            compileStubs(primaryKotlinTypes, object : JavaStubGenerator.StubHints {}, libraryRoots)
        }
        val (javaTypes, javaSummary) = parseJava(javaInspector)
        return Result(kotlinBySourceSet, javaTypes, runtime, javaInspector, javaSummary)
    }

    /**
     * The parse of a configuration in which at least one source set holds Java AND Kotlin sources -- javalin's
     * `src/main/java`, where `Handler.java` takes a Kotlin `Context` and 28 Kotlin files take a `Handler`. The
     * directional flows above cannot: Kotlin-first minted a K2 library copy of every Java type it named, and the
     * Java front end, never given the set (it holds `.kt`), parsed none of its `.java` files at all -- 0 Java types
     * on javalin.
     *
     * Each set is scanned by the Java front end in its own order, and the Kotlin front end is interleaved per set
     * ([SourceSetInterleave]): its declarations, on the Java types it names, before javac attributes the set; stubs
     * from those declarations, which javac then reads; its bodies once the set's Java types are committed. A
     * Kotlin-only set is converted whole when a set scanned by javac needs it, and at the end otherwise.
     */
    private fun parseInterleaved(config: InputConfiguration, observers: List<KotlinParseObserver>): Result {
        val sourceSets = config.sourceSets().filter { !it.externalLibrary() }
        val kotlinSets = sourceSets.filter { hasExtension(it, ".kt") }
        val javaSets = sourceSets.filter { hasExtension(it, ".java") }
        val kotlinSetNames = kotlinSets.map { it.name() }.toSet()
        val javaSetIdentity = javaSets.toSet()

        val stubSet: SourceSet = SourceSetImpl.Builder().setName("mixed-stubs")
            .setUri(stubDir.toUri()).setExternalLibrary(true).build()
        val javaInspector = settings.newJavaInspector.get()
        val javaBase = SourceSetImpl.javaBase()
        val projectClassPath = config.classPathParts()
        val javaConfig = InputConfigurationImpl.Builder().addClassPathParts(stubSet)
        projectClassPath.forEach { javaConfig.addClassPathParts(it) }
        if (projectClassPath.none { it.partOfJdk() }) {
            javaConfig.addClassPathParts(javaBase).addClassPath("jmod:java.se")
        }
        // as in parse: a Kotlin-only dependency is satisfied by the stubs; a Java-scanned one is its rebuilt instance
        val rebuiltJavaSet = LinkedHashMap<SourceSet, SourceSet>()
        dependencyOrder(javaSets).forEach { js ->
            val keptDeps = js.dependencies().mapNotNull { d ->
                when {
                    d.externalLibrary() -> d
                    d in javaSetIdentity -> rebuiltJavaSet[d] ?: d
                    else -> null
                }
            }
            val rebuilt = js.withDependencies(keptDeps + stubSet)
            rebuiltJavaSet[js] = rebuilt
            javaConfig.addSourceSets(rebuilt)
        }
        settings.beforeInitialize?.accept(javaInspector)
        javaInspector.initialize(javaConfig.build())
        javaInspector.onlyPreload()

        val libraryRoots = config.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }.filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        val orderedKotlin = dependencyOrder(kotlinSets)
        // a Kotlin-only set reads the Java-only sets' sources through K2's java-sources module; a mixed set's own
        // Java files are in its own module's roots already
        val javaOnlyRoots = javaSets.filter { it.name() !in kotlinSetNames }.flatMap { it.sourceDirectories() }
        // what a stub may name besides libraries: a Java-only set's types by its build output, on the class path; a
        // mixed set's own Java (no class file yet), or a Java-only set that has no output, by its sources. ⛔ Not every
        // set by its sources: javac then compiled whatever the stubs reached without that set's options (on the
        // jfocus workspace, maddi-java-openjdk's javac internals without its --add-exports), and the parse failed
        val (builtJava, unbuiltJava) = javaSets.partition { it.name() !in kotlinSetNames && outputOf(it) != null }
        val javaOutputDirs = builtJava.mapNotNull { outputOf(it) }
        val javaSourceDirs = unbuiltJava.flatMap { it.sourceDirectories() }.filter { Files.exists(it) }
        val kotlinByName = orderedKotlin.associateBy { it.name() }
        // ⛔ the CONFIGURATION's sets: the interleave hands over the rebuilt Java set, whose dependencies kept only
        // libraries and other Java-scanned sets. Walking those, a Kotlin-only upstream set was never converted, so
        // a mixed set's stubs could not name its types (Exposed: exposed-maven-plugin on exposed-plugin-core).
        val originalByName = sourceSets.associateBy { it.name() }
        val stubbed = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<TypeInfo, Boolean>())

        frontEnd.projectScan(javaInspector.runtime(), javaInspector.infoByFqn(), javaInspector.compiledTypesManager())
            .open(orderedKotlin, libraryRoots, jdkHome, javaOnlyRoots).use { kotlin ->
                fun convertUpstream(ss: SourceSet) {
                    ss.dependencies().forEach { d ->
                        val k = kotlinByName[d.name()] ?: return@forEach
                        if (d.name() in javaSetIdentity.map { it.name() }) return@forEach // scanned by javac, in order
                        convertUpstream(k)
                        if (!kotlin.isDeclared(k)) kotlin.convert(k)
                    }
                }
                javaInspector.setInterleave(object : SourceSetInterleave {
                    override fun beforeAttribution(sourceSet: SourceSet, sourceTypes: java.util.function.Function<String, TypeInfo>) {
                        convertUpstream(originalByName[sourceSet.name()] ?: sourceSet)
                        kotlinByName[sourceSet.name()]?.let { k -> kotlin.declare(k) { fqn -> sourceTypes.apply(fqn) } }
                        // every Kotlin type made so far that javac has no class file for yet
                        val fresh = kotlin.declaredTypes().filter { it.primaryType() === it && stubbed.add(it) }
                        if (fresh.isNotEmpty()) {
                            val hints = object : JavaStubGenerator.StubHints {
                                // no body yet, but an abstract declaration is one kotlinc left abstract
                                override fun hasBody(method: MethodInfo) = !method.isAbstract
                                override fun delegation(constructor: MethodInfo) = kotlin.delegationOf(constructor)
                            }
                            compileStubs(fresh, hints, libraryRoots + javaOutputDirs, javaSourceDirs)
                        }
                    }

                    override fun afterCommit(sourceSet: SourceSet) {
                        kotlinByName[sourceSet.name()]?.let { kotlin.complete(it) }
                    }
                })
                val (javaTypes, javaSummary) = try {
                    parseJava(javaInspector)
                } finally {
                    javaInspector.setInterleave(null)
                }
                orderedKotlin.filter { !kotlin.isDeclared(it) }.forEach { kotlin.convert(it) }
                kotlin.observe(observers)
                val kotlinBySourceSet = LinkedHashMap<SourceSet, List<TypeInfo>>()
                orderedKotlin.forEach { kotlinBySourceSet[it] = kotlin.result.getValue(it) }
                return Result(kotlinBySourceSet, javaTypes, javaInspector.runtime(), javaInspector, javaSummary)
            }
    }

    private fun parseJava(javaInspector: JavaInspector): Pair<List<TypeInfo>, Summary> {
        val summary = javaInspector.parse(mapOf(), settings.parseOptions)
        val parseResult = if (settings.tolerateParseErrors && summary.haveErrors()) summary.parseResultIgnoringErrors()
        else summary.parseResult()
        return parseResult.primaryTypes().toList() to summary
    }

    private fun hasExtension(sourceSet: SourceSet, extension: String): Boolean =
        sourceSet.sourceDirectories().filter { Files.exists(it) }.any { dir ->
            Files.walk(dir).use { paths -> paths.anyMatch { it.fileName.toString().endsWith(extension) } }
        }

    /** Topological order (dependencies before dependents) over the given source sets; ignores library deps. */
    private fun dependencyOrder(sourceSets: List<SourceSet>): List<SourceSet> {
        val set = sourceSets.toSet()
        val ordered = LinkedHashSet<SourceSet>()
        fun visit(ss: SourceSet) {
            if (ss in ordered || ss !in set) return
            ss.dependencies().forEach { if (it in set) visit(it) }
            ordered.add(ss)
        }
        sourceSets.forEach { visit(it) }
        return ordered.toList()
    }

    /** A source set's build output, when its `uri` names a directory or jar that exists. */
    private fun outputOf(sourceSet: SourceSet): Path? =
        sourceSet.uri()?.takeIf { it.scheme == "file" }?.let(::uriToPath)?.takeIf { Files.exists(it) && it != Path.of("/") }

    private fun uriToPath(uri: URI): Path? =
        runCatching { if (uri.scheme == "file") Paths.get(uri) else Paths.get(uri.schemeSpecificPart) }.getOrNull()

    /**
     * @param libraryRoots the configuration's library jars, which must be on the stub compiler's **class
     *        path**: a Kotlin signature routinely mentions a library type (coil's `ImageRequest` exposes
     *        `okio.FileSystem`, and `kotlin.Pair` / `CoroutineContext` / `Function1` come from the stdlib), and
     *        javac cannot compile a stub naming a type it cannot resolve.
     */
    /**
     * @param javaSourceDirs a mixed source set's Java sources, on the **source path**: a stub made from a Kotlin
     *        declaration names the Java types it takes and returns, which have no class file yet. `-implicit:none`,
     *        so javac reads them to resolve the stubs and writes nothing for them.
     */
    /**
     * @param types the primary types to stub, each into its own source; [hints] as [JavaStubGenerator.stub] takes them.
     *
     * ⭐ A javac refusal of an override's RETURN type is answered, not reported: kotlinc compiles such an override
     * with a bridge returning the overridden member's erasure, which a source stub cannot hold beside the declared
     * one (see [JavaStubGenerator.StubHints.returnType]). The refused methods are re-stubbed with the method's own
     * erasure -- or, where that is `Void` (Kotlin's `Nothing`) or a primitive, with the erasure javac asked for --
     * and the set is compiled again. Every other error still fails the compilation. Exposed: 45 refusals of this
     * one kind (`List<List<X>> arguments()` overriding `Iterable<Iterable<X>>`, `Nothing` for `List<String>`).
     */
    private fun compileStubs(types: List<TypeInfo>, hints: JavaStubGenerator.StubHints, libraryRoots: List<Path>,
                             javaSourceDirs: List<Path> = emptyList()) {
        val bridged = HashMap<MethodInfo, String>()
        // per type, the forwarders kotlinc adds for a DefaultImpls interface: "name/arity" -> [return, parameters]
        val forwarders = HashMap<TypeInfo, LinkedHashMap<String, Array<String>>>()
        val withBridges = object : JavaStubGenerator.StubHints by hints {
            override fun returnType(method: MethodInfo): String? = bridged[method] ?: hints.returnType(method)
            override fun extraMethods(type: TypeInfo): List<String> = hints.extraMethods(type) +
                    forwarders[type].orEmpty().map { (key, rp) ->
                        "public ${rp[2]}${rp[0]} ${key.substringBefore('/')}(${rp[1]}) { throw new RuntimeException(\"stub\"); }"
                    }
        }
        // the interfaces kotlinc forwards for are compiled library ones: their exact generic signatures are read here
        val libraries = java.net.URLClassLoader(libraryRoots.map { it.toUri().toURL() }.toTypedArray(),
            ClassLoader.getPlatformClassLoader())
        try {
            compileStubRounds(types, withBridges, bridged, forwarders, libraries, libraryRoots, javaSourceDirs)
        } finally {
            libraries.close()
        }
    }

    private fun compileStubRounds(types: List<TypeInfo>, withBridges: JavaStubGenerator.StubHints,
                                  bridged: HashMap<MethodInfo, String>,
                                  forwarders: HashMap<TypeInfo, LinkedHashMap<String, Array<String>>>,
                                  libraries: ClassLoader, libraryRoots: List<Path>, javaSourceDirs: List<Path>) {
        val nested = types.flatMap { it.recursiveSubTypeStream().toList() }.associateBy { it.fullyQualifiedName() }
        repeat(STUB_ROUNDS) {
            val stubsByFqn = types.associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it, withBridges) }
            val diagnostics = DiagnosticCollector<JavaFileObject>()
            if (compileStubsOnce(stubsByFqn, libraryRoots, javaSourceDirs, diagnostics)) return
            val errors = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
            var progress = false
            errors.filter { it.code == "compiler.err.override.incompatible.ret" }.forEach { d ->
                val methods = refusedMethods(d, stubsByFqn, types)
                methods.filter { it !in bridged }.forEach { bridged[it] = bridgeReturn(d, it); progress = true }
                if (methods.isEmpty()) forwarderOnLine(d, stubsByFqn, nested, forwarders)?.let { rp ->
                    REQUIRED_RETURN.find(d.getMessage(java.util.Locale.ROOT))?.groupValues?.get(1)?.trim()
                        ?.let(::erase)?.takeIf { it != rp[0] }?.let { rp[0] = it; progress = true }
                }
            }
            // a missing implementation only counts once no return was refused: a refused override does not implement
            if (!progress) errors.filter { it.code == "compiler.err.does.not.override.abstract" }.forEach { d ->
                val m = MISSING_ABSTRACT.find(d.getMessage(java.util.Locale.ROOT)) ?: return@forEach
                val type = nested[m.groupValues[1]] ?: return@forEach
                val parameters = splitTopLevel(m.groupValues[3]).map(::erase)
                val key = m.groupValues[2] + "/" + parameters.size
                val map = forwarders.getOrPut(type) { LinkedHashMap() }
                if (key !in map) {
                    map[key] = librarySignature(libraries, m.groupValues[4], m.groupValues[2], parameters.size)
                        ?: arrayOf("java.lang.Object", parameters.withIndex().joinToString(", ") { (i, t) -> "$t p$i" }, "")
                    progress = true
                }
            }
            if (!progress || errors.any { it.code !in BRIDGEABLE }) failStubs(stubsByFqn, diagnostics)
        }
        val stubsByFqn = types.associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it, withBridges) }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        if (!compileStubsOnce(stubsByFqn, libraryRoots, javaSourceDirs, diagnostics)) failStubs(stubsByFqn, diagnostics)
    }

    /** The methods a diagnostic's stub line declares: its type's (or a nested type's) methods of that name and arity. */
    private fun refusedMethods(d: Diagnostic<out JavaFileObject>, stubsByFqn: Map<String, String>,
                               types: List<TypeInfo>): List<MethodInfo> {
        val fqn = d.source?.toUri()?.schemeSpecificPart?.removePrefix("///")?.removeSuffix(".java")?.replace('/', '.')
            ?: return emptyList()
        val line = stubsByFqn[fqn]?.lines()?.getOrNull(d.lineNumber.toInt() - 1) ?: return emptyList()
        val match = STUB_METHOD.find(line) ?: return emptyList()
        val name = match.groupValues[1]
        val arity = Regex(" p\\d+\\b").findAll(match.groupValues[2]).count()
        val type = types.firstOrNull { it.fullyQualifiedName() == fqn } ?: return emptyList()
        return type.recursiveSubTypeStream().toList().flatMap { it.methods() }
            .filter { it.name() == name && it.parameters().size == arity }
    }

    /** The forwarder [d]'s line declares, when it is one we added rather than a CST method. */
    private fun forwarderOnLine(d: Diagnostic<out JavaFileObject>, stubsByFqn: Map<String, String>,
                                nested: Map<String, TypeInfo>,
                                forwarders: Map<TypeInfo, LinkedHashMap<String, Array<String>>>): Array<String>? {
        val fqn = d.source?.toUri()?.schemeSpecificPart?.removePrefix("///")?.removeSuffix(".java")?.replace('/', '.')
            ?: return null
        val line = stubsByFqn[fqn]?.lines()?.getOrNull(d.lineNumber.toInt() - 1) ?: return null
        val match = STUB_METHOD.find(line) ?: return null
        val key = match.groupValues[1] + "/" + Regex(" p\\d+\\b").findAll(match.groupValues[2]).count()
        return forwarders.entries.firstOrNull { (t, map) ->
            (t.fullyQualifiedName() == fqn || t.fullyQualifiedName().startsWith("$fqn.")) && key in map
        }?.value?.get(key)
    }

    /**
     * [interfaceName]'s method [name] of [arity] as Java source -- [return, parameters, type parameters] -- read off
     * the library class: `<E extends kotlin.coroutines.CoroutineContext.Element> E get(Key<E>)` cannot be written from
     * javac's message, which prints `<E>get(Key<E>)` without the bound. Null when the class is not a library's.
     */
    private fun librarySignature(libraries: ClassLoader, interfaceName: String, name: String, arity: Int): Array<String>? {
        val parts = interfaceName.split('.')
        val type = (parts.size - 1 downTo 1).firstNotNullOfOrNull { i ->
            runCatching {
                Class.forName(parts.take(i).joinToString(".") + "$" + parts.drop(i).joinToString("$"), false, libraries)
            }.getOrNull()
        } ?: runCatching { Class.forName(interfaceName, false, libraries) }.getOrNull() ?: return null
        val method = type.methods.firstOrNull { it.name == name && it.parameterCount == arity } ?: return null
        fun java(t: java.lang.reflect.Type) = t.typeName.replace('$', '.')
        val typeParameters = method.typeParameters.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", "> ") { tv ->
            tv.name + tv.bounds.filter { it != Any::class.java }.takeIf { it.isNotEmpty() }
                ?.joinToString(" & ", " extends ") { java(it) }.orEmpty()
        } ?: ""
        return arrayOf(java(method.genericReturnType),
            method.genericParameterTypes.withIndex().joinToString(", ") { (i, t) -> "${java(t)} p$i" }, typeParameters)
    }

    /** A Java type as javac prints it, erased: no type arguments; a type variable (no package) is Object. */
    private fun erase(type: String): String {
        var depth = 0
        val sb = StringBuilder()
        type.trim().removePrefix("? extends ").forEach { c ->
            when (c) {
                '<' -> depth++
                '>' -> depth--
                else -> if (depth == 0) sb.append(c)
            }
        }
        val erased = sb.toString().replace("...", "[]")
        val base = erased.substringBefore('[')
        return if ('.' in base || base in PRIMITIVES) erased else "java.lang.Object" + erased.substring(base.length)
    }

    private fun splitTopLevel(parameters: String): List<String> {
        if (parameters.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        parameters.forEachIndexed { i, c ->
            when (c) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) { parts += parameters.substring(start, i); start = i + 1 }
            }
        }
        parts += parameters.substring(start)
        return parts.map { it.trim() }
    }

    /** The method's own erasure; where that cannot override anything (`Void`, a primitive), the erasure javac asked for. */
    private fun bridgeReturn(d: Diagnostic<out JavaFileObject>, m: MethodInfo): String {
        val own = m.returnType()
        val ownErasure = (own.typeParameter()?.let { "java.lang.Object" } ?: own.typeInfo()?.fullyQualifiedName()
            ?: "java.lang.Object") + "[]".repeat(own.arrays())
        if (own.arrays() > 0 || !(own.isPrimitiveExcludingVoid || ownErasure == "java.lang.Void")) return ownErasure
        val required = REQUIRED_RETURN.find(d.getMessage(java.util.Locale.ROOT))?.groupValues?.get(1)?.trim()
        return required?.let(::erase) ?: ownErasure
    }

    private fun failStubs(stubsByFqn: Map<String, String>, diagnostics: DiagnosticCollector<JavaFileObject>): Nothing {
        diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }.forEach { System.err.println(it) }
        // the diagnostics name `/p/K.java` lines of sources that exist only in memory: keep them where the
        // lines can be read
        val kept = Files.createTempDirectory("mixed-stub-sources")
        stubsByFqn.forEach { (fqn, code) ->
            val file = kept.resolve(fqn.replace('.', '/') + ".java")
            Files.createDirectories(file.parent)
            Files.writeString(file, code)
        }
        error("stub compilation failed; the stub sources are in $kept")
    }

    private fun compileStubsOnce(stubsByFqn: Map<String, String>, libraryRoots: List<Path>, javaSourceDirs: List<Path>,
                                 diagnostics: DiagnosticCollector<JavaFileObject>): Boolean {
        val compiler = ToolProvider.getSystemJavaCompiler()
        compiler.getStandardFileManager(null, null, null).use { fm ->
            fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(stubDir.toFile()))
            // the stub directory too: stubs made for an earlier source set are what these may name
            fm.setLocation(StandardLocation.CLASS_PATH, libraryRoots.map { it.toFile() } + stubDir.toFile())
            if (javaSourceDirs.isNotEmpty()) fm.setLocation(StandardLocation.SOURCE_PATH, javaSourceDirs.map { it.toFile() })
            val files = stubsByFqn.map { (fqn, code) -> inMemorySource(fqn, code) }
            val options = if (javaSourceDirs.isEmpty()) null else listOf("-implicit:none", "-proc:none")
            return compiler.getTask(null, UnnamedModule(fm), diagnostics, options, null, files).call()
        }
    }

    private fun inMemorySource(fqn: String, code: String): JavaFileObject =
        object : SimpleJavaFileObject(
            URI.create("string:///" + fqn.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
        }
}

/**
 * The stub compiler's file manager with every `module-info` on the source path hidden. A source directory holding one
 * puts javac in module mode, and it then asks whether each in-memory stub (`string:///k/K.java`) lies in that module's
 * source location, which the standard file manager answers with an IllegalArgumentException: "An exception has
 * occurred in the compiler", on the jfocus workspace, whose sets are named modules. The Java parse compiles in the
 * unnamed module too (`ignoreModule`).
 */
private class UnnamedModule(fm: StandardJavaFileManager) : ForwardingJavaFileManager<StandardJavaFileManager>(fm) {
    override fun getJavaFileForInput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind)
            : JavaFileObject? =
        if (location == StandardLocation.SOURCE_PATH && className == "module-info") null
        else super.getJavaFileForInput(location, className, kind)

    override fun list(location: JavaFileManager.Location, packageName: String, kinds: Set<JavaFileObject.Kind>,
                      recurse: Boolean): Iterable<JavaFileObject> {
        val listed = super.list(location, packageName, kinds, recurse)
        return if (location != StandardLocation.SOURCE_PATH) listed
        else listed.filter { !it.isNameCompatible("module-info", JavaFileObject.Kind.SOURCE) }
    }
}

/**
 * How often [MixedProjectInspector] re-stubs before giving up. javac names ONE missing implementation per class per
 * round (`minusKey`, then `get`, then `fold`), and a forwarder's `Object` return may be refused once more; a round
 * without progress fails at once.
 */
private const val STUB_ROUNDS = 12

/** The refusal it answers, and the consequence that disappears with it (the refused override did not count). */
private val BRIDGEABLE = setOf("compiler.err.override.incompatible.ret", "compiler.err.does.not.override.abstract")

/** A stub method line: `public [static] [abstract] [default] [<T>] R name(T p0, U p1) ...`. */
private val STUB_METHOD = Regex("""([A-Za-z_$][\w$]*)\(([^)]*)\)\s*(\{|;|throws)""")

/** javac's "return type X is not compatible with Y", in the ROOT locale. */
private val REQUIRED_RETURN = Regex("""is not compatible with ([^\n]+)""")

/** javac's "C is not abstract and does not override abstract method [<R>]m(P) in I", in the ROOT locale. */
private val MISSING_ABSTRACT = Regex("""^(\S+) is not abstract and does not override abstract method (?:<[^(]*>)?([\w$]+)\((.*)\) in (\S+)""")

// `void` too: a `Nothing` override of a `Unit` member is bridged by `void` (javap on Exposed's BatchUpdateStatement)
private val PRIMITIVES = setOf("int", "long", "short", "byte", "char", "boolean", "float", "double", "void")
