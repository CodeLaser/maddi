package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.StringConcat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestStringConcat extends CommonTest {
    @Test
    public void test0() {
        Expression e1 = make(r.negate(a));
        assertEquals("\"abc\"+!a", e1.toString());
        Expression e2 = make(r.negate(k));
        assertEquals("\"abc\"+-k", e2.toString());
        Expression e3 = r.binaryOperator((BinaryOperator) e2);
        assertEquals("\"abc\"+-k", e3.toString());
        assertInstanceOf(StringConcat.class, e3);
    }

    private Expression make(Expression rhs) {
        return r.newBinaryOperatorBuilder()
                .setParameterizedType(r.stringParameterizedType())
                .setOperator(r.plusOperatorString())
                .setLhs(r.newStringConstant("abc"))
                .setRhs(rhs)
                .setPrecedence(r.precedenceAdditive()).build();
    }

}
