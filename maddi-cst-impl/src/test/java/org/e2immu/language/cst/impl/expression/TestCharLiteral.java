package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCharLiteral {

    @Test
    public void test() {
        assertEquals("'a'", print('a'));
        assertEquals("'\\0'", print('\0'));
        assertEquals("'\\u0001'", print('\u0001'));
        assertEquals("'\\u0010'", print('\u0010'));
    }

    private static String print(char c) {
        return new CharConstantImpl(List.of(), null, null, c).toString();
    }
}
