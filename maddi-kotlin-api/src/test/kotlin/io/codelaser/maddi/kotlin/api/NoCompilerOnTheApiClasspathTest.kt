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
package io.codelaser.maddi.kotlin.api

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * ⛔ <b>The rule this module exists to keep.</b> A dependency added here lands on every consumer's runtime
 * classpath — that is how the defect in G46 happened: {@code maddi-kotlin-k2} declared
 * {@code implementation("org.jetbrains.kotlin:kotlin-compiler")}, which is invisible on a consumer's COMPILE
 * path and fully present on their RUNTIME path. The 62 MB fat jar then shadowed 174 of ANTLR's classes, 787
 * of guava's and 115 of JNA's, and a downstream conformance oracle died on a {@code NoSuchMethodError} from a
 * ProGuard-minimised {@code CharStreams} with one method left.
 *
 * <p>So: this module carries the front end's contract and nothing that can parse Kotlin. The check is not
 * "is the dependency declared" — it is "can the class be loaded", which is what a consumer experiences.
 */
class NoCompilerOnTheApiClasspathTest {

    private fun loadable(fqn: String): Class<*>? =
        try {
            Class.forName(fqn, false, NoCompilerOnTheApiClasspathTest::class.java.classLoader)
        } catch (_: ClassNotFoundException) {
            null
        }

    @Test
    fun neitherTheCompilerNorTheIntellijPlatformIsReachable() {
        for (fqn in listOf(
            "org.jetbrains.kotlin.psi.KtFile",                        // the compiler's PSI
            "org.jetbrains.kotlin.analysis.api.KaSession",            // the Analysis API
            "com.intellij.openapi.project.Project",                   // the bundled IntelliJ platform
            "org.antlr.v4.runtime.CharStreams",                       // the class that actually broke (G46)
            "com.google.common.collect.ImmutableList",                // shadowed too, member-for-member by luck
            "com.sun.jna.Native",                                     // and this
        )) {
            assertNull(loadable(fqn), "$fqn must not be reachable from maddi-kotlin-api: a consumer inherits "
                                      + "this module's runtime classpath, and a fat compiler jar on it shadows "
                                      + "every library it bundles")
        }
    }

    /** ⭐ The negative control: the CST types that DO cross the boundary must of course be here. */
    @Test
    fun theSharedCstTypesAreReachable() {
        for (fqn in listOf(
            "io.codelaser.maddi.cst.api.info.TypeInfo",
            "io.codelaser.maddi.cst.api.runtime.Runtime",
            "io.codelaser.maddi.inspection.resource.InfoByFqn",
        )) {
            assertNotNull(loadable(fqn), "$fqn is part of the contract and must be loadable")
        }
    }
}
