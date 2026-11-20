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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;


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

    @DisplayName("byte and short constants (CodeLaser/maddi-mod#110)")
    @Test
    public void test3() {
        context.push(typeInfo);
        for (Expression e : List.<Expression>of(
                runtime.newByte(List.of(), runtime.parseSourceFromCompact2("1-2:3-4"), (byte) -1),
                runtime.newShort(List.of(), runtime.parseSourceFromCompact2("1-2:3-4"), (short) 300))) {
            String encoded = codec.encodeExpression(context, e).toString();
            Expression decoded = codec.decodeExpression(context, makeD(encoded));
            assertEquals(e, decoded, encoded);
        }
    }

    @DisplayName("array creation: its synthetic constructor is written as the array type (CodeLaser/maddi-mod#110)")
    @Test
    public void test4() {
        context.push(typeInfo);
        var arrayType = runtime.newParameterizedType(runtime.intTypeInfo(), 1);
        ConstructorCall cc = runtime.newConstructorCallBuilder()
                .setSource(runtime.parseSourceFromCompact2("1-2:3-4"))
                .setConstructor(runtime.newArrayCreationConstructor(arrayType))
                .setDiamond(runtime.diamondNo())
                .setConcreteReturnType(arrayType)
                .setParameterExpressions(List.of(runtime.newInt(3)))
                .build();
        String encoded = codec.encodeExpression(context, cc).toString();
        ConstructorCall decoded = (ConstructorCall) codec.decodeExpression(context, makeD(encoded));
        assertTrue(decoded.constructor().isSyntheticArrayConstructor(), encoded);
        assertEquals(arrayType, decoded.constructor().returnType());
        assertEquals(cc.toString(), decoded.toString());
        assertEquals(encoded, codec.encodeExpression(context, decoded).toString());
    }

    @DisplayName("array creation with an initializer: the absent dimension and the initializer (CodeLaser/maddi-mod#110)")
    @Test
    public void test5() {
        context.push(typeInfo);
        var arrayType = runtime.newParameterizedType(runtime.intTypeInfo(), 1);
        var initializer = runtime.newArrayInitializerBuilder()
                .setSource(runtime.parseSourceFromCompact2("1-5:1-9"))
                .setCommonType(runtime.intParameterizedType())
                .setExpressions(List.of(runtime.newInt(1), runtime.newInt(2)))
                .build();
        ConstructorCall cc = runtime.newConstructorCallBuilder()
                .setSource(runtime.parseSourceFromCompact2("1-2:3-4"))
                .setConstructor(runtime.newArrayCreationConstructor(arrayType))
                .setDiamond(runtime.diamondNo())
                .setConcreteReturnType(arrayType)
                .setParameterExpressions(List.of(runtime.newEmptyExpression()))
                .setArrayInitializer(initializer)
                .build();
        String encoded = codec.encodeExpression(context, cc).toString();
        ConstructorCall decoded = (ConstructorCall) codec.decodeExpression(context, makeD(encoded));
        assertEquals(initializer, decoded.arrayInitializer());
        assertEquals(initializer.parameterizedType(), decoded.arrayInitializer().parameterizedType());
        assertEquals(cc.parameterExpressions(), decoded.parameterExpressions());
        assertEquals(encoded, codec.encodeExpression(context, decoded).toString());
    }
}
