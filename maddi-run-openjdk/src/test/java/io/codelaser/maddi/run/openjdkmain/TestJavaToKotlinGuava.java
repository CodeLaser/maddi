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

package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.cst.print.kotlin.KotlinPrintOptions;
import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ THE RATCHET FOR JAVA → KOTLIN TRANSLATION, a third corpus: guava's main sources through
 * {@link JavaToKotlinRatchet}, with no nullability verdicts.
 * <p>
 * Why guava: generic collections, caches and concurrency utilities, about twice langchain4j-core's size, with a test
 * suite of over 860,000 JUnit 3 and 4 tests (guava-tests), which the vintage engine runs one package at a time. Not
 * here: the test sources are translated and compiled to class files, the tests do not run.
 */
@Tag("slow")
public class TestJavaToKotlinGuava {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("guava");
        assertTrue(corpus.types().size() > 500, "only " + corpus.types().size() + " primary types");
        // the tests' counts would be reported only, and their three passes take over an hour: maddi-mod's
        // TestJavaToKotlinGuavaNullability runs and ratchets them
        new JavaToKotlinRatchet("guava", Path.of("src/test/resources/j2k/guava.ratchet"), JavaToKotlinRatchet.TESTS,
                false).run(corpus, KotlinPrintOptions.DEFAULT);
    }
}
