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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.kotlin.api.KotlinSession
import io.codelaser.maddi.kotlin.api.KotlinProjectScanner
import io.codelaser.maddi.kotlin.api.ConstructorDelegation
import io.codelaser.maddi.kotlin.api.KotlinParseObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Parses a whole Kotlin project — several [SourceSet]s with real source **directories** on disk, over a real
 * library **classpath** — in ONE standalone K2 session, then drives [KotlinScan] per source set. This is the
 * multi-module analogue of [KotlinScan.parse] (which builds a single-module session from in-memory files over
 * the process classpath); the driver ([io.codelaser.maddi.inspection.kotlin.KotlinInspector]) lives one module
 * up and translates an `InputConfiguration` into the plain inputs here (it cannot see the K2 Analysis API).
 *
 * The session has one `KtSourceModule` per source set, each wired to the JDK, the library classpath, and its
 * upstream source-set modules. Conversion runs in the given (dependency) order and shares one [InfoByFqn], so a
 * type in a dependent set resolves to the very same [TypeInfo] produced for its upstream set.
 */
class KotlinProjectScan(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    // Mixed-language: the Java front-end's CompiledTypesManager, so `java.*`/classpath types resolve to ONE
    // shared bytecode-authoritative TypeInfo. Null = standalone Kotlin (K2 loads library types itself).
    private val compiledTypesManager: CompiledTypesManager? = null,
) : KotlinProjectScanner {

    /**
     * @param orderedSourceSets source sets in dependency order (a set's dependencies appear before it). Each
     *        must carry its `sourceDirectories()` and (for cross-set links) its `dependencies()`.
     * @param libraryRoots      jars/class-dirs for the library classpath (kotlin-stdlib, third-party, …).
     * @param jdkHome           the JDK to resolve `java.*`/`kotlin`-mapped types against.
     * @param javaSourceRoots   directories of Java **source** the Kotlin sets reference (mixed-language,
     *        Kotlin→Java). K2 resolves the symbols from these (its `addSourceRoot` includes `.java`); the CST
     *        `TypeInfo` still comes from the shared registry/CTM (those Java sets must be parsed first).
     */
    @JvmOverloads
    override fun parse(orderedSourceSets: List<SourceSet>, libraryRoots: List<Path>, jdkHome: Path,
              javaSourceRoots: List<Path>,
              observers: List<KotlinParseObserver>): Map<SourceSet, List<TypeInfo>> =
        open(orderedSourceSets, libraryRoots, jdkHome, javaSourceRoots).use { session ->
            orderedSourceSets.forEach { session.convert(it) }
            session.observe(observers)
            // ⚠ always logged, including the 0: the absence of a warning is not evidence of absence, and
            // this number is not visible to the placeholder census (the CST is well formed, just wrong)
            org.slf4j.LoggerFactory.getLogger(KotlinProjectScan::class.java)
                .info("elvis lowerings re-evaluating their left operand: {} (see KotlinBodyConverter#controlFlowElvisLowering)",
                        session.elvisReEvaluations)
            session.result
        }

    /**
     * The session [parse] runs, left open for a driver that interleaves another front end between a source set's
     * declarations and its bodies (see [Session.declare]). Close it when done: see [parse] for why.
     */
    @JvmOverloads
    override fun open(orderedSourceSets: List<SourceSet>, libraryRoots: List<Path>, jdkHome: Path,
             javaSourceRoots: List<Path>): Session {
        // The session's project lives until this disposable is disposed: IntelliJ's Disposer tree is static, so an
        // undisposed session -- every PSI file, every FIR cache -- stays reachable for the life of the JVM. A host
        // that parses more than once (the refactoring server re-parses after every write) kept one full detekt
        // session per parse: 1,413 live KtFiles more each time. Nothing reads PSI after the session is closed.
        val disposable = Disposer.newDisposable("maddi KotlinProjectScan")
        try {
            return Session(disposable, orderedSourceSets, libraryRoots, jdkHome, javaSourceRoots)
        } catch (t: Throwable) {
            Disposer.dispose(disposable)
            throw t
        }
    }

    inner class Session internal constructor(private val disposable: Disposable, orderedSourceSets: List<SourceSet>,
                                             libraryRoots: List<Path>, jdkHome: Path,
                                             javaSourceRoots: List<Path>) : KotlinSession {
        // a source set is one K2 module, or -- a Kotlin-multiplatform target's fragments -- a dependsOn chain of them
        private val modulesBySourceSet = LinkedHashMap<SourceSet, List<KaSourceModule>>()
        private val session = buildStandaloneAnalysisAPISession(disposable) {
            val jvm = JvmPlatforms.defaultJvmPlatform
            buildKtModuleProvider {
                platform = jvm
                val jdk = buildKtSdkModule {
                    platform = jvm
                    addBinaryRootsFromJdkHome(jdkHome, isJre = false)
                    libraryName = "jdk"
                }
                addModule(jdk)
                val library = buildKtLibraryModule {
                    platform = jvm
                    addBinaryRoots(libraryRoots)
                    libraryName = "classpath"
                }
                addModule(library)
                // a single module holding the referenced Java source, so every Kotlin module can resolve those
                // types (without laying the .java files into each Kotlin module's own roots)
                val javaExisting = javaSourceRoots.filter { Files.exists(it) }
                val javaModule = if (javaExisting.isEmpty()) null else buildKtSourceModule {
                    moduleName = "java-sources"
                    platform = jvm
                    javaExisting.forEach { addSourceRoot(it) }
                    addRegularDependency(jdk)
                    addRegularDependency(library)
                }
                javaModule?.let { addModule(it) }
                // dependency order => a dependent finds its already-built upstream module in the map
                orderedSourceSets.forEach { ss ->
                    val directories = ss.sourceDirectories().filter { Files.exists(it) }
                    val fragments = multiplatformFragments(directories)
                    val modules = ArrayList<KaSourceModule>()
                    (fragments?.map { listOf(it) } ?: listOf(directories)).forEachIndexed { i, roots ->
                        val module = buildKtSourceModule {
                            moduleName = if (fragments == null) ss.name() else "${ss.name()}#${roots.single().parent.fileName}"
                            platform = jvm
                            if (fragments != null) languageVersionSettings = MULTIPLATFORM
                            roots.forEach { addSourceRoot(it) }
                            addRegularDependency(jdk)
                            addRegularDependency(library)
                            javaModule?.let { addRegularDependency(it) }
                            // and a FRIEND: kotlinc compiles a test set as its main's friend, which sees `internal`
                            // (javalin's tests call `internal object CorsUtils`, 21 unresolved refs). Friendship is
                            // granted to every upstream set: the code compiled, so an `internal` it names was
                            // visible to it -- no reference can resolve here that kotlinc refused.
                            ss.dependencies().forEach { dep -> modulesBySourceSet[dep]?.forEach {
                                addRegularDependency(it)
                                addFriendDependency(it)
                            } }
                            // each fragment refines the one before it: an `expect` is matched to its `actual`
                            modules.lastOrNull()?.let { addDependsOnDependency(it) }
                        }
                        addModule(module)
                        modules += module
                    }
                    modulesBySourceSet[ss] = modules
                }
            }
        }.also { it.registerKDocResolution() }

        // one registry for the project: a set's members name the members of the sets upstream of it
        private val references = KotlinReferenceRegistry()
        private val scans = LinkedHashMap<SourceSet, KotlinScan>()
        private val sourceSetOf = LinkedHashMap<KtFile, String>()
        private val declaredTypes = LinkedHashMap<SourceSet, List<TypeInfo>>()

        /** The converted types per source set, in the order the sets completed. */
        override val result = LinkedHashMap<SourceSet, List<TypeInfo>>()

        private fun ktFiles(ss: SourceSet): List<KtFile> {
            val modules = checkNotNull(modulesBySourceSet[ss]) { "source set ${ss.name()} is not in this session" }
            return modules.flatMap { session.modulesWithFiles[it] ?: emptyList() }.filterIsInstance<KtFile>()
        }

        /**
         * Declarations of [ss] (types, hierarchy, signatures), no bodies: see [KotlinScan.declare]. Dependency order.
         * [javaSourceTypes]: see [KotlinTypeMapper.javaSourceTypes], for a set whose Java sources are not parsed yet.
         */
        override fun declare(ss: SourceSet, javaSourceTypes: ((String) -> TypeInfo?)?): List<TypeInfo> {
            check(ss !in scans) { "source set ${ss.name()} declared twice" }
            val scan = KotlinScan(runtime, ss, infoByFqn, compiledTypesManager).also {
                it.references = references
                it.javaSourceTypes = javaSourceTypes
            }
            scans[ss] = scan
            val files = ktFiles(ss)
            files.forEach { sourceSetOf[it] = ss.name() }
            return scan.declare(files).also { declaredTypes[ss] = it }
        }

        /** The bodies and commits of [ss], after [declare]: see [KotlinScan.complete]. */
        override fun complete(ss: SourceSet): List<TypeInfo> {
            val scan = checkNotNull(scans[ss]) { "source set ${ss.name()} completed before it was declared" }
            scan.javaSourceTypes = null // the Java types are registered by now; nothing more is made on request
            return scan.complete().also { result[ss] = it }
        }

        override fun isDeclared(ss: SourceSet): Boolean = ss in scans

        /** Every type declared so far, in every set: the completed ones, and those between declare and complete. */
        override fun declaredTypes(): List<TypeInfo> = declaredTypes.values.flatten()

        override fun isCompleted(ss: SourceSet): Boolean = ss in result

        override fun convert(ss: SourceSet): List<TypeInfo> {
            declare(ss, null)
            return complete(ss)
        }


        /** See [KotlinScan.delegationOf]; [constructor] may belong to any set of this session. */
        override fun delegationOf(constructor: io.codelaser.maddi.cst.api.info.MethodInfo): ConstructorDelegation? =
            scans.values.firstNotNullOfOrNull { it.delegationOf(constructor) }


        /** See [KotlinScan.hasOrAwaitsBody]; [method] may belong to any set of this session. */
        override fun hasOrAwaitsBody(method: io.codelaser.maddi.cst.api.info.MethodInfo): Boolean =
            scans.values.any { it.hasOrAwaitsBody(method) }

        /** After every set is completed, so a reference into an upstream set finds its CST; the session is still alive. */
        /**
         * ⚠ How many elvis lowerings re-evaluated their left operand — see
         * [KotlinBodyConverter.controlFlowElvisLowering]. Not a placeholder, so the census cannot see it:
         * the CST is well formed and says something the source does not, which is the one failure mode worse
         * than a hole. Reported so it is a number rather than a worry.
         */
        val elvisReEvaluations: Int get() = scans.values.sumOf { it.elvisReEvaluations }

        override fun observe(observers: List<KotlinParseObserver>) {
            val allTypes = result.values.flatten()
            // only an observer built by THIS front end can read PSI; a host-side marker is all the boundary carries
            observers.filterIsInstance<K2ParseObserver>()
                .forEach { it.observe(runtime, sourceSetOf.keys.toList(), allTypes) { f -> sourceSetOf.getValue(f) } }
        }

        override fun close() = Disposer.dispose(disposable)
    }
}

/**
 * A Kotlin-multiplatform target's source directories, when [directories] are its fragments: each `src/<fragment>/kotlin`,
 * `commonMain` (or `commonTest`) first, listed in refinement order -- `commonMain`, `nonAndroidMain`, …, `jvmMain` for
 * coil-core's JVM target. Each becomes its own K2 module depending on the one before, so an `expect` in one is matched
 * to its `actual` in a later one, as kotlinc matches them. Flattened into one module, the two are rival declarations
 * of one name and K2 resolves neither (51 of coil's 102 placeholders). Null for anything else: splitting an ordinary
 * multi-directory set would hide the later directories from the earlier ones.
 */
internal fun multiplatformFragments(directories: List<Path>): List<Path>? {
    if (directories.size < 2) return null
    val fragments = directories.map { dir ->
        dir.parent?.fileName?.toString()?.takeIf { dir.fileName?.toString() == "kotlin" && dir.parent?.parent?.fileName?.toString() == "src" }
            ?: return null
    }
    val common = fragments.first()
    if (common != "commonMain" && common != "commonTest") return null
    val suffix = common.removePrefix("common")
    return directories.takeIf { fragments.all { it.endsWith(suffix) } }
}

private val MULTIPLATFORM = org.jetbrains.kotlin.config.LanguageVersionSettingsImpl(
    org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE, org.jetbrains.kotlin.config.ApiVersion.LATEST,
    specificFeatures = mapOf(org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
            to org.jetbrains.kotlin.config.LanguageFeature.State.ENABLED))
