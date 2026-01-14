package org.e2immu.analyzer.modification.analyzer.field;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFieldAnalyzer extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
            }
            """;

    @DisplayName("constructor and getter")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        assertEquals("this.set←0:set", set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class).toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isUnmodified());
        assertTrue(set.isFinal());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
                public void add(C c) {
                    set.add(c);
                }
            }
            """;

    @DisplayName("constructor, getter, add")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set←0:set,this.set∋0:c", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified());
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isModified()); // via add!
    }

}
