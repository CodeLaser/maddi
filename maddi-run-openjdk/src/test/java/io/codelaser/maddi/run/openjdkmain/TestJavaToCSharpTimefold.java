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

import io.codelaser.maddi.run.j2cs.JavaToCSharpRatchet;
import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * timefold-solver's core/main translated to C# and judged by Roslyn; the numbers are held by
 * {@code src/test/resources/j2cs/timefold.ratchet}. See {@link JavaToCSharpRatchet}. The large corpus: the reactor
 * parses whole (65 source sets), its first main source set, core/main, is translated. Generics-heavy (F-bounded
 * score types, functional interfaces of up to five arguments), where fernflower and langchain4j are not.
 */
@Tag("slow")
// Only READS the shared timefold checkout; as maddi-mod's TestTimefoldSolver, kept apart from the tests that rewrite it.
@Tag("corpus-timefold-solver")
public class TestJavaToCSharpTimefold {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("timefold-solver");
        assertTrue(corpus.types().size() > 1500, "only " + corpus.types().size() + " primary types");
        new JavaToCSharpRatchet("timefold", Path.of("src/test/resources/j2cs/timefold.ratchet")).run(corpus);
    }
}
