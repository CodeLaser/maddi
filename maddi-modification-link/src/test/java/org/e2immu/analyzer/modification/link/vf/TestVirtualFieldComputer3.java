package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Companion to {@link TestVirtualFieldComputer} / {@link TestVirtualFieldComputer2}; covers aspects that were not
 * yet tested and pins down (and flags) the inconsistencies listed in {@code vf/virtual-fields.md}.
 */
public class TestVirtualFieldComputer3 extends CommonTest {

    // ===== mutability of a plain class without type parameters =====

    @DisplayName("mutable plain class has a §m field")
    @Test
    public void mutablePlainClass() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo stringBuilder = javaInspector.compiledTypesManager().getOrLoad(StringBuilder.class);
        VirtualFields vf = vfc.compute(stringBuilder);
        assertNotNull(vf.mutable(), "StringBuilder is mutable -> §m expected");
        assertEquals("java.lang.StringBuilder", vf.mutable().owner().fullyQualifiedName());
        // hidden content of a no-type-parameter class is the type itself, named with a per-computer counter
        assertEquals("java.lang.StringBuilder", vf.hiddenContent().type().typeInfo().fullyQualifiedName());
    }

    @DisplayName("immutable plain class has no §m field")
    @Test
    public void immutablePlainClass() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo string = javaInspector.compiledTypesManager().getOrLoad(String.class);
        assertNull(vfc.compute(string).mutable(), "String is immutable -> no §m");
        TypeInfo integer = javaInspector.compiledTypesManager().getOrLoad(Integer.class);
        assertNull(vfc.compute(integer).mutable(), "Integer is immutable -> no §m");
    }

    // ===== utility classes and Comparable produce nothing =====

    @DisplayName("utility class (only static members) has no virtual fields")
    @Test
    public void utilityClass() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo math = javaInspector.compiledTypesManager().getOrLoad(Math.class);
        assertEquals(VirtualFields.NONE, vfc.compute(math));
    }

    // ===== three type parameters: FLAT container, not the pairwise-combination container =====

    @Language("java")
    private static final String THREE = "package a.b; interface Three<A,B,C> { A a(); B b(); C c(); }";

    @DisplayName("three type parameters -> flat container (NOT pairwise combinations)")
    @Test
    public void threeTypeParameters() {
        TypeInfo three = javaInspector.parse("a.b.Three", THREE);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vf = vfc.compute(three);
        assertEquals("§m - §ABC §abc", vf.toString());

        TypeInfo container = vf.hiddenContent().type().typeInfo();
        // INCONSISTENCY #1 (see virtual-fields.md): the class comment of VirtualFieldComputer describes a container
        // with pairwise combinations, e.g. TSV(T t, S s, V v, TS ts, SV sv, TV tv). The implementation only
        // produces one component per parameter, so we get exactly three fields here, not six.
        List<String> fieldNames = container.fields().stream().map(FieldInfo::name).toList();
        assertEquals(List.of("§a", "§b", "§c"), fieldNames);
    }

    // ===== functional interface outside java.util.function: compute() vs needsVirtual() disagree =====

    @DisplayName("functional interface outside java.util.function: compute() and needsVirtual() disagree")
    @Test
    public void functionalInterfaceOutsideJavaUtilFunction() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo runnable = javaInspector.compiledTypesManager().getOrLoad(Runnable.class);
        assertTrue(runnable.isFunctionalInterface(), "Runnable is a functional interface");

        // INCONSISTENCY #3 (see virtual-fields.md): compute() only short-circuits the java.util.function package by
        // NAME, so Runnable (java.lang) still gets virtual fields...
        VirtualFields vf = vfc.compute(runnable);
        assertNotEquals(VirtualFields.NONE, vf);
        assertNotNull(vf.mutable());

        // ...while Util.needsVirtual() excludes ALL functional interfaces via isFunctionalInterface().
        assertFalse(Util.needsVirtual(runnable.asParameterizedType()));
    }

    @DisplayName("functional interface IN java.util.function: no virtual fields")
    @Test
    public void functionalInterfaceInJavaUtilFunction() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo function = javaInspector.compiledTypesManager()
                .getOrLoad(java.util.function.Function.class);
        assertEquals(VirtualFields.NONE, vfc.compute(function));
    }

    // ===== Comparable: hidden content never escapes -> multiplicity 0 =====

    @DisplayName("Comparable has multiplicity 0 and no virtual fields")
    @Test
    public void comparable() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo comparable = javaInspector.compiledTypesManager().getOrLoad(Comparable.class);
        assertEquals(0, vfc.maxMultiplicityFromMethods(comparable));
        assertEquals(VirtualFields.NONE, vfc.compute(comparable));
    }
}
