package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>#150 — THE DISCRIMINATING EXPERIMENT.</b> Root cause is NOT established, and this file exists to
 * establish it rather than to assert a fix.
 * <p>
 * SYMPTOM: parsing {@code x-pack ml/test} drops one compilation unit with <i>"Trying to compute access of
 * ...WordCountMapReducer.WordCounts (from modifiers: PACKAGE), but access of enclosing type
 * ...WordCountMapReducer not yet set"</i>, and {@code SummaryImpl.parseResult()} then refuses the WHOLE
 * ParseResult — one bad unit costs the entire run (#94).
 * <p>
 * ⛔ THE OBVIOUS EXPLANATION WAS REFUTED BY MEASUREMENT. I first held that depth-1 nesting is safe and
 * depth-2 fatal. A brace-depth scan of 30,964 corpus files found the shape in only two files, and the
 * other one — {@code server/main}'s {@code search/suggest/Suggest.java}, whose {@code Suggestion} /
 * {@code Entry} / {@code Option} chain is nested three deep and used as its own type argument — IS IN THE
 * PARSE AND PARSES FINE. So depth is necessary-looking but not sufficient, and something else discriminates.
 * <p>
 * The two remaining differences between the fatal file and the healthy one, from the gap record:
 * <ol>
 *   <li>WHERE THE ENCLOSING GENERIC IS DECLARED. In ml both enclosing generics are in OTHER compilation
 *       units, so completing those symbols may force javac to resolve the deep nested type ahead of the
 *       visitor's own order. In {@code Suggest} the outer generic is in the SAME unit.</li>
 *   <li>ACCESS OF THE NESTED TYPES. ml's are package-private — the message even says "from modifiers:
 *       PACKAGE" — where {@code Suggest}'s are public.</li>
 * </ol>
 * Both are varied here, independently, so the answer is attributed to a factor rather than to the pile.
 * ⚠ A 2×2 whose four cells all pass would ALSO be a result: it would mean the reduction is missing a
 * third factor, and the next step is the real file rather than another guess.
 */
public class TestGap150NestedAccessOrder {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(sourceSet)
                .build();
        javaInspector.initialize(ic);
    }

    // ---- the base, in a SEPARATE compilation unit: a generic whose parameter the subject must satisfy ----
    @Language("java")
    private static final String BASE_OTHER_UNIT = """
            package a.b;
            public abstract class Base<T extends Base.Marker> {
                public interface Marker { }
                public abstract T make();
            }
            """;

    /** The ml shape: deep nested type, PACKAGE-private, feeding a generic declared in ANOTHER unit. */
    @Language("java")
    private static final String OTHER_UNIT_PACKAGE = """
            package a.b;
            class SubjectOtherPackage extends Base<SubjectOtherPackage.Outer.Inner> {
                static class Outer {
                    static class Inner implements Base.Marker { }
                }
                public Outer.Inner make() { return new Outer.Inner(); }
            }
            """;

    /** Same, but the nested types are PUBLIC — isolating factor (2). */
    @Language("java")
    private static final String OTHER_UNIT_PUBLIC = """
            package a.b;
            class SubjectOtherPublic extends Base<SubjectOtherPublic.Outer.Inner> {
                public static class Outer {
                    public static class Inner implements Base.Marker { }
                }
                public Outer.Inner make() { return new Outer.Inner(); }
            }
            """;

    /**
     * The Suggest shape: the enclosing generic is in the SAME compilation unit — isolating factor (1).
     * ⚠ Declared as a SECOND TOP-LEVEL type in the file, not as a nested class of the subject. The first
     * attempt made the subject extend its OWN nested class; that derailed the scan before it reached the
     * subject at all (an assert in indexJavaLangForJavaDocParsing), so the cell measured nothing. A control
     * that cannot run is not a control.
     */
    @Language("java")
    private static final String SAME_UNIT_PACKAGE = """
            package a.b;
            interface LocalMarkerP { }
            abstract class LocalBaseP<T extends LocalMarkerP> { abstract T make(); }
            class SubjectSamePackage extends LocalBaseP<SubjectSamePackage.Outer.Inner> {
                static class Outer {
                    static class Inner implements LocalMarkerP { }
                }
                Outer.Inner make() { return new Outer.Inner(); }
            }
            """;

    /** Same unit, PUBLIC nested types: the cell closest to the corpus file that parses fine. */
    @Language("java")
    private static final String SAME_UNIT_PUBLIC = """
            package a.b;
            interface LocalMarkerQ { }
            abstract class LocalBaseQ<T extends LocalMarkerQ> { abstract T make(); }
            class SubjectSamePublic extends LocalBaseQ<SubjectSamePublic.Outer.Inner> {
                public static class Outer {
                    public static class Inner implements LocalMarkerQ { }
                }
                Outer.Inner make() { return new Outer.Inner(); }
            }
            """;

    /**
     * A unit that shares nothing with the subject. ⚠ Its only job is to hold the number of compilation
     * units EQUAL across all four cells: the same-unit cells naturally have one unit and the other-unit
     * cells two, and unit COUNT would otherwise be a third, uncontrolled factor.
     */
    @Language("java")
    private static final String FILLER = """
            package a.b;
            class Filler { int n() { return 1; } }
            """;

    /**
     * @return the parse-failure text, or "" when the unit parsed. ⛔ Reports rather than asserts, because
     * the point of this fixture is to LOCATE the discriminator: a test that only says pass/fail cannot say
     * WHICH factor moved it.
     */
    private String parseOutcome(String name, Map<String, String> units) {
        Summary summary = javaInspector.parseMultiSourceSet(Map.of(sourceSet, units),
                JavaInspectorImpl.DETAILED_SOURCES);
        List<Throwable> problems = summary.parseExceptions().stream()
                .map(Summary.ParseException::throwable).toList();
        if (problems.isEmpty()) return "";
        return problems.stream().map(t -> String.valueOf(t.getMessage())).findFirst().orElse("?");
    }

    @DisplayName("#150 cell 1/4: other-unit generic, PACKAGE-private nested — the ml shape")
    @Test
    public void otherUnitPackage() {
        String r = parseOutcome("otherUnitPackage",
                Map.of("a.b.Base", BASE_OTHER_UNIT, "a.b.SubjectOtherPackage", OTHER_UNIT_PACKAGE));
        System.out.println("#150 CELL otherUnitPackage -> " + (r.isEmpty() ? "PARSED" : r));
    }

    @DisplayName("#150 cell 2/4: other-unit generic, PUBLIC nested")
    @Test
    public void otherUnitPublic() {
        String r = parseOutcome("otherUnitPublic",
                Map.of("a.b.Base", BASE_OTHER_UNIT, "a.b.SubjectOtherPublic", OTHER_UNIT_PUBLIC));
        System.out.println("#150 CELL otherUnitPublic -> " + (r.isEmpty() ? "PARSED" : r));
    }

    @DisplayName("#150 cell 3/4: same-unit generic, PACKAGE-private nested")
    @Test
    public void sameUnitPackage() {
        String r = parseOutcome("sameUnitPackage", Map.of("a.b.Filler", FILLER, "a.b.SubjectSamePackage", SAME_UNIT_PACKAGE));
        System.out.println("#150 CELL sameUnitPackage -> " + (r.isEmpty() ? "PARSED" : r));
    }

    @DisplayName("#150 cell 4/4: same-unit generic, PUBLIC nested — the Suggest shape")
    @Test
    public void sameUnitPublic() {
        String r = parseOutcome("sameUnitPublic", Map.of("a.b.Filler", FILLER, "a.b.SubjectSamePublic", SAME_UNIT_PUBLIC));
        System.out.println("#150 CELL sameUnitPublic -> " + (r.isEmpty() ? "PARSED" : r));
    }

    /**
     * CELL 5 — THE REFINED HYPOTHESIS, after all four of the 2×2 reproduced.
     * <p>
     * Neither filed factor discriminates, so the minimal shape is SUFFICIENT on its own and it is
     * {@code Suggest.java} that is the exception, not ml. What every reproducing cell shares is that the
     * DEPTH-2 type is the DIRECT type argument of the supertype clause. Suggest is not like that:
     * <pre>Suggest implements Iterable&lt;Suggest.Suggestion&lt;? extends Entry&lt;? extends Option&gt;&gt;&gt;</pre>
     * its direct type argument is {@code Suggestion}, which is depth ONE; the deeper types appear only
     * inside WILDCARD BOUNDS of that argument.
     * <p>
     * ⛔ SO r40's CORPUS SCAN MEASURED THE WRONG THING. It scanned brace depth of the DECLARATION and
     * found both files "depth ≥ 2", which is true and not the question. THE DEFECT IS ABOUT THE USE, NOT
     * THE DECLARATION — a distinction no declaration-shaped finder can make.
     * <p>
     * PREDICTION, written before running: this cell PARSES.
     */
    @Language("java")
    private static final String NESTED_IN_WILDCARD = """
            package a.b;
            interface MarkerW { }
            abstract class BaseW<T extends MarkerW> { abstract T make(); }
            interface HolderW<X> { }
            class SubjectWildcard extends BaseW<SubjectWildcard.Shallow> {
                static class Shallow implements MarkerW, HolderW<SubjectWildcard.Shallow.Deep> {
                    static class Deep { }
                }
                Shallow make() { return new Shallow(); }
            }
            """;

    @DisplayName("#150 cell 5: the depth-2 type appears only INSIDE a type argument — the Suggest shape")
    @Test
    public void nestedOnlyInsideTypeArgument() {
        String r = parseOutcome("nestedInWildcard", Map.of("a.b.SubjectWildcard", NESTED_IN_WILDCARD));
        System.out.println("#150 CELL nestedInWildcard -> " + (r.isEmpty() ? "PARSED" : r));
    }
}
