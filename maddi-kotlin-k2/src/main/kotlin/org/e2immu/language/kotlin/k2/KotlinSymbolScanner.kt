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

package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.resource.InfoByFqn
import java.net.URI

/**
 * The internal type loader for library/external types — the Kotlin analogue of openjdk's
 * `ClassSymbolScanner`. Where javac completes a `.class` file and `ClassSymbolScanner` converts the
 * resulting symbol, the K2 Analysis API has already resolved the JDK and stdlib for us; this loader's
 * job is to mint a CST [TypeInfo] for a referenced library type, keyed by its **JVM** fully-qualified
 * name (Kotlin's mapped types are translated to their JVM identity in [KotlinScan]).
 *
 * Like `ClassSymbolScanner`, it deposits into the shared [InfoByFqn] registry (so there is one
 * `TypeInfo` per type across language front-ends, per the Info identity invariant), and reuses the
 * runtime's predefined `java.lang` types so `Object`/`String`/boxed primitives are the very same
 * instances the Java parsers use. Shells carry the correct identity (FQN) and arity (formal type
 * parameters) — enough for generics — but not yet a full hierarchy (parent is `Object`, no interfaces);
 * deeper loading is a later increment.
 */
class KotlinSymbolScanner(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    private val librarySourceSet: SourceSet,
) {
    init {
        // seed the predefined java.lang types into the shared registry
        runtime.predefinedObjects().forEach { infoByFqn.put(it.fullyQualifiedName(), it, librarySourceSet) }
    }

    /** Return the [TypeInfo] for a library type identified by its JVM FQN, creating a shell on first use. */
    fun getOrLoad(jvmFqn: String, arity: Int): TypeInfo {
        infoByFqn.getType(jvmFqn, librarySourceSet)?.let { return it }
        val shell = createShell(jvmFqn, arity)
        infoByFqn.put(jvmFqn, shell, librarySourceSet)
        return shell
    }

    private fun createShell(jvmFqn: String, arity: Int): TypeInfo {
        val packageName = jvmFqn.substringBeforeLast('.', missingDelimiterValue = "")
        val simpleName = jvmFqn.substringAfterLast('.')
        // carry the external-library source set (a stub's source set is null -> analyzer AssertionError)
        val compilationUnit = runtime.newCompilationUnitBuilder()
            .setPackageName(packageName)
            .setURI(URI.create("library:/" + packageName.replace('.', '/')))
            .setSourceSet(librarySourceSet)
            .build()
        val typeInfo = runtime.newTypeInfo(compilationUnit, simpleName)
        repeat(arity) { i ->
            val tp = runtime.newTypeParameter(i, "T$i", typeInfo)
            tp.builder().setTypeBounds(listOf()).commit()
            typeInfo.builder().addOrSetTypeParameter(tp)
        }
        typeInfo.builder()
            .setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .setAccess(runtime.accessPublic())
            .commit()
        return typeInfo
    }
}
