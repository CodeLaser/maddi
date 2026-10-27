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

    @DisplayName("too complex to analyse, and reduced to one term by the trivial step: that term, not an Or of one")
    @Test
    public void tooComplexOrOfOne() {
        // complexity 1: every disjunction is 'too complex', and only the trivial step (4b) runs
        io.codelaser.maddi.cst.impl.expression.eval.EvalOr evalOr = new io.codelaser.maddi.cst.impl.expression.eval.EvalOr(r,
                new io.codelaser.maddi.cst.impl.expression.eval.EvalOptions(1));
        Expression x1 = r.equals(r.newChar('!'), x);
        assertEquals(x1, evalOr.eval(List.of(x1, FALSE)), "the false is removed, one term is left");
        assertEquals(x1, evalOr.eval(List.of(x1, x1)), "A || A, one term is left");
        assertEquals(FALSE, evalOr.eval(List.of(FALSE, FALSE)), "nothing is left");
        Expression x2 = r.equals(r.newChar('<'), x);
        assertEquals("'!'==x||'<'==x", evalOr.eval(List.of(x1, x2)).toString(), "two terms stay an Or");
    }

    @DisplayName("(A || X) && (!A || X) resolves to X, and only that shape (maddi#117)")
    @Test
    public void resolution() {
        assertEquals("b", r.and(r.or(a, b), r.or(b, r.negate(a))).toString());
        // c is not dropped: a=false, b=false, c=true satisfies the input
        assertEquals("(a||b||c)&&(!a||b)", r.and(r.or(a, b, c), r.or(b, r.negate(a))).toString());
        // two opposite pairs: a=true, b=false, d=false satisfies the input
        assertEquals("(a||b||d)&&(!a||!b||d)",
                r.and(r.or(a, b, d), r.or(r.negate(a), r.negate(b), d)).toString());
    }
}
