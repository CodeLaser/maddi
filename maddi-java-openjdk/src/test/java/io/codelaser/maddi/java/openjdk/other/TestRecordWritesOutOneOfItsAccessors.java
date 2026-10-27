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
