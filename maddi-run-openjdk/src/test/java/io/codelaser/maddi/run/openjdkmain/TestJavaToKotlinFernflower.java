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
 * ⛔ THE RATCHET FOR JAVA → KOTLIN TRANSLATION, the printer alone: fernflower's main sources through
 * {@link JavaToKotlinRatchet} (the class comment there says what is measured and how), with no nullability
 * verdicts: every declaration is non-null, as Kotlin reads a Java platform type. maddi-mod's
 * TestJavaToKotlinFernflowerNullability runs the same with the verdicts of its NullabilityPass.
 * <p>
 * Why fernflower: 199 primary types, 46.7k lines of real, old-style Java, and no dependency beyond the JDK and
 * JetBrains' annotations, so every compile error is the translation's and none is a missing library.
 * <p>
 * ⚠ No prepwork. The printer's getter/setter collapse and primary-constructor reconstruction need it, and it is
 * moving from maddi-mod to maddi (2026-10). When it lands, run it here before printing: the numbers move, and the
 * ratchet records that like any other improvement.
 */
@Tag("slow")
public class TestJavaToKotlinFernflower {

    @Test
    public void test() throws Exception {
        JavaToKotlinRatchet.Corpus corpus = JavaToKotlinRatchet.parse("fernflower");
        assertTrue(corpus.types().size() > 150, "only " + corpus.types().size() + " primary types");
        new JavaToKotlinRatchet("fernflower", Path.of("src/test/resources/j2k/fernflower.ratchet"),
                JavaToKotlinRatchet.TESTS)
                .run(corpus, KotlinPrintOptions.DEFAULT);
    }
}
