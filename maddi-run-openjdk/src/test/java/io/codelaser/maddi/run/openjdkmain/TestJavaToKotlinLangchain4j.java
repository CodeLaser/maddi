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
        new JavaToKotlinRatchet("langchain4j", Path.of("src/test/resources/j2k/langchain4j.ratchet"),
                JavaToKotlinRatchet.TESTS)
                .run(corpus, KotlinPrintOptions.DEFAULT);
    }
}
