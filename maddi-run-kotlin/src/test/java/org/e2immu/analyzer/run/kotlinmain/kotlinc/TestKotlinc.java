package org.e2immu.analyzer.run.kotlinmain.kotlinc;

import org.e2immu.analyzer.run.config.compile.CompileListToSourceSets;
import org.e2immu.language.cst.api.element.SourceSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1–3 coverage for the kotlinc source-set reconstruction: the {@link Kotlinc} tokenizer, the shared
 * {@link CompileListToSourceSets} engine over Kotlin invocations (friend-path linking, Kotlin source-dir
 * inference), and {@link ParseKotlincList} end to end. Portable: all paths are built under a temp directory.
 */
public class TestKotlinc {

    // a real-shape Gradle main compile line (see kotlin-source-sets.md §3), abbreviated
    private static final String GRADLE_MAIN =
            "2026-07-07T21:03:07 [DEBUG] [org.gradle.api.Task] v: Kotlin compiler args: " +
            "-jvm-target 17 -module-name maddi-x_main -jdk-home /opt/jdk -no-stdlib -no-reflect " +
            "-classpath /repo/a.jar:/repo/b.jar -api-version 2.0 -language-version 2.0 -Xjsr305=strict " +
            "-d /proj/maddi-x/build/classes/kotlin/main " +
            "/proj/maddi-x/src/main/kotlin/a/b/Foo.kt /proj/maddi-x/src/main/kotlin/a/b/Bar.kt";

    @Test
    public void parseGradleMainLine() {
        // strip the marker prefix the way ParseKotlincList's GRADLE pattern does
        String args = GRADLE_MAIN.replaceFirst(".*Kotlin compiler args:\\s*", "");
        Kotlinc k = Kotlinc.parse(args);
        assertNotNull(k);
        assertEquals("/proj/maddi-x/build/classes/kotlin/main", k.destination());
        assertEquals("maddi-x_main", k.moduleName());
        assertEquals(17, k.jvmTarget());
        assertEquals("2.0", k.apiVersion());
        assertEquals("/opt/jdk", k.jdkHome());
        assertEquals(List.of("/repo/a.jar", "/repo/b.jar"), k.classpath());
        assertEquals(2, k.sourceFiles().size());
        assertTrue(k.sourceFiles().getFirst().endsWith("a/b/Foo.kt"));
        assertTrue(k.friendPaths().isEmpty());
        assertTrue(k.modulePath().isEmpty());
        assertTrue(k.sourcePath().isEmpty());
    }

    @Test
    public void parseFriendPathsOnTestCompile() {
        String args = "-jvm-target 17 -module-name maddi-x_test " +
                "-Xfriend-paths=/proj/maddi-x/build/classes/kotlin/main " +
                "-d /proj/maddi-x/build/classes/kotlin/test /proj/maddi-x/src/test/kotlin/a/b/FooTest.kt";
        Kotlinc k = Kotlinc.parse(args);
        assertNotNull(k);
        assertEquals(List.of("/proj/maddi-x/build/classes/kotlin/main"), k.friendPaths());
        assertEquals("/proj/maddi-x/build/classes/kotlin/test", k.destination());
    }

    @Test
    public void ignoresPluginAndParamlessFlagsWithoutEatingSources() {
        String args = "-Xplugin=/x/kapt.jar -P plugin:foo:bar=1 -no-stdlib -java-parameters -progressive " +
                "-d /proj/m/build/classes/kotlin/main /proj/m/src/main/kotlin/A.kt";
        Kotlinc k = Kotlinc.parse(args);
        assertNotNull(k);
        assertEquals("/proj/m/build/classes/kotlin/main", k.destination());
        assertEquals(1, k.sourceFiles().size(), "the -P value must not have swallowed the source file");
        assertTrue(k.sourceFiles().getFirst().endsWith("A.kt"));
    }

    @Test
    public void engineLinksTestToMainViaFriendPaths() throws IOException {
        Path tmp = Files.createTempDirectory("k-link");
        Path mainSrc = tmp.resolve("maddi-x/src/main/kotlin/a/b/Foo.kt");
        Path testSrc = tmp.resolve("maddi-x/src/test/kotlin/a/b/FooTest.kt");
        Files.createDirectories(mainSrc.getParent());
        Files.createDirectories(testSrc.getParent());
        Files.writeString(mainSrc, "package a.b\nclass Foo(val id: Int)\n");
        Files.writeString(testSrc, "package a.b\nclass FooTest\n");
        String mainOut = tmp.resolve("maddi-x/build/classes/kotlin/main").toString();

        Kotlinc main = new Kotlinc(17, "2.0", "2.0", mainOut,
                "maddi-x_main", null, List.of(), List.of(), List.of(mainSrc.toString()), List.of(), null);
        Kotlinc test = new Kotlinc(17, "2.0", "2.0", tmp.resolve("maddi-x/build/classes/kotlin/test").toString(),
                "maddi-x_test", null, List.of(mainOut), List.of(), List.of(testSrc.toString()), List.of(mainOut), null);

        CompileListToSourceSets.Result result = new CompileListToSourceSets().compute(List.of(main, test));
        assertEquals(2, result.jSourceSets().size());
        SourceSet mainSet = result.jSourceSets().getFirst().sourceSet();
        SourceSet testSet = result.jSourceSets().getLast().sourceSet();

        assertEquals("kotlin/main", mainSet.name());
        assertEquals("kotlin/test", testSet.name());
        assertFalse(mainSet.test());
        assertTrue(testSet.test(), "friend-paths presence marks the test set");
        assertTrue(testSet.dependencies().contains(mainSet), "test must depend on main");
        assertEquals(1, testSet.dependencies().stream().filter(d -> d.equals(mainSet)).count(),
                "no duplicate main edge from classpath + friend-paths");
    }

    @Test
    public void inferSourceDirFromSemicolonlessKotlinPackage() throws IOException {
        Path tmp = Files.createTempDirectory("k-src");
        Path foo = tmp.resolve("proj/src/main/kotlin/a/b/Foo.kt");
        Files.createDirectories(foo.getParent());
        Files.writeString(foo, "package a.b\n\nclass Foo(val id: Int)\n"); // NB no semicolon

        Kotlinc main = new Kotlinc(17, "2.0", "2.0",
                tmp.resolve("proj/build/classes/kotlin/main").toString(),
                "proj_main", null, List.of(), List.of(), List.of(foo.toString()), List.of(), null);

        CompileListToSourceSets.Result result = new CompileListToSourceSets().compute(List.of(main));
        SourceSet set = result.jSourceSets().getFirst().sourceSet();
        assertEquals(List.of(tmp.resolve("proj/src/main/kotlin")), set.sourceDirectories(),
                "package a.b (no ';') must resolve the src/main/kotlin root");
    }

    @Test
    public void parseMavenLog() throws IOException {
        // kotlin-maven-plugin emits labelled multi-line DEBUG blocks (no -Xfriend-paths; main↔test via classpath)
        Path log = Path.of("src/test/resources/kotlinc/mvnKmvn.txt");
        List<Kotlinc> list = new ParseKotlincList().kotlincLines(log);
        assertEquals(2, list.size());

        Kotlinc main = list.getFirst();
        assertEquals("/proj/kmvn/target/classes", main.destination());
        assertEquals("kmvn", main.moduleName());
        assertEquals(List.of("/proj/kmvn/src/main/kotlin", "/proj/kmvn/target/generated-sources/annotations"),
                main.sourcePath(), "Maven gives source directories directly");
        assertTrue(main.sourceFiles().isEmpty());

        Kotlinc test = list.getLast();
        assertEquals("/proj/kmvn/target/test-classes", test.destination());
        assertTrue(test.classpath().contains("/proj/kmvn/target/classes"), "main output is on the test classpath");

        var ic = new ParseKotlincList().inputConfiguration(list, List.of());
        SourceSet mainSet = ic.sourceSets().stream().filter(s -> "target/main".equals(s.name())).findFirst().orElseThrow();
        SourceSet testSet = ic.sourceSets().stream().filter(s -> "target/test-classes".equals(s.name())).findFirst().orElseThrow();
        assertFalse(mainSet.test());
        assertTrue(testSet.test(), "target/test-classes is recognized as a test set");
        assertTrue(testSet.dependencies().contains(mainSet), "test depends on main via classpath output-identity");
        assertEquals(List.of(Path.of("/proj/kmvn/src/main/kotlin"), Path.of("/proj/kmvn/target/generated-sources/annotations")),
                mainSet.sourceDirectories());
        assertTrue(ic.classPathParts().stream().anyMatch(s -> s.name().equals("kotlin-stdlib-2.1.0.jar")));
    }

    @Test
    public void parseLogFileToInputConfiguration() throws IOException {
        Path log = Files.createTempFile("kotlinc", ".txt");
        Files.writeString(log, GRADLE_MAIN + "\n");
        var inputConfiguration = new ParseKotlincList().parse(log);
        assertEquals(1, inputConfiguration.sourceSets().size());
        assertEquals("kotlin/main", inputConfiguration.sourceSets().getFirst().name());
        // the two classpath jars survive as external libraries (plus the java.se jmod closure)
        assertTrue(inputConfiguration.classPathParts().stream().anyMatch(s -> s.name().equals("a.jar")));
        assertTrue(inputConfiguration.classPathParts().stream().anyMatch(s -> s.name().equals("b.jar")));
        assertTrue(inputConfiguration.classPathParts().stream().anyMatch(SourceSet::partOfJdk));
    }
}
