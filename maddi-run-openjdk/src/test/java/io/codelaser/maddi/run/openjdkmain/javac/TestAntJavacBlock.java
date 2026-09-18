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

package io.codelaser.maddi.run.openjdkmain.javac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The <b>fourth</b> build tool, and the only one whose compile invocation does not fit on a line.
 * <p>
 * {@link ParseJavacList} reads a single-line {@code javac …}, Gradle's {@code Compiler arguments: …} and
 * Maven's {@code [DEBUG] -d …}. {@code ant -verbose} prints a BLOCK: a {@code Compilation arguments:} header,
 * one quoted argument per line, then {@code Files to be compiled:} with one path per line. All three existing
 * patterns are single-line, so an Ant corpus produced <b>zero</b> compile invocations — and therefore a
 * configuration with no source sets at all, which is not a degradation but a total loss.
 * <p>
 * Found on Apache Cassandra (2026-09-18), whose build is Ant. The campaign shipped a converter script outside
 * the product to get past it and filed the missing route as a gap; this fixture is that script's contract,
 * inside the product, against a block copied from the real log.
 * <p>
 * ⚠ <b>The conversion is a FORMAT change and nothing else</b> — no argument added, dropped or reordered — and
 * the last test is what keeps it one: an argument containing whitespace REFUSES, because
 * {@link Javac#parse} splits on whitespace and such an argument would silently become two, the second of
 * which is then read as a source file or eats the token after it. A source set built from a corrupted command
 * line is far harder to notice than a refusal that names the argument.
 */
public class TestAntJavacBlock {

    /** verbatim shape from {@code ant -verbose}, with the file list shortened */
    private static final List<String> LOG = List.of(
            "Buildfile: /checkout/cassandra/build.xml",
            "build-project:",
            "    [javac] Compilation arguments:",
            "    [javac] '-d'",
            "    [javac] '/checkout/cassandra/build/classes/main'",
            "    [javac] '-classpath'",
            "    [javac] '/checkout/cassandra/build/lib/jars/guava-33.jar:/checkout/cassandra/build/lib/jars/jna.jar'",
            "    [javac] '-source'",
            "    [javac] '17'",
            "    [javac] '-target'",
            "    [javac] '17'",
            "    [javac] '-encoding'",
            "    [javac] 'UTF-8'",
            "    [javac] The ' characters around the executable and arguments are",
            "    [javac] not part of the command.",
            "    [javac] Files to be compiled:",
            "    [javac]     /checkout/cassandra/src/java/org/apache/cassandra/db/Keyspace.java",
            "    [javac]     /checkout/cassandra/src/java/org/apache/cassandra/dht/Murmur3Partitioner.java",
            "    [javac] Compiling 2 source files to /checkout/cassandra/build/classes/main",
            "BUILD SUCCESSFUL");

    private static Javac only(List<String> log) {
        List<String> folded = ParseJavacList.foldAntBlocks(log);
        List<Javac> parsed = folded.stream().map(new ParseJavacList()::javacLine).filter(j -> j != null).toList();
        assertEquals(1, parsed.size(), () -> "exactly one invocation: " + folded);
        return parsed.getFirst();
    }

    @DisplayName("⛔ an ant -verbose javac BLOCK becomes one compile invocation")
    @Test
    public void anAntBlockIsOneInvocation() {
        Javac javac = only(LOG);

        assertEquals("/checkout/cassandra/build/classes/main", javac.destination());
        assertEquals(2, javac.classpath().size(), () -> "both jars: " + javac.classpath());
        assertEquals(17, javac.sourceRelease());
        assertEquals(17, javac.targetRelease());
        assertEquals(0, javac.release(), "Ant states -source, not --release; that difference is real");
        assertEquals("UTF-8", javac.encoding());
        assertEquals(List.of("/checkout/cassandra/src/java/org/apache/cassandra/db/Keyspace.java",
                        "/checkout/cassandra/src/java/org/apache/cassandra/dht/Murmur3Partitioner.java"),
                javac.sourceFiles(),
                "the sources come after 'Files to be compiled:', past Ant's own chatter");
    }

    /**
     * ⚠ The veto that has to be able to vanish: a fold that fired on any line holding {@code [javac]}, or that
     * dropped what it did not recognise, would pass the test above and destroy every existing log.
     */
    @DisplayName("control: a log with no Ant block is returned unchanged, so Gradle and Maven are untouched")
    @Test
    public void aLogWithoutAnAntBlockIsUnchanged() {
        List<String> gradle = List.of("some noise",
                "10:02:31.114 [DEBUG] Compiler arguments: -d /build/classes/java/main -source 21 /src/A.java",
                "more noise");
        assertSame(gradle, ParseJavacList.foldAntBlocks(gradle),
                "not merely equal: a log with no block must not even be copied");

        Javac javac = only(gradle);
        assertEquals("/build/classes/java/main", javac.destination());
        assertEquals(List.of("/src/A.java"), javac.sourceFiles());
    }

    @DisplayName("control: two blocks in one log are two invocations, and the lines between them survive")
    @Test
    public void twoBlocks() {
        List<String> two = new java.util.ArrayList<>(LOG);
        two.addAll(LOG);
        List<String> folded = ParseJavacList.foldAntBlocks(two);
        assertEquals(2, folded.stream().filter(l -> l.startsWith("javac ")).count(), () -> folded.toString());
        assertEquals(2, folded.stream().filter(l -> l.equals("BUILD SUCCESSFUL")).count(),
                () -> "a non-block line is passed through, not swallowed: " + folded);
    }

    /**
     * ⛔ The refusal. Ant quotes each argument, so the whitespace is unambiguous in the LOG and is lost only in
     * the single-line form — which is precisely why this must not be converted quietly.
     */
    @DisplayName("⛔ an argument containing whitespace REFUSES rather than silently becoming two")
    @Test
    public void anArgumentWithWhitespaceRefuses() {
        List<String> withSpace = List.of(
                "    [javac] Compilation arguments:",
                "    [javac] '-d'",
                "    [javac] '/checkout/build/my classes'",
                "    [javac] Files to be compiled:",
                "    [javac]     /checkout/src/A.java");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ParseJavacList.foldAntBlocks(withSpace));
        assertTrue(e.getMessage().contains("/checkout/build/my classes"),
                () -> "the refusal must name the argument: " + e.getMessage());
    }

    /**
     * ⚠ The same class of loss, the other way round: an EMPTY argument does not become two, it disappears. A
     * {@code -sourcepath} whose value vanished would shift every argument after it by one.
     */
    @DisplayName("⛔ an EMPTY argument refuses too: join-then-split drops it entirely")
    @Test
    public void anEmptyArgumentRefuses() {
        List<String> withEmpty = List.of(
                "    [javac] Compilation arguments:",
                "    [javac] '-sourcepath'",
                "    [javac] ''",
                "    [javac] '-d'",
                "    [javac] '/checkout/build/classes'",
                "    [javac] Files to be compiled:",
                "    [javac]     /checkout/src/A.java");

        assertThrows(IllegalStateException.class, () -> ParseJavacList.foldAntBlocks(withEmpty));
    }

    /**
     * ⚠ Control for the one above: Gradle's {@code -sourcepath ""} is the two-character string, not an empty
     * argument, and it must still go through — {@code Javac.splitPath} recognises it by name.
     */
    @DisplayName("control: a QUOTED empty path (the two characters \"\") is a real argument and passes")
    @Test
    public void aQuotedEmptyPathIsNotAnEmptyArgument() {
        Javac javac = only(List.of(
                "    [javac] Compilation arguments:",
                "    [javac] '-sourcepath'",
                "    [javac] '\"\"'",
                "    [javac] '-d'",
                "    [javac] '/checkout/build/classes'",
                "    [javac] Files to be compiled:",
                "    [javac]     /checkout/src/A.java"));

        assertEquals("/checkout/build/classes", javac.destination());
        assertEquals(List.of(), javac.sourcePath());
    }
}
