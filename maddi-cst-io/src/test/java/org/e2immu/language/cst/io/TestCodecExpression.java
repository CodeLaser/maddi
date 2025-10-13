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

package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.expression.EnclosedExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestCodecExpression extends CommonTest {

    @DisplayName("field reference")
    @Test
    public void test1() {
        context.push(typeInfo);

        String encoded = "[\"F\",[\"Ta.b.C\",\"Ff(0)\"]]";
        FieldReference fr = runtime.newFieldReference(f);
        assertEquals(encoded, codec.encodeVariable(context, fr).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(fr, codec.decodeVariable(context, d));
    }

    @DisplayName("field reference with local variable scope in enclosed expression")
    @Test
    public void test2() {
        context.push(typeInfo);
        LocalVariable lv = runtime.newLocalVariable("lv", typeInfo.asParameterizedType());
        VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(lv).setSource(runtime.parseSourceFromCompact2("2-3:4-5")).build();
        EnclosedExpression ee = runtime.newEnclosedExpressionBuilder().setSource(runtime.parseSourceFromCompact2("1-2:3-4")).setExpression(scope).build();
        FieldReference fr = runtime.newFieldReference(f, ee, f.type());
        assertEquals("(lv).f", fr.toString());

        String encoded = """
                ["F",["Ta.b.C","Ff(0)"],["enclosedExpression","1-2:3-4",["variableExpression","2-3:4-5",["L","lv","Ta.b.C"]]]]\
                """;
        assertEquals(encoded, codec.encodeVariable(context, fr).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(fr, codec.decodeVariable(context, d));
    }
}
