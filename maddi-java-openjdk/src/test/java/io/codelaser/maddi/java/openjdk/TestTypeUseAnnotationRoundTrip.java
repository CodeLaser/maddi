package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>TYPE-USE annotations (JLS 9.7.4, {@code ElementType.TYPE_USE}, Java 8) are dropped by the parse.</b>
 * <p>
 * A declaration annotation belongs to a method, field, parameter or class, and maddi carries those: they
 * survive into {@code MethodInfo.annotations()}, {@code FieldInfo.annotations()}, and even
 * {@code TypeParameterImpl.annotations()} for {@code <@Foo T>}. A TYPE-USE annotation belongs to a USE OF A
 * TYPE, and there is frequently no declaration to attach it to at all — in
 * {@code BiFunction<K, @Nullable V, @Nullable V>} the annotations sit on the second and third type
 * arguments, where no method, field, parameter or class exists. The only thing they can attach to is the
 * type, and {@code ParameterizedType} has no slot for them.
 * <p>
 * ⛔ MOVING ONE TO THE DECLARATION CHANGES ITS MEANING, so this is not a placement detail.
 * {@code @Nullable} on a method says "this method may return null"; the same annotation on the type
 * argument of {@code CompletableFuture<@Nullable E>} says "the future may complete with null". The method
 * that produced this test never returns null. NullAway reports the two separately, and named the second:
 * <i>"mismatched type parameter nullability"</i>.
 * <p>
 * ⚠ WHY IT SURFACES ONLY WHEN GENERATING SOURCE. Editing existing source by character range leaves the
 * surrounding text alone, so annotations survive untouched — which is why this went unseen. A type PRINTED
 * FROM THE MODEL can only carry what the model holds. Found by the refactoring toolkit's extract-interface
 * operation, which generates the new interface file: the implementation kept
 * {@code CompletableFuture<@Nullable CacheEntryListenerException> chainSynchronous()} while the generated
 * interface declared {@code CompletableFuture<CacheEntryListenerException> chainSynchronous()}, so the class
 * was no longer a legal implementation of the interface extracted from it.
 * <p>
 * The assertion is a round trip: parse, print, and require every annotation still to be there. It fails on
 * the type-use positions and passes on the declaration ones, which is the whole point — the two are asserted
 * side by side so a fix cannot regress what already works.
 */
public class TestTypeUseAnnotationRoundTrip extends CommonTest {

    /** Declared TYPE_USE, so it is legal exactly in the positions the subject uses it. */
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

    private static final String SUBJECT = """
            package a.b;
            import ann.Nullable;
            import java.util.List;
            import java.util.Map;
            import java.util.concurrent.CompletableFuture;

            public class X {
                @Deprecated
                public List<@Nullable String> insideTypeArgument() { return null; }

                public CompletableFuture<@Nullable String> theCaffeineShape() { return null; }

                public Map<String, @Nullable List<@Nullable String>> nested() { return null; }

                public void asParameter(List<@Nullable String> in) { }

                public @Nullable String onTheReturnType() { return null; }
            }
            """;

    private String parseAndPrint() {
        TypeInfo x = scan(false, Map.of("ann.Nullable", NULLABLE, "a.b.X", SUBJECT))
                .primaryTypes().stream()
                .filter(t -> "a.b.X".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        OutputBuilder ob = runtime.newCompilationUnitPrinter(x.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(120).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.CHOP_DOWN).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    /**
     * ⛔ WHOLE FILE, NOT A SUBSTRING. Every earlier version of this test asserted
     * {@code printed.contains("List<@Nullable String>")} and friends, and that is how the MISSING IMPORT went
     * unnoticed for several rounds: every annotation was in the output, each substring matched, and the file
     * would not have compiled. A generated file is only correct as a whole — imports, placement and all — so
     * that is what is compared.
     * <p>
     * ⚠ The qualification ({@code java.util.List}) and the layout are the printer's own decisions at
     * CHOP_DOWN / 120 columns, and are reproduced here as-is. If a formatting change makes this fail, read
     * the diff before assuming an annotation was lost.
     */
    @DisplayName("the printed file is exactly the source, annotations and all")
    @Test
    public void roundTripEmitsTheSameCode() {
        @Language("java")
        String expected = """
                package a.b;
                import ann.Nullable;
                import java.util.List;
                import java.util.Map;
                import java.util.concurrent.CompletableFuture;
                public class X {
                    @Deprecated public java.util.List<@Nullable String> insideTypeArgument() { return null; }
                    public CompletableFuture<@Nullable String> theCaffeineShape() { return null; }
                    public Map<String, @Nullable java.util.List<@Nullable String>> nested() { return null; }
                    public void asParameter(java.util.List<@Nullable String> in) { }
                    public @Nullable String onTheReturnType() { return null; }
                }
                """;
        // trailing newline: the text block always ends with one, the printer does not always emit one.
        assertEquals(expected.stripTrailing(), parseAndPrint().stripTrailing());
    }
}
