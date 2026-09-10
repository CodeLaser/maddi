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
 * A record may write out one of its accessors itself, and then no second one is supplied.
 *
 * <h2>The construct</h2>
 * Java allows a record to give an explicit body to any of its accessors:
 * <pre>
 * record ChatRequest(String model, int seed) {
 *     &#64;Override
 *     public int seed() {
 *         return seed &lt; 0 ? 0 : seed;
 *     }
 * }
 * </pre>
 * That declaration REPLACES the one the compiler would otherwise supply (JLS 8.10.3): the record has
 * exactly one {@code seed()} method, the one written in the file. {@code model()} is still supplied.
 *
 * <h2>What goes wrong</h2>
 * {@code ParseTypeDeclaration} builds an accessor for EVERY record component, with no check for one the
 * record already declares:
 * <pre>
 * createAccessors(rs, recordFields).forEach(accessor -&gt; {
 *     builder.addMethod(accessor);
 *     context.resolver().addRecordAccessor(accessor);
 * });
 * …
 * private List&lt;MethodInfo&gt; createAccessors(RecordSynthetics rs, List&lt;RecordField&gt; recordFields) {
 *     return recordFields.stream().map(rf -&gt; rs.createAccessor(rf.fieldInfo())).toList();
 * }
 * </pre>
 * So the type ends up holding the written {@code seed()} AND a supplied {@code seed()}, and committing
 * the type fails outright:
 * <pre>
 * java.lang.AssertionError: Two methods with the same FQN and return type?
 *     a.b.ChatRequest.seed() vs a.b.ChatRequest.seed()
 *     at MethodMapImpl.addToReturn(MethodMapImpl.java:85)
 *     at TypeInspectionImpl$Builder.commit(TypeInspectionImpl.java:344)
 *     at ResolverImpl.resolve(ResolverImpl.java:190)
 * </pre>
 * The record cannot be parsed at all -- not a wrong answer, a failed parse.
 *
 * <p>Compare {@code createSyntheticConstructor} immediately above it, which DOES guard:
 * {@code if (!haveConstructorMatchingFields(builder, recordFields))}. The accessors have no such guard.
 *
 * <p>The openjdk front end does not have this defect: {@code ClassSymbolScanner} looks for an existing
 * method of that name and arity before creating one, and its counterpart test
 * {@code io.codelaser.maddi.java.openjdk.other.TestRecordWritesOutOneOfItsAccessors} passes.
 *
 * <p>Found while investigating why a supplied accessor reported {@code isSynthetic() == false}; see
 * {@link TestRecordAccessorIsSynthetic}. No corpus has hit this one yet, because CodeLaser parses
 * through the openjdk front end.
 */
public class TestRecordWritesOutOneOfItsAccessors extends CommonTestParse {

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
        TypeInfo request = parse(INPUT);
        assertTrue(request.typeNature().isRecord());

        // findUniqueMethod throws when there is more than one; that is the duplicate check.
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
