package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The 'assigned' component of METHOD_LINKS: own-field SLOT writes, orthogonal to the object-modification
 * set 'modified'. One test per shape; each pins both sets so their separation stays visible.
 */
public class TestGapOwnFieldModification extends CommonTest {

    private MethodLinkedVariables compute(String fqn, String src, String methodName) {
        TypeInfo type = javaInspector.parse(fqn, src);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(type);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo mi = type.methodStream().filter(m -> m.name().equals(methodName)).findFirst()
                .orElseGet(() -> type.constructors().getFirst());
        return lc.doMethod(mi);
    }

    private void assertSets(String expectAssigned, String expectModified, String fqn, String src, String methodName) {
        MethodLinkedVariables mlv = compute(fqn, src, methodName);
        assertEquals(expectAssigned, mlv.sortedAssignedString());
        assertEquals(expectModified, mlv.sortedModifiedString());
    }

    @DisplayName("int field increment: assigned records the slot, modified only the owner")
    @Test
    public void increment() {
        @Language("java")
        String src = """
                package a.b;
                class C1 { int i; void m() { i++; } }
                """;
        assertSets("this.i", "this", "a.b.C1", src, "m");
    }

    @DisplayName("int field assignment from a constant")
    @Test
    public void assignConstant() {
        @Language("java")
        String src = """
                package a.b;
                class C2 { int i; void m() { this.i = 3; } }
                """;
        assertSets("this.i", "this", "a.b.C2", src, "m");
    }

    @DisplayName("setter: assigned records the slot even though the param link also shows it")
    @Test
    public void setter() {
        @Language("java")
        String src = """
                package a.b;
                class C3 { String s; void m(String in) { this.s = in; } }
                """;
        assertSets("this.s", "this", "a.b.C3", src, "m");
    }

    @DisplayName("computed reassignment, no parameter involved")
    @Test
    public void computedReassign() {
        @Language("java")
        String src = """
                package a.b;
                class C4 { String s; void m() { this.s = s + "x"; } }
                """;
        assertSets("this.s", "this", "a.b.C4", src, "m");
    }

    @DisplayName("content modification is NOT a slot write: assigned stays empty")
    @Test
    public void contentModification() {
        @Language("java")
        String src = """
                package a.b;
                import java.util.List;
                class C5 { List<String> list; void m(String x) { list.add(x); } }
                """;
        assertSets("", "this, this.list", "a.b.C5", src, "m");
    }

    @DisplayName("array element write: the array field's object is modified, no field slot is assigned")
    @Test
    public void arrayElementWrite() {
        @Language("java")
        String src = """
                package a.b;
                class C6 { int[] a; void m() { a[0] = 1; } }
                """;
        assertSets("", "this, this.a", "a.b.C6", src, "m");
    }

    @DisplayName("nested field assignment d.j = 1: the leaf slot is assigned, d's object is modified")
    @Test
    public void nestedFieldAssignment() {
        @Language("java")
        String src = """
                package a.b;
                class C7 { static class D { int j; } D d; void m() { d.j = 1; } }
                """;
        assertSets("this.d.j", "this, this.d", "a.b.C7", src, "m");
    }

    @DisplayName("transitive: m() calls inc() which increments the field")
    @Test
    public void transitiveThroughThis() {
        @Language("java")
        String src = """
                package a.b;
                class C8 { int i; void inc() { i++; } void m() { inc(); } }
                """;
        assertSets("this.i", "this", "a.b.C8", src, "m");
    }

    @DisplayName("transitive through an own-field receiver: d.incJ() surfaces this.d.j")
    @Test
    public void transitiveThroughOwnField() {
        @Language("java")
        String src = """
                package a.b;
                class C9 {
                    static class D { int j; void incJ() { j++; } }
                    D d;
                    void m() { d.incJ(); }
                }
                """;
        assertSets("this.d.j", "this, this.d", "a.b.C9", src, "m");
    }

    @DisplayName("a parameter receiver does not surface own fields")
    @Test
    public void parameterReceiverExcluded() {
        @Language("java")
        String src = """
                package a.b;
                class C10 {
                    static class D { int j; void incJ() { j++; } }
                    void m(D d) { d.incJ(); }
                }
                """;
        assertSets("", "a.b.C10.m(a.b.C10.D):0:d", "a.b.C10", src, "m");
    }

    @DisplayName("constructor assignments are captured too")
    @Test
    public void constructorAssignment() {
        @Language("java")
        String src = """
                package a.b;
                class C11 { String s; C11(String in) { this.s = in; } }
                """;
        assertSets("this.s", "this", "a.b.C11", src, "<init>");
    }
}
