package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>#150 — FIXED.</b> This file began as the discriminating EXPERIMENT (root cause unknown) and is now the
 * REGRESSION SPEC: all five cells must parse. The experiment's record is kept below because it cost two
 * refuted hypotheses and one wrongly-measured corpus scan, and because cell 5 is what identified the
 * discriminator.
 * <p>
 * ⛔⛔ <b>AND THE ESTABLISHED ROOT CAUSE NAMED THE WRONG CODE SITE.</b> The gap record stated the failing
 * caller was {@code ScanCompilationUnit.continueType()} — <i>"the only type-level caller"</i> — and proposed
 * moving access computation into the late pass there. A stack trace from this fixture shows the throw never
 * reaches that class: it comes from {@link ClassSymbolScanner#loadType}, and the proposed fix would not have
 * touched the failing path. ▶ <b>A ROOT CAUSE ESTABLISHED FROM A SHAPE IS NOT A LOCATION; PRINT THE STACK.</b>
 * <p>
 * THE MECHANISM. {@code loadType} pre-loads the enclosing type before computing access, precisely so the
 * enclosing's access is available. That pre-load is a <b>no-op in exactly the one case it exists for</b>:
 * when the enclosing is an ancestor already on the stack, {@code recursionPrevention.add} returns false and
 * the body — including the enclosing's own {@code computeAccess} — is skipped. ⇒ <b>a recursion guard that
 * silently turns "already in progress" into "already done"</b>. What puts the ancestor on the stack is
 * {@code convert(cs.getSuperclass())} reaching back DOWN into a nested type: loading {@code Outer} pre-loads
 * {@code Subject}, whose supertype clause {@code Base<Subject.Outer.Inner>} resolves {@code Inner}, whose own
 * pre-load of {@code Outer} is the no-op. The chain is built UPWARD while the supertype conversion re-enters
 * DOWNWARD past the type being loaded.
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
     * Asserts the units parse, and on failure reports the message AND the e2immu frames of the stack — the
     * frames are what refuted the gap record's stated code site, so they belong in the failure output rather
     * than in a one-off debugging session.
     */
    private void assertParses(String name, Map<String, String> units) {
        String r = parseOutcome(name, units);
        System.out.println("#150 CELL " + name + " -> " + (r.isEmpty() ? "PARSED" : r));
        assertEquals("", r, "#150 regression in cell " + name);
    }

    /**
     * @return the parse-failure text plus the e2immu stack frames, or "" when the unit parsed.
     */
    private String parseOutcome(String name, Map<String, String> units) {
        Summary summary = javaInspector.parseMultiSourceSet(Map.of(sourceSet, units),
                JavaInspectorImpl.DETAILED_SOURCES);
        List<Throwable> problems = summary.parseExceptions().stream()
                .map(Summary.ParseException::throwable).toList();
        if (problems.isEmpty()) return "";
        Throwable t = problems.getFirst();
        StringBuilder sb = new StringBuilder(String.valueOf(t.getMessage()));
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getClassName().startsWith("io.codelaser.maddi")) sb.append("\n      at ").append(e);
        }
        return sb.toString();
    }

    @DisplayName("#150 cell 1/4: other-unit generic, PACKAGE-private nested — the ml shape")
    @Test
    public void otherUnitPackage() {
        assertParses("otherUnitPackage",
                Map.of("a.b.Base", BASE_OTHER_UNIT, "a.b.SubjectOtherPackage", OTHER_UNIT_PACKAGE));
    }

    @DisplayName("#150 cell 2/4: other-unit generic, PUBLIC nested")
    @Test
    public void otherUnitPublic() {
        assertParses("otherUnitPublic",
                Map.of("a.b.Base", BASE_OTHER_UNIT, "a.b.SubjectOtherPublic", OTHER_UNIT_PUBLIC));
    }

    @DisplayName("#150 cell 3/4: same-unit generic, PACKAGE-private nested")
    @Test
    public void sameUnitPackage() {
        assertParses("sameUnitPackage", Map.of("a.b.Filler", FILLER, "a.b.SubjectSamePackage", SAME_UNIT_PACKAGE));
    }

    @DisplayName("#150 cell 4/4: same-unit generic, PUBLIC nested — the Suggest shape")
    @Test
    public void sameUnitPublic() {
        assertParses("sameUnitPublic", Map.of("a.b.Filler", FILLER, "a.b.SubjectSamePublic", SAME_UNIT_PUBLIC));
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
        assertParses("nestedInWildcard", Map.of("a.b.SubjectWildcard", NESTED_IN_WILDCARD));
    }

    /**
     * CELL 6 — A DEEPER REPRODUCTION: the type argument is THREE levels below the subject, not two.
     * <p>
     * Written to run the fix's top-down walk for more than one iteration, on the reasoning that cells 1–4
     * leave exactly ONE ancestor without an access. ⚠ <b>THE MEASUREMENT SAYS OTHERWISE, AND IT IS WORTH
     * RECORDING.</b> Unfixed, this cell throws on {@code a.b.SubjectDeep.L1.L2} with {@code L1} missing — one
     * level ABOVE where the argument points. The scanner reaches the chain early enough that only one
     * ancestor is ever missing at the moment of the throw, so <b>the loop's multi-iteration path is
     * DEFENSIVE, not exercised by any cell here</b>. It is kept because it costs nothing and the reasoning
     * behind top-down ordering still holds; it is not covered, and this comment says so rather than letting
     * the cell's existence imply it.
     * <p>
     * ▶ <b>A FIXTURE THAT PICKS ITS OWN SHAPE CANNOT EXPOSE A FIX WHOSE SCOPE IS TOO NARROW</b> (#98's
     * pre-flight scope, again) — and the reverse also bit here: it cannot confirm a scope is EXERCISED
     * either. Verified RED against the unfixed scanner, like cells 1–4.
     */
    @Language("java")
    private static final String DEPTH_3_ARGUMENT = """
            package a.b;
            interface MarkerD { }
            abstract class BaseD<T extends MarkerD> { abstract T make(); }
            class SubjectDeep extends BaseD<SubjectDeep.L1.L2.L3> {
                static class L1 {
                    static class L2 {
                        static class L3 implements MarkerD { }
                    }
                }
                L1.L2.L3 make() { return new L1.L2.L3(); }
            }
            """;

    @DisplayName("#150 cell 6: a depth-3 type argument — TWO ancestors lack an access, so the walk iterates")
    @Test
    public void twoMissingAncestors() {
        assertParses("twoMissingAncestors", Map.of("a.b.SubjectDeep", DEPTH_3_ARGUMENT));
    }

    /**
     * CELL 7 — ⚠ AN OPEN QUESTION, RECORDED AS A MEASUREMENT RATHER THAN A CLAIM.
     * <p>
     * This is the ml file's arrangement, minimised: the subject is itself NESTED and its supertype's direct
     * type argument is the subject's OWN nested type, with the same redundant same-unit import the corpus
     * file carries.
     * <pre>
     * class InternalItemSetMapReduceAggregationTests {                                    // the primary
     *     static class WordCountMapReducer extends AbstractItemSetMapReducer&lt;WordCounts, ...&gt; {
     *         static class WordCounts implements ToXContent, Writeable, Closeable { }
     * </pre>
     * ⛔ <b>IT PARSES EVEN AGAINST THE UNFIXED SCANNER</b> — measured, not assumed. So this minimisation does
     * NOT reproduce the corpus failure, and the gap's stated rule ("a type's own depth-2 nested type as the
     * DIRECT type argument of its supertype clause") does not predict it either: relative to its own subject
     * {@code WordCounts} is at depth ONE, which is the {@code Suggest} arrangement that parses. Whatever the
     * ml file's remaining ingredient is, it is not captured here.
     * <p>
     * Kept as an asserted control rather than deleted, because it pins a shape that must keep parsing, and
     * because the next person to touch #150 should know this reduction was tried and came back negative
     * rather than repeat it. ▶ <b>A MINIMISATION THAT DOES NOT REPRODUCE IS A RESULT, NOT A FAILED ATTEMPT</b>
     * — and the one thing not to do with it is quietly assert it green and call it corpus coverage.
     */
    @Language("java")
    private static final String CORPUS_ARRANGEMENT = """
            package a.b;
            import a.b.OuterMost.Mid.Deep;
            interface MarkerC { }
            abstract class BaseC<T extends MarkerC> { abstract T make(); }
            class OuterMost {
                static class Mid extends BaseC<Deep> {
                    static class Deep implements MarkerC { }
                    Deep make() { return new Deep(); }
                }
            }
            """;

    @DisplayName("#150 cell 7: the minimised ml arrangement — PARSES UNFIXED, so it does not reproduce")
    @Test
    public void minimisedCorpusArrangementDoesNotReproduce() {
        assertParses("corpusArrangement", Map.of("a.b.OuterMost", CORPUS_ARRANGEMENT));
    }
}
