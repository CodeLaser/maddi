package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAndOr extends CommonTest {

    @Test
    public void test() {
        Expression nx1 = r.negate(r.equals(r.newChar('!'), x));
        Expression nx2 = r.negate(r.equals(r.newChar('<'), x));
        Expression nx3 = r.negate(r.equals(r.newChar('>'), x));
        Expression nx4 = r.negate(r.equals(r.newChar('='), x));
        Expression nx5 = r.negate(r.equals(r.newChar('~'), x));
        Expression nx6 = r.negate(r.equals(r.newChar('^'), x));

        Expression x1 = r.equals(r.newChar('!'), x);
        Expression x2 = r.equals(r.newChar('<'), x);
        Expression x3 = r.equals(r.newChar('>'), x);
        Expression x4 = r.equals(r.newChar('='), x);
        Expression x5 = r.equals(r.newChar('~'), x);
        Expression x7 = r.equals(r.newChar('&'), x);

        Expression and = r.and(nx1, nx2, nx3, nx4, nx5, nx6);
        assertEquals("'!'!=x&&'<'!=x&&'='!=x&&'>'!=x&&'^'!=x&&'~'!=x", and.toString());

        Expression or = r.or(x1, x2, x3, x4, x5, x7);
        assertEquals("'!'==x||'&'==x||'<'==x||'='==x||'>'==x||'~'==x", or.toString());

        Expression combined = r.and(List.of(and, or));
        assertEquals("'&'==x", combined.toString());
    }

    @DisplayName("different ordering, currently problematic")
    @Test
    public void test1b() {
        Expression nx1 = r.negate(r.equals(r.newChar('!'), x));
        Expression nx2 = r.negate(r.equals(r.newChar('<'), x));
        Expression nx3 = r.negate(r.equals(r.newChar('>'), x));
        Expression nx4 = r.negate(r.equals(r.newChar('='), x));
        Expression nx5 = r.negate(r.equals(r.newChar('b'), x));
        Expression nx6 = r.negate(r.equals(r.newChar('^'), x));

        Expression x1 = r.equals(r.newChar('!'), x);
        Expression x2 = r.equals(r.newChar('<'), x);
        Expression x3 = r.equals(r.newChar('>'), x);
        Expression x4 = r.equals(r.newChar('='), x);
        Expression x5 = r.equals(r.newChar('^'), x);
        Expression x7 = r.equals(r.newChar('a'), x);

        Expression and = r.and(nx1, nx2, nx3, nx4, nx5, nx6);
        assertEquals("'!'!=x&&'<'!=x&&'='!=x&&'>'!=x&&'^'!=x&&'b'!=x", and.toString());

        Expression or = r.or(x1, x2, x3, x4, x5, x7);
        assertEquals("'!'==x||'<'==x||'='==x||'>'==x||'^'==x||'a'==x", or.toString());

        Expression combined = r.and(List.of(and, or));
        assertEquals("'a'==x", combined.toString());
    }

    @DisplayName("equals to 1b, but this was the original one with strings")
    @Test
    public void test2() {
        Expression nx1 = r.negate(r.equals(r.newStringConstant("!="), s));
        Expression nx2 = r.negate(r.equals(r.newStringConstant("<"), s));
        Expression nx3 = r.negate(r.equals(r.newStringConstant("<="), s));
        Expression nx4 = r.negate(r.equals(r.newStringConstant("=="), s));
        Expression nx5 = r.negate(r.equals(r.newStringConstant("=~"), s));
        Expression nx6 = r.negate(r.equals(r.newStringConstant("le"), s));

        Expression x1 = r.equals(r.newStringConstant("!="), s);
        Expression x2 = r.equals(r.newStringConstant("<"), s);
        Expression x3 = r.equals(r.newStringConstant("<="), s);
        Expression x4 = r.equals(r.newStringConstant("=="), s);
        Expression x5 = r.equals(r.newStringConstant("=~"), s);
        Expression x7 = r.equals(r.newStringConstant("IN"), s);

        Expression and = r.and(nx1, nx2, nx3, nx4, nx5, nx6);
        assertEquals("\"!=\"!=s&&\"<\"!=s&&\"<=\"!=s&&\"==\"!=s&&\"=~\"!=s&&\"le\"!=s", and.toString());

        Expression or = r.or(x1, x2, x3, x4, x5, x7);
        assertEquals("\"!=\"==s||\"<\"==s||\"<=\"==s||\"==\"==s||\"=~\"==s||\"IN\"==s", or.toString());

        Expression combined = r.and(List.of(and, or));
        assertEquals("\"IN\"==s", combined.toString());
    }

    // tests the final "SKIP" in EvalAnd.equalsAndOr
    @Test
    public void test3() {
        Expression nx1 = r.negate(r.equals(r.newStringConstant("!"), s));
        Expression x2 = r.equals(r.newStringConstant("<"), s);
        Expression x3 = r.equals(r.newStringConstant(">"), s);
        Expression oo = r.newOrBuilder().addExpression(x3).addExpression(nx1).build();
        Expression ee = r.newAndBuilder().addExpression(x2).addExpression(oo).build();
        assertEquals("\"<\"==s&&(\">\"==s||\"!\"!=s)", ee.toString());
        Expression e = r.and(x2, r.or(x3, nx1));
        assertEquals("\"<\"==s", e.toString());
    }

    @Test
    public void test4() {
        Expression nx1 = r.negate(r.equals(r.newChar('!'), x));
        Expression nx2 = r.negate(r.equals(r.newChar('<'), x));
        Expression nx3 = r.negate(r.equals(r.newChar('>'), x));
        Expression x7 = r.equals(r.newChar('a'), x);
        Expression e = r.and(nx1, nx2, nx3, x7);
        assertEquals("'a'==x", e.toString());
    }
}
