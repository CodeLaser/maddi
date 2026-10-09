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
 * ⛔ THE RATCHET FOR JAVA → KOTLIN TRANSLATION, a second corpus: langchain4j-core's main sources through
 * {@link JavaToKotlinRatchet}, with no nullability verdicts. maddi-mod's TestJavaToKotlinLangchain4jNullability runs
 * the same with the verdicts of its NullabilityPass.
 * <p>
 * Why langchain4j-core: an API of builders and value objects, unlike fernflower's algorithms. Its collection fields
 * and parameters are mostly read and rarely changed, where the translation chooses Kotlin's {@code List} or
 * {@code MutableList}. Its tests (JUnit 5, Mockito, AssertJ; two kotest files, which the Jupiter launcher leaves
 * alone) run as fernflower's do.
 */
@Tag("slow")
public class TestJavaToKotlinLangchain4j {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("langchain4j");
        assertTrue(corpus.types().size() > 250, "only " + corpus.types().size() + " primary types");
        new JavaToKotlinRatchet("langchain4j", Path.of("src/test/resources/j2k/langchain4j.ratchet"))
                .run(corpus, KotlinPrintOptions.DEFAULT);
    }
}
