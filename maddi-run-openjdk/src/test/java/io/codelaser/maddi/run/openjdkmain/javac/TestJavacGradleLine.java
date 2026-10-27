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

package io.codelaser.maddi.run.openjdkmain.javac;

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
     * ⛔⛔ THE WARNING POLICY WAS BEING THROWN AWAY BY THE {@code -X} CATCH-ALL, IN SILENCE — and this very line,
     * a real Elasticsearch compile, carries all of it: {@code -Werror} fell through to "ignoring unknown option"
     * and both {@code -Xlint} arguments were dropped by {@code option.startsWith("-X")}.
     *
     * <p>⭐ javac's own line is the strongest evidence there is for this fact: not what a build file declares,
     * but what the build actually passed, after every append and every subtraction.
     *
     * <p>⚠ {@code -Xdoclint:all} is on this line too and is NOT warning policy — it configures the javadoc
     * checker. A prefix test on {@code -X} would take it; the test is on {@code -Xlint}.
     */
    @DisplayName("the warning policy is read off the line, and -Xdoclint is not part of it")
    @Test
    public void warningPolicyIsRead() {
        Javac j = Javac.parse(GRADLE_LINE);

        assertEquals(List.of("-Werror", "-Xlint:all,-path,-serial", "-Xlint:-module,-exports"), j.warningFlags(),
                "in the order the build passed them, and -Xdoclint:all is not among them");
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
        // ⛔ ...and the API level this states is NONE. -source sets the language level and leaves the API at the
        // running JDK's; only --release pins the API (through ct.sym). effectiveRelease() used to fall back to
        // -source, which made maddi read a java.base the build never compiled against: measured on maddi itself,
        // whose Gradle build emits exactly this line for two modules, 391 dropped compilation units.
        assertEquals(0, j.effectiveRelease(), "-source is not --release");
    }

    /** And a real {@code --release} still answers it, which is the half that must not be lost. */
    @DisplayName("--release states the API level; -source does not")
    @Test
    public void releaseStatesTheApiLevel() {
        Javac withRelease = Javac.parse("-d /out --release 21 /src/A.java");
        assertEquals(21, withRelease.effectiveRelease());

        Javac neither = Javac.parse("-d /out /src/A.java");
        assertEquals(0, neither.effectiveRelease());
    }

    /** javac's own short aliases, which were being ignored: {@code -cp} for the class path, {@code -p} for modules. */
    @DisplayName("javac's -cp and -p aliases are honoured")
    @Test
    public void shortAliases() {
        Javac j = Javac.parse("-d /out -cp /a.jar -p /m.jar /src/A.java");

        assertEquals(List.of("/a.jar"), j.classpath());
        assertEquals(List.of("/m.jar"), j.modulePath());
    }

    /**
     * ⛔ {@code --add-exports} was not in the valued-option table, so the flag was ignored and its value read on
     * its own -- the package opened, lost. maddi's {@code maddi-java-openjdk} compiles against javac's internals
     * with five of these, and the parse dropped 8 of its 17 compilation units for want of them.
     */
    @DisplayName("--add-exports is recorded, per occurrence and in both spellings; its value is not a source file")
    @Test
    public void addExportsIsRecorded() {
        Javac j = Javac.parse("-d /out -source 25"
                              + " --add-exports jdk.compiler/com.sun.tools.javac.code=my.module,ALL-UNNAMED"
                              + " --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED /src/A.java");

        assertEquals(List.of("jdk.compiler/com.sun.tools.javac.code=my.module,ALL-UNNAMED",
                        "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"), j.addExports());
        assertEquals(List.of("/src/A.java"), j.sourceFiles());
        assertEquals("/out", j.destination());
    }

    /**
     * The language level a set's build compiled at, kept for a consumer (which syntax a refactoring may write):
     * {@code --release} when stated, else {@code -source}; and {@code --enable-preview}, a flag without a value
     * that must not eat the source file after it.
     */
    @DisplayName("languageLevel is --release, else -source; --enable-preview is captured")
    @Test
    public void languageLevelAndPreview() {
        Javac preview = Javac.parse("-d /out -source 21 --enable-preview /src/A.java");
        assertEquals(21, preview.languageLevel());
        assertTrue(preview.enablePreview());
        assertEquals(0, preview.effectiveRelease(), "-source still states no API");
        assertEquals(List.of("/src/A.java"), preview.sourceFiles());

        Javac release = Javac.parse("-d /out --release 17 /src/A.java");
        assertEquals(17, release.languageLevel());
        assertFalse(release.enablePreview());

        Javac neither = Javac.parse("-d /out /src/A.java");
        assertTrue(neither.languageLevel() <= 0);
        assertFalse(neither.enablePreview());
    }
}
