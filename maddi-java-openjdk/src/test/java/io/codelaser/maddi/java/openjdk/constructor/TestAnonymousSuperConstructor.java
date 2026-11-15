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

package io.codelaser.maddi.java.openjdk.constructor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;

/** maddi#194: the creation of an anonymous class carries the superclass constructor that runs. */
public class TestAnonymousSuperConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;

            public class X {
                static class Point {
                    Point(int x) { }
                    Point(String s) { }
                    Point(int x, int y) { }
                }
                class Inner {
                    Inner(long l) { }
                }
                Object ofInt() { return new Point(6) { }; }
                Object ofString() { return new Point("6") { }; }
                Object ofTwo() { return new Point(6, 7) { }; }
                Object ofInterface() { return new Runnable() { public void run() { } }; }
                Object ofInner(X x) { return x.new Inner(3L) { }; }
                Object plain() { return new Point(6); }
            }
            """;

    private static ConstructorCall creation(TypeInfo x, String method) {
        List<ConstructorCall> calls = new ArrayList<>();
        x.findUniqueMethod(method, method.equals("ofInner") ? 1 : 0).methodBody().visit((Element e) -> {
            if (e instanceof ConstructorCall cc) calls.add(cc);
            return true;
        });
        assertEquals(1, calls.size());
        return calls.getFirst();
    }

    @DisplayName("same arity overloads: the constructor javac resolved, not a guess")
    @Test
    public void test() {
        TypeInfo x = scan("a.b.X", INPUT);
        assertSuper("a.b.X.Point.<init>(int)", creation(x, "ofInt"));
        assertSuper("a.b.X.Point.<init>(String)", creation(x, "ofString"));
        assertSuper("a.b.X.Point.<init>(int,int)", creation(x, "ofTwo"));
        assertSuper("java.lang.Object.<init>()", creation(x, "ofInterface"));
        assertSuper("a.b.X.Inner.<init>(long)", creation(x, "ofInner"));

        ConstructorCall plain = creation(x, "plain");
        assertTrue(plain.callsConstructorOfCreatedType());
        assertFalse(plain.isAnonymousCreation());
        assertEquals("a.b.X.Point.<init>(int)", plain.constructor().fullyQualifiedName());
    }

    private static void assertSuper(String expected, ConstructorCall cc) {
        assertTrue(cc.isAnonymousCreation());
        assertFalse(cc.callsConstructorOfCreatedType(), "the created type is the anonymous class");
        MethodInfo constructor = cc.constructor();
        assertNotNull(constructor, "no superclass constructor on " + cc);
        assertEquals(expected, constructor.fullyQualifiedName());
        assertNotSame(cc.anonymousClass(), constructor.typeInfo());
    }
}
