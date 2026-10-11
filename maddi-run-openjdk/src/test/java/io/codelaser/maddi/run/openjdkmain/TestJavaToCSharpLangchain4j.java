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
