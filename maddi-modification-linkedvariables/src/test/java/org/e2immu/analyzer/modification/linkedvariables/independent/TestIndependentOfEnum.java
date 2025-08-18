package org.e2immu.analyzer.modification.linkedvariables.independent;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;

public class TestIndependentOfEnum extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            import java.io.Serializable;
            
            public enum E {
              A, B, C
            }
            """;

    @DisplayName("independent of serializable enum")
    @Test
    public void test1() {
        TypeInfo E = javaInspector.parse(INPUT1);
        assertFalse(E.isExtensible());
        FieldInfo A = E.getFieldByName("A", true);
        assertFalse(A.type().typeInfo().isExtensible());
        List<Info> ao = prepWork(E);
        analyzer.go(ao);

        HiddenContentTypes hctE = E.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class);
        assertFalse(hctE.hasHiddenContent());

        assertSame(ValueImpl.IndependentImpl.INDEPENDENT, E.analysis().getOrNull(INDEPENDENT_TYPE,
                ValueImpl.IndependentImpl.class));
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE, E.analysis().getOrNull(IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class));
    }
}
