package io.codelaser.maddi.modification.link.impl.graph;

import io.codelaser.maddi.modification.link.impl.LinkNatureImpl;
import io.codelaser.maddi.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestClosureWitnessIndex {
    
    @Test
    public void test() {
        WitnessIndex<String, LinkNature> witnessIndex = new WitnessIndex<>(LinkNature::score, java.util.Comparator.comparing(Object::toString));
        Closure<String, LinkNature> closure = new Closure<>(LinkNature::best);
        closure.add("a", "b", LinkNatureImpl.IS_ELEMENT_OF);
        assertEquals("a ∈ b   \n", closure.print(Object::toString, String::compareTo, witnessIndex));
    }
}
