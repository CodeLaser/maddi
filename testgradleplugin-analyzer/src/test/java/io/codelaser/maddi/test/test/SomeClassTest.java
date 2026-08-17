package io.codelaser.maddi.test.test;

import io.codelaser.maddi.test.main.ASecondMainClass;
import io.codelaser.maddi.test.main.SomeClass;
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
