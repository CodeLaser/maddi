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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * ⭐ <b>What makes every other test in this module evidence for the isolation.</b> The corpus runs here parse
 * ~1,200 Kotlin types; they would do that just as happily with the compiler flat on the classpath, and then
 * they would prove nothing about G46. So: assert the compiler is <b>not reachable</b> from this JVM, while
 * the front end — installed by {@link K2RealmTestBootstrap} from a realm — works anyway.
 *
 * <p>If these go red because a dependency put the compiler back on the classpath, every other green
 * result in this module quietly stops meaning what it says.
 */
public class TestCompilerIsNotOnTheClasspath {

    private static Class<?> loadable(String fqn) {
        try {
            return Class.forName(fqn, false, TestCompilerIsNotOnTheClasspath.class.getClassLoader());
        } catch (ClassNotFoundException notHere) {
            return null;
        }
    }

    @Test
    public void theCompilerAndTheIntellijPlatformAreUnreachable() {
        assertNull(loadable("org.jetbrains.kotlin.psi.KtFile"), "the Kotlin compiler is back on the classpath");
        assertNull(loadable("com.intellij.openapi.project.Project"), "…and with it the IntelliJ platform");
    }

    /**
     * ⭐ <b>The reported defect, as a regression test.</b> ANTLR IS on this classpath — the real one, through
     * jgrapht-io — and that is fine. What broke downstream was <i>which jar answered</i>: the fat compiler
     * jar's ProGuard-minimised copy of {@code CharStreams} keeps only {@code fromString(String, String)},
     * the one overload Kotlin itself calls, so checkstyle's {@code fromString(String)} became a
     * {@code NoSuchMethodError}. Asking for that method is therefore a direct test of the shadowing.
     *
     * <p>⚠ My first version of this asserted {@code CharStreams} was <i>unloadable</i>, which was simply
     * wrong — and it went red for the right reason, against a classpath that was already correct.
     */
    @Test
    public void antlrIsTheRealAntlr() throws NoSuchMethodException {
        Class<?> charStreams = loadable("org.antlr.v4.runtime.CharStreams");
        assertNotNull(charStreams, "ANTLR should be on this classpath, through jgrapht-io");
        assertNotNull(charStreams.getMethod("fromString", String.class),
                "this is the compiler jar's minimised copy, which has only fromString(String, String)");
        assertNotShadowed(charStreams);
    }

    /**
     * The same question for every other root the fat jar carries: whatever is on this classpath, none of it
     * may be ANSWERED by the compiler jar. A class that is simply absent is not a problem — a class served
     * from the wrong jar is. (This is the "first-one-wins census" in miniature, on one JVM.)
     */
    @Test
    public void nothingOnThisClasspathIsServedByTheCompilerJar() {
        for (String fqn : java.util.List.of(
                "org.antlr.v4.runtime.CharStreams",
                "com.google.common.collect.ImmutableList",
                "com.sun.jna.Native",
                "org.jline.reader.LineReader",
                "com.fasterxml.aalto.stax.InputFactoryImpl",
                "it.unimi.dsi.fastutil.ints.IntArrayList",
                "org.jdom.Element",
                "org.apache.log4j.Level",
                "javax.inject.Inject")) {
            Class<?> loaded = loadable(fqn);
            if (loaded != null) assertNotShadowed(loaded);
        }
    }

    private static void assertNotShadowed(Class<?> type) {
        var source = type.getProtectionDomain().getCodeSource();
        String from = source == null || source.getLocation() == null ? "" : source.getLocation().toString();
        assertFalse(from.contains("kotlin-compiler"),
                () -> type.getName() + " is being answered by " + from
                      + " — the fat compiler jar is on the classpath and shadowing it");
    }

    @Test
    public void andTheFrontEndStillWorks() {
        assertNotNull(KotlinFrontEnds.get(), "the realm bootstrap did not install a front end");
        assertNotSame(getClass().getClassLoader(), KotlinFrontEnds.get().getClass().getClassLoader(),
                "the front end came from this JVM's own classpath, so no realm was involved");
    }
}
