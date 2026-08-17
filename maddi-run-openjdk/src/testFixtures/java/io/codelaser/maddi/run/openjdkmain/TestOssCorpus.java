package io.codelaser.maddi.run.openjdkmain;

import java.nio.file.Path;

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
 * Each corpus test does {@code assumeTrue(Files.exists(config(...)))}, so it skips cleanly when the
 * corpus — or its locally generated {@code inputConfiguration.json} — is absent. No machine-specific
 * path, no symlink: the input configuration is generated on the machine that runs the test, so the
 * absolute paths it contains are that machine's own.
 * <p>
 * It lives in <b>test fixtures</b> rather than this module's test scope, for the same reason
 * {@code CloneBenchCorpus} does: more than one module's corpus tests need it, and they are not all
 * downstream of each other. {@code maddi-run-kotlin}'s Kotlin corpus tests (see
 * {@code TestCoilJvmSlice}) resolve the same root, and cannot see this module's test classes.
 */
public final class TestOssCorpus {
    private TestOssCorpus() {
    }

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
}
