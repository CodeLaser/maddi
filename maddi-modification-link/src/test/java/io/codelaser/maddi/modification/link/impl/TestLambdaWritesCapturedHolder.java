package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 A lambda that writes THROUGH a captured holder -- an array element, a field of a captured object -- has an effect
 its creator sees (#94): applied by forEach to a collection's elements, the holder holds one of them, as after the
 equivalent `for` loop.
 */
public class TestLambdaWritesCapturedHolder extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            class X {
                static class Box { StringBuilder sb; }
                StringBuilder loop(List<StringBuilder> xs) { StringBuilder r = null; for (StringBuilder x : xs) { r = x; } return r; }
                StringBuilder[] array(List<StringBuilder> xs) { StringBuilder[] r = new StringBuilder[1]; xs.forEach(x -> { r[0] = x; }); return r; }
                StringBuilder box(List<StringBuilder> xs) { Box b = new Box(); xs.forEach(x -> { b.sb = x; }); return b.sb; }
                StringBuilder value(Map<String, StringBuilder> m) { StringBuilder[] r = new StringBuilder[1]; m.forEach((k, v) -> { r[0] = v; }); return r[0]; }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        // the control: a loop
        assertEquals("[-] --> loop∈0:xs.§$s", tlc.doMethod(X.findUniqueMethod("loop", 1)).toString());
        // an array element, a field of a captured object
        assertEquals("[-] --> array[0]∈0:xs.§$s,array[0].§m~0:xs.§m", tlc.doMethod(X.findUniqueMethod("array", 1)).toString());
        assertEquals("[-] --> box∈0:xs.§$s,box.§m~0:xs.§m", tlc.doMethod(X.findUniqueMethod("box", 1)).toString());
        // a BiConsumer: the value's slice [-2], not the key's [-1]
        assertEquals("[-] --> value∈0:m.§$$s[-2],value.§m~0:m.§m", tlc.doMethod(X.findUniqueMethod("value", 1)).toString());
    }
}
