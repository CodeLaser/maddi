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
