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
package io.codelaser.maddi.kotlin.realm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ⛔⛔ <b>THE REALM HAD THE SAME DEFECT IT WAS BUILT TO REMOVE, ONE LEVEL DOWN.</b>
 *
 * The Analysis API needs IntelliJ's PATCHED coroutines (`kotlinx.coroutines.internal.intellij.IntellijCoroutines`
 * exists only there). `maddi-kotlin-k2` asked for it with `runtimeOnly(…intellij-1)` and removed the upstream
 * copy that `kotlin-compiler` drags in with a `dependencySubstitution` in its own `configurations.all`. But a
 * substitution rule belongs to the project that declares it: the realm's `k2Runtime` is a DIFFERENT project's
 * configuration, so it received BOTH —
 *
 *     k2Runtime[6]  kotlinx-coroutines-core-jvm-1.8.0.jar            (upstream, via kotlin-compiler:2.4.0)
 *     k2Runtime[10] kotlinx-coroutines-core-jvm-1.10.2-intellij-1.jar (the patch)
 *
 * and the realm answers from the first: every coroutines class both carry came from UPSTREAM 1.8.0, and only
 * `IntellijCoroutines` from the patch. First-one-wins, inside the boundary built against first-one-wins.
 *
 * ⚠ And the shipped CLI disagreed with the tests. `K2Realm.jarsIn` SORTS a `lib-k2/` directory by name, and
 * `…-1.10.2-intellij-1.jar` sorts before `…-1.8.0.jar` — so the distribution loaded the patch while every test
 * over `-Dmaddi.k2.classpath` loaded upstream. The suite was verifying a realm nobody ships.
 *
 * The fix is an `exclude` on `kotlin-compiler`'s edge in `maddi-kotlin-k2`: unlike a substitution, an exclude
 * travels with the dependency, so every configuration that resolves the front end inherits it — this module's,
 * `maddi-run-kotlin`'s `lib-k2`, and each consumer's.
 */
class RealmCoroutinesTest {

    private val realmJars = ClasspathCensus.split(
        System.getProperty("maddi.k2.classpath")
            ?: error("the test JVM needs -Dmaddi.k2.classpath (see this module's build.gradle.kts)"))

    @Test
    fun theRealmCarriesExactlyOneCoroutines() {
        val providers = ClasspathCensus.providers(realmJars, "kotlinx/coroutines/Job.class")

        assertEquals(1, providers.size,
            "the realm must carry ONE kotlinx.coroutines; a second copy is dead code that answers first: "
            + providers.map { it.fileName })
    }

    @Test
    fun andItIsThePatchTheAnalysisApiNeeds() {
        val providers = ClasspathCensus.providers(realmJars, "kotlinx/coroutines/Job.class")
        val patch = ClasspathCensus.providers(
            realmJars, "kotlinx/coroutines/internal/intellij/IntellijCoroutines.class")

        assertEquals(1, patch.size, "IntelliJ's patched coroutines must be in the realm: $patch")
        assertEquals(patch.first(), providers.first(),
            "Job must be answered by the SAME jar as IntellijCoroutines, or the realm runs a mixed coroutines")
    }

    /** the census itself, on two jars it can reason about: the later entry's shared class is counted dead */
    @Test
    fun theCensusCountsWhatAnEarlierEntryAnswers(@TempDir dir: Path) {
        val first = jar(dir.resolve("a.jar"), "p/Shared.class", "p/OnlyA.class")
        val second = jar(dir.resolve("b.jar"), "p/Shared.class", "p/OnlyB.class", "module-info.class")

        val census = ClasspathCensus.census(listOf(first, second))

        assertEquals(1, census.size, "only the later entry loses anything")
        val row = census.first()
        assertEquals(second, row.entry())
        assertEquals(1, row.shadowedClasses())
        assertEquals(2, row.classes(), "module-info is not answered by first-one-wins, so it is not counted")
        assertEquals(first, row.firstShadower())
        assertEquals("p/Shared.class", row.example())
        assertTrue(ClasspathCensus.census(listOf(first)).isEmpty())
    }

    private fun jar(path: Path, vararg entries: String): Path {
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            for (e in entries) {
                zip.putNextEntry(ZipEntry(e))
                zip.write(byteArrayOf(0xCA.toByte(), 0xFE.toByte()))
                zip.closeEntry()
            }
        }
        return path
    }
}
