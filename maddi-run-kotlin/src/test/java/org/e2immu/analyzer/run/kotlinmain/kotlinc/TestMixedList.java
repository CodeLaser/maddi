package org.e2immu.analyzer.run.kotlinmain.kotlinc;

import org.e2immu.analyzer.run.config.compile.CompileInvocation;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.Test;

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

    @Test
    public void javaSourceSetDependsOnKotlinSourceSet() throws IOException {
        Path tmp = Files.createTempDirectory("mixed");
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
        Path logFile = Files.createTempFile("mixed", ".txt");
        Files.writeString(logFile, log);

        List<CompileInvocation> invocations = new ParseMixedList().invocations(logFile);
        assertEquals(2, invocations.size(), "one kotlinc + one javac invocation");

        InputConfiguration config = new ParseMixedList().parse(logFile);
        SourceSet kotlinSet = config.sourceSets().stream().filter(s -> "kotlin/main".equals(s.name())).findFirst().orElseThrow();
        SourceSet javaSet = config.sourceSets().stream().filter(s -> "java/main".equals(s.name())).findFirst().orElseThrow();
        assertTrue(javaSet.dependencies().contains(kotlinSet),
                "the Java source set links to the Kotlin source set by output identity");
        // each source set kept its own inferred root
        assertEquals(List.of(tmp.resolve("proj/src/main/kotlin")), kotlinSet.sourceDirectories());
        assertEquals(List.of(tmp.resolve("proj/src/main/java")), javaSet.sourceDirectories());
    }
}
