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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A record accessor stays synthetic even when something calls it.
 *
 * <p>Writing {@code record Request(String model, int seed) {}} declares two methods that appear nowhere
 * in the file: {@code model()} and {@code seed()}. The compiler supplies both, so both must report
 * {@code isSynthetic() == true}. That flag is the only way a caller can tell "the compiler supplied this,
 * there is no text for it in the file" from "someone wrote this out".
 *
 * <h2>What goes wrong</h2>
 * {@code ScanCompilationUnit}, on reaching a record component, looks for an accessor that already exists:
 * <pre>
 * MethodInfo existing = typeInfo.methodStream()
 *         .filter(mi -&gt; mi.name().equals(fieldName) &amp;&amp; mi.parameters().isEmpty())
 *         .findFirst().orElse(null);
 * if (existing == null) {
 *     MethodInfo accessor = recordSynthetics.createAccessor(fieldInfo);   // sets synthetic = true
 *     …
 * } else {
 *     // A call-site resolution (ClassSymbolScanner.ensureMethod) can materialise a BARE accessor stub
 *     // for this record component — non-synthetic, empty body, no GET_SET_FIELD link — before this
 *     // record scan runs.
 *     runtime.setGetSetField(existing, fieldInfo, false, -1, false);      // does NOT set synthetic
 * }
 * </pre>
 * So when the source contains a CALL to the accessor, resolving that call can create the stub first; the
 * record scan then takes the {@code existing} branch, repairs the {@code GET_SET_FIELD} link that branch
 * was written to repair, and leaves {@code synthetic} at its default of false. The accessor is then a
 * method that reports "written out by hand" while having no source text at all.
 *
 * <p>Which branch is taken is scan-order dependent, which is why the same record reports one thing in one
 * project and the other thing in another.
 *
 * <h2>What it cost, measured</h2>
 * CodeLaser's dead-code remover asks {@code isSynthetic()} to decide whether a method has text in the file
 * that can be deleted. Told false for a supplied accessor, it asked its editor to delete the source range
 * {@code 0:0..0:0} and the whole run died:
 * <pre>
 * java.lang.AssertionError: EditCollector.remove was given beginPos=0 (a column is 1-based, so the
 * lowest legal value is 1), for the range 0:0..0:0
 * </pre>
 * on {@code io.trino.plugin.ai.functions.OpenAiClient.ChatRequest.seed()}, trino {@code 27e3b9c8d62},
 * whose one call site is {@code .setAttribute(GEN_AI_REQUEST_SEED, body.seed())} in the same file. No
 * measurement of that project could be produced at all.
 *
 * <p>{@link TestRecord#test1} covers the same record WITHOUT a call to the accessor, and passes — there
 * the {@code existing == null} branch runs and the flag is set correctly.
 */
public class TestRecordAccessorCalledIsStillSynthetic extends CommonTest {

    /** The trino shape: the record is nested in a class, and that class calls the accessors. */
    @Language("java")
    private static final String INPUT = """
            package a.b;

            public class Client {

                public String chat(String prompt) {
                    Request body = new Request(prompt, 0);
                    return body.model() + body.seed();
                }

                public record Request(String model, int seed) {
                }
            }
            """;

    @DisplayName("an accessor that is called is still supplied by the compiler, so still synthetic")
    @Test
    public void aCalledAccessorIsStillSynthetic() {
        TypeInfo client = scan("a.b.Client", INPUT);
        TypeInfo request = client.findSubType("Request");
        assertTrue(request.typeNature().isRecord());

        MethodInfo model = request.findUniqueMethod("model", 0);
        assertTrue(model.isSynthetic(),
                "a.b.Client.Request.model() appears NOWHERE in the source -- the compiler supplies it."
                + " Its source is " + model.source() + ", which carries no position, so a caller that"
                + " believes it was written out has no text to edit. body.model() is called on line 6,"
                + " and resolving that call is what makes ScanCompilationUnit take the `existing` branch"
                + " that never sets the flag.");

        MethodInfo seed = request.findUniqueMethod("seed", 0);
        assertTrue(seed.isSynthetic(),
                "the same for a.b.Client.Request.seed(); its source is " + seed.source());
    }
}
