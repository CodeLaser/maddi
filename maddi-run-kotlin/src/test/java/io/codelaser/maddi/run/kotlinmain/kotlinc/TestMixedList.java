package io.codelaser.maddi.run.kotlinmain.kotlinc;

import io.codelaser.maddi.run.config.compile.CompileInvocation;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5: one build log holding both {@code kotlinc} and {@code javac} invocations produces one
 * {@link InputConfiguration} in which a Java source set links to a Kotlin source set by output identity (the
 * Java compile's classpath contains the Kotlin module's output directory).
 */
public class TestMixedList {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root, so each call still gets its own unique
     * directory and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @TempDir
    private Path tempRoot;

    @Test
    public void javaSourceSetDependsOnKotlinSourceSet() throws IOException {
        Path tmp = Files.createTempDirectory(tempRoot, "mixed");
        Path fooKt = tmp.resolve("proj/src/main/kotlin/a/Foo.kt");
        Path barJava = tmp.resolve("proj/src/main/java/b/Bar.java");
        Files.createDirectories(fooKt.getParent());
        Files.createDirectories(barJava.getParent());
        Files.writeString(fooKt, "package a\nclass Foo(val id: Int)\n");
        Files.writeString(barJava, "package b;\npublic class Bar { a.Foo foo; }\n");

        String kotlinOut = tmp.resolve("proj/build/classes/kotlin/main").toString();
        String javaOut = tmp.resolve("proj/build/classes/java/main").toString();

        // real build tools emit compileKotlin before compileJava; the Java classpath carries the Kotlin output
        String log = "10:00 [DEBUG] [org.gradle.api.Task] :proj:compileKotlin v: Kotlin compiler args: " +
                "-jvm-target 17 -module-name proj_main -d " + kotlinOut + " " + fooKt + "\n" +
                "10:01 [DEBUG] :proj:compileJava Compiler arguments: " +
                "-d " + javaOut + " -classpath " + kotlinOut + " -source 17 -target 17 " + barJava + "\n";
        Path logFile = Files.createTempFile(tempRoot, "mixed", ".txt");
        Files.writeString(logFile, log);

        List<CompileInvocation> invocations = new ParseMixedList().invocations(logFile);
        assertEquals(2, invocations.size(), "one kotlinc + one javac invocation");

        InputConfiguration config = new ParseMixedList().parse(logFile);
        // ⚠ ONE MODULE, ONE KIND, TWO SOURCE SETS: java is the unmarked language, kotlin takes the segment
        SourceSet kotlinSet = config.sourceSets().stream().filter(s -> "proj/kotlin/main".equals(s.name())).findFirst().orElseThrow();
        SourceSet javaSet = config.sourceSets().stream().filter(s -> "proj/main".equals(s.name())).findFirst().orElseThrow();
        assertTrue(javaSet.dependencies().contains(kotlinSet),
                "the Java source set links to the Kotlin source set by output identity");
        // each source set kept its own inferred root
        assertEquals(List.of(tmp.resolve("proj/src/main/kotlin")), kotlinSet.sourceDirectories());
        assertEquals(List.of(tmp.resolve("proj/src/main/java")), javaSet.sourceDirectories());
    }

    /**
     * A mixed Maven module: kotlin-maven-plugin's labelled block, then maven-compiler-plugin's javac line, both
     * writing {@code target/classes} (javalin's layout: {@code .kt} and {@code .java} side by side). One output
     * directory is one source set holding both languages -- not a source set plus its own classes as a library.
     */
    @Test
    public void mavenKotlinAndJavaIntoOneDirectoryAreOneSourceSet() throws IOException {
        Path tmp = Files.createTempDirectory(tempRoot, "mixed-mvn");
        Path main = tmp.resolve("proj/src/main/java");
        Path testKotlin = tmp.resolve("proj/src/test/kotlin");
        Path testJava = tmp.resolve("proj/src/test/java");
        for (Path dir : List.of(main, testKotlin, testJava)) Files.createDirectories(dir);
        String classes = tmp.resolve("proj/target/classes").toString();
        String testClasses = tmp.resolve("proj/target/test-classes").toString();
        String jar = tmp.resolve("lib.jar").toString();

        String log = "[DEBUG] Compiling Kotlin sources from [" + main + "]\n" +
                "[DEBUG] some unrelated plugin line\n" +
                "[DEBUG] Classpath: " + classes + ":" + jar + "\n" +
                "[DEBUG] Classes directory is " + classes + "\n" +
                "[DEBUG] Module name is proj\n" +
                "[DEBUG] -d " + classes + " -classpath " + classes + ":" + jar + " -sourcepath " + main
                + ": --release 17 -encoding UTF-8\n" +
                "[DEBUG] Compiling Kotlin sources from [" + testKotlin + ", " + testJava + "]\n" +
                "[DEBUG] Classpath: " + testClasses + ":" + classes + ":" + jar + "\n" +
                "[DEBUG] Classes directory is " + testClasses + "\n" +
                "[DEBUG] Module name is proj\n" +
                "[DEBUG] -d " + testClasses + " -classpath " + testClasses + ":" + classes + ":" + jar
                + " -sourcepath " + testJava + ": --release 17 -encoding UTF-8\n";
        Path logFile = Files.createTempFile(tempRoot, "mixed-mvn", ".txt");
        Files.writeString(logFile, log);

        assertEquals(4, new ParseMixedList().invocations(logFile).size(), "two Maven blocks, two javac lines");

        InputConfiguration config = new ParseMixedList().parse(logFile);
        List<SourceSet> sets = config.sourceSets().stream().filter(s -> !s.externalLibrary()).toList();
        assertEquals(2, sets.size(), () -> "one source set per output directory: " + sets);
        SourceSet mainSet = sets.stream().filter(s -> !s.test()).findFirst().orElseThrow();
        SourceSet testSet = sets.stream().filter(SourceSet::test).findFirst().orElseThrow();
        assertEquals(List.of(main), mainSet.sourceDirectories());
        assertEquals(List.of(testKotlin, testJava), testSet.sourceDirectories());
        assertEquals(17, mainSet.sourceRelease(), "javac's options survive the merge");
        assertTrue(testSet.dependencies().contains(mainSet));
        assertTrue(config.classPathParts().stream().noneMatch(p -> p.uri().toString().endsWith("target/classes")),
                "the module's own output is not also a library");
    }
}
