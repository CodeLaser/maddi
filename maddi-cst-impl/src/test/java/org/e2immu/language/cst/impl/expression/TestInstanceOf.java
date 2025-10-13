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

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.Expression;
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

