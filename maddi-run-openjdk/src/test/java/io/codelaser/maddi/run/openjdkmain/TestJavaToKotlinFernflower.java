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
        new JavaToKotlinRatchet("fernflower", Path.of("src/test/resources/j2k/fernflower.ratchet"))
                .run(corpus, KotlinPrintOptions.DEFAULT);
    }
}
