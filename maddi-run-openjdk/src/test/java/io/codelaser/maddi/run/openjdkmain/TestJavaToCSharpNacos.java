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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.run.j2cs.JavaToCSharpRatchet;
import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;

/**
 * Nacos's {@code api/main} translated to C# and judged by Roslyn; the numbers are held by
 * {@code src/test/resources/j2cs/nacos.ratchet}. See {@link JavaToCSharpRatchet}.
 * <p>
 * Why nacos, beside timefold, langchain4j and fernflower: application-style code, builders and value objects of a
 * service, whose wildcards bound concrete types ({@code List<? extends Instance>}) where timefold's bound type
 * parameters ({@code ? extends Solution_}), which C#'s declaration-site variance ({@code CSharpVariance}) converts
 * for reference types only. As the Kotlin trial corpus (maddi-mod's TestJavaToKotlinNacosNullability): one module
 * of 35, the others parsed, their classes on the class path.
 */
@Tag("slow")
public class TestJavaToCSharpNacos {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("nacos", "api/main", null, false, true);
        assertTrue(corpus.types().size() > 200, "only " + corpus.types().size() + " primary types");
        new JavaToCSharpRatchet("nacos", Path.of("src/test/resources/j2cs/nacos.ratchet")).run(corpus);
    }
}
