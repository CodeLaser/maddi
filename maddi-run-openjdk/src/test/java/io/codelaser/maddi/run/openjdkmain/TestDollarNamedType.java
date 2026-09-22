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

package io.codelaser.maddi.run.openjdkmain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A type whose simple name ends in {@code $} is not, by that fact alone, an analysis-hints shadow.
 * <p>
 * {@code $} is a legal Java identifier, and real libraries use it: {@code io.vavr.$} is a hand-written public class
 * in vavr 1.0.1 (its pattern-matching entry point). Guarding "did the user point the analyzer at a hints directory?"
 * on the name suffix alone therefore aborts the analysis of an ordinary project — measured 2026-09-21 on vavr, where
 * the whole run died with {@code AssertionError: It looks like the analysis hints types are part of the primary types
 * of the parse result}, and only {@code -disableassertions} got past it.
 * <p>
 * The discriminator that actually holds is structural, and it is the one {@link
 * io.codelaser.maddi.aapi.parser.AnalysisHintsParser} itself uses: a shadow is a type nested inside a class that
 * declares {@code public static final String PACKAGE_NAME = "<a package>"}. Both halves are asserted here.
 */
public class TestDollarNamedType {

    /**
     * The regression: a top-level {@code $}, and a nested {@code Inner$}, in an ordinary project. Neither is a
     * shadow (no enclosing {@code PACKAGE_NAME}), so the run must complete normally.
     */
    @Test
    public void dollarNamedTypesAreOrdinaryTypes(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("p"));
        // modelled on io.vavr.$
        Files.writeString(src.resolve("p/$.java"), """
                package p;
                public class $ {
                    public static String hello() { return "hi"; }
                }
                """);
        Files.writeString(src.resolve("p/Outer.java"), """
                package p;
                public class Outer {
                    public static class Inner$ {
                        public String greet() { return $.hello(); }
                    }
                    public String go() { return new Inner$().greet(); }
                }
                """);

        // prep AND modification: the suffix heuristic sat in two places, the parse-result check and the
        // call-graph check, and only the modification step reaches the second.
        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP + "," + Main.AS_MODIFICATION});

        assertEquals(Main.EXIT_OK, exit, "a project containing a type named $ must analyse normally");
    }

    /**
     * The other half, and the reason the guard exists: analysis-hint sources really must not be fed to the normal
     * analyzer, because their shadows would be analysed as if they were the library types they stand for. A class
     * declaring {@code PACKAGE_NAME} with {@code $}-suffixed nested types must still be refused.
     */
    @Test
    public void realAnalysisHintSourcesAreStillRefused(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("h"));
        Files.writeString(src.resolve("h/JavaLang.java"), """
                package h;
                public class JavaLang {
                    public static final String PACKAGE_NAME = "java.lang";
                    class Object$ {
                        public String toString() { return null; }
                    }
                }
                """);

        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP + "," + Main.AS_MODIFICATION});

        assertEquals(Main.EXIT_INTERNAL_EXCEPTION, exit,
                "analysis-hint sources fed to the normal analyzer must still trip the guard");
    }
}
