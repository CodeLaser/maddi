package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * maddi#145: a field REFERENCED before its declaration is reached keeps the type-use annotations of its type
 * arguments, like a field nobody referenced.
 * <p>
 * The reference ({@code all()} below, scanned first) creates the field from its javac symbol, whose type carries no
 * type-use annotation; the declaration is then visited on the "already known" branch of {@code createField}, which
 * kept that symbol-built type. Found on guava's {@code Ordering.ArbitraryOrderingHolder.ARBITRARY_ORDERING}, an
 * {@code Ordering<@Nullable Object>} that read as {@code Ordering<Object>}, so the Kotlin translation's field and
 * the method returning it disagreed. The method-side counterpart is {@link TestTypeUseAnnotationOnACalledMethod}.
 */
public class TestTypeUseAnnotationOnAReferencedField extends CommonTest {

    private static final String TYPE_ONLY = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE_USE)
            public @interface TypeOnly {}
            """;

    private static final String SUBJECT = """
            package a.b;
            import ann.TypeOnly;
            import java.util.ArrayList;
            import java.util.Comparator;
            import java.util.List;

            public abstract class AS<T> implements Comparator<T> {
                static List<@TypeOnly Object> all() { return AllHolder.ALL; }
                static AS<@TypeOnly Object> arbitrary() { return ArbitraryHolder.ARBITRARY; }

                private static final class AllHolder {
                    static final List<@TypeOnly Object> ALL = new ArrayList<>();
                }
                private static final class ArbitraryHolder {
                    static final AS<@TypeOnly Object> ARBITRARY = new Arbitrary();
                }
                private static final class UnreferencedHolder {
                    static final List<@TypeOnly Object> NOBODY = new ArrayList<>();
                }
                static class Arbitrary extends AS<@TypeOnly Object> {
                    public int compare(@TypeOnly Object a, @TypeOnly Object b) { return 0; }
                }
            }
            """;

    private static List<String> argumentAnnotations(FieldInfo fieldInfo) {
        return fieldInfo.type().parameters().stream().map(ParameterizedType::annotations)
                .map(list -> list.stream().map(AnnotationExpression::typeInfo).map(TypeInfo::simpleName).toList()
                        .toString())
                .toList();
    }

    private static FieldInfo field(TypeInfo as, String holder, String name) {
        return as.findSubType(holder).getFieldByName(name, true);
    }

    @Test
    public void referencedAndUnreferencedAgree() {
        TypeInfo as = scan(false, Map.of("ann.TypeOnly", TYPE_ONLY, "a.b.AS", SUBJECT)).primaryTypes().stream()
                .filter(t -> "a.b.AS".equals(t.fullyQualifiedName())).findFirst().orElseThrow();

        assertEquals(List.of("[TypeOnly]"), argumentAnnotations(field(as, "UnreferencedHolder", "NOBODY")),
                "never referenced: the fresh-field path, the reference");
        assertEquals(List.of("[TypeOnly]"), argumentAnnotations(field(as, "AllHolder", "ALL")),
                "referenced by all() before its declaration: the already-known path");
        assertEquals(List.of("[TypeOnly]"), argumentAnnotations(field(as, "ArbitraryHolder", "ARBITRARY")),
                "typed by the enclosing generic class, as guava's ARBITRARY_ORDERING");
    }
}
