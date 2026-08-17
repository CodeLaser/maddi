package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOverrides extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestOverrides.class);

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.function.Function;
            class X {
                ArrayList<Integer> intList;
            
                Integer getInt(int index) {
                    return intList.get(index);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        MethodInfo getInt = X.findUniqueMethod("getInt", 1);
        MethodCall mc = (MethodCall) getInt.methodBody().statements().getFirst().expression();
        assertEquals("java.util.AbstractList.get(int),java.util.List.get(int)",
                mc.methodInfo().overrides().stream().map(Object::toString).sorted().collect(Collectors.joining(",")));

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayDeque;
            import java.util.LinkedList;
            import java.util.List;
            import java.util.Deque;
            class X {
                Deque<Integer> deque = new ArrayDeque<>();
                LinkedList<Integer> intList;
            
                List<Integer> rev() {
                    return intList.reversed();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = scan("a.b.X", INPUT2);
        MethodInfo getInt = X.findUniqueMethod("rev", 0);
        MethodCall mc = (MethodCall) getInt.methodBody().statements().getFirst().expression();
        assertEquals("java.util.Deque.reversed(),java.util.List.reversed(),java.util.SequencedCollection.reversed()",
                mc.methodInfo().overrides().stream().map(Object::toString).sorted().collect(Collectors.joining(",")));

        List<TypeInfo> loaded = List.copyOf(classSymbolScanner.typesLoaded());
        Set<String> loadedFqns = loaded.stream().map(TypeInfo::fullyQualifiedName).collect(Collectors.toUnmodifiableSet());
        assertTrue(loadedFqns.contains("java.util.Deque"));

        for (TypeInfo typeInfo : loaded) {
            if (!typeInfo.hasBeenInspected()) {
                classSymbolScanner.commitType(typeInfo);
                LOGGER.info("Committed {}", typeInfo);
            }
        }

        assertTrue(mc.methodInfo().overrides().stream().allMatch(mi -> mi.typeInfo().hasBeenInspected()));
    }
    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
                interface I { int i(); }
                record Y(int i) implements I {}
            }
            """;

    @DisplayName("subtypes in call graph")
    @Test
    public void test4() {
        TypeInfo X = scan("a.b.C", INPUT3);
        TypeInfo Y = X.findSubType("Y");
        MethodInfo i = Y.findUniqueMethod("i", 0);
        assertTrue(i.isSynthetic());
        assertEquals("[a.b.C.I.i()]", i.overrides().toString());
    }
}
