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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInstanceOf extends CommonTest {


    @Test
    public void test() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        assertEquals("a instanceof Boolean", e1.toString());
        assertTrue(r.sortAndSimplify(false, e1).isBoolValueTrue());

        Expression e2 = newInstanceOf(a, r.newParameterizedType(r.integerTypeInfo(), 0));
        assertEquals("a instanceof Integer", e2.toString());
        assertTrue(r.sortAndSimplify(false, e2).isBoolValueFalse());
    }

    @Test
    public void test2() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        assertEquals("a instanceof Boolean", e1.toString());

        // object simply disappears!
        Expression e2 = newInstanceOf(a, r.newParameterizedType(r.objectTypeInfo(), 0));
        assertEquals("a instanceof Object", e2.toString());
        assertTrue(r.sortAndSimplify(false, e2).isBoolValueTrue());

        assertEquals("a instanceof Boolean", r.and(e1, e2).toString());

        // not object is not possible
        assertEquals("false", r.and(e1, r.negate(e2)).toString());
    }

    @Test
    public void test3() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.objectTypeInfo(), 0));
        assertEquals("a instanceof Object", e1.toString());

        // object simply disappears!
        Expression e2 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        assertEquals("a instanceof Boolean", r.and(e1, e2).toString());

        // a instanceof Object remains
        // the negation is expanded!
        assertEquals("a instanceof Object&&(null==a||!(a instanceof Boolean))", r.and(e1, r.negate(e2)).toString());
    }

    private static final String AND_OF_ORS = "(null==a||!(a instanceof Boolean))&&(null==a||!(a instanceof Character))";

    @Test
    public void test4() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        Expression e2 = newInstanceOf(a, r.newParameterizedType(r.boxed(r.charTypeInfo()), 0));

        Expression or1 = r.or(r.negate(e1), r.equals(r.nullConstant(), a));
        assertEquals("null==a||!(a instanceof Boolean)", or1.toString());
        Expression or2 = r.or(r.negate(e2), r.equals(r.nullConstant(), a));
        assertEquals("null==a||!(a instanceof Character)", or2.toString());

        assertEquals(10, or1.complexity());
        Expression and = r.and(or1, or2);
        assertEquals(AND_OF_ORS, and.toString());

        Expression and1 = r.and(r.negate(e1), r.negate(e2));
        assertEquals("(null==a||!(a instanceof Boolean))&&(null==a||!(a instanceof Character))", and1.toString());
        Expression or3 = r.or(and1, r.equals(r.nullConstant(), a));
        assertEquals(AND_OF_ORS, or3.toString());
    }

    @Test
    public void test5() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        Expression e2 = r.negate(r.equals(r.nullConstant(), a));
        Expression and = r.and(e2, e1);
        assertEquals("a instanceof Boolean", and.toString());
    }

    @Test
    public void test6() {
        Expression e1 = newInstanceOf(a, r.newParameterizedType(r.boxedBooleanTypeInfo(), 0));
        Expression e2 = r.equals(r.nullConstant(), a);
        Expression and = r.and(e2, e1);
        assertEquals("false", and.toString());
    }
}

