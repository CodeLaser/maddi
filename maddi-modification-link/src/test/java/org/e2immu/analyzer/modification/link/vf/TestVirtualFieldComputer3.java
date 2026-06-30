package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @DisplayName("functional interface outside java.util.function: compute() and needsVirtual() DISAGREE (#3)")
    @Test
    public void functionalInterfaceOutsideJavaUtilFunction() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo runnable = javaInspector.compiledTypesManager().getOrLoad(Runnable.class);
        assertTrue(runnable.isFunctionalInterface(), "Runnable is a functional interface");

        // INCONSISTENCY #3 (see virtual-fields.md), deliberately left in place: compute() only short-circuits the
        // java.util.function package by NAME, so Runnable (java.lang) still gets virtual fields...
        VirtualFields vf = vfc.compute(runnable);
        assertNotEquals(VirtualFields.NONE, vf);
        assertNotNull(vf.mutable());

        // ...while Util.needsVirtual() excludes ALL functional interfaces. This divergence is load-bearing:
        // aligning needsVirtual() to compute() breaks TestModificationFunctional (modification propagation through
        // custom functional interfaces, which go via the SAM/lambda path rather than virtual-field hidden content).
        assertFalse(Util.needsVirtual(runnable.asParameterizedType()));
    }

    @DisplayName("functional interface IN java.util.function: compute() and needsVirtual() both say 'no'")
    @Test
    public void needsVirtualFunctionInJavaUtilFunction() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo function = javaInspector.compiledTypesManager().getOrLoad(java.util.function.Function.class);
        assertEquals(VirtualFields.NONE, vfc.compute(function));
        assertFalse(Util.needsVirtual(function.asParameterizedType()));
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

    // ===== primitives, void, primitive arrays =====

    @DisplayName("primitive and void have no virtual fields; a primitive array does (arrays>0)")
    @Test
    public void primitivesAndVoid() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals(VirtualFields.NONE, vfc.compute(runtime.intParameterizedType(), false).virtualFields());
        assertEquals(VirtualFields.NONE, vfc.compute(runtime.voidParameterizedType(), false).virtualFields());
        // the NONE short-circuit is gated on arrays==0, so a primitive *array* still gets virtual fields,
        // with a concrete ($) hidden-content field
        assertEquals("§m - boolean[] §$s",
                vfc.compute(runtime.booleanParameterizedType().copyWithArrays(1), false).virtualFields().toString());
    }

    @DisplayName("unbounded wildcard ? has no virtual fields; List<?> has §m but no hidden content")
    @Test
    public void wildcard() {
        TypeInfo h = javaInspector.parse("a.b.W", """
                package a.b;
                import java.util.List;
                public class W { void m(List<?> w) { } }
                """);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        ParameterizedType listWildcard = h.findUniqueMethod("m", 1).parameters().getFirst().parameterizedType();
        ParameterizedType wildcard = listWildcard.parameters().getFirst();
        assertEquals(VirtualFields.NONE, vfc.compute(wildcard, false).virtualFields());
        assertEquals("§m - /", vfc.compute(listWildcard, false).virtualFields().toString());
    }

    // ===== type parameters (bare, bounded) =====

    @Language("java")
    private static final String TPS = "package a.b; public class H<T, N extends Number> { }";

    @DisplayName("bare and bounded top-level type parameters: temporary base field, no §m, bound irrelevant")
    @Test
    public void typeParameters() {
        TypeInfo h = javaInspector.parse("a.b.H", TPS);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        // a bare type parameter at multiplicity 0 is the recursion base: hidden content = the parameter, no §m
        assertEquals("/ - T §t", vfc.compute(h.typeParameters().getFirst().asSimpleParameterizedType(), false)
                .virtualFields().toString());
        // an explicit bound (N extends Number) does not change this
        assertEquals("/ - N §n", vfc.compute(h.typeParameters().get(1).asSimpleParameterizedType(), false)
                .virtualFields().toString());
    }

    // ===== enum =====

    @DisplayName("a fieldless enum is classified as a utility class -> no virtual fields")
    @Test
    public void fieldlessEnum() {
        TypeInfo h = javaInspector.parse("a.b.H", "package a.b; public class H { enum E { A, B } }");
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        // note: this falls out of the utilityClass() heuristic (all fields static); harmless, since an enum has no
        // hidden content, but worth pinning as it differs from a plain immutable class like String (-> '/ - String §n')
        assertEquals(VirtualFields.NONE, vfc.compute(h.findSubType("E")));
    }

    // ===== collectTypeParametersFromVirtualField (public helper used by ShallowMethodLinkComputer) =====

    @DisplayName("collectTypeParametersFromVirtualField walks (container) hidden-content types")
    @Test
    public void collectTypeParameters() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        // List<E> -> hidden content E[] -> {E}
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        Set<TypeParameter> listTps = VirtualFieldComputer.collectTypeParametersFromVirtualField(
                vfc.compute(list).hiddenContent().type());
        assertEquals(Set.of("E"), listTps.stream().map(TypeParameter::simpleName).collect(Collectors.toSet()));

        // Map<K,V> -> hidden content §KV[] (container with §k:K, §v:V) -> {K, V}
        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        Set<TypeParameter> mapTps = VirtualFieldComputer.collectTypeParametersFromVirtualField(
                vfc.compute(map).hiddenContent().type());
        assertEquals(Set.of("K", "V"), mapTps.stream().map(TypeParameter::simpleName).collect(Collectors.toSet()));

        // a concrete hidden content (boolean[] -> §$s) has no type parameters -> {}
        assertEquals(Set.of(), VirtualFieldComputer.collectTypeParametersFromVirtualField(
                vfc.compute(runtime.booleanParameterizedType().copyWithArrays(1), false)
                        .virtualFields().hiddenContent().type()));
    }
}
