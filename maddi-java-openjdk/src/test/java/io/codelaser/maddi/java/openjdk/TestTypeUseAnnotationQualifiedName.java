package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CodeLaser/maddi#113. The source reaches the nested types {@code P} and {@code C} by simple name through
 * {@code import static q.A.M.*}; the printer qualifies them from the primary type ({@code A.M.P}). A type-use
 * annotation must then sit before the simple name (JLS 9.7.4): {@code A.M.@NN P<T>}. {@code @NN A.M.P<T>} does
 * not compile. Seen on vavr's {@code API} and {@code Try} (jspecify {@code @NonNull Pattern0<T>}, 63 errors).
 */
public class TestTypeUseAnnotationQualifiedName extends CommonTest {

    private static final String NN = """
            package q;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE_USE)
            public @interface NN { }
            """;

    private static final String A = """
            package q;
            import static q.A.M.*;
            public class A {
                public static final class M {
                    public interface P<T> { }
                    public interface C<T> { }
                }
                static <T> void pattern(@NN P<T> p) { }
                static <T> void cases(@NN C<? extends T> @NN ... cases) { }
            }
            """;

    @DisplayName("an annotated nested type printed qualified carries its annotation before the simple name")
    @Test
    public void test() {
        TypeInfo a = scan(false, Map.of("q.NN", NN, "q.A", A)).primaryTypes().stream()
                .filter(t -> "q.A".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        OutputBuilder ob = runtime.newCompilationUnitPrinter(a.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(120).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.CHOP_DOWN).build();
        String printed = new Formatter2Impl(runtime, options).write(ob);
        assertFalse(printed.contains("@NN A.M."), printed);
        assertTrue(printed.contains("A.M.@NN P<T> p") || printed.contains("@NN P<T> p"), printed);
        assertTrue(printed.contains("A.M.@NN C<? extends T> @NN ... cases") || printed.contains("@NN C<? extends T> @NN ... cases"),
                printed);
    }
}
