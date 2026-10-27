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

package io.codelaser.maddi.cst.io;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestCodecExpression extends CommonTest {

    @DisplayName("field reference")
    @Test
    public void test1() {
        context.push(new Codec.TypeAndSorted(typeInfo));

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
