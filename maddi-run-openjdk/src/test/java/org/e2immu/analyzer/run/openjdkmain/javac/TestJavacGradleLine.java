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

package org.e2immu.analyzer.run.openjdkmain.javac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Javac#parse} against a line <b>Gradle</b> writes, rather than one Maven writes.
 * <p>
 * ⚠ WHY THIS FILE EXISTS: both existing javac fixtures are Maven logs ({@code mvnLangchain4j.txt.gz},
 * {@code mvnTimefold-solver.txt.gz}), and every project configured for the {@code build-input-configuration}
 * route is a Maven project. The javac-log front end had therefore never been shown a Gradle command line, and
 * Gradle's differs in one way that matters.
 * <p>
 * ⛔ <b>GRADLE EMITS A GNU-STYLE {@code --module-path=<path>}, AND THAT FORM USED TO BE DROPPED IN SILENCE.</b>
 * It was harmless only by luck: Gradle also writes the space-separated form earlier on the same line, so the
 * value got set anyway — while the {@code =} token, falling through to the lookahead branch, ate the
 * {@code --module-version=...} that followed it. A build tool emitting only the inline form would have produced
 * a source set with an <b>empty module path</b> and no complaint, and on a modular project an empty module path
 * is not a degradation, it is every cross-module reference failing to resolve.
 * <p>
 * The line below is copied from a real run — {@code :libs:core:compileJava --debug} on the Elasticsearch corpus,
 * shortened in the file lists only. Its option shapes are otherwise verbatim, including the two spellings of
 * {@code --module-path}, {@code -sourcepath ""}, {@code -classpath ""}, and {@code --patch-module}, whose value
 * contains an {@code '='} and must NOT be read as an inline option.
 */
public class TestJavacGradleLine {

    private static final String JSR305 = "/gradle/caches/jsr305-3.0.2.jar";
    private static final String LOGGING = "/es/libs/logging/build/classes/java/main";
    private static final String MODULE_PATH = JSR305 + ":" + LOGGING;

    private static final String GRADLE_LINE = String.join(" ",
            "--release", "21",
            "-d", "/es/libs/core/build/classes/java/main",
            "-encoding", "UTF-8",
            "-h", "/es/libs/core/build/generated/sources/headers/java/main",
            "-g",
            "-sourcepath", "\"\"",
            "-proc:none",
            "-s", "/es/libs/core/build/generated/sources/annotationProcessor/java/main",
            "-XDuseUnsharedTable=true",
            "-classpath", "\"\"",
            "--module-path", MODULE_PATH,
            "-Werror",
            "-Xlint:all,-path,-serial",
            "-Xdoclint:all",
            "--module-path=" + MODULE_PATH,
            "--module-version=9.6.0-SNAPSHOT",
            "-Xlint:-module,-exports",
            "--patch-module", "java.base=/es/libs/core/build/jdk21-foreign-api.jar",
            "/es/libs/core/src/main/java/module-info.java",
            "/es/libs/core/src/main/java/org/elasticsearch/core/Assertions.java",
            "/es/libs/core/src/main/java/org/elasticsearch/core/Booleans.java");

    /** ⛔⛔ THE HEADLINE: the whole line parses, and every option the source-set builder reads is present. */
    @DisplayName("a Gradle javac line parses, module path and all")
    @Test
    public void gradleLineParses() {
        Javac j = Javac.parse(GRADLE_LINE);

        assertEquals("/es/libs/core/build/classes/java/main", j.destination());
        assertEquals("UTF-8", j.encoding());
        assertEquals(21, j.release());
        assertEquals(List.of(JSR305, LOGGING), j.modulePath());
        // `-sourcepath ""` and `-classpath ""` are Gradle's way of saying "everything is on the module path"
        assertEquals(List.of(), j.sourcePath(), "an explicitly empty -sourcepath is empty, not null-and-ignored");
        assertEquals(List.of(), j.classpath());
        assertEquals(3, j.sourceFiles().size(), "" + j.sourceFiles());
        assertTrue(j.generatedSourceFilesDestination().endsWith("/annotationProcessor/java/main"));
    }

    /**
     * ⛔⛔ THE DEFECT, ISOLATED. Only the inline spelling — which is what a build tool is free to emit, and what
     * Gradle does emit alongside the other. Before the fix this returned an empty module path.
     */
    @DisplayName("--module-path=<path> alone is honoured, not silently dropped")
    @Test
    public void inlineLongOptionAlone() {
        Javac j = Javac.parse("-d /out --module-path=" + MODULE_PATH + " /src/A.java");

        assertNotNull(j.modulePath(), "the inline form used to be ignored, leaving this null");
        assertEquals(List.of(JSR305, LOGGING), j.modulePath());
        assertEquals("/out", j.destination());
    }

    /**
     * ⚠ CONTROL, AND IT IS WHY THE INLINE RULE IS RESTRICTED TO {@code --}. A value may itself contain an
     * {@code '='}: {@code --patch-module java.base=x.jar} takes its value as a SEPARATE token, and
     * {@code -Akey=value} is an annotation-processor option. Reading the first {@code '='} of any token as an
     * option separator would turn both into unknown options — and, in the {@code --patch-module} case, would stop
     * consuming the following token, so the jar path would then be read as a source file.
     */
    @DisplayName("CONTROL: a VALUE containing '=' is not mistaken for an inline option")
    @Test
    public void valueContainingEqualsIsNotAnInlineOption() {
        Javac j = Javac.parse("-d /out --patch-module java.base=/es/foreign.jar -Akey=value /src/A.java");

        assertEquals("/out", j.destination());
        assertEquals(List.of("/src/A.java"), j.sourceFiles(),
                "the --patch-module value must be consumed as that option's argument, not read as a source file");
    }

    /**
     * ⚠ CONTROL: the Maven spellings keep working. That is what the two {@code .gz} fixtures cover end to end,
     * asserted here in the small so a change to the option table fails here first and legibly.
     */
    @DisplayName("CONTROL: the space-separated Maven spellings are unchanged")
    @Test
    public void mavenSpellingsUnchanged() {
        Javac j = Javac.parse("-d /target/classes -classpath /a.jar:/b.jar -sourcepath /src/main/java"
                              + " -source 17 -target 17 -encoding UTF-8 /src/main/java/A.java");

        assertEquals("/target/classes", j.destination());
        assertEquals(List.of("/a.jar", "/b.jar"), j.classpath());
        assertEquals(List.of("/src/main/java"), j.sourcePath());
        assertEquals(17, j.sourceRelease());
        assertEquals(17, j.targetRelease());
    }

    /** javac's own short aliases, which were being ignored: {@code -cp} for the class path, {@code -p} for modules. */
    @DisplayName("javac's -cp and -p aliases are honoured")
    @Test
    public void shortAliases() {
        Javac j = Javac.parse("-d /out -cp /a.jar -p /m.jar /src/A.java");

        assertEquals(List.of("/a.jar"), j.classpath());
        assertEquals(List.of("/m.jar"), j.modulePath());
    }
}
