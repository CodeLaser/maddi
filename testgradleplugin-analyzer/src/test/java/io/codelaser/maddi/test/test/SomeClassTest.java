package org.e2immu.test.test;

import org.e2immu.test.main.ASecondMainClass;
import org.e2immu.test.main.SomeClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SomeClassTest {
    @Test
    public void test() {
        SomeClass<String> sc = new SomeClass<>() {
            @Override
            public String make() {
                return "!";
            }
        };
        assertEquals("!", sc.make());

        ASecondMainClass aSecondMainClass = new ASecondMainClass();
    }
}
