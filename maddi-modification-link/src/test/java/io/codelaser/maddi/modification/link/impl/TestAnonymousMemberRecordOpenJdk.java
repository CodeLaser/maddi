package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Task #33, OPENJDK front (this module's CommonTest uses the openjdk inspector): the elasticsearch
 * shape — a member record of an anonymous class, forward-referenced from a sibling method. The
 * javac-side scanner must deliver a fully source-built member (bodies present), and prep must run
 * clean over it. Twin of prepwork's TestAnonymousMemberRecord (in-house parser front).
 */
public class TestAnonymousMemberRecordOpenJdk extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.stream.Stream;
            class X {
                interface Support { Object example(); }
                Support make() {
                    return new Support() {
                        @Override
                        public Object example() {
                            return Stream.of(new byte[]{1}).map(Cmp::new).sorted().toList();
                        }
                        private record Cmp(byte[] bytes) implements Comparable<Cmp> {
                            @Override
                            public int compareTo(Cmp o) { return 0; }
                        }
                    };
                }
            }
            """;

    @DisplayName("openjdk scan + prep over a forward-referenced member record of an anonymous class")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        var make = X.findUniqueMethod("make", 0);
        assertNotNull(make.methodBody(), "make()'s body must survive");
        var rs = (ReturnStatement) make.methodBody().statements().getFirst();
        var cc = (ConstructorCall) rs.expression();
        TypeInfo anon = cc.anonymousClass();
        assertNotNull(anon);
        TypeInfo cmp = anon.findSubType("Cmp");
        cmp.constructorAndMethodStream().forEach(mi ->
                assertNotNull(mi.methodBody(), "source-built member method must have a body: " + mi));
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
    }
}
