package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;

public class TestLinkNature {


    @Test
    public void test() {
        List<LinkNature> all = new ArrayList<>();
        Collections.addAll(all,
                OBJECT_GRAPH_OVERLAPS, IS_IN_OBJECT_GRAPH, OBJECT_GRAPH_CONTAINS,
                SHARES_ELEMENTS, SHARES_FIELDS,
                IS_SUBSET_OF, IS_SUPERSET_OF, IS_ELEMENT_OF, CONTAINS_AS_MEMBER,
                IS_IDENTICAL_TO, IS_FIELD_OF, CONTAINS_AS_FIELD);
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (LinkNature ln : all) sb.append(" ").append(ln);
        sb.append("\n");
        for (LinkNature ln1 : all) {
            sb.append(ln1).append("  ");
            for (LinkNature ln2 : all) {
                LinkNature combined = ln1.combine(ln2);
                sb.append(" ").append(combined);
            }
            sb.append("\n");
        }
        sb.append("\n");
        System.out.println(sb);
    }
}
