package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.Expression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAndSorting extends CommonTest {

    // null checks go to the front
    @Test
    public void test1() {
        Expression sIsNotNull = r.negate(r.equals(s, r.nullConstant()));
        assertEquals("null!=s", sIsNotNull.toString());
        Expression sLength = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.andOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.&&()", sLength.toString());
        Expression a1 = r.and(sIsNotNull, sLength);
        assertEquals("null!=s&&s.&&()", a1.toString());
        Expression a2 = r.and(sLength, sIsNotNull);
        assertEquals("null!=s&&s.&&()", a2.toString());
    }

    // method calls remain in the same order
    @Test
    public void test2() {
        Expression sIsNotNull = r.negate(r.equals(s, r.nullConstant()));
        assertEquals("null!=s", sIsNotNull.toString());
        Expression s1 = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.andOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.&&()", s1.toString());
        Expression s2 = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.orOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.||()", s2.toString());

        Expression a1 = r.and(sIsNotNull, s1, s2);
        assertEquals("null!=s&&s.&&()&&s.||()", a1.toString());
        Expression a2 = r.and(s2, sIsNotNull, s1);
        assertEquals("null!=s&&s.||()&&s.&&()", a2.toString());
    }
}
