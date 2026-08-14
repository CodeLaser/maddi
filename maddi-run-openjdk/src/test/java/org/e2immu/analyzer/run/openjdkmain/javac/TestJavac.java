package org.e2immu.analyzer.run.openjdkmain.javac;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ParseJavacList against a compile log that is CHECKED IN, and nothing else.
 *
 * <p>This class was a driver rather than a test. It carried fourteen methods reading
 * {@code src/test/resources/*.txt} logs that are not in the repository -- captured on one machine, kept
 * outside it -- and asserted absolute paths under {@code /Users/bnaudts/...}. It was {@code @Disabled} at
 * class level, so none of that ran; what it actually produced was a red on any machine that enabled it,
 * and a {@code NoSuchFileException} naming a file nobody could obtain.
 *
 * <p>Two compile logs are in {@code src/test/resources/javac/}, and only those two can be asserted
 * anywhere. {@code mvnTimefold-solver.txt.gz} has its own class, {@link TestJavacTimefoldSolverCore};
 * this one keeps {@code mvnLangchain4j.txt.gz}. A log is a fixed artefact, so its expected values cannot
 * drift the way a corpus does -- these assertions are as stable as the file.
 *
 * <p>To use it as a driver again, capture a log and point a local copy at it; do not commit a test that
 * reads a path only one machine has.
 */
public class TestJavac {

    @Test
    public void testLangchain4jMvn() throws IOException {
        Path path = Path.of("src/test/resources/javac/mvnLangchain4j.txt.gz");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(315, javacList.size());
        List<Javac> hibernate = javacList.stream().filter(javac -> javac.destination()
                .contains("langchain4j-hibernate")).toList();
        assertEquals(3, hibernate.size());
        {
            Javac j = hibernate.getFirst();
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/classes"));
            assertEquals(11, j.classpath().size());
            assertEquals(9, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(1, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/main/java"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-sources/annotations"));
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = hibernate.get(1);
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/classes"));
            assertEquals(11, j.classpath().size());
            assertEquals(9, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(2, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/main/java"));
            assertTrue(j.sourcePath().getLast().endsWith("langchain4j/langchain4j-hibernate/target/generated-sources/annotations"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-sources/annotations"));
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = hibernate.getLast();
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/test-classes"));
            assertEquals(96, j.classpath().size());
            assertEquals(89, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            // ONE entry, unlike the two main compilations above: the log's -sourcepath for this
            // invocation is "<...>/src/test/java:" -- a single directory and a trailing empty segment,
            // which is dropped. The generated directory is not on the source path here at all; it
            // appears only as -s, asserted on the next line, and note it is generated-test-sources/
            // test-annotations, not .../annotations. The class asserted two entries while it was
            // @Disabled, so nothing ever checked that against the log actually committed here.
            assertEquals(1, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/test/java"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-test-sources/test-annotations"));
            assertEquals("UTF-8", j.encoding());
        }
    }
}
