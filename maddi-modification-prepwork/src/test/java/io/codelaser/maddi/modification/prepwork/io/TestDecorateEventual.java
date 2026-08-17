/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.io;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The write twin of {@code AnnotationToProperty}'s eventual parsing: {@code DecoratorImpl} must turn the computed
 * eventual verdicts back into {@code @Immutable(after=)} / {@code @FinalFields(after=)} / {@code @Mark} /
 * {@code @Only} / {@code @TestMark} / {@code @NotModified(after=)} / {@code @Final(after=)} decorations. Without
 * this, the eventual nature — the novel output of the analysis — never reaches any IDE surface (all three
 * front-ends render {@code DecoratorImpl.annotations(...)}). See road-to-immutability §060 and
 * docs/eventual-info-hierarchy.md "Task 4".
 */
public class TestDecorateEventual extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class X {
                private boolean f;
                public void commit() { this.f = true; }
                public boolean isReady() { return f; }
                public String readAfter() { return "x"; }
                public String onlyAfter() { return "x"; }
            }
            """;

    private static AnnotationExpression named(List<AnnotationExpression> list, String simpleName) {
        return list.stream().filter(a -> simpleName.equals(a.typeInfo().simpleName())).findFirst().orElse(null);
    }

    @DisplayName("computed eventual verdicts render as the after-mark annotations")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        FieldInfo f = X.getFieldByName("f", true);
        MethodInfo commit = X.findUniqueMethod("commit", 0);
        MethodInfo isReady = X.findUniqueMethod("isReady", 0);
        MethodInfo readAfter = X.findUniqueMethod("readAfter", 0);
        MethodInfo onlyAfter = X.findUniqueMethod("onlyAfter", 0);

        // set the computed eventual verdicts by hand (what the eventual analyzer produces)
        X.analysis().set(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                new ValueImpl.EventuallyImmutableImpl("f", ValueImpl.ImmutableImpl.IMMUTABLE_HC));
        commit.analysis().set(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.mark("f"));
        isReady.analysis().set(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.testMark("f", true));
        onlyAfter.analysis().set(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.only("f", true));
        readAfter.analysis().set(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD,
                new ValueImpl.SetOfStringsImpl(Set.of("f")));
        f.analysis().set(PropertyImpl.EVENTUALLY_FINAL_FIELD, new ValueImpl.SetOfStringsImpl(Set.of("f")));

        DecoratorImpl d = new DecoratorImpl(runtime, javaInspector.mainSources());

        // type -> @Immutable(hc=true, after="f")
        AnnotationExpression imm = named(d.annotations(X), "Immutable");
        assertNotNull(imm, "expected @Immutable on the type");
        assertTrue(imm.extractBoolean("hc"), "hc=true");
        assertEquals("f", imm.extractString("after", ""));

        // @Mark("f")
        AnnotationExpression mark = named(d.annotations(commit), "Mark");
        assertNotNull(mark, "expected @Mark on commit");
        assertEquals("f", mark.extractString("value", ""));

        // @TestMark("f")
        AnnotationExpression testMark = named(d.annotations(isReady), "TestMark");
        assertNotNull(testMark, "expected @TestMark on isReady");
        assertEquals("f", testMark.extractString("value", ""));

        // @Only(after="f")
        AnnotationExpression only = named(d.annotations(onlyAfter), "Only");
        assertNotNull(only, "expected @Only on onlyAfter");
        assertEquals("f", only.extractString("after", ""));
        assertEquals("", only.extractString("before", ""));

        // @NotModified(after="f")
        AnnotationExpression notModified = named(d.annotations(readAfter), "NotModified");
        assertNotNull(notModified, "expected @NotModified(after=) on readAfter");
        assertEquals("f", notModified.extractString("after", ""));

        // @Final(after="f")
        AnnotationExpression finalAfter = named(d.annotations(f), "Final");
        assertNotNull(finalAfter, "expected @Final(after=) on field f");
        assertEquals("f", finalAfter.extractString("after", ""));
    }
}
