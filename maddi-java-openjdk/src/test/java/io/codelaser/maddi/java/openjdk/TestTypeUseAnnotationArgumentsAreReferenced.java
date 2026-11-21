package io.codelaser.maddi.java.openjdk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.TypeInfo;

/**
 * A class literal in the ARGUMENTS of a type-use annotation on a type argument is a code reference:
 * {@code List<@WithConverter(Trimmed.class) String>} uses {@code Trimmed}. typesReferenced listed the annotation type and
 * not its arguments, so the type existed nowhere but in the text: refactor-base dropped its import as unused, and graphs
 * built from typesReferenced had no edge (Quarkus vertx-http config interfaces, the modularization campaign's QE-31).
 * A declaration annotation already walked its arguments; this pins the type-use position, nested and on a return type.
 */
public class TestTypeUseAnnotationArgumentsAreReferenced extends CommonTest {

    private static final String WITH_CONVERTER = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.TYPE_USE})
            public @interface WithConverter {
                Class<?> value();
            }
            """;

    private static final String TRIMMED = """
            package conv;
            public final class Trimmed {
            }
            """;

    private static final String PART_CONVERTER = """
            package conv;
            public final class PartConverter {
            }
            """;

    private static final String CONFIG = """
            package a.b;
            import java.util.List;
            import java.util.Optional;
            import ann.WithConverter;
            import conv.PartConverter;
            import conv.Trimmed;
            public interface Config {
                List<@WithConverter(Trimmed.class) String> rolesAllowed();
                Optional<List<@WithConverter(PartConverter.class) String>> parts();
            }
            """;

    @Test
    public void classLiteralInTypeUseAnnotationIsReferenced() {
        TypeInfo config = scan(false, Map.of("ann.WithConverter", WITH_CONVERTER, "conv.Trimmed", TRIMMED,
                "conv.PartConverter", PART_CONVERTER, "a.b.Config", CONFIG)).primaryTypes().stream()
                .filter(t -> "a.b.Config".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        Set<String> referenced = config.typesReferenced(e -> !(e instanceof JavaDoc))
                .map(Element.TypeReference::typeInfo).filter(Objects::nonNull)
                .map(TypeInfo::fullyQualifiedName).collect(Collectors.toSet());
        assertTrue(referenced.contains("ann.WithConverter"), referenced.toString());
        assertTrue(referenced.contains("conv.Trimmed"), "one level deep: " + referenced);
        assertTrue(referenced.contains("conv.PartConverter"), "two levels deep: " + referenced);
    }
}
