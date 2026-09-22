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
package io.codelaser.maddi.kotlin.api

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.api.type.ParameterizedType
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import java.nio.file.Path
import java.util.ServiceLoader

/**
 * <b>The Kotlin front end, as the host sees it.</b> Every type named here is loaded by the HOST's
 * classloader; the implementation and the 62 MB of compiler it needs are loaded inside a realm of their own
 * (docs/kotlin-classloader-isolation.md).
 *
 * <p>⛔ The invariant that makes it work: [Runtime], [TypeInfo], [SourceSet], [InfoByFqn] and
 * [CompiledTypesManager] must be loaded ONCE, by the host, and imported into the realm. The mixed
 * inspector's whole point is that a cross-language reference resolves to a <i>single</i> [TypeInfo]; two
 * copies of the CST classes and that silently stops being true, with no error anywhere.
 */
interface KotlinFrontEnd {

    /** One source set, sources given as text. The string-parse entry point. */
    fun sourceScan(runtime: Runtime, sourceSet: SourceSet, infoByFqn: InfoByFqn,
                   compiledTypesManager: CompiledTypesManager?): KotlinSourceScan

    /**
     * A whole project: one module per source set, wired to the JDK, the library classpath and its upstream
     * sets. [compiledTypesManager] is the Java front end's, so `java.*` and classpath types resolve to one
     * shared bytecode-authoritative [TypeInfo]; null means standalone Kotlin.
     */
    fun projectScan(runtime: Runtime, infoByFqn: InfoByFqn,
                    compiledTypesManager: CompiledTypesManager?): KotlinProjectScanner

    /** Where every project declaration is referenced — the oracle a rename census compares against. */
    fun referenceIndex(): KotlinReferenceIndex

    /** How much of what K2 resolves the CST actually records. An instrument; [samplesPerCell] tunes its report. */
    fun referenceRecall(samplesPerCell: Int): KotlinReferenceRecall

    companion object {
        /**
         * The implementation, by [ServiceLoader] against [loader]. ⭐ The loader argument is the whole point:
         * pass the realm's and the front end is found inside it, with the compiler jars that are not on the
         * host's classpath. The default finds it on the ordinary classpath.
         */
        @JvmStatic
        @JvmOverloads
        fun load(loader: ClassLoader = KotlinFrontEnd::class.java.classLoader): KotlinFrontEnd =
            ServiceLoader.load(KotlinFrontEnd::class.java, loader).firstOrNull()
                ?: throw IllegalStateException(
                    "no KotlinFrontEnd implementation is visible to . maddi-kotlin-k2 provides one;"
                    + " a host that isolates it must hand this method the realm's classloader.")
    }
}

/**
 * The one place the front end is resolved, so a host that isolates it changes one line and not every call
 * site. ⚠ Deliberately an explicit [install] rather than configuration-by-magic: the realm needs a jar list
 * that only the host knows, and a front end that quietly fell back to the classpath would reintroduce the
 * leak this boundary exists to close — silently, which is the failure mode of the original defect.
 */
object KotlinFrontEnds {
    @Volatile
    private var instance: KotlinFrontEnd? = null

    /** Use [frontEnd] from here on. A host builds one over its realm; a test may supply a fake. */
    @JvmStatic
    fun install(frontEnd: KotlinFrontEnd) {
        instance = frontEnd
    }

    /** Whether a host has installed one. */
    @JvmStatic
    fun isInstalled(): Boolean = instance != null

    /** The installed front end, or the one on the ordinary classpath if the host installed none. */
    @JvmStatic
    fun get(): KotlinFrontEnd =
        instance ?: synchronized(this) { instance ?: KotlinFrontEnd.load().also { instance = it } }
}

/** One source set's worth of Kotlin, from text. */
interface KotlinSourceScan {
    fun parse(filesByName: Map<String, String>): List<TypeInfo>
    fun parse(filesByName: Map<String, String>, javaFilesByName: Map<String, String>): List<TypeInfo>
}

/** A whole project's Kotlin. [parse] is [open] plus convert-everything-and-close. */
interface KotlinProjectScanner {
    fun parse(orderedSourceSets: List<SourceSet>, libraryRoots: List<Path>, jdkHome: Path,
              javaSourceRoots: List<Path>, observers: List<KotlinParseObserver>): Map<SourceSet, List<TypeInfo>>

    /**
     * The session [parse] runs, left open for a driver that interleaves another front end between a source
     * set's declarations and its bodies. ⚠ Close it: IntelliJ's `Disposer` tree is static, so an undisposed
     * session keeps every PSI file and FIR cache reachable for the life of the JVM.
     */
    fun open(orderedSourceSets: List<SourceSet>, libraryRoots: List<Path>, jdkHome: Path,
             javaSourceRoots: List<Path>): KotlinSession
}

/** A live K2 session. Declarations first, bodies later, so a Java front end can run in between. */
interface KotlinSession : AutoCloseable {
    fun declare(ss: SourceSet, javaSourceTypes: ((String) -> TypeInfo?)?): List<TypeInfo>
    fun complete(ss: SourceSet): List<TypeInfo>
    fun convert(ss: SourceSet): List<TypeInfo>
    fun isDeclared(ss: SourceSet): Boolean
    fun isCompleted(ss: SourceSet): Boolean
    fun declaredTypes(): List<TypeInfo>
    fun delegationOf(constructor: MethodInfo): ConstructorDelegation?
    fun hasOrAwaitsBody(method: MethodInfo): Boolean
    fun observe(observers: List<KotlinParseObserver>)
    val result: Map<SourceSet, List<TypeInfo>>
}

/**
 * What a constructor delegates to (`: super(...)` / `: this(...)`), which a Java stub must reproduce: a stub
 * without it calls the parent's no-argument constructor, and plenty of parents have none. A parameter typed by
 * a type parameter is null — its substitution is the use site's. [thrown] carries a Java or library parent
 * constructor's checked exceptions, which kotlinc never has to declare and javac does.
 */
class ConstructorDelegation(val isSuper: Boolean, val parameterTypes: List<ParameterizedType?>,
                            val thrown: List<ParameterizedType>)

/**
 * <b>Reference recall: how much of what K2 resolves the CST actually records.</b> An instrument, not a gate.
 *
 * ⚠ Deliberately narrow: the per-reference rows are classified by vocabulary that belongs to the front end
 * (site, region, target, tier), so the contract exposes the counts and the rendered report — everything its
 * one consumer reads. Widen it when something needs more, not in advance.
 */
interface KotlinReferenceRecall : KotlinParseObserver {
    val libraryReferences: Int
    val unresolvedReferences: Int
    val failedReferences: Int
    val filesWithoutCst: Int

    /** The number of project references measured. */
    fun rowCount(): Int

    fun report(): String
}

/**
 * Something that reads a parse through the K2 session that produced it, while PSI resolution is still alive.
 * ⚠ A marker here on purpose: the callback takes the compiler's own PSI types, so it cannot cross the
 * boundary. A host constructs an observer through [KotlinFrontEnd] and passes it back; it never calls it.
 */
interface KotlinParseObserver
