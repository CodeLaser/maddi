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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseMethodReference extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
                interface I {
                   String map(C c);
                }
                Function<C, String> mapper(I i) {
                    return i::map;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        TypeInfo i = typeInfo.findSubType("I");
        assertSame(typeInfo, i.primaryType());
        MethodInfo map = i.findUniqueMethod("map", 1);
        assertSame(map, i.singleAbstractMethod());

        MethodInfo mapper = typeInfo.findUniqueMethod("mapper", 1);
        if (mapper.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodReference mr) {
            assertEquals("i::map", mr.toString());
            if (mr.scope() instanceof VariableExpression ve) {
                assertEquals("i", ve.variable().simpleName());
            } else fail();
            assertSame(map, mr.methodInfo());
            assertEquals("Type String", mr.concreteReturnType().toString());
            assertEquals("[Type a.b.C]", mr.concreteParameterTypes().toString());
            assertEquals("Type java.util.function.Function<a.b.C,String>", mr.parameterizedType().toString());
        } else fail();
    }
}
