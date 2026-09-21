package io.codelaser.maddi.java.openjdk.lombok;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @SuperBuilder} generates classes that EXTEND -- {@code ChildBuilderImpl extends ChildBuilder<..>},
 * {@code ChildBuilder<C, B> extends Base.BaseBuilder<C, B>} -- and nobody typed their {@code extends}. With
 * detailed sources on, the scan looked the keyword up in the text: "Cannot find keyword extends", and the
 * compilation unit was lost.
 */
public class TestSuperBuilder extends CommonTest {

    @Language("java")
    private static final String BASE = """
            package a.b;
            import lombok.Data;
            import lombok.NoArgsConstructor;
            import lombok.experimental.SuperBuilder;
            @Data
            @SuperBuilder
            @NoArgsConstructor
            public class Base implements java.io.Serializable {
                protected int processId;
            }
            """;

    // an EARLIER type in the file has the keyword: it must not be taken for the generated class's
    @Language("java")
    private static final String CHILD = """
            package a.b;
            import lombok.Data;
            import lombok.EqualsAndHashCode;
            import lombok.NoArgsConstructor;
            import lombok.experimental.SuperBuilder;
            @Data
            @SuperBuilder
            @NoArgsConstructor
            @EqualsAndHashCode(callSuper = true)
            public class Child extends Base {
                private String host;
            }
            """;

    @Test
    public void test() {
        Map<String, TypeInfo> types = scan(false, "a.b.Base", BASE, "a.b.Child", CHILD);
        TypeInfo child = types.get("a.b.Child");
        assertEquals("a.b.Base", child.parentClass().typeInfo().fullyQualifiedName());
        assertNotNull(child.findUniqueMethod("setHost", 1));
        TypeInfo builder = child.subTypes().stream().filter(t -> "ChildBuilder".equals(t.simpleName()))
                .findFirst().orElseThrow();
        assertEquals("a.b.Base.BaseBuilder", builder.parentClass().typeInfo().fullyQualifiedName());
        assertTrue(child.subTypes().stream().anyMatch(t -> "ChildBuilderImpl".equals(t.simpleName())));
    }
}
