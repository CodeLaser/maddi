package io.codelaser.maddi.java.openjdk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>GAP #12's residual.</b> When the same type arrives twice — once committed from a stale jar, once again from
 * source with a different member — the scanner threw a <b>bare</b> {@code UnsupportedOperationException} while
 * logging the three facts that identify the cause, and the parse's top-level verdict read
 * <i>"Can only switch to ParseResult when there are no parse exceptions"</i>. It named neither the type nor the
 * artifact and presented as a SOURCE problem. It cost a session: 18 units dropped, <b>10 of them never touched
 * by the edit</b>, while javac was green.
 * <p>
 * ▶ <b>THE INFORMATION WAS NEVER MISSING — THE PRESENTATION WAS.</b> These tests pin the message, because the
 * message is the entire deliverable: there is no behaviour change to assert.
 * <p>
 * These are the message's unit tests. <b>The crash itself IS reproduced</b>, end to end with a real compiled
 * jar, by {@code TestGap12StaleJarReproduction} in {@code maddi-inspection-openjdk} — and that reproduction is
 * what found the polarity defect below, which every test in this file had passed straight over.
 * ▶ <b>A UNIT TEST OF A MESSAGE CONFIRMS THE MESSAGE YOU IMAGINED, NOT THE ONE THE FAILURE PRODUCES.</b>
 */
public class TestGap12StaleArtifactDiagnosis {

    private static final String TYPE = "a.b.WebhookService";
    private static final String MEMBER = "WebhookService(Settings,ClusterSettings)";
    private static final List<String> EXISTING = List.of("WebhookService(Settings)", "send(Event)");

    @DisplayName("#12: a jar-backed committed type plus a source arrival names the JAR, its mtime, and the remedy")
    @Test
    public void jarBackedNamesTheJarAndTheRemedy() throws Exception {
        Path jar = Files.createTempFile("stale-plugin-", ".jar");
        try {
            URI origin = URI.create("jar:file:" + jar + "!/a/b/WebhookService.class");
            String m = StaleArtifactDiagnosis.message(TYPE, origin, "plugin-bundle", true, null, MEMBER, EXISTING);

            assertTrue(m.contains(TYPE), m);
            assertTrue(m.contains("THE JAR IS STALE"), m);
            assertTrue(m.contains(jar.toString()), "must name the jar itself, not just the class: " + m);
            assertTrue(m.contains("last modified"), "the mtime is what lets a reader see it predates the edit: " + m);
            assertTrue(m.contains("REBUILD OR DELETE IT"), m);
            assertTrue(m.contains("plugin-bundle"), "the source set it was attributed to: " + m);
            assertTrue(m.contains(MEMBER), m);
            assertTrue(m.contains("send(Event)"), "the existing members are the evidence of disagreement: " + m);
            // the sentence that would have saved the session: the failures need not be where you edited
            assertTrue(m.contains("need not be the ones you changed"), m);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    /**
     * ⛔ THE CONTROL, and it is the one that matters: the message must NOT claim a stale jar when there is no
     * jar. A diagnosis that always blames the artifact is exactly as useless as one that never does, and #88
     * cost a session by putting a confidently wrong blocker in a pre-flight.
     */
    @DisplayName("#12 control: two SOURCE arrivals must not be blamed on an artifact")
    @Test
    public void sourceOnlyIsNotBlamedOnAJar() {
        URI origin = URI.create("file:/home/x/src/a/b/WebhookService.java");
        String m = StaleArtifactDiagnosis.message(TYPE, origin, "main", true, null, MEMBER, EXISTING);

        assertFalse(m.contains("THE JAR IS STALE"), "there is no jar here: " + m);
        assertTrue(m.contains("NOT the stale-artifact case"), m);
        assertTrue(m.contains(TYPE), m);
        assertTrue(m.contains(MEMBER), m);
    }

    @DisplayName("#12: a jar-backed type whose second arrival is NOT source states the possibility, not the cause")
    @Test
    public void jarBackedWithoutSourceArrivalIsHedged() {
        URI origin = URI.create("jar:file:/tmp/lib/foo.jar!/a/b/WebhookService.class");
        String m = StaleArtifactDiagnosis.message(TYPE, origin, null, false, origin, MEMBER, EXISTING);

        assertFalse(m.contains("THE JAR IS STALE"), "BOTH sides are the same jar, so nothing is stale: " + m);
        assertTrue(m.contains("came from a JAR"), m);
        assertTrue(m.contains("/tmp/lib/foo.jar"), m);
    }

    /**
     * ⛔ THE POLARITY THAT THE FIRST VERSION OF THIS MESSAGE GOT WRONG, and only the end-to-end reproduction
     * exposed it. Here the SOURCE definition committed first and the JAR arrived second — the mirror of the ES
     * incident, where a plugin bundle committed first. The first version asked only whether the COMMITTED side
     * was jar-backed and therefore printed "this is NOT the stale-artifact case" while looking straight at a
     * stale jar. ▶ ONE WORKED EXAMPLE IS NOT A SPECIFICATION.
     */
    @DisplayName("#12: the jar on the INCOMING side is named too — the mirror polarity")
    @Test
    public void staleJarOnTheIncomingSideIsNamed() throws Exception {
        Path jar = Files.createTempFile("stale-bundle-", ".jar");
        try {
            URI committed = URI.create("mem:///test-protocol/a/b/WebhookService.java");
            URI incoming = URI.create("jar:file:" + jar + "!/a/b/WebhookService.class");
            String m = StaleArtifactDiagnosis.message(TYPE, committed, "main", false, incoming, MEMBER, EXISTING);

            assertTrue(m.contains("THE JAR IS STALE"), "source committed, jar arrived second: " + m);
            assertTrue(m.contains(jar.toString()), m);
            assertTrue(m.contains("FROM A COMPILED ARTIFACT"), m);
            assertTrue(m.contains("second definition came from"), m);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @DisplayName("#12: an exploded classes directory is not a jar")
    @Test
    public void classesDirectoryIsNotAJar() {
        assertNull(StaleArtifactDiagnosis.jarFileOf(URI.create("file:/p/build/classes/java/main/a/b/T.class")));
        assertNull(StaleArtifactDiagnosis.jarFileOf(null));
        assertEquals(Path.of("/p/libs/x.jar"),
                StaleArtifactDiagnosis.jarFileOf(URI.create("jar:file:/p/libs/x.jar!/a/T.class")));
        // a bare jar URI with no entry part still identifies the artifact
        assertEquals(Path.of("/p/libs/x.jar"), StaleArtifactDiagnosis.jarFileOf(URI.create("file:/p/libs/x.jar")));
    }

    /** A diagnostic runs on the failure path, where the caller has already lost; it must never be the thrower. */
    @DisplayName("#12: the diagnosis never throws, whatever it is handed")
    @Test
    public void neverThrows() {
        assertDoesNotThrow(() -> StaleArtifactDiagnosis.message(TYPE, null, null, true, null, MEMBER, List.of()));
        assertDoesNotThrow(() -> StaleArtifactDiagnosis.message(TYPE,
                URI.create("jar:file:/does/not/exist.jar!/a/T.class"), null, true, null, MEMBER, EXISTING));
        assertNull(StaleArtifactDiagnosis.lastModified(Path.of("/definitely/not/here.jar")));
        assertDoesNotThrow(() -> StaleArtifactDiagnosis.message(TYPE, null, null, false, null, MEMBER, EXISTING,
                List.of("line 7: package com.example.ann does not exist")));
    }

    // ---- the classpath-gap branch: neither side is a jar, and the committed source did not compile ----

    private static final URI SRC = URI.create("file:/p/src/main/java/a/b/WebhookService.java");
    private static final URI CLASSES = URI.create("file:/p/target/classes/a/b/WebhookService.class");

    /** One missing package produces one error per use site; this is the real shape, 50 of them in one file. */
    private static List<String> fiftyMissingPackageErrors() {
        return java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> "line " + (7 + i) + ": package com.example.ann does not exist")
                .toList();
    }

    /**
     * ⛔ THE REGRESSION THIS BRANCH EXISTS FOR. Source committed, class file arrived second, NO jar on either
     * side — and the old message ended "the member lists above are the evidence to read". Two independent
     * readers followed that steer within 24 hours to the same wrong conclusion (that a compact record
     * constructor loses its canonical parameters; {@code TestCompactConstructorProbe} refutes it). The file had
     * simply not compiled.
     */
    @DisplayName("#12b: a committed source that did not compile blames the CLASSPATH, not the member lists")
    @Test
    public void javacErrorsOutrankTheMemberLists() {
        String m = StaleArtifactDiagnosis.message(TYPE, SRC, "definition/main", false, CLASSES, MEMBER, EXISTING,
                fiftyMissingPackageErrors());

        assertTrue(m.contains("DID NOT COMPILE"), m);
        assertTrue(m.contains("50 javac error(s)"), "the COUNT is the diagnosis: " + m);
        assertTrue(m.contains("package com.example.ann does not exist"), "javac's own words: " + m);
        assertTrue(m.contains("ERROR RECOVERY"), "the mechanism that truncates the type: " + m);
        assertTrue(m.contains("FEWER PARAMETERS"), "the exact symptom that was misread twice: " + m);
        assertTrue(m.contains("ARTIFACT OF THESE ERRORS"), m);
        assertTrue(m.contains("definition/main"), "name the source set whose classpath to fix: " + m);
        assertTrue(m.contains("START WITH THE JAVAC ERRORS"), m);
        // ⛔ the steer that caused the misdiagnosis must be GONE, not merely accompanied
        assertFalse(m.contains("the member lists above are the evidence to read"),
                "the old steer is what produced two wrong diagnoses: " + m);
        // the errors must come BEFORE the verdict, or a reader meets the member lists first anyway
        assertTrue(m.indexOf("DID NOT COMPILE") < m.indexOf("NOT the stale-artifact case"), m);
    }

    /** Capping is fine; capping silently is not — "50 errors in one file" is itself the signal. */
    @DisplayName("#12b: the error list is deduplicated and capped, but says what it dropped")
    @Test
    public void theCapIsNotSilent() {
        List<String> many = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> "line " + i + ": cannot find symbol Distinct" + i).toList();
        String m = StaleArtifactDiagnosis.message(TYPE, SRC, "main", false, CLASSES, MEMBER, EXISTING, many);
        assertTrue(m.contains("12 javac error(s)"), m);
        assertTrue(m.contains("further distinct error(s)"), "a truncated list must declare the truncation: " + m);
        assertTrue(m.contains("and 7 further"), "12 distinct, 5 shown: " + m);
    }

    /**
     * ⛔ THE GROUPING KEY IS THE ERROR TEXT, NOT THE LINE. One missing import produces the SAME complaint at 50
     * different line numbers — the real shape. Keyed on the whole rendered line, nothing would collapse, and the
     * reader would meet five near-identical lines without the one fact that matters: a SINGLE package is behind
     * all fifty. <b>"50 errors, 1 distinct" is what says "this file has no classpath" rather than "this file has
     * a typo"</b>, and it is the sentence that sends the reader to the dependency list.
     */
    @DisplayName("#12b: fifty errors at fifty line numbers, one missing package — reported as 1 distinct")
    @Test
    public void sameComplaintAcrossManyLinesGroupsToOne() {
        String m = StaleArtifactDiagnosis.message(TYPE, SRC, "main", false, CLASSES, MEMBER, EXISTING,
                fiftyMissingPackageErrors());
        assertTrue(m.contains("50 javac error(s), 1 distinct"),
                "the line numbers differ, the complaint does not: " + m);
        assertFalse(m.contains("further distinct error(s)"), "nothing was dropped, so claim nothing was: " + m);
        assertTrue(m.contains("line 7: package com.example.ann does not exist"),
                "the FIRST line carrying the complaint is kept, so a location survives grouping: " + m);
        // exactly one error line, not fifty and not five
        assertEquals(1, m.lines().filter(l -> l.contains("package com.example.ann")).count(), m);
    }

    /**
     * ⛔ THE CONTROL, in the spirit of {@code sourceOnlyIsNotBlamedOnAJar}: a diagnosis that always blames the
     * classpath is exactly as useless as one that never does. With no javac errors the message must be
     * byte-for-byte what it was before this branch existed.
     */
    @DisplayName("#12b control: a clean compile is never blamed on the classpath")
    @Test
    public void noJavacErrorsMeansNoClasspathBlame() {
        String withEmpty = StaleArtifactDiagnosis.message(TYPE, SRC, "main", false, CLASSES, MEMBER, EXISTING,
                List.of());
        assertFalse(withEmpty.contains("DID NOT COMPILE"), withEmpty);
        assertFalse(withEmpty.contains("javac error"), withEmpty);
        assertTrue(withEmpty.contains("the member lists above are the evidence to read"),
                "with nothing better to offer, the old steer is still the right one: " + withEmpty);

        String legacy = StaleArtifactDiagnosis.message(TYPE, SRC, "main", false, CLASSES, MEMBER, EXISTING);
        assertEquals(legacy, withEmpty, "the 7-arg form must be exactly the 8-arg form with no errors");
    }

    /**
     * A stale jar is still a stale jar: javac errors are additional evidence, and must not displace the verdict
     * that names the artifact and the remedy.
     */
    @DisplayName("#12b: javac errors do not suppress the stale-jar verdict")
    @Test
    public void aStaleJarIsStillReportedAlongsideTheErrors() throws Exception {
        Path jar = Files.createTempFile("stale-both-", ".jar");
        try {
            URI incoming = URI.create("jar:file:" + jar + "!/a/b/WebhookService.class");
            String m = StaleArtifactDiagnosis.message(TYPE, SRC, "main", false, incoming, MEMBER, EXISTING,
                    List.of("line 7: package com.example.ann does not exist"));
            assertTrue(m.contains("THE JAR IS STALE"), "the jar verdict survives: " + m);
            assertTrue(m.contains("REBUILD OR DELETE IT"), m);
            assertTrue(m.contains("DID NOT COMPILE"), "and the errors are still shown: " + m);
        } finally {
            Files.deleteIfExists(jar);
        }
    }
}
