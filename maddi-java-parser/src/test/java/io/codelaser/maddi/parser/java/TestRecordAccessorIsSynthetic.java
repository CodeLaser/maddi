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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An accessor that the compiler supplies for a record component reports itself as synthetic.
 *
 * <p>Writing {@code record ChatRequest(String model, int seed) {}} declares two methods that appear
 * nowhere in the file: {@code model()} and {@code seed()}. Their bodies are supplied by the compiler.
 * {@link io.codelaser.maddi.inspection.api.util.RecordSynthetics#createAccessor} builds them, gives each
 * {@code runtime.noSource()} as its source, and calls {@code setSynthetic(true)} on the builder.
 *
 * <p>An accessor a record WRITES OUT itself is a different thing: it has real source text, and it must
 * report {@code isSynthetic() == false}, because deleting it from the file is a meaningful edit.
 *
 * <p>Why this matters to a caller: the only way to tell the two apart is {@code isSynthetic()}. A caller
 * that has a {@link MethodInfo} and wants to edit the file it came from has nothing else to go on — the
 * supplied accessor has no source text, so there is no range to delete. CodeLaser's dead-code remover
 * asked to delete the range {@code 0:0..0:0} for a supplied accessor and died:
 * <pre>
 * java.lang.AssertionError: EditCollector.remove was given beginPos=0 (a column is 1-based, so the
 * lowest legal value is 1), for the range 0:0..0:0
 * </pre>
 * for {@code io.trino.plugin.ai.functions.OpenAiClient.ChatRequest.seed()}, on trino {@code 27e3b9c8d62}.
 * The engine had checked {@code isSynthetic()} and been told {@code false}.
 */
public class TestRecordAccessorIsSynthetic extends CommonTestParse {

    @Language("java")
    private static final String NESTED = """
            package a.b;

            public class Client {

              public record Request(String model, int seed) {
              }
            }
            """;

    @DisplayName("the same, for a record NESTED IN A CLASS -- the shape trino and CodeLaser hit")
    @Test
    public void theSuppliedAccessorOfANestedRecordIsSynthetic() {
        TypeInfo client = parse(NESTED);
        TypeInfo request = client.findSubType("Request");
        assertTrue(request.typeNature().isRecord());

        MethodInfo supplied = request.findUniqueMethod("model", 0);
        assertTrue(supplied.isSynthetic(),
                "`model()` appears nowhere in the source of a.b.Client.Request. Its source is "
                + supplied.source() + ". CodeLaser was told isSynthetic()=false for exactly this shape,"
                + " io.trino...OpenAiClient.ChatRequest.seed(), and tried to delete text that is not there.");
    }

    @Language("java")
    private static final String INPUT = """
            package a.b;

            record ChatRequest(String model, int seed) {

              public int doubled() {
                return seed * 2;
              }
            }
            """;

    @DisplayName("a record accessor supplied by the compiler is synthetic; one written out is not")
    @Test
    public void theSuppliedAccessorIsSynthetic() {
        TypeInfo typeInfo = parse(INPUT);
        assertTrue(typeInfo.typeNature().isRecord());

        MethodInfo supplied = typeInfo.findUniqueMethod("model", 0);
        assertTrue(supplied.isSynthetic(),
                "`model()` appears nowhere in the source -- the compiler supplies it, and"
                + " RecordSynthetics.createAccessor calls setSynthetic(true) when building it."
                + " Its source is " + supplied.source() + ", which carries no position, so a caller that"
                + " believes this method was written out has no text to edit.");

        MethodInfo written = typeInfo.findUniqueMethod("doubled", 0);
        assertFalse(written.isSynthetic(),
                "CONTROL: `doubled()` IS written out in the record body, so it must not be reported as"
                + " supplied by the compiler. Its source is " + written.source());
    }
}
