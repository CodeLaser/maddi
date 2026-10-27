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

import io.codelaser.maddi.cst.print.csharp.CSharpLibraries;
import io.codelaser.maddi.cst.print.csharp.CSharpProgram;
import io.codelaser.maddi.run.j2cs.JavaToCSharpRatchet;
import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * langchain4j's core sources translated to C# and judged by Roslyn, the second corpus after fernflower: records,
 * builders, default methods, annotations, Optional and streams. The numbers are held by
 * {@code src/test/resources/j2cs/langchain4j.ratchet}. See {@link JavaToCSharpRatchet}. The second test translates
 * with the other options of the library mappings (slf4j as Serilog, Jackson as Json.NET), so that they keep
 * compiling: {@code langchain4j-alternatives.ratchet}.
 */
@Tag("slow")
public class TestJavaToCSharpLangchain4j {

    private static JavaToKotlinRatchet.Corpus corpus;

    private static synchronized JavaToKotlinRatchet.Corpus corpus() throws Exception {
        if (corpus == null) {
            corpus = JavaToKotlinRatchet.parse("langchain4j");
            assertTrue(corpus.types().size() > 250, "only " + corpus.types().size() + " primary types");
        }
        return corpus;
    }

    @Test
    public void test() throws Exception {
        new JavaToCSharpRatchet("langchain4j", Path.of("src/test/resources/j2cs/langchain4j.ratchet")).run(corpus());
    }

    @Test
    public void alternatives() throws Exception {
        CSharpProgram.Policy policy = CSharpProgram.Policy.DEFAULT
                .withLibraries(CSharpLibraries.choose("slf4j=serilog,jackson=newtonsoft"));
        new JavaToCSharpRatchet("langchain4j-alternatives",
                Path.of("src/test/resources/j2cs/langchain4j-alternatives.ratchet"), policy).run(corpus());
    }
}
