package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>THE SPEC: where an annotation belongs is decided by its {@code @Target}, and javac says so in the class
 * file.</b>
 * <p>
 * The question this answers is "when {@code @Nullable} is written before a return type, does it belong to the
 * method, to the type, or to both?". It is not a matter of taste — the compiler already decides it, and the
 * decision is observable. Compiling the three shapes below and reading the class file with {@code javap -v}:
 * <pre>
 * public @Either   String m()      TYPE_USE + METHOD   RuntimeVisibleAnnotations  AND  RuntimeVisibleTypeAnnotations
 * public @TypeOnly String m()      TYPE_USE only                                       RuntimeVisibleTypeAnnotations
 * public List&lt;@Either String&gt; m()  (a type argument)                                   RuntimeVisibleTypeAnnotations
 * </pre>
 * ▶ <b>THE RULE:</b> an annotation applicable to BOTH a declaration and a type context lands on BOTH. One
 * applicable only to a type context lands ONLY on the type — never on the declaration.
 * <p>
 * ⛔ <b>SO TODAY'S BEHAVIOUR IS WRONG, NOT MERELY INCOMPLETE.</b> maddi attaches a return-position annotation
 * to the METHOD unconditionally ({@code ScanCompilationUnit:876}), including when it is TYPE_USE only. That is
 * a placement javac never produces. It matters in practice because
 * {@code org.jspecify.annotations.Nullable} — the annotation caffeine uses, and the one whose loss produced
 * the NullAway failure — is declared {@code @Target(ElementType.TYPE_USE)} and nothing else (verified against
 * jspecify-1.0.0.jar).
 * <p>
 * ⚠ <b>THE FIX ADDS, IT DOES NOT MOVE — for the both-targets case.</b> {@code TestDeclarationAnnotationsBaseline}
 * asserts that a both-targets annotation is found on the method today, and per the table above that is
 * CORRECT and must stay. What has to change is the TYPE_USE-only case, where the annotation must stop being
 * put on the declaration, and every case, where it must additionally reach the type.
 */
public class TestTypeUseAnnotationTargetRouting extends CommonTest {

    private static final String EITHER = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target({ElementType.TYPE_USE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Either {}
            """;

    /** The jspecify shape: a type context and nothing else. */
    private static final String TYPE_ONLY = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE_USE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface TypeOnly {}
            """;

    private static final String SUBJECT = """
            package a.b;
            import ann.Either;
            import ann.TypeOnly;

            public class X {
                public @Either String bothTargets() { return null; }

                public @TypeOnly String typeUseOnly() { return null; }
            }
            """;

    private TypeInfo parse() {
        return scan(false, Map.of("ann.Either", EITHER, "ann.TypeOnly", TYPE_ONLY, "a.b.X", SUBJECT))
                .primaryTypes().stream()
                .filter(t -> "a.b.X".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
    }

    private static MethodInfo method(TypeInfo x, String name) {
        return x.methodStream().filter(m -> name.equals(m.simpleName())).findFirst().orElseThrow();
    }

    private static List<String> names(List<AnnotationExpression> annotations) {
        return annotations.stream().map(a -> a.typeInfo().simpleName()).sorted().toList();
    }

    private String print(TypeInfo x) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(x.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(140).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @DisplayName("CORRECT TODAY: an annotation targeting BOTH is on the method — javac emits it there too")
    @Test
    public void bothTargetsIsOnTheMethod() {
        assertEquals(List.of("Either"), names(method(parse(), "bothTargets").annotations()),
                "javac writes RuntimeVisibleAnnotations for this one, so the declaration is a correct home"
                + " for it. A fix must ADD the type, not move it off the method.");
    }

    @DisplayName("GAP: a TYPE_USE-only annotation must NOT be recorded on the method")
    @Test
    public void typeUseOnlyIsNotOnTheMethod() {
        assertEquals(List.of(), names(method(parse(), "typeUseOnly").annotations()),
                "javac emits NO RuntimeVisibleAnnotations for a TYPE_USE-only annotation — only"
                + " RuntimeVisibleTypeAnnotations. maddi puts it on the method anyway"
                + " (ScanCompilationUnit:876 attaches unconditionally), a placement the compiler never"
                + " produces. org.jspecify.annotations.Nullable is exactly this shape.");
    }

    @DisplayName("GAP: a TYPE_USE-only annotation prints in TYPE position, not before the modifiers")
    @Test
    public void typeUseOnlyPrintsInTypePosition() {
        String printed = print(parse());
        assertTrue(printed.contains("@TypeOnly String typeUseOnly()"),
                "it annotates the return TYPE, so it belongs between the modifiers and the type."
                + " Printed today as '@TypeOnly public String ...', i.e. as a modifier of the method.\n"
                + printed);
    }
}
