package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
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
 * <b>BASELINE. What declaration annotations do TODAY — written so a type-use fix cannot break them
 * silently.</b>
 * <p>
 * ⛔ EVERY TEST HERE MUST PASS BEFORE AND AFTER. This is a safety net, not a gap list. Its companions
 * {@code TestTypeUseAnnotationRoundTrip} and {@code TestTypeUseAnnotationDistinguishesUses} fail on
 * purpose; nothing in this file may.
 * <p>
 * The reason it exists: {@code ScanCompilationUnit.convertTypeWithAnnotations} extracts a type's
 * annotations and hands them to a consumer, and two of its three call sites then attach them to the
 * DECLARATION — {@code builder.setReturnType(returnType).addAnnotations(annots)} at :876 for a method,
 * {@code parameterInfo.builder().addAnnotations(annots)} at :885 for a parameter. That routing is what a
 * fix would have to change, and it cannot be changed blind: an annotation legal in both positions is
 * currently found on the declaration, and callers may depend on that.
 * <p>
 * ⚠ THE LAST TWO TESTS PIN BEHAVIOUR THAT A FIX IS EXPECTED TO CHANGE. They are not asserting that the
 * current routing is right — they record where the annotation lands today, so that moving it produces a
 * loud, reviewable failure rather than a silent behavioural shift. If you are implementing the fix and one
 * of them fails, that is the signal to decide deliberately, update it, and check who reads those lists.
 */
public class TestDeclarationAnnotationsBaseline extends CommonTest {

    /** Legal ONLY on declarations. Cannot be a type-use annotation, so its routing is unambiguous. */
    private static final String DECL_ONLY = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marked {
                String value() default "";
            }
            """;

    /** Legal in BOTH positions — the ambiguous case that decides where the parser puts it. */
    private static final String BOTH = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.PARAMETER})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Either {}
            """;

    private static final String SUBJECT = """
            package a.b;
            import ann.Marked;
            import ann.Either;

            @Marked("on the class")
            public class X {
                @Marked("on the field")
                public String field = "";

                @Marked("on the method")
                public String method() { return null; }

                public String withParameter(@Marked("on the parameter") String in) { return null; }

                public @Either String eitherInReturnPosition() { return null; }

                public String eitherOnParameter(@Either String in) { return null; }
            }
            """;

    private TypeInfo parse() {
        return scan(false, Map.of("ann.Marked", DECL_ONLY, "ann.Either", BOTH, "a.b.X", SUBJECT))
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

    // ---------------------------------------------------------------- unambiguous declaration positions

    @DisplayName("a declaration annotation on a CLASS reaches TypeInfo.annotations()")
    @Test
    public void onClass() {
        assertEquals(List.of("Marked"), names(parse().annotations()));
    }

    @DisplayName("a declaration annotation on a FIELD reaches FieldInfo.annotations()")
    @Test
    public void onField() {
        FieldInfo fi = parse().fields().stream().filter(f -> "field".equals(f.simpleName()))
                .findFirst().orElseThrow();
        assertEquals(List.of("Marked"), names(fi.annotations()));
    }

    @DisplayName("a declaration annotation on a METHOD reaches MethodInfo.annotations()")
    @Test
    public void onMethod() {
        assertEquals(List.of("Marked"), names(method(parse(), "method").annotations()));
    }

    @DisplayName("a declaration annotation on a PARAMETER reaches ParameterInfo.annotations()")
    @Test
    public void onParameter() {
        ParameterInfo pi = method(parse(), "withParameter").parameters().getFirst();
        assertEquals(List.of("Marked"), names(pi.annotations()));
    }

    @DisplayName("declaration annotations survive parse and print, in their own positions")
    @Test
    public void declarationAnnotationsRoundTrip() {
        String printed = print(parse());
        assertTrue(printed.contains("@Marked(\"on the class\")"), printed);
        assertTrue(printed.contains("@Marked(\"on the field\")"), printed);
        assertTrue(printed.contains("@Marked(\"on the method\")"), printed);
        assertTrue(printed.contains("@Marked(\"on the parameter\")"), printed);
    }

    @DisplayName("an annotation's arguments survive, not just its name")
    @Test
    public void argumentsSurvive() {
        AnnotationExpression a = method(parse(), "method").annotations().getFirst();
        assertEquals("Marked", a.typeInfo().simpleName());
        assertEquals("on the method", a.extractString("value", ""),
                "the key/value pairs are converted too, not only the annotation type");
    }

    // ------------------------------------------------- the ambiguous position: where does it land TODAY

    @DisplayName("CORRECT: an annotation targeting METHOD too is on the method — and must stay there")
    @Test
    public void eitherInReturnPositionStaysOnTheMethod() {
        MethodInfo mi = method(parse(), "eitherInReturnPosition");
        assertEquals(List.of("Either"), names(mi.annotations()),
                "@Either targets both TYPE_USE and METHOD, and javac emits RuntimeVisibleAnnotations for"
                + " it, so the method IS a correct home. This must keep passing: the fix adds the annotation"
                + " to the type as well, it does not move it off the declaration.");
    }

    @DisplayName("CORRECT: an annotation targeting PARAMETER too is on the parameter — and must stay there")
    @Test
    public void eitherOnParameterStaysOnTheParameter() {
        ParameterInfo pi = method(parse(), "eitherOnParameter").parameters().getFirst();
        assertEquals(List.of("Either"), names(pi.annotations()),
                "@Either targets PARAMETER too, so the parameter is a correct home. Same rule as above:"
                + " the fix adds, it does not move.");
    }
}
