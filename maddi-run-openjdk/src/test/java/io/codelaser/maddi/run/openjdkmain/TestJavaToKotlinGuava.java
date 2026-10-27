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
