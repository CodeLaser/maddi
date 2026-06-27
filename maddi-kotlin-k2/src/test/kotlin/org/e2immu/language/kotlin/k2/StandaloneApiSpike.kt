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

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * M0/M1 spike: prove the K2 Standalone Analysis API builds a session over an in-memory Kotlin file
 * and yields resolved symbols (the analogue of javac's Trees/Types for -java-openjdk).
 */
class StandaloneApiSpike {

    @Test
    fun resolvesSimpleClass() {
        // 1. lay the source down in a temp source root (standalone resolves from roots)
        val srcRoot = Files.createTempDirectory("k2-spike-src")
        val ktPath = srcRoot.resolve("Foo.kt")
        Files.writeString(ktPath, "class Foo { fun bar(): Int = 1 }\n")

        // 2. build a standalone Analysis API session for one JVM source module
        val session = buildStandaloneAnalysisAPISession {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                addModule(
                    buildKtSourceModule {
                        moduleName = "main"
                        platform = JvmPlatforms.defaultJvmPlatform
                        addSourceRoot(srcRoot)
                    }
                )
            }
        }

        // 3. recover the KtFile that was loaded
        val ktFile: KtFile = session.modulesWithFiles.values
            .flatten()
            .filterIsInstance<KtFile>()
            .single { it.name == "Foo.kt" }

        // 4. resolve symbols and assert the shape
        analyze(ktFile) {
            val foo = ktFile.declarations.single()
            val fooSymbol = foo.symbol as KaClassSymbol
            assertEquals("Foo", fooSymbol.name?.asString())

            val bar = fooSymbol.memberScope.declarations
                .filterIsInstance<KaNamedFunctionSymbol>()
                .single { it.name.asString() == "bar" }
            assertNotNull(bar)
            // resolved return type renders as "kotlin/Int" (ClassId form)
            assertEquals("kotlin/Int", bar.returnType.toString().substringBefore('?'))
        }
    }
}
