/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.kotlin.k2

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
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * M0/M1 spike: prove the K2 Standalone Analysis API builds a session over an in-memory Kotlin file
 * and yields resolved symbols (the analogue of javac's Trees/Types for -java-openjdk).
 */
class StandaloneApiSpike {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root: each call still gets its own unique
     * directory, and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @field:TempDir
    lateinit var tempRoot: Path

    @Test
    fun resolvesSimpleClass() {
        // 1. lay the source down in a temp source root (standalone resolves from roots)
        val srcRoot = Files.createTempDirectory(tempRoot, "k2-spike-src")
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
