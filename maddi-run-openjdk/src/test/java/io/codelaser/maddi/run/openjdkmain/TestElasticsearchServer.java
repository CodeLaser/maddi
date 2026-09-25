package io.codelaser.maddi.run.openjdkmain;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The elasticsearch <b>server</b> sources: the large-method stress corpus (the work ceiling's
 * degradation bucket lives here — see LinkComputerImpl's WORK_REPORT notes). Not on the certified
 * proving-ground list; this driver exists for A/B and capacity runs.
 * <p>
 * ⛔ IT DOES NOT READ {@code <corpus>/inputConfiguration.json}, AND THAT IS THE POINT. Every other
 * corpus test here reads the file the corpus Taskfile generates on this machine. For elasticsearch
 * that file is a whole-reactor capture — <b>521 source sets</b>, nearly all of them x-pack and qa
 * modules this test has no interest in, and 27 parsing/inspection errors of their own take the run
 * to {@code EXIT_PARSER_ERROR} before the server closure is reached at all.
 * <p>
 * The slice is therefore <b>checked in</b>, at {@value #CONFIG_RESOURCE}: {@code server/main} as
 * the only source set, its 19 {@code libs/*} dependencies as compiled class directories, and the
 * JDK jmods. Regenerate with {@code task corpus:config:elasticsearch-server}.
 * <p>
 * ⚠ The 26 external jars (lucene, log4j, hppc, joni, …) are deliberately NOT named: they live in
 * the Gradle cache, and pinning those absolute paths is what makes a generated configuration
 * un-checkable-in — they rot, and a rotted path is silent. References into them degrade to
 * "unresolved references on the partial classpath" warnings, which do not move the exit code.
 * <p>
 * Historically OOM'd at 8G on this closure: run with TESTXMX=24G or more if it comes back short.
 * <p>
 * ⚠ IT RUNS FOR ~54 MINUTES (53m43s measured 2026-08-25 at the module's 12G), which is inside the
 * memory-aware battery's 1h per-bucket timeout but not by much — and a bucket that times out is
 * recorded as `timeout`, with no verdict at all rather than a red one. If it starts tripping,
 * raise the runner's --timeout rather than assuming the corpus broke.
 */
@Tag("slow")
public class TestElasticsearchServer {

    /** The checked-in slice. Every path in it is relative to the corpus root; see {@link #resolve}. */
    static final String CONFIG_RESOURCE = "/corpus/elasticsearch-server.json";

    /** Scheme for an external artifact named by coordinate rather than path; see {@link #inGradleCache}. */
    static final String GRADLE_CACHE = "gradle-cache:";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (see TestTimefoldSolver)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("io.codelaser.maddi.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    /**
     * The SOURCES are what can be absent, not the configuration. Gating on the configuration — now
     * checked in, hence always present — would be a gate that can never close, and gating on a name
     * nothing generates is how this driver skipped every run for its first 20 days.
     */
    private static void assumeCorpus() {
        Assumptions.assumeTrue(Files.isDirectory(TestOssCorpus.dir("elasticsearch").resolve("server/src/main/java")),
                "requires the elasticsearch corpus checkout (task corpus:elasticsearch)");
    }

    /**
     * Absolutize the committed slice against this machine's corpus root, into {@code tempDir}.
     * <p>
     * ⛔ THE COMMITTED FILE IS RELATIVE AND THE READER REQUIRES ABSOLUTE, deliberately on both sides.
     * {@code ComputeSourceSets.absoluteURI} states the reader's half: a class-path part must carry an
     * absolute hierarchical file URI, because the openjdk inspector does a bare
     * {@code Path.of(classPathPart.uri())} and {@code Path.of} throws "URI is not hierarchical" on the
     * opaque {@code file:<relative>} form. Only source directories are resolved against
     * {@code workingDirectory}.
     * <p>
     * That invariant was written about Gradle-cache jars, which are machine-specific whichever way you
     * write them. This slice's class path is INSIDE the corpus, where relative is what makes the file
     * reviewable and identical everywhere — so the committed artifact stays relative and the
     * absolutizing happens here, rather than by weakening a documented contract in the inspector.
     */
    private static Path resolve(Path tempDir) throws IOException {
        Path corpus = TestOssCorpus.dir("elasticsearch").toAbsolutePath().normalize();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root;
        try (InputStream in = TestElasticsearchServer.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (in == null) throw new IllegalStateException(CONFIG_RESOURCE + " is not on the test classpath");
            root = (ObjectNode) mapper.readTree(in);
        }
        root.put("workingDirectory", corpus.toString());
        for (String field : new String[]{"classPathParts", "sourceSets"}) {
            for (JsonNode node : root.withArray(field)) {
                String uri = node.path("uri").asText("");
                // jmod: entries are symbolic and carry no path; only the two relative forms move
                if (uri.startsWith("file:") && !uri.startsWith("file:/")) {
                    ((ObjectNode) node).put("uri", "file:" + corpus.resolve(uri.substring(5)));
                } else if (uri.startsWith(GRADLE_CACHE)) {
                    ((ObjectNode) node).put("uri", "file:" + inGradleCache(corpus, uri.substring(GRADLE_CACHE.length())));
                }
            }
        }
        Path out = tempDir.resolve("elasticsearch-server.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), root);
        return out;
    }


    /**
     * Locate {@code <group>/<artifact>/<version>/<file>.jar} in this machine's Gradle cache.
     * <p>
     * ⛔ THESE 26 JARS CANNOT BE DROPPED, which is why the slice goes to this trouble. maddi
     * tolerates an incomplete class path — unresolved references degrade to warnings — but JAVAC
     * DOES NOT: attributing {@code server/main} with lucene absent dies inside the compiler,
     * {@code NullPointerException ... "t1" is null} at {@code Types.sideCast}, validating a generic
     * supertype it cannot see. The run aborts before maddi's own tolerance is consulted.
     * <p>
     * ⚠ THE SHA1 DIRECTORY IS THE ONE PART NOT COMMITTED. Gradle stores an artifact at
     * {@code caches/modules-2/files-2.1/<group>/<artifact>/<version>/<sha1>/<file>}, and that digest
     * differs per resolved artifact. The coordinate above it is identical on every machine, so the
     * committed file carries the coordinate and this globs the single level between.
     * <p>
     * The corpus's own copy comes first: {@code corpus/scripts/vendor-libraries.py} moves every jar a
     * generated configuration names into {@code <TEST_OSS_ROOT>/lib/<project>/}, in Maven layout, because
     * Gradle deletes a cache entry it has not itself used for 30 days (18 jars, 2026-09-25).
     */
    private static Path inGradleCache(Path corpus, String coordinate) throws IOException {
        String[] gavf = coordinate.split("/");
        if (gavf.length == 4) {
            Path vendored = corpus.getParent().resolve("lib").resolve(corpus.getFileName())
                    .resolve(gavf[0].replace('.', '/')).resolve(gavf[1]).resolve(gavf[2]).resolve(gavf[3]);
            if (Files.isRegularFile(vendored)) return vendored;
        }
        String home = System.getenv("GRADLE_USER_HOME");
        Path modules = (home == null || home.isBlank() ? Path.of(System.getProperty("user.home"), ".gradle")
                : Path.of(home)).resolve("caches/modules-2/files-2.1");
        int lastSlash = coordinate.lastIndexOf('/');
        Path versionDir = modules.resolve(coordinate.substring(0, lastSlash));
        String fileName = coordinate.substring(lastSlash + 1);
        if (Files.isDirectory(versionDir)) {
            try (var digests = Files.list(versionDir)) {
                Path hit = digests.map(dir -> dir.resolve(fileName)).filter(Files::isRegularFile)
                        .findFirst().orElse(null);
                if (hit != null) return hit;
            }
        }
        // deliberately not a skip and not a warning: a missing jar takes javac down inside itself,
        // and "cannot find lucene" is a far better report than that NPE
        throw new IllegalStateException("not in the Gradle cache: " + coordinate + " (looked under "
                                        + versionDir + "). Build the corpus with `task corpus:elasticsearch`.");
    }

    @Test
    public void test(@TempDir Path tempDir) throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + resolve(tempDir)
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk"
        });
        // EXIT_ANALYZER_ERROR stays tolerated, as it was for the whole-reactor configuration: this
        // corpus is a capacity driver, not a verdict baseline. Crash-class exits (internal exception,
        // parser, inspection, IO) still fail the test.
        assertTrue(exitValue == Main.EXIT_OK || exitValue == Main.EXIT_ANALYZER_ERROR,
                "unexpected exit " + exitValue);
    }
}
