/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.util.corpus.Corpora;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ The guard that makes every other corpus test's green mean something. A corpus test that SKIPS because
 * its checkout is absent reports the same build outcome as one that analysed 9,000 types — and `slowTest`
 * exists to measure corpora, so it sets {@code maddi.corpus.required} and a missing corpus fails there.
 *
 * <p>⚠ This is the identity check for that mechanism, not a corpus test: it is deliberately NOT tagged
 * "slow" and needs no checkout. Without it the requirement could quietly stop working — the very failure
 * mode it exists to prevent, one level up.
 */
public class TestCorpusRequirement {

    private static final String ABSENT = "no-such-corpus-xyzzy";

    @Test
    public void aMissingCorpusSkipsWhenItIsNotRequired() {
        String previous = System.setProperty(Corpora.REQUIRED_PROPERTY, "false");
        try {
            TestAbortedException aborted = assertThrows(TestAbortedException.class,
                    () -> Corpora.oss(ABSENT).requireConfig(),
                    "a contributor without the checkouts must still get a green build");
            assertTrue(aborted.getMessage().contains(ABSENT), aborted.getMessage());
        } finally {
            restore(previous);
        }
    }

    @Test
    public void aMissingCorpusFAILSWhenItIsRequired() {
        String previous = System.setProperty(Corpora.REQUIRED_PROPERTY, "true");
        try {
            AssertionError failed = assertThrows(AssertionError.class,
                    () -> Corpora.oss(ABSENT).requireConfig(),
                    "under -D" + Corpora.REQUIRED_PROPERTY + " an absent corpus is a failure, not a skip");
            assertTrue(failed.getMessage().contains(ABSENT), failed.getMessage());
            // the remedy must travel with the failure: whoever sees this in CI is not the person who set it up
            // -- the one task that takes a corpus from nothing to configured, in the repository that holds the corpus
            // Taskfile since the maddi split
            assertTrue(failed.getMessage().contains("task corpus:ready NAME=" + ABSENT + "` in maddi"),
                    failed.getMessage());
        } finally {
            restore(previous);
        }
    }

    /** The same, for the directory form the elasticsearch/guava style tests use. */
    @Test
    public void theDirectoryFormIsGuardedToo() {
        String previous = System.setProperty(Corpora.REQUIRED_PROPERTY, "true");
        try {
            assertThrows(AssertionError.class, () -> Corpora.oss(ABSENT).requireDir("server/src/main/java"));
        } finally {
            restore(previous);
        }
    }

    /**
     * A configuration whose class path names a jar that is gone: the corpus is incomplete, and a pinned count taken
     * from it measures the Gradle cache. The control is the same file with the jar present, which must pass.
     */
    @Test
    public void aMissingClassPathJarFailsWhenRequired(@TempDir Path tmp) throws Exception {
        Path present = Files.createFile(tmp.resolve("present-1.0.jar"));
        Path config = tmp.resolve("inputConfiguration.json");
        String template = "{\"classPathParts\": [{\"name\": \"x\", \"uri\": \"%s\"}]}";
        String previous = System.setProperty(Corpora.REQUIRED_PROPERTY, "true");
        try {
            Files.writeString(config, template.formatted(present.toUri()));
            assertEquals(config, Corpora.oss(ABSENT).requireClassPath(config), "the control: every jar present");

            Files.writeString(config, template.formatted(tmp.resolve("evicted-2.0.jar").toUri()));
            AssertionError failed = assertThrows(AssertionError.class,
                    () -> Corpora.oss(ABSENT).requireClassPath(config));
            assertTrue(failed.getMessage().contains("evicted-2.0.jar"), failed.getMessage());
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) System.clearProperty(Corpora.REQUIRED_PROPERTY);
        else System.setProperty(Corpora.REQUIRED_PROPERTY, previous);
    }
}
