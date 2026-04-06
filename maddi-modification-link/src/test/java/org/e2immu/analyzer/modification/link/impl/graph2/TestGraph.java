package org.e2immu.analyzer.modification.link.impl.graph2;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

public class TestGraph {
    public static final String START = "a";
    LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    @Test
    public void test() {
        LabeledGraph<String, LinkNature> graph = new LabeledGraph<>();
        graph.addEdge(START, "b", IS_IDENTICAL_TO);
      
    }

}
