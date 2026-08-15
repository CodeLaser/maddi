package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>A wildcard's bound keeps its array dimensions and its type arguments.</b>
 * <p>
 * {@code ClassSymbolScanner.convertTree}'s wildcard branch rebuilt the bound from {@code base.typeInfo()} alone,
 * passing a literal {@code 0} for the dimensions and {@code List.of()} for the type arguments. Every other branch
 * preserves dimensions — the array branch even descends through {@code JCAnnotatedType} so
 * {@code char[] @Nullable []} counts correctly — which is exactly what made this one easy to miss.
 * <p>
 * ⛔ <b>IT CRASHED ONLY FOR PRIMITIVE ELEMENT TYPES, AND THAT WAS THE LUCKY CASE.</b> {@code ? extends int[]}
 * collapsed to a bare {@code int}, which as a type argument tripped {@code ParameterizedTypeImpl}'s "no primitive
 * type arguments" assertion (guava's {@code TypeTokenTest:676}, {@code TypesTest:202}). But
 * {@code isPrimitiveExcludingVoid()} is {@code arrays == 0 && typeInfo.isPrimitiveExcludingVoid()}, so for
 * REFERENCE elements nothing fired: {@code ? extends Object[]} silently became {@code ? extends Object} and was
 * used that way, in every corpus, for as long as the branch existed. The visible crash was two files; the silent
 * corruption was everywhere.
 * <p>
 * ⭐ Two losses beyond the reported one, found by probing every wildcard shape rather than only the crashing
 * one: the TYPE-PARAMETER leg of the same branch dropped arrays too ({@code ? extends T[]} → {@code ? extends
 * T}), and the {@code List.of()} argument dropped the bound's own type arguments, so
 * {@code ? extends List<String>} came out RAW. Neither produced any diagnostic at all.
 */
public class TestWildcardBoundIsNotFlattened extends CommonTest {

    @Language("java")
    private static final String SRC = """
            package a.b;
            import java.util.List;
            public class W<T> {
                List<? extends Object[]> refArray;
                List<? super String[][]> refArrayTwoDims;
                List<? extends List<String>> nestedArgs;
                List<? extends T[]> typeParamArray;
                List<Object[]> nonWildcardArray;
                List<? extends Object> plainRef;
            }
            """;

    /** The shape that threw: a PRIMITIVE array bound, from guava's TypeTokenTest:676 and TypesTest:202. */
    @Language("java")
    private static final String PRIMITIVE_SRC = """
            package a.b;
            import java.util.List;
            public class P {
                Iterable<? extends int[]> oneDim;
                List<? extends int[][]> twoDims;
            }
            """;

    private static String render(ParameterizedType pt) {
        StringBuilder sb = new StringBuilder();
        if (pt.wildcard() != null) sb.append(pt.wildcard().isExtends() ? "? extends " : "? super ");
        if (pt.typeParameter() != null) sb.append(pt.typeParameter().simpleName());
        else if (pt.typeInfo() != null) sb.append(pt.typeInfo().simpleName());
        else sb.append("?");
        if (!pt.parameters().isEmpty()) {
            sb.append(pt.parameters().stream().map(TestWildcardBoundIsNotFlattened::render)
                    .collect(Collectors.joining(",", "<", ">")));
        }
        sb.append("[]".repeat(Math.max(0, pt.arrays())));
        return sb.toString();
    }

    private Map<String, String> fieldTypes(String fqn, String source) {
        TypeInfo t = scan(fqn, source);
        return t.fields().stream().collect(Collectors.toMap(FieldInfo::name, f -> render(f.type())));
    }

    @DisplayName("a reference-typed wildcard bound keeps its dimensions and its arguments")
    @Test
    public void referenceBoundsSurviveIntact() {
        Map<String, String> f = fieldTypes("a.b.W", SRC);

        assertEquals("List<? extends Object[]>", f.get("refArray"));
        assertEquals("List<? super String[][]>", f.get("refArrayTwoDims"));
        // the List.of() loss: the bound's own arguments
        assertEquals("List<? extends List<String>>", f.get("nestedArgs"));
        // the type-parameter leg of the same branch
        assertEquals("List<? extends T[]>", f.get("typeParamArray"));

        // controls: a non-wildcard array was never affected, and '? extends Object' is normalised by javac to
        // the UNBOUND wildcard, which this branch returns before reaching the bound at all
        assertEquals("List<Object[]>", f.get("nonWildcardArray"));
        assertEquals("List<? super ?>", f.get("plainRef"),
                "javac normalises '? extends Object' to '?'; the renderer prints the unbound form this way");
    }

    /**
     * ⛔ Pre-fix this method does not fail an assertion here — it throws {@code AssertionError} out of
     * {@code scan}, from {@code ParameterizedTypeImpl}'s "no primitive type arguments" check.
     */
    @DisplayName("a primitive array bound no longer collapses to a bare primitive")
    @Test
    public void primitiveArrayBoundDoesNotCollapse() {
        Map<String, String> f = fieldTypes("a.b.P", PRIMITIVE_SRC);
        assertEquals("Iterable<? extends int[]>", f.get("oneDim"));
        assertEquals("List<? extends int[][]>", f.get("twoDims"));
    }
}
