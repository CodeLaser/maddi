package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// bytecode models a type-parameter bound 'T extends X' with an EXTENDS wildcard on the bound; the printer must
// not render that as the invalid '<T extends ? extends X>' (this surfaced when IsolateMethod stubbed such a
// method from a compiled library). Inner type-argument wildcards (e.g. '? super T') must still be printed.
public class TestTypeBoundWildcardPrinting extends CommonIsolateMethodTest {

    @DisplayName("bytecode-loaded bounded generic prints without a leading bound wildcard")
    @Test
    public void boundWildcardNotPrinted() {
        parse("a.b.X", "package a.b; class X {}"); // initialize the inspector
        TypeInfo enumMap = javaInspector.compiledTypesManager().getOrLoad(java.util.EnumMap.class);
        assertEquals("K extends Enum<K>", enumMap.typeParameters().getFirst()
                .print(javaInspector.runtime().qualificationSimpleNames(), true).toString());

        TypeInfo collections = javaInspector.compiledTypesManager().getOrLoad(java.util.Collections.class);
        MethodInfo sort = collections.methodStream()
                .filter(m -> m.name().equals("sort") && m.parameters().size() == 1)
                .findFirst().orElseThrow();
        // inner '? super T' is preserved, only the bound's own leading wildcard is dropped
        assertEquals("T extends Comparable<? super T>", sort.typeParameters().getFirst()
                .print(javaInspector.runtime().qualificationSimpleNames(), true).toString());
    }
}
