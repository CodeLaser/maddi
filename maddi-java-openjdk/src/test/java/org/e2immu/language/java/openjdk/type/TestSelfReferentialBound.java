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

package org.e2immu.language.java.openjdk.type;

import org.assertj.core.api.AbstractAssert;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
Captures bug (b): a self-referential ("F-bounded") generic bound that mentions more than one of the
type's parameters is inspected with too few type arguments. This is what derails
ParameterizedType.concreteSuperType / IsAssignableFrom2 on the langchain4j

  abstract class GuardrailResultAssert<A extends GuardrailResultAssert<A, R, F>,
                                       R extends GuardrailResult<R>,
                                       F extends GuardrailResult.Failure>

where the bound of A came out as GuardrailResultAssert<A> (one argument) instead of three.

The two tests below localise the defect: the source-parsed case is handled correctly, while the
bytecode (ASM signature) path drops the extra type arguments of a self-referential bound.
*/
public class TestSelfReferentialBound extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            class X {
                static class GuardrailResult<R extends GuardrailResult<R>> {
                    static class Failure { }
                }
                abstract static class AbstractObjectAssert<S, A2> {
                }
                abstract static class GuardrailResultAssert<
                        A extends GuardrailResultAssert<A, R, F>,
                        R extends GuardrailResult<R>,
                        F extends GuardrailResult.Failure>
                        extends AbstractObjectAssert<A, R> {
                }
            }
            """;

    // CONTROL: the source parser keeps all three type arguments of the self-referential bound. Passes.
    @Test
    public void sourceParsedBoundKeepsAllArguments() {
        TypeInfo X = scan("a.b.X", INPUT);
        TypeInfo assertType = X.findSubType("GuardrailResultAssert");
        List<TypeParameter> typeParameters = assertType.typeParameters();
        assertEquals(3, typeParameters.size()); // A, R, F

        ParameterizedType boundOfA = typeParameters.getFirst().typeBounds().getFirst();
        assertSame(assertType, boundOfA.typeInfo());
        assertEquals(3, boundOfA.parameters().size(),
                "source: bound of A should be GuardrailResultAssert<A, R, F>, but was " + boundOfA.detailedString());
    }

    /*
    CAPTURE: the same shape loaded from bytecode. AssertJ's AbstractAssert is declared

      public abstract class AbstractAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL>

    so the bound of SELF is AbstractAssert<SELF, ACTUAL> -- two type arguments. Bug (b) makes the
    bytecode inspector keep only the first, producing AbstractAssert<SELF>. This test asserts the
    correct shape and currently fails, pinning the defect to the bytecode signature parser.
    */
    @Language("java")
    public static final String INPUT_ASSERTJ = """
            package a.b;
            import org.assertj.core.api.AbstractAssert;
            class Y {
                AbstractAssert<?, ?> field;
            }
            """;

    @Test
    public void byteCodeBoundKeepsAllArguments() {
        // parse a snippet that references AbstractAssert, so the compiled type is loaded
        TypeInfo Y = scan("A.b.Y", INPUT_ASSERTJ);
        TypeInfo abstractAssert = Y.getFieldByName("field", true).type().typeInfo();
        assertNotNull(abstractAssert);
        assertEquals(AbstractAssert.class.getCanonicalName(), abstractAssert.fullyQualifiedName());
        List<TypeParameter> typeParameters = abstractAssert.typeParameters();
        assertEquals(2, typeParameters.size()); // SELF, ACTUAL

        TypeParameter self = typeParameters.getFirst();
        List<ParameterizedType> bounds = self.typeBounds();
        assertEquals(1, bounds.size());
        ParameterizedType bound = bounds.getFirst();
        assertSame(abstractAssert, bound.typeInfo(), "the bound of SELF is AbstractAssert itself");

        assertEquals(2, bound.parameters().size(),
                "bytecode: bound of SELF should be AbstractAssert<SELF, ACTUAL> with two type arguments, but was "
                + bound.detailedString());
        assertSame(typeParameters.get(0), bound.parameters().get(0).typeParameter());
        assertSame(typeParameters.get(1), bound.parameters().get(1).typeParameter());
    }
}
