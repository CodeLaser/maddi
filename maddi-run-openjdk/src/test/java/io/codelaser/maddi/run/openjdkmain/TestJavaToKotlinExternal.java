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

import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/*
 The KotlinConf scoreboard (CodeLaser/maddi-mod#36): a Java -> Kotlin translation made elsewhere, IntelliJ J2K's,
 judged by the ratchets' own rules (two compiles, drop-in files, class files, the corpus's tests with the Kotlin classes
 in front), so its numbers stand next to maddi's. Report only, in build/j2k/external-<corpus>/; no ratchet file.

   -Dmaddi.j2k.external=<dir>  the translation: <dir>/src holds the main .kt files by package path, <dir>/test-src the
                               translated tests if any (maddi-mod scripts/j2k-baseline/j2k.py writes src/)
   -Dmaddi.j2k.corpus=<name>   the corpus, as Corpora.oss knows it (langchain4j, fernflower, ...)
 Skipped without them.
 */
@Tag("slow")
public class TestJavaToKotlinExternal {

    @Test
    public void test() throws Exception {
        String external = System.getProperty("maddi.j2k.external");
        String corpusName = System.getProperty("maddi.j2k.corpus");
        assumeTrue(external != null && corpusName != null, "-Dmaddi.j2k.external and -Dmaddi.j2k.corpus not set");
        Path dir = Path.of(external).toAbsolutePath();
        // a translation that is not there is not a translation with no compiling file (a lab run on a machine without
        // the directory judged 0 files and reported success)
        try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(dir.resolve("src"))) {
            assertTrue(walk.anyMatch(p -> p.toString().endsWith(".kt")), "no .kt files under " + dir.resolve("src"));
        }
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse(corpusName);
        JavaToKotlinRatchet.measureOnly("external-" + corpusName)
                .judgeExternal(corpus, dir.resolve("src"), dir.resolve("test-src"));
    }
}
