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

package io.codelaser.maddi.java.openjdk.translate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * A translated local class keeps its fully qualified name ('a.b.X.1$m2$It'), as a rewired one does. Rebuilt from its
 * simple name, two local classes of one name in two methods became one 'a.b.X.It', equal to neither, and the codec
 * could no longer find the one a stored summary named (hibernate's SelectableConsumer, after jfocus's loop transform).
 */
public class TestTranslateLocalClassName extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int a;
                int b;
                int m1() {
                    class It { int g() { return a; } }
                    return new It().g();
                }
                int m2(int k) {
                    class It { int g() { return a + k; } }
                    return new It().g();
                }
            }
            """;

    private static List<String> localNames(TypeInfo typeInfo) {
        return typeInfo.methods().stream()
                .map(m -> ((LocalTypeDeclaration) m.methodBody().statements().getFirst()).typeInfo().fullyQualifiedName())
                .toList();
    }

    @Test
    public void test() {
        TypeInfo X = scan("a.b.X", INPUT);
        List<String> before = localNames(X);
        assertEquals(2, before.stream().distinct().count(), before.toString());

        FieldInfo a = X.getFieldByName("a", true);
        FieldInfo b = X.getFieldByName("b", true);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(runtime.newFieldReference(a), runtime.newFieldReference(b))
                .build();
        TypeInfo translated = X.translate(tm).getFirst();
        assertNotSame(X, translated);
        assertEquals(before, localNames(translated));
    }
}
