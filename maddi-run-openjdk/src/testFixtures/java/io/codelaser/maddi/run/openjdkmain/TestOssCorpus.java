package io.codelaser.maddi.run.openjdkmain;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locates the local "test-oss" corpus of open-source projects the corpus tests run against
 * (see {@code corpus/} at the repository root for how to install and build it).
 * <p>
 * Resolution order for the corpus root:
 * <ol>
 *   <li>system property {@code -Dtest.oss.root=...}</li>
 *   <li>environment variable {@code TEST_OSS_ROOT} (exported by the corpus Taskfile)</li>
 *   <li>default {@code ../../test-oss}: relative to a module's working directory, i.e. a sibling
 *       of the maddi checkout — the same convention the {@code testarchive}/{@code testtransform}
 *       corpora use.</li>
 * </ol>
 * A corpus test calls {@link #requireConfig} (or {@link #requireDir}), which SKIPS when the corpus — or
 * its locally generated {@code inputConfiguration.json} — is absent, so a contributor without the
 * checkouts still gets a green build. No machine-specific path, no symlink: the input configuration is
 * generated on the machine that runs the test, so the absolute paths it contains are that machine's own.
 * <p>
 * ⛔ Except under {@code -D}{@value #REQUIRED_PROPERTY}{@code =true}, where the same absence is a
 * FAILURE. A skipped corpus test reports exactly the same green as one that ran and measured a corpus,
 * which is the vacuous-success shape {@code AGENTS.md} §Commands warns about — and the whole point of
 * {@code slowTest} is to measure corpora, so that task turns the property on. Pass
 * {@code -Pcorpus.optional} to get the skipping behaviour back.
 * <p>
 * It lives in <b>test fixtures</b> rather than this module's test scope, for the same reason
 * {@code CloneBenchCorpus} does: more than one module's corpus tests need it, and they are not all
 * downstream of each other. {@code maddi-run-kotlin}'s Kotlin corpus tests (see
 * {@code TestCoilJvmSlice}) resolve the same root, and cannot see this module's test classes.
 */
public final class TestOssCorpus {
    private TestOssCorpus() {
    }

    /**
     * When true, a missing corpus fails instead of skipping. Set by {@code slowTest} (see
     * {@code buildSrc/.../java-library-conventions.gradle.kts}) and intended for CI.
     */
    public static final String REQUIRED_PROPERTY = "maddi.corpus.required";

    public static final Path ROOT = resolveRoot();

    private static Path resolveRoot() {
        String p = System.getProperty("test.oss.root");
        if (p == null || p.isBlank()) p = System.getenv("TEST_OSS_ROOT");
        return p == null || p.isBlank() ? Path.of("../../test-oss") : Path.of(p);
    }

    /** The checkout directory of a corpus project, e.g. {@code <root>/guava}. */
    public static Path dir(String project) {
        return ROOT.resolve(project);
    }

    /** The locally generated input configuration of a corpus project, e.g. {@code <root>/guava/inputConfiguration.json}. */
    public static Path config(String project) {
        return ROOT.resolve(project).resolve("inputConfiguration.json");
    }

    /** The input configuration of {@code project}; skips the test when absent, or fails under {@link #REQUIRED_PROPERTY}. */
    public static Path requireConfig(String project) {
        return require(project, config(project), "input configuration",
                "generate it with `task corpus:config:" + project + "` at the repo root");
    }

    private static final Pattern JAR_URI = Pattern.compile("\"uri\"\\s*:\\s*\"(file:[^\"]+?\\.jar)\"");

    /**
     * {@link #requireConfig}, and every class-path jar it names must exist. ⛔ For a test that PINS a count: the
     * configuration names jars in the shared Gradle cache by absolute path, and one that is evicted is not an
     * error anywhere — its calls just stop resolving. coil's placeholder pin was recorded as 283 with
     * {@code okio-jvm} gone from the cache; the jar came back when an unrelated build re-downloaded it, and the same
     * code counted 208. A missing jar is treated like a missing corpus: a skip, or a failure under
     * {@link #REQUIRED_PROPERTY}.
     */
    public static Path requireCompleteConfig(String project) {
        return requireClassPath(project, requireConfig(project));
    }

    /** The class-path half of {@link #requireCompleteConfig}, for any configuration file. */
    public static Path requireClassPath(String project, Path config) {
        String json;
        try {
            json = Files.readString(config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Matcher m = JAR_URI.matcher(json);
        List<Path> missing = m.results().map(r -> Path.of(URI.create(r.group(1))))
                .filter(p -> !Files.exists(p)).toList();
        if (missing.isEmpty()) return config;
        return require(project, missing.getFirst(), missing.size() + " class-path jar(s), the first",
                "they are named by " + config.toAbsolutePath().normalize()
                + "; re-resolve them into the Gradle cache (the paths are content hashes, so the same artifact"
                + " lands at the same path) or regenerate the configuration, and re-record the pins");
    }

    /** A directory inside {@code project}'s checkout; skips the test when absent, or fails under {@link #REQUIRED_PROPERTY}. */
    public static Path requireDir(String project, String relative) {
        return require(project, dir(project).resolve(relative), "directory",
                "install it with the `corpus/` Taskfile at the repo root");
    }

    private static Path require(String project, Path path, String what, String remedy) {
        if (Files.exists(path)) return path;
        String message = "the " + project + " corpus is absent: no " + what + " at "
                         + path.toAbsolutePath().normalize() + "; " + remedy;
        if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
            // ⛔ Not an assumption: -D says this run must measure a corpus, and a skip here would be
            // reported as success by every CI that reads the build outcome.
            throw new AssertionError(message + " — and -D" + REQUIRED_PROPERTY
                                     + "=true says a corpus test may not be skipped (pass -Pcorpus.optional to skip).");
        }
        return Assumptions.abort(message);
    }
}
