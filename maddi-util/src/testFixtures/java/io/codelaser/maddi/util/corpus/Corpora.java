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
package io.codelaser.maddi.util.corpus;

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
 * Finds a test corpus on this machine: its checkout, and the {@code inputConfiguration.json} that was
 * generated from it.
 *
 * <p><b>Why there is one of these.</b> Four classes used to do this job — {@code TestOssCorpus} and
 * {@code CloneBenchCorpus} here in maddi, {@code OssCorpus} and {@code CodeLaserCorpus} in
 * jfocus-transform — two per family of corpus, one per repository. They disagreed in ways nobody
 * chose: two walked up the directory hierarchy to find a corpus and two did not, so the same corpus
 * was found by a test in one repository and silently skipped by a test in another. This class is
 * their union, it lives in the one repository every consumer already builds against, and there is now
 * nowhere else for the resolution rule to drift to.
 *
 * <h2>Two families, because they live in different places</h2>
 * <dl>
 *   <dt>{@link #oss(String)}</dt>
 *   <dd>An open-source corpus, inside a {@code test-oss} directory that holds all of them. Override the container with
 *   {@code -D}{@value #OSS_ROOT_PROPERTY} or {@value #OSS_ROOT_ENV}; otherwise the first
 *   {@code <ancestor>/test-oss} above the working directory wins.</dd>
 *
 *   <dt>{@link #codeLaser(String)}</dt>
 *   <dd>A corpus that is a repository checked out <i>beside</i> the others rather than inside a
 *   container. ⛔ WHICH ONES THOSE ARE IS NOT WRITTEN HERE, and must not be: this class is in a
 *   public repository, the names include private ones, and it needs none of them — the caller passes
 *   the name. The list lives in each catalogue directory, beside the tests that use it.
 *   Override with {@code -D}{@value #CODELASER_ROOT_PROPERTY} or {@value #CODELASER_ROOT_ENV};
 *   otherwise the first {@code <ancestor>/<name>} above the working directory wins.</dd>
 * </dl>
 *
 * <h2>The CALL decides whether absence is fatal, not the family</h2>
 * This is the one thing that changed on purpose, and it is why 80 call sites were converted by hand
 * rather than by a search and replace. Before, the class you happened to import decided: a miss in
 * {@code OssCorpus} was always a skip and a miss in {@code CodeLaserCorpus} was always a throw. Now
 * every call site says which it means:
 * <ul>
 *   <li>{@link Corpus#assumeAvailable()} and the {@code requireX} methods — <b>skip</b> the test. For
 *   a corpus that is large, optional, and legitimately absent on a contributor's machine.</li>
 *   <li>{@link Corpus#require()} — <b>fail</b>. For a corpus that is supposed to be checked out, where
 *   an absence is a broken setup and not a reason to report success.</li>
 * </ul>
 * ⛔ And {@code -D}{@value #REQUIRED_PROPERTY}{@code =true} turns every skip in here into a failure.
 * maddi's {@code slowTest} task sets it, because a skipped corpus test prints the same green as one
 * that analysed nine thousand types — which is the shape that let the clone-bench tests run against
 * nothing at all until 2026-07-21. Anything that measures corpora should set it.
 */
public final class Corpora {

    /** When true, an absent corpus fails instead of skipping. Set by maddi's {@code slowTest} task. */
    public static final String REQUIRED_PROPERTY = "maddi.corpus.required";

    /** Directory that holds every open-source corpus checkout. */
    public static final String OSS_DIRECTORY = "test-oss";
    public static final String OSS_ROOT_PROPERTY = "test.oss.root";
    public static final String OSS_ROOT_ENV = "TEST_OSS_ROOT";
    public static final String CODELASER_ROOT_PROPERTY = "test.codelaser.root";
    public static final String CODELASER_ROOT_ENV = "TEST_CODELASER_ROOT";

    /** The file the corpus commands generate beside a corpus's sources, and the only one read here. */
    public static final String INPUT_CONFIGURATION = "inputConfiguration.json";

    /**
     * ⚠ LEGACY OVERRIDES, PASSED IN BY THE CALLER. A corpus that had its own property and environment
     * variable before {@value #CODELASER_ROOT_ENV} existed keeps working, and this class does not need
     * to know which corpus that is -- naming one here would put a private corpus's name in a public
     * repository for no gain. The caller that owns the corpus names its own legacy pair.
     */
    private Corpora() {
    }

    /** Thrown by {@link Corpus#require()}: a corpus that is supposed to be present is not. */
    public static class CorpusNotFoundException extends RuntimeException {
        public CorpusNotFoundException(String message) {
            super(message);
        }
    }

    /** An open-source corpus inside the {@code test-oss} container. */
    public static Corpus oss(String name) {
        return new Corpus(name, Family.OSS, null);
    }

    /** A corpus that is a repository checked out beside the others. */
    public static Corpus codeLaser(String name) {
        return new Corpus(name, Family.CODELASER, null);
    }

    /**
     * As {@link #codeLaser(String)}, honouring one older property/environment pair that used to locate
     * this corpus before {@value #CODELASER_ROOT_ENV} existed. Consulted after the general override and
     * before the walk-up, so a machine that still exports the old variable keeps working.
     */
    public static Corpus codeLaser(String name, String legacyProperty, String legacyEnv) {
        return new Corpus(name, Family.CODELASER, new String[]{legacyProperty, legacyEnv});
    }

    /**
     * The {@code test-oss} container itself. Never null, never throws, and the path need not exist —
     * for callers that list what is on disk rather than asking about one corpus.
     */
    public static Path ossRoot() {
        String root = override(OSS_ROOT_PROPERTY, OSS_ROOT_ENV);
        if (root != null) return Path.of(root);
        Path found = walkUp(OSS_DIRECTORY);
        // The conventional relative path when nothing was found, so a skip message still names a
        // plausible location instead of an empty one.
        return found != null ? found : Path.of("..", "..", OSS_DIRECTORY);
    }

    private enum Family {OSS, CODELASER}

    private static String override(String property, String env) {
        String v = System.getProperty(property);
        if (v == null || v.isBlank()) v = System.getenv(env);
        return v == null || v.isBlank() ? null : v;
    }

    /**
     * The first {@code <ancestor>/<relative>} that is a directory, walking up from the working
     * directory, or null.
     *
     * <p>⛔ THE WALK-UP IS THE POINT, and two of the four classes this replaces did not have it. A
     * fixed {@code ../../<name>} only resolves when the working directory is a module directory exactly
     * two levels below a checkout that also holds the corpus. That is not academic: one of maddi's own
     * corpus test suites had been silently skipping for exactly this reason -- the corpus is a sibling
     * of the repositories while {@code ../../<name>} from a module directory points inside the
     * workspace. Converting those call sites to this class makes them run for the first time.
     */
    private static Path walkUp(String relative) {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(relative);
            if (Files.isDirectory(candidate)) return candidate;
        }
        return null;
    }

    /** One corpus on this machine. Nothing is resolved until you ask. */
    public static final class Corpus {
        private final String name;
        private final Family family;

        private final String[] legacy;

        private Corpus(String name, Family family, String[] legacy) {
            this.name = name;
            this.family = family;
            this.legacy = legacy;
        }

        public String name() {
            return name;
        }

        /** Where the checkout is, or would be. Never null; may not exist. */
        public Path dir() {
            if (family == Family.OSS) return ossRoot().resolve(name);
            String root = override(CODELASER_ROOT_PROPERTY, CODELASER_ROOT_ENV);
            if (root == null && legacy != null) root = override(legacy[0], legacy[1]);
            if (root != null) return Path.of(root).resolve(name);
            Path found = walkUp(name);
            return found != null ? found : Path.of("..", "..", name);
        }

        /** Whether the checkout is there. */
        public boolean available() {
            return Files.isDirectory(dir());
        }

        /** The generated input configuration beside the sources. May not exist. */
        public Path config() {
            return dir().resolve(INPUT_CONFIGURATION);
        }

        /**
         * The checkout, <b>failing</b> when it is absent — for a corpus that is supposed to be checked
         * out, where a skip would report a broken setup as success.
         */
        public Path require() {
            Path d = dir();
            if (Files.isDirectory(d)) return d;
            throw new CorpusNotFoundException(describe("no directory", d));
        }

        /** Skip the calling test unless the checkout is there. */
        public void assumeAvailable() {
            orSkip(dir(), "no directory");
        }

        /** A directory inside the checkout; skips when it is absent. */
        public Path requireDir(String relative) {
            return orSkip(dir().resolve(relative), "no directory");
        }

        /** The input configuration; skips when it is absent. */
        public Path requireConfig() {
            return orSkip(config(), "no " + INPUT_CONFIGURATION);
        }

        /**
         * {@link #requireConfig()}, and every class-path jar it names must exist too.
         *
         * <p>⛔ FOR ANY TEST THAT PINS A COUNT. The configuration names jars in the shared Gradle cache
         * by absolute path, and a jar evicted from that cache is not an error anywhere — the calls that
         * needed it simply stop resolving. coil's placeholder pin was recorded as 283 primary types
         * with {@code okio-jvm} gone from the cache; the jar came back when an unrelated build
         * re-downloaded it, and the same code then counted 208. A missing jar is treated exactly like a
         * missing corpus.
         */
        public Path requireCompleteConfig() {
            return requireClassPath(requireConfig());
        }

        /** The class-path half of {@link #requireCompleteConfig()}, for any configuration file. */
        public Path requireClassPath(Path configuration) {
            String json;
            try {
                json = Files.readString(configuration);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Matcher m = JAR_URI.matcher(json);
            List<Path> missing = m.results().map(r -> Path.of(URI.create(r.group(1))))
                    .filter(p -> !Files.exists(p)).toList();
            if (missing.isEmpty()) return configuration;
            String what = missing.size() + " class-path jar(s), the first";
            return orSkip(missing.getFirst(), what,
                    "they are named by " + configuration.toAbsolutePath().normalize()
                    + "; re-resolve them into the Gradle cache (the paths are content hashes, so the"
                    + " same artifact lands at the same path) or regenerate the configuration, and"
                    + " re-record the pins");
        }

        private Path orSkip(Path path, String what) {
            return orSkip(path, what, remedy());
        }

        private Path orSkip(Path path, String what, String remedy) {
            if (Files.exists(path)) return path;
            String message = describe(what, path) + "; " + remedy;
            if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
                // ⛔ Not an assumption: -D says this run must measure a corpus, and a skip here would
                // be reported as success by every CI that reads the build outcome.
                throw new AssertionError(message + " — and -D" + REQUIRED_PROPERTY
                                         + "=true says a corpus test may not be skipped.");
            }
            return Assumptions.abort(message);
        }

        private String describe(String what, Path path) {
            return "the '" + name + "' corpus is absent: " + what + " at "
                   + path.toAbsolutePath().normalize();
        }

        /** What to do about it — the remedy travels with the message, because whoever reads it in CI
         *  is not the person who set the machine up. */
        private String remedy() {
            if (family == Family.OSS) {
                // the corpus Taskfile lives here, in maddi (in maddi-mod from the split until 2026-10-03, see
                // docs/roadmap/split-maddi-into-three-repositories.md §7)
                return "provision it with `task corpus:ready NAME=" + name + "` in maddi, or point "
                       + OSS_ROOT_ENV + " (or -D" + OSS_ROOT_PROPERTY + ") at the corpus root";
            }
            return "check out the '" + name + "' repository beside this one (e.g. under ~/git), or point "
                   + CODELASER_ROOT_ENV + " (or -D" + CODELASER_ROOT_PROPERTY + ") at the directory holding it";
        }
    }

    private static final Pattern JAR_URI = Pattern.compile("\"uri\"\\s*:\\s*\"(file:[^\"]+?\\.jar)\"");
}
