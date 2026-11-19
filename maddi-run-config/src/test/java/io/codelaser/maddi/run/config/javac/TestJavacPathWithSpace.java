package io.codelaser.maddi.run.config.javac;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
