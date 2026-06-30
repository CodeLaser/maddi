package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Evidence for virtual-fields.md inconsistency #1 (flat vs pairwise container for >=3 type parameters).
 * <p>
 * A Guava-style {@code Table<R,C,V>} uses three type parameters. The flat container ({@code §RCV{§r,§c,§v}}) can
 * express the singletons (R, C, V) via slices {@code [-1]/[-2]/[-3]} and the full triple, but it CANNOT express a
 * proper sub-tuple such as (C,V). The consequence, pinned below, is that the sub-tuple view methods {@code row()} and
 * {@code column()} get NO link at all — i.e. they are (incorrectly) treated as independent of the table, so a
 * modification to such a live view would not propagate back. The pairwise-combination container (with §cv / §rv
 * components) is what would capture these.
 */
public class TestVirtualFieldTable extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.NotModified;
            import java.util.Map;
            import java.util.Set;
            public interface Table<R,C,V> {
                @Independent(hc = true) @NotModified V get(R r, C c);
                @Independent(hc = true) @NotModified Set<R> rowKeySet();
                @Independent(hc = true) @NotModified Map<C,V> row(R r);
                @Independent(hc = true) @NotModified Map<R,V> column(C c);
                @Independent(hc = true) @NotModified Map<R, Map<C,V>> rowMap();
                V put(@Independent(hc = true) R r, @Independent(hc = true) C c, @Independent(hc = true) V v);
            }
            """;

    @DisplayName("flat container for 3 type parameters: singletons link, proper sub-tuples do not")
    @Test
    public void test() {
        TypeInfo table = javaInspector.parse("a.b.Table", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(table);
        new ShallowAnalyzer(runtime, Element::annotations, true).go(List.of(table));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        // flat container: §RCV{§r,§c,§v}; slices [-1]=R (component 0), [-2]=C, [-3]=V
        assertEquals("§m - §RCV[] §rcvs", vfc.compute(table).toString());

        LinkComputer lc = new LinkComputerImpl(javaInspector);

        // singletons: correctly sliced out of the container
        assertEquals("[-, -] --> get∈this.§rcvs[-3]", link(lc, table, "get"));          // V
        assertEquals("[] --> rowKeySet.§rs⊆this.§rcvs[-1]", link(lc, table, "rowKeySet")); // R
        assertEquals("[0:r∈this*.§rcvs[-1], 1:c∈this*.§rcvs[-2], 2:v∈this*.§rcvs[-3]] --> put∈this*.§rcvs[-3]",
                link(lc, table, "put"));

        // GAP (#1): proper sub-tuples of a 3-parameter type cannot be expressed by the flat container, so these
        // view-returning methods get NO link (reported as independent of the table). With a pairwise-combination
        // container these would be e.g. row.§cvs ⊆ this.§rcvs[slice for (C,V)].
        assertEquals("[-] --> -", link(lc, table, "row"));       // Map<C,V>  -- should link (C,V), but does not
        assertEquals("[-] --> -", link(lc, table, "column"));    // Map<R,V>  -- should link (R,V), but does not
        // rowMap(): only the outer R is linked; the nested (C,V) is collapsed to a concrete '$' and lost
        assertEquals("[] --> rowMap.§r$s⊆this.§rcvs[-1]", link(lc, table, "rowMap"));
    }

    private static String link(LinkComputer lc, TypeInfo table, String name) {
        MethodInfo mi = table.methodStream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
        MethodLinkedVariables mlv = lc.doMethod(mi);
        return mlv.toString();
    }
}
