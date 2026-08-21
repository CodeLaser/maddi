package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>Two uses of ONE type, one annotated and one not, must be two distinct values.</b>
 * <p>
 * The obvious worry about carrying a type-use annotation on {@code ParameterizedType} is identity: if a
 * {@code ParameterizedType} were a unique, shared object per type, then {@code @Nullable String} and plain
 * {@code String} would have to be the same object, and annotating one would annotate every {@code String}
 * in the model.
 * <p>
 * ⭐ IT IS NOT SHARED, AND THE PRECEDENT IS ALREADY IN THE CLASS. {@code ParameterizedTypeImpl} is a VALUE:
 * {@code equals}/{@code hashCode} are computed over (typeInfo, typeParameter, parameters, arrays, wildcard,
 * nullable), and {@code withNullable} <i>constructs a new instance</i> rather than mutating or returning a
 * canonical one. NULLABILITY IS ALREADY A PER-USE DIMENSION OF THE TYPE — {@code NullableState}, added for
 * Kotlin's {@code String?} — and it is part of {@code equals} precisely so that the same {@code String} used
 * two ways is two unequal values. So the slot a Java type-use annotation needs already exists and is already
 * proven to work; what is missing is that NOTHING IN THE JAVA PATH EVER SETS IT ({@code withNullable} has no
 * caller anywhere outside the type's own API and impl).
 * <p>
 * The first test is the positive control: it demonstrates the model CAN hold the distinction. The two that
 * follow show the Java parser does not populate it, so the distinction is lost at parse — which is why the
 * generated interface in the refactoring toolkit could not print it back.
 * <p>
 * ⚠ {@code hashCode} deliberately excludes {@code nullable} (see its comment: keeping hashes unchanged from
 * before nullability existed, so hash-ordered collections keep their iteration order). That is legal — equal
 * objects still share a hash — and it means annotated and unannotated uses land in the same bucket and are
 * separated by {@code equals}. A type-use annotation should follow the same rule rather than invent another.
 */
public class TestTypeUseAnnotationDistinguishesUses extends CommonTest {

    private static final String NULLABLE = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE_USE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Nullable {}
            """;

    /** The same type argument, used twice in one compilation unit: annotated once, plain once. */
    private static final String SUBJECT = """
            package a.b;
            import ann.Nullable;
            import java.util.List;

            public class X {
                public List<@Nullable String> annotated() { return null; }

                public List<String> plain() { return null; }
            }
            """;

    private TypeInfo parse() {
        return scan(false, Map.of("ann.Nullable", NULLABLE, "a.b.X", SUBJECT))
                .primaryTypes().stream()
                .filter(t -> "a.b.X".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
    }

    /** The single type argument of the named method's {@code List<...>} return type. */
    private ParameterizedType typeArgumentOf(TypeInfo x, String methodName) {
        MethodInfo mi = x.methodStream().filter(m -> methodName.equals(m.simpleName()))
                .findFirst().orElseThrow();
        return mi.returnType().parameters().getFirst();
    }

    private String print(TypeInfo x) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(x.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(140).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @DisplayName("CONTROL: a ParameterizedType is a value, so one type can carry two different per-use states")
    @Test
    public void parameterizedTypeIsAValueNotASharedObject() {
        ParameterizedType plain = typeArgumentOf(parse(), "plain");
        ParameterizedType marked = plain.withNullable(NullableState.NULLABLE);

        assertNotSame(plain, marked,
                "withNullable must construct a new instance; if it returned the same object, a per-use"
                + " dimension could not exist at all");
        assertNotEquals(plain, marked,
                "the two uses must compare unequal, or the model cannot tell them apart");
        assertEquals(NullableState.UNSPECIFIED, plain.nullable(),
                "a Java type carries no nullability today");
        assertEquals(NullableState.NULLABLE, marked.nullable());
    }

    @DisplayName("GAP: the annotated and the plain use of String must not be the same value")
    @Test
    public void annotatedAndPlainUseAreDistinct() {
        TypeInfo x = parse();
        ParameterizedType annotated = typeArgumentOf(x, "annotated");
        ParameterizedType plain = typeArgumentOf(x, "plain");

        assertNotEquals(plain, annotated,
                "'List<@Nullable String>' and 'List<String>' state different things about the element, so"
                + " their type arguments must not be equal. They are equal today because the annotation is"
                + " dropped at parse, leaving two identical plain Strings.");
    }

    @DisplayName("the annotation reaches the model, on the annotated use only")
    @Test
    public void annotationReachesTheModel() {
        TypeInfo x = parse();

        // ⚠ ASSERTED ON annotations(), NOT ON nullable(). Both dimensions live on the type, but they are
        // different things and the decision was deliberate: nullable() is a three-valued flag for Kotlin's
        // 'String?' and cannot say WHICH @Nullable was written — caffeine uses org.jspecify, questdb
        // org.jetbrains, trino jakarta. Printing the annotation back needs its identity and its import, so
        // Java type-use annotations are carried as annotations, and nullable() is left to Kotlin.
        assertEquals(List.of(), typeArgumentOf(x, "plain").annotations(),
                "the plain use must carry nothing — a fix must not mark every String");
        assertEquals(List.of("Nullable"),
                typeArgumentOf(x, "annotated").annotations().stream()
                        .map(a -> a.typeInfo().simpleName()).toList(),
                "the annotated use must carry @Nullable on the TYPE, keeping the annotation's identity.");
    }

    @DisplayName("GAP: printing back keeps the two uses distinguishable")
    @Test
    public void printedSourceKeepsThemDistinct() {
        String printed = print(parse());
        assertTrue(printed.contains("<@Nullable String>"),
                "the annotated use must print with its annotation. Printed:\n" + printed);
        assertFalse(printed.replace("<@Nullable String>", "").contains("<@Nullable String>"),
                "and only the annotated one. Printed:\n" + printed);
    }
}
