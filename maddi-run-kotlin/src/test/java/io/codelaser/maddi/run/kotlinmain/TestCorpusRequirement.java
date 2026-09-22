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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.run.openjdkmain.TestOssCorpus;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

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
        String previous = System.setProperty(TestOssCorpus.REQUIRED_PROPERTY, "false");
        try {
            TestAbortedException aborted = assertThrows(TestAbortedException.class,
                    () -> TestOssCorpus.requireConfig(ABSENT),
                    "a contributor without the checkouts must still get a green build");
            assertTrue(aborted.getMessage().contains(ABSENT), aborted.getMessage());
        } finally {
            restore(previous);
        }
    }

    @Test
    public void aMissingCorpusFAILSWhenItIsRequired() {
        String previous = System.setProperty(TestOssCorpus.REQUIRED_PROPERTY, "true");
        try {
            AssertionError failed = assertThrows(AssertionError.class,
                    () -> TestOssCorpus.requireConfig(ABSENT),
                    "under -D" + TestOssCorpus.REQUIRED_PROPERTY + " an absent corpus is a failure, not a skip");
            assertTrue(failed.getMessage().contains(ABSENT), failed.getMessage());
            // the remedy must travel with the failure: whoever sees this in CI is not the person who set it up
            assertTrue(failed.getMessage().contains("corpus:config:"), failed.getMessage());
        } finally {
            restore(previous);
        }
    }

    /** The same, for the directory form the elasticsearch/guava style tests use. */
    @Test
    public void theDirectoryFormIsGuardedToo() {
        String previous = System.setProperty(TestOssCorpus.REQUIRED_PROPERTY, "true");
        try {
            assertThrows(AssertionError.class, () -> TestOssCorpus.requireDir(ABSENT, "server/src/main/java"));
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) System.clearProperty(TestOssCorpus.REQUIRED_PROPERTY);
        else System.setProperty(TestOssCorpus.REQUIRED_PROPERTY, previous);
    }
}
