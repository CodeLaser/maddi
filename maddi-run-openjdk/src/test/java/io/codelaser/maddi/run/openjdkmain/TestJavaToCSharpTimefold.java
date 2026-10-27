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
 * score types, functional interfaces of up to five arguments), where fernflower and langchain4j are not. Without
 * Gizmo, Quarkus's bytecode generation, which .NET has no counterpart for: {@code timefold-gizmo.csmap}.
 */
@Tag("slow")
// Only READS the shared timefold checkout; as maddi-mod's TestTimefoldSolver, kept apart from the tests that rewrite it.
@Tag("corpus-timefold-solver")
public class TestJavaToCSharpTimefold {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("timefold-solver");
        assertTrue(corpus.types().size() > 1500, "only " + corpus.types().size() + " primary types");
        new JavaToCSharpRatchet("timefold", Path.of("src/test/resources/j2cs/timefold.ratchet"))
                .withMapping("timefold-gizmo.csmap").run(corpus);
    }
}
