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

package org.e2immu.analyzer.run.openjdkmain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Guards the error-reporting fix: a source file that fails to parse must be surfaced (openjdk diagnostics now flow
 * into the {@code Summary}) and the process must exit with the parser error code — <em>not</em> the previous
 * silent exit 0.
 */
public class TestErrorReporting {

    @Test
    public void parseErrorExitsWithParserCodeNotSilentZero(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("x"));
        Files.writeString(src.resolve("x/Broken.java"), """
                package x;
                public class Broken {
                    @@@ this is not valid java @@@
                }
                """);

        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP});

        assertNotEquals(Main.EXIT_OK, exit, "a parse error must not report success");
        assertEquals(Main.EXIT_PARSER_ERROR, exit, "a parse error must exit with the parser error code");
    }

    /**
     * Prep-phase fault isolation: a failure while analyzing one method must be recorded and skipped, letting the
     * rest of the run complete, instead of an uncaught throwable aborting the process. The source below (a
     * qualified explicit {@code super()} in a class extending another class's inner class) currently trips an
     * assertion in downstream get/set analysis; whatever the outcome, the run must end with a clean exit code and
     * <em>never</em> the internal-exception code that an escaped throwable would produce. This asserts the isolation
     * happens inside {@code PrepAnalyzer} (exit ANALYSER_ERROR, or OK once the get/set bug is fixed upstream), not
     * merely that a run-level backstop caught it (which would be EXIT_INTERNAL_EXCEPTION).
     */
    @Test
    public void prepMethodFailureIsIsolatedNotFatal(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("p"));
        Files.writeString(src.resolve("p/A.java"), """
                package p;
                public class A {
                    public class Inner { }
                    static class Sub extends A.Inner {
                        Sub(A a) {
                            a.super();
                        }
                    }
                }
                """);

        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP});

        assertNotEquals(Main.EXIT_INTERNAL_EXCEPTION, exit,
                "a per-method prep failure must be isolated, not escape as an uncaught throwable");
    }
}
