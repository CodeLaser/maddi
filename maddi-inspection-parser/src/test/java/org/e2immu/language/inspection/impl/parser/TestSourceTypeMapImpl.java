package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.element.CompilationUnitImpl;
import org.e2immu.language.cst.impl.info.TypeInfoImpl;
import org.e2immu.language.inspection.api.parser.TypeMap;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.language.inspection.resource.TypeMapImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestSourceTypeMapImpl {

    public static final String A_B_T = "a.b.T";
    SourceSet set1 = new SourceSetImpl("set1", List.of(), URI.create("file:/a/b"), StandardCharsets.UTF_8,
            false, false, false, false, false, Set.of(), Set.of());
    CompilationUnit abT1Cu = new CompilationUnitImpl(set1, URI.create("file:/a/b/T.java"), List.of(),
            null, List.of(), "a.b", null);
    TypeInfo abT1 = new TypeInfoImpl(abT1Cu, "T");

    SourceSet set2 = new SourceSetImpl("set2", List.of(), URI.create("file:/a/b"), StandardCharsets.UTF_8,
            false, false, false, false, false, Set.of(), Set.of());
    CompilationUnit abT2Cu = new CompilationUnitImpl(set2, URI.create("file:/a/b/T1.java"), List.of(),
            null, List.of(), "a.b", null);
    TypeInfo abT2 = new TypeInfoImpl(abT2Cu, "T");

    @Test
    public void test() {
        TypeMap stm = new TypeMapImpl();
        sequence(stm);
        sequence(stm);
    }

    private void sequence(TypeMap stm) {
        stm.put(abT1);
        assertSame(abT1, stm.get(A_B_T, set1));
        assertSame(abT1, stm.get(A_B_T, set2));
        assertSame(abT1, stm.get(A_B_T, null));

        stm.put(abT2);
        assertSame(abT1, stm.get(A_B_T, set1));
        assertSame(abT2, stm.get(A_B_T, set2));
        assertThrows(NullPointerException.class, () -> stm.get(A_B_T, null));

        stm.invalidate(abT1);
        assertSame(abT2, stm.get(A_B_T, set2));
        assertSame(abT2, stm.get(A_B_T, set1));
        assertSame(abT2, stm.get(A_B_T, null));

        stm.invalidate(abT2);
        assertNull(stm.get(A_B_T, set1));
        assertNull(stm.get(A_B_T, null));
    }
}
