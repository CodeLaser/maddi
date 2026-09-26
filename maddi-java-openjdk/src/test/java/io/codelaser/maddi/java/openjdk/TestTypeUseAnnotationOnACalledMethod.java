package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A TYPE_USE-only annotation lands on the TYPE whichever way the method was first created.
 * <p>
 * A method that is CALLED before its declaration is reached (here: by {@code caller()}, scanned first) is created
 * from its javac symbol by {@code ClassSymbolScanner.addMethodToType}; the declaration is then visited on the
 * "already known" branch of {@code visitMethod}. That branch did not apply the {@code @Target} routing of the
 * fresh-method branch: the return type kept the symbol-built instance (no annotation at all), and every modifier
 * annotation of a parameter went onto the parameter DECLARATION, TYPE_USE-only or not. Two identical signatures
 * then parsed differently depending on whether something called them earlier in the file.
 * <p>
 * Found on timefold-solver's {@code PlannerBenchmarkConfig}: each {@code createFromXmlReader(Reader, ClassLoader)}
 * overload, called by its sibling, lost its jspecify {@code @NonNull} return; each uncalled
 * {@code createFromXmlReader(Reader)} kept it.
 */
public class TestTypeUseAnnotationOnACalledMethod extends CommonTest {

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
            import java.util.List;

            public class X {
                public String caller() {
                    return called("x", null).toString() + calledWithList(null);
                }

                public static @TypeOnly X called(@TypeOnly String s, @TypeOnly Object o) { return new X(); }

                public int calledWithList(List<@TypeOnly String> list) { return 0; }

                public static @TypeOnly X uncalled(@TypeOnly String s, @TypeOnly Object o) { return new X(); }

                public int uncalledWithList(List<@TypeOnly String> list) { return 0; }
            }
            """;

    private static List<String> names(List<AnnotationExpression> annotations) {
        return annotations.stream().map(a -> a.typeInfo().simpleName()).sorted().toList();
    }

    /** Where every annotation of the method's signature ended up, in one comparable string. */
    private static String placement(MethodInfo m) {
        StringBuilder sb = new StringBuilder("method=" + names(m.annotations())
                                             + " return=" + names(m.returnType().annotations()));
        for (ParameterInfo pi : m.parameters()) {
            sb.append(" | ").append(pi.name()).append(": decl=").append(names(pi.annotations()))
                    .append(" type=").append(names(pi.parameterizedType().annotations()));
            pi.parameterizedType().parameters().forEach(arg ->
                    sb.append(" typeArg=").append(names(arg.annotations())));
        }
        return sb.toString();
    }

    private static MethodInfo method(TypeInfo x, String name) {
        return x.methodStream().filter(m -> name.equals(m.simpleName())).findFirst().orElseThrow();
    }

    @DisplayName("a method called before its declaration routes TYPE_USE annotations like an uncalled one")
    @Test
    public void calledAndUncalledAgree() {
        TypeInfo x = scan(false, Map.of("ann.TypeOnly", TYPE_ONLY, "a.b.X", SUBJECT)).primaryTypes().stream()
                .filter(t -> "a.b.X".equals(t.fullyQualifiedName())).findFirst().orElseThrow();

        String expected = "method=[] return=[TypeOnly] | s: decl=[] type=[TypeOnly] | o: decl=[] type=[TypeOnly]";
        assertEquals(expected, placement(method(x, "uncalled")), "the fresh-method path, the reference");
        assertEquals(expected, placement(method(x, "called")),
                "created from its symbol first (caller() calls it), so visited on the already-known branch");

        String expectedList = "method=[] return=[] | list: decl=[] type=[] typeArg=[TypeOnly]";
        assertEquals(expectedList, placement(method(x, "uncalledWithList")));
        assertEquals(expectedList, placement(method(x, "calledWithList")));
    }
}
