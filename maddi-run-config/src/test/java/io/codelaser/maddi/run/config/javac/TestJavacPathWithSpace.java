package io.codelaser.maddi.run.config.javac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Maven's -X logs javac's arguments unquoted. quarkus's "integration-tests/simple with space" (its own test of paths
 * with spaces) then read as `-d .../simple` and two unknown words, and main and test collided on ".../simple". The
 * file system tells the words of a path from the arguments around it.
 */
public class TestJavacPathWithSpace {

    @TempDir
    Path tmp;

    @Test
    public void aPathWithSpacesIsOnePath() throws IOException {
        Path module = tmp.resolve("simple with space");
        Path classes = Files.createDirectories(module.resolve("target/classes"));
        Path sources = Files.createDirectories(module.resolve("src/main/java"));
        Path jar = Files.createFile(tmp.resolve("lib.jar"));
        Path source = Files.writeString(Files.createDirectories(sources.resolve("p")).resolve("A.java"), "package p; class A {}");
        String line = "-d " + classes + " -classpath " + classes + ":" + jar + " -sourcepath " + sources
                      + ": " + source + " --release 17 -encoding UTF-8";

        Javac javac = Javac.parse(line);

        assertEquals(classes.toString(), javac.destination());
        assertEquals(List.of(classes.toString(), jar.toString()), javac.classpath());
        assertEquals(List.of(sources.toString()), javac.sourcePath());
        assertEquals(List.of(source.toString()), javac.sourceFiles());
        assertEquals(17, javac.release());
        assertEquals("UTF-8", javac.encoding());
    }

    /** A path that does not exist (a log read away from its build) is left as it was: nothing to tell it by. */
    @Test
    public void aMissingPathIsLeftAlone() {
        Javac javac = Javac.parse("-d /nowhere/simple with space/target/classes --release 17");
        assertEquals("/nowhere/simple", javac.destination());
        assertEquals(17, javac.release());
    }

    /**
     * The whole derivation, main and test of a module whose directory has a space: the rejoined path reached
     * `URI.create("file:" + path)` and aborted it ("Illegal character in path"). Now two source sets, test depending on
     * main, their URIs percent-encoded.
     */
    @Test
    public void aModuleWithASpaceDerivesItsSourceSets() throws IOException {
        derive("simple with space");
    }

    private void derive(String dir) throws IOException {
        Path module = tmp.resolve(dir);
        Path classes = Files.createDirectories(module.resolve("target/classes"));
        Path testClasses = Files.createDirectories(module.resolve("target/test-classes"));
        Path main = Files.createDirectories(module.resolve("src/main/java/p"));
        Path test = Files.createDirectories(module.resolve("src/test/java/p"));
        Path a = Files.writeString(main.resolve("A.java"), "package p; public class A {}");
        Path t = Files.writeString(test.resolve("TA.java"), "package p; class TA { A a; }");
        Path log = tmp.resolve("compile.log");
        Files.writeString(log, "javac -d " + classes + " -classpath " + classes + " --release 17 " + a + "\n"
                               + "javac -d " + testClasses + " -classpath " + testClasses + ":" + classes
                               + " --release 17 " + t + "\n");

        var config = new ParseJavacList().parse(log);

        var sets = config.sourceSets().stream().filter(s -> !s.externalLibrary()).toList();
        assertEquals(2, sets.size(), () -> String.valueOf(sets));
        var testSet = sets.stream().filter(s -> s.uri().getPath().endsWith("test-classes")).findFirst().orElseThrow();
        var mainSet = sets.stream().filter(s -> s.uri().getPath().endsWith("target/classes")).findFirst().orElseThrow();
        assertEquals(classes.toString(), mainSet.uri().getPath());
        assertEquals(true, testSet.dependencies().contains(mainSet), () -> String.valueOf(testSet.dependencies()));
    }

    /**
     * The same module as Maven's -X logs it, the form the corpus route ({@code _config:maven-log}) feeds in: one
     * {@code [DEBUG] -d ...} line per compilation, unquoted. quarkus's {@code integration-tests/simple with space}
     * came out of it as a source set named and located at {@code .../simple}, a directory that does not exist, which
     * the gate's corpus pre-flight reports as MISSING. Name, URI, source directory and files must keep the space.
     */
    @Test
    public void aMavenLoggedModuleWithASpaceKeepsItWhole() throws IOException {
        Path module = tmp.resolve("integration-tests/simple with space");
        Path classes = Files.createDirectories(module.resolve("target/classes"));
        Path testClasses = Files.createDirectories(module.resolve("target/test-classes"));
        Path main = Files.createDirectories(module.resolve("src/main/java/p"));
        Path test = Files.createDirectories(module.resolve("src/test/java/p"));
        Path a = Files.writeString(main.resolve("A.java"), "package p; public class A {}");
        Path t = Files.writeString(test.resolve("TA.java"), "package p; class TA { A a; }");
        Path log = tmp.resolve("compile.javac.log");
        Files.writeString(log,
                "[DEBUG] -d " + classes + " -classpath " + classes + ": -sourcepath " + module.resolve("src/main/java")
                + ": " + a + " -s " + module.resolve("target/generated-sources/annotations") + " -g -nowarn --release 17"
                + " -encoding UTF-8\n"
                + "[DEBUG] -d " + testClasses + " -classpath " + testClasses + ":" + classes + ": -sourcepath "
                + module.resolve("src/test/java") + ": " + t + " -g -nowarn --release 17 -encoding UTF-8\n");

        var config = new ParseJavacList().parse(log);

        var sets = config.sourceSets().stream().filter(s -> !s.externalLibrary()).toList();
        assertEquals(2, sets.size(), () -> String.valueOf(sets));
        for (var set : sets) {
            String where = set.name() + " " + set.uri() + " " + set.sourceDirectories();
            assertTrue(Files.exists(Path.of(set.uri())), () -> "uri does not exist: " + where);
            assertTrue(set.uri().getPath().contains("simple with space"), () -> "uri cut at the space: " + where);
            for (Path dir : set.sourceDirectories()) {
                assertTrue(Files.isDirectory(dir.isAbsolute() ? dir : module.resolve(dir)) || dir.toString()
                        .contains("simple with space"), () -> "source directory cut at the space: " + where);
            }
            assertTrue(!set.name().endsWith("/simple") && !set.name().equals("simple"),
                    () -> "name cut at the space: " + where);
            System.out.println("SPACESET " + where);
        }
    }
}
