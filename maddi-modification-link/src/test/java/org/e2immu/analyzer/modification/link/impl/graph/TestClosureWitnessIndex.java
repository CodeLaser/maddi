package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestClosureWitnessIndex {
    
    @Test
    public void test() {
        WitnessIndex<String, LinkNature> witnessIndex = new WitnessIndex<>(LinkNature::score);
        Closure<String, LinkNature> closure = new Closure<>(LinkNature::best);
        closure.add("a", "b", LinkNatureImpl.IS_ELEMENT_OF);
        assertEquals("", closure.print(Object::toString, String::compareTo, witnessIndex));
    }
}
