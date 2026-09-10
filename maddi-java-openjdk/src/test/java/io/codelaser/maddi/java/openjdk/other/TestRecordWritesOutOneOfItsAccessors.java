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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A record may write out one of its accessors itself, and then no second one is supplied.
 *
 * <p>Java allows a record to give an explicit body to any of its accessors. When it does, that
 * declaration REPLACES the one the compiler would otherwise supply -- there is exactly one
 * {@code seed()} method, the one in the file. The others are still supplied as usual.
 *
 * <p>This is the counterpart of {@link TestRecordAccessorCalledIsStillSynthetic}: there, nobody writes
 * the accessor and it must report {@code isSynthetic() == true}; here, someone does, and it must report
 * {@code false}, because deleting it from the file is a meaningful edit.
 *
 * <p>Written to establish whether the duplicate-accessor defect seen in {@code maddi-java-parser} --
 * where {@code ParseTypeDeclaration.createAccessors} builds an accessor for EVERY record component with
 * no check for one already declared, and the type then holds two methods with the same signature -- is
 * also present in this front end.
 */
public class TestRecordWritesOutOneOfItsAccessors extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;

            public record ChatRequest(String model, int seed) {

                @Override
                public int seed() {
                    return seed < 0 ? 0 : seed;
                }
            }
            """;

    @DisplayName("an accessor written out by the record replaces the supplied one, rather than joining it")
    @Test
    public void theWrittenAccessorReplacesTheSuppliedOne() {
        TypeInfo request = scan("a.b.ChatRequest", INPUT);
        assertTrue(request.typeNature().isRecord());

        // findUniqueMethod throws if there is more than one; that is the duplicate check.
        MethodInfo seed = request.findUniqueMethod("seed", 0);
        assertFalse(seed.isSynthetic(),
                "a.b.ChatRequest.seed() IS written out, with the body `return seed < 0 ? 0 : seed;`, so it"
                + " must not be reported as supplied by the compiler. Its source is " + seed.source());

        MethodInfo model = request.findUniqueMethod("model", 0);
        assertTrue(model.isSynthetic(),
                "CONTROL: model() is not written out, so it is still supplied. Its source is "
                + model.source());
    }
}
