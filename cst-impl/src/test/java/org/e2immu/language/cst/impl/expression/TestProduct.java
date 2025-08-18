package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestProduct extends CommonTest {

    @Test
    public void test0() {
        Expression klm = r.product(k, r.sum(l, m));
        assertEquals("k*l+k*m", klm.toString());
        Expression kml = r.product(k, r.sum(m, l));
        assertEquals("k*l+k*m", kml.toString());
        Expression ml2 = r.product(r.sum(m, l), r.newInt(2));
        assertEquals("2*l+2*m", ml2.toString());
    }

    @Test
    public void test1() {
        Expression s1 = r.sum(r.product(k, l), i);
        assertEquals("i+k*l", s1.toString());
        Expression p1 = r.product(n, m);
        assertEquals("m*n", p1.toString());

        Expression s2 = r.sum(k, r.newInt(2));
        assertEquals("2+k", s2.toString());

        Expression all = r.sum(r.sum(s1, s2), p1);
        assertEquals("2+i+k+k*l+m*n", all.toString());
        Expression product = r.product(all, all);
        assertEquals(140, product.complexity());
        assertEquals("4+4*i+i*i+4*k+k*k+2*i*k+4*k*l+k*l*k*l+2*k*k*l+4*m*n+m*n*m*n+2*i*k*l+2*i*m*n+2*k*m*n+2*k*l*m*n",
                product.toString());
    }

    @Test
    public void test2() {
        Expression all = r.sum(r.sum(r.sum(r.product(k, l), i), r.sum(k, r.newInt(2))), m);
        assertEquals("2+i+k+m+k*l", all.toString());
        Expression product = r.product(all, all);
        assertEquals(122, product.complexity());
        assertEquals("4+4*i+i*i+4*k+k*k+4*m+m*m+2*i*k+2*i*m+4*k*l+k*l*k*l+2*k*k*l+2*k*m+2*i*k*l+2*m*k*l",
                product.toString());
    }

    @Test
    public void test3() {
        assertEquals("Type Double", dd.variable().parameterizedType().toString());
        Expression times100 = r.product(dd, r.newInt(100));
        assertEquals("100*dd", times100.toString());
        assertEquals("Type double", times100.parameterizedType().toString());
    }
}
