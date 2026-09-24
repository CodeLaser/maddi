/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ⭐ <b>Does the modification analysis reach the same conclusion about a lowered Kotlin shape as about the
 * Java a human would write for it?</b>
 *
 * <p>The statements-in-expression-position family is a set of LOWERINGS: `try` used as a value, an `if`
 * whose branch is a block, `x ?: return`, a null-safe chain. Each rewrites Kotlin into a CST shape Java can
 * express. Everything that had been measured about them until now was about the parse — placeholders,
 * statement indexes, prep isolating nothing. None of that says the analyzer concludes the RIGHT thing, and a
 * lowering can be well formed and wrong: `val t = f() ?: return` evaluated `f()` twice for months without a
 * single red test.
 *
 * <p>So both sides call the SAME Java helper ({@code s.Box}) and differ only in the shape. Any disagreement
 * is the lowering, not the standard library.
 *
 * <h2>What is asserted</h2>
 * Per method pair: whether the method is non-modifying, and whether its {@code Box} parameter is left
 * unmodified. Those two are the sensors that a mis-shaped tree moves — a modification that happens in a
 * `catch` arm, or through a link the lowering broke, shows up here and almost nowhere else.
 */
public class TestLoweredShapesVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestLoweredShapesVsJava.class);

    /** Called identically from both sides, so the comparison is about shape and nothing else. */
    private static final String BOX = """
            package s;
            import java.util.ArrayList;
            import java.util.List;
            public class Box {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                public int size() { return items.size(); }
                public Box next() { return this; }
                public int addAndSize(String s) { items.add(s); return items.size(); }
                public String addAndEcho(String s) { items.add(s); return s; }
                public void touch() { items.add("t"); }
                public void feed(StringSink sink, String t) { sink.accept(t); }
                public void feedBox(BoxSink sink, Box target) { sink.accept(target); }
                // a getter that modifies (a counting/caching getter): Kotlin sees it as the property `count`, so
                // a property reference to it is where bound vs unbound decides the verdict
                public int getCount() { items.add("c"); return items.size(); }
                public int pull(IntSource source) { return source.get(); }
                public int pullBox(BoxToInt f, Box target) { return f.apply(target); }
                public int pullList(ListToInt f, java.util.ArrayList<String> l) { return f.apply(l); }
            }
            """;

    private static final String KOTLIN = """
            package a
            import s.Box
            class K {
                fun tryAsValue(b: Box, t: String): Int {
                    val v = try { b.size() } catch (e: RuntimeException) { b.add(t); -1 }
                    return v
                }
                fun ifAsValue(b: Box, t: String, c: Boolean): Int {
                    val v = if (c) { b.add(t); 1 } else { b.size() }
                    return v
                }
                fun elvisGuard(b: Box?, t: String): Int {
                    val x = b ?: return 0
                    x.add(t)
                    return x.size()
                }
                fun safeChain(b: Box?, t: String): Int {
                    b?.next()?.add(t)
                    return 0
                }
                fun readOnlyChain(b: Box?): Int = b?.next()?.size() ?: 0
                fun whenAsValue(b: Box, t: String, n: Int): Int {
                    val v = when (n) { 0 -> { b.add(t); 1 } else -> b.size() }
                    return v
                }
                fun elvisThrow(b: Box?, t: String): Int {
                    val x = b ?: throw IllegalStateException("null")
                    x.add(t)
                    return x.size()
                }
                // ⚠ the three OFF-SPINE shapes: `c.addAndSize(t)` sits where the hoist deliberately does not
                // reach (an argument, the right of `?:`, a branch arm), so it is still converted twice.
                fun argOffSpine(b: Box?, c: Box, t: String): Int = b?.addAndSize(c.addAndEcho(t)) ?: 0
                fun elvisRightModifies(b: Box?, c: Box, t: String): Int = b?.size() ?: c.addAndSize(t)
                fun armModifies(b: Box?, c: Box, t: String): Int {
                    val v = if (b == null) c.addAndSize(t) else b.size()
                    return v
                }
                fun dupOffSpine(b: Box, c: Box?, t: String): Int = b.addAndSize(c?.addAndEcho(t) ?: t)
                // callable references: bound (`c::add`, the caller's own object) and unbound (`Box::touch`,
                // whose receiver is the sink's argument). Both sides call the SAME Java method with the SAME
                // functional interface, so a disagreement is the reference and not the library.
                fun refBound(b: Box, c: Box, t: String) { b.feed(c::add, t) }
                fun refUnbound(b: Box, c: Box, t: String) { b.feedBox(Box::touch, c) }
                // property references: the getter, bound (`c::count`) and unbound (`Box::count`); and a library
                // property (`ArrayList::size`), which must reach the class file's `size()`
                fun propBound(b: Box, c: Box): Int = b.pull(c::count)
                fun propUnbound(b: Box, c: Box): Int = b.pullBox(Box::count, c)
                fun propLibrary(b: Box, l: java.util.ArrayList<String>): Int = b.pullList(java.util.ArrayList<String>::size, l)
                fun ternaryArm(b: Box?, c: Box, t: String): Int = if (b == null) c.addAndSize(t) else b.size()
                // a context parameter is the LEADING JVM parameter, and a caller in the same context passes its own on
                context(c: Box) fun ctxModifies(b: Box, t: String): Int { c.add(t); return b.size() }
                context(c: Box) fun ctxCaller(b: Box, t: String): Int = ctxModifies(b, t)
                fun arrayStore(a: Array<Box>, b: Box) { a[0] = b }
                fun arrayElementModified(a: Array<Box>, t: String) { a[0].add(t) }
                fun arrayRead(a: Array<Box>): Int = a[0].size()
                fun expressionBodiedTry(b: Box, t: String): Int =
                    try { b.size() } catch (e: RuntimeException) { b.add(t); -1 }
            }
            // member extensions (detekt's CheckstyleOutputReport): two receivers, the dispatch one implicit. On the
            // JVM an instance method taking the extension receiver as argument 0 -- JReport, as a human writes it
            class KReport {
                val id: String = "x"
                private val Box.label: String get() = "n" + size()
                private fun Any.esc(): String = toString().trim()
                fun render(b: Box): String = b.label.esc() + b.esc()
                fun touchVia(b: Box, t: String) { b.poke(t) }
                private fun Box.poke(t: String) { add(t) }
            }
            class KHolder(private val box: Box) {
                fun touch(t: String) { box.next()?.add(t) }
                fun count(): Int = box.size()
            }
            """;

    /** The same five, as a human writes them in Java — which is what each lowering claims to produce. */
    private static final String JAVA = """
            package b;
            import s.Box;
            public class J {
                public int tryAsValue(Box b, String t) {
                    int v;
                    try { v = b.size(); } catch (RuntimeException e) { b.add(t); v = -1; }
                    return v;
                }
                public int ifAsValue(Box b, String t, boolean c) {
                    int v;
                    if (c) { b.add(t); v = 1; } else { v = b.size(); }
                    return v;
                }
                public int elvisGuard(Box b, String t) {
                    if (b == null) return 0;
                    Box x = b;
                    x.add(t);
                    return x.size();
                }
                public int safeChain(Box b, String t) {
                    Box t1 = b == null ? null : b.next();
                    if (t1 != null) t1.add(t);
                    return 0;
                }
                public int readOnlyChain(Box b) {
                    Box t1 = b == null ? null : b.next();
                    return t1 == null ? 0 : t1.size();
                }
                public int whenAsValue(Box b, String t, int n) {
                    int v;
                    if (n == 0) { b.add(t); v = 1; } else { v = b.size(); }
                    return v;
                }
                public int elvisThrow(Box b, String t) {
                    if (b == null) throw new IllegalStateException("null");
                    Box x = b;
                    x.add(t);
                    return x.size();
                }
                public int argOffSpine(Box b, Box c, String t) {
                    Integer r = b == null ? null : b.addAndSize(c.addAndEcho(t));
                    return r == null ? 0 : r;
                }
                public int elvisRightModifies(Box b, Box c, String t) {
                    Integer r = b == null ? null : b.size();
                    return r != null ? r : c.addAndSize(t);
                }
                public int armModifies(Box b, Box c, String t) {
                    int v;
                    if (b == null) { v = c.addAndSize(t); } else { v = b.size(); }
                    return v;
                }
                public int dupOffSpine(Box b, Box c, String t) {
                    String s = c == null ? null : c.addAndEcho(t);
                    return b.addAndSize(s != null ? s : t);
                }
                public void refBound(Box b, Box c, String t) { b.feed(c::add, t); }
                public void refUnbound(Box b, Box c, String t) { b.feedBox(Box::touch, c); }
                public int propBound(Box b, Box c) { return b.pull(c::getCount); }
                public int propUnbound(Box b, Box c) { return b.pullBox(Box::getCount, c); }
                public int propLibrary(Box b, java.util.ArrayList<String> l) { return b.pullList(java.util.ArrayList::size, l); }
                public int ternaryArm(Box b, Box c, String t) { return b == null ? c.addAndSize(t) : b.size(); }
                public int ctxModifies(Box c, Box b, String t) { c.add(t); return b.size(); }
                public int ctxCaller(Box c, Box b, String t) { return ctxModifies(c, b, t); }
                public void arrayStore(Box[] a, Box b) { a[0] = b; }
                public void arrayElementModified(Box[] a, String t) { a[0].add(t); }
                public int arrayRead(Box[] a) { return a[0].size(); }
                public int expressionBodiedTry(Box b, String t) {
                    try { return b.size(); } catch (RuntimeException e) { b.add(t); return -1; }
                }
            }
            """;

    private static final String SINKS = """
            package s;
            public interface StringSink { void accept(String s); }
            """;

    private static final String BOX_SINK = """
            package s;
            public interface BoxSink { void accept(Box b); }
            """;

    private static final String INT_SINKS = """
            package s;
            public interface IntSource { int get(); }
            """;

    private static final String BOX_TO_INT = """
            package s;
            public interface BoxToInt { int apply(Box b); }
            """;

    private static final String LIST_TO_INT = """
            package s;
            public interface ListToInt { int apply(java.util.ArrayList<String> l); }
            """;

    /** A FIELD holding the helper, so the type-level verdict has something to say. */
    private static final String JAVA_HOLDER = """
            package b;
            import s.Box;
            public class JHolder {
                private final Box box;
                public JHolder(Box box) { this.box = box; }
                public void touch(String t) { Box t1 = box.next(); if (t1 != null) t1.add(t); }
                public int count() { return box.size(); }
            }
            """;

    private static final String JAVA_REPORT = """
            package b;
            import s.Box;
            public final class JReport { // final: a Kotlin class is, and an extensible type has hidden content
                private final String id = "x";
                public String getId() { return id; }
                private String getLabel(Box b) { return "n" + b.size(); }
                private String esc(Object o) { return o.toString().trim(); }
                public String render(Box b) { return esc(getLabel(b)) + esc(b); }
                public void touchVia(Box b, String t) { poke(b, t); }
                private void poke(Box b, String t) { b.add(t); }
            }
            """;

    /** The KReport/JReport rows: the member extensions and the public methods that call them. */
    private static final List<String> REPORT_METHODS = List.of("getLabel", "esc", "render", "touchVia", "poke");

    private static final List<String> METHODS =
            List.of("tryAsValue", "ifAsValue", "elvisGuard", "safeChain", "readOnlyChain",
                    "whenAsValue", "elvisThrow", "expressionBodiedTry",
                    "argOffSpine", "elvisRightModifies", "armModifies", "ternaryArm", "dupOffSpine",
                    "refBound", "refUnbound", "propBound", "propUnbound", "propLibrary", "ctxModifies", "ctxCaller",
                    "arrayStore", "arrayElementModified", "arrayRead");

    @Test
    public void everyLoweredShapeAgreesWithTheJavaItClaimsToProduce(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("s/StringSink.java"), SINKS);
        Files.writeString(jDir.resolve("s/BoxSink.java"), BOX_SINK);
        Files.writeString(jDir.resolve("s/IntSource.java"), INT_SINKS);
        Files.writeString(jDir.resolve("s/BoxToInt.java"), BOX_TO_INT);
        Files.writeString(jDir.resolve("s/ListToInt.java"), LIST_TO_INT);
        Files.writeString(jDir.resolve("b/J.java"), JAVA);
        Files.writeString(jDir.resolve("b/JHolder.java"), JAVA_HOLDER);
        Files.writeString(jDir.resolve("b/JReport.java"), JAVA_REPORT);
        Files.writeString(jDir.resolve("s/Box.java"), BOX);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        // ⛔ the guard that keeps this test from agreeing by accident: a shape the front end could not read
        // becomes a placeholder, and two sides can then agree on a verdict neither derived from the code. Every
        // shape below must be FULLY converted, or the comparison proves nothing about the lowering.
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), "unread Kotlin would make the comparison vacuous: " + census.dumpLines());

        // without the annotated APIs java.util.List is an unknown and NOTHING can be concluded on either
        // side, which would make this comparison vacuously equal
        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList(),
                "prep must isolate nothing: an isolated element is an unanswered question, not an answer");
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build()).analyze(order, callGraph);

        TypeInfo k = type(primaryTypes, "a.K");
        TypeInfo j = type(primaryTypes, "b.J");
        StringBuilder report = new StringBuilder("\n");
        StringBuilder kotlinSide = new StringBuilder();
        StringBuilder javaSide = new StringBuilder();
        for (String name : METHODS) {
            String kv = verdict(method(k, name));
            String jv = verdict(method(j, name));
            report.append(String.format("%-14s kotlin: %-44s java: %s%n", name, kv, jv));
            kotlinSide.append(name).append(' ').append(kv).append('\n');
            javaSide.append(name).append(' ').append(jv).append('\n');
        }
        // ⛔ identity check for the duplication probe: if these no longer stand more than once, the three
        // off-spine shapes stopped testing what they were added to test and this comparison is vacuous.
        int kDup = callsTo(method(k, "dupOffSpine"), "addAndEcho");
        report.append(String.format("%-18s addAndEcho in the Kotlin tree: %d×   in the Java tree: %d×%n",
                "dupOffSpine", kDup, callsTo(method(j, "dupOffSpine"), "addAndEcho")));
        // ⛔ identity check: if the Kotlin tree stops holding the call twice, this row no longer asks the
        // duplication question and the agreement below proves nothing about it.
        assertEquals(2, kDup, "dupOffSpine must still DUPLICATE the modifying call, or it tests nothing");

        // ⛔ identity check for the property-reference rows: each side must hold ONE method reference, to the
        // SAME method. Two sides referencing nothing, or the wrong accessor, could still agree on a verdict.
        for (String row : List.of("propBound", "propUnbound", "propLibrary")) {
            List<String> kRefs = referencedMethods(method(k, row));
            report.append(String.format("%-14s references kotlin: %s   java: %s%n", row, kRefs,
                    referencedMethods(method(j, row))));
            assertEquals(1, kRefs.size(), row + " must hold exactly one method reference: " + kRefs);
            assertEquals(referencedMethods(method(j, row)), kRefs, row + " must reference the getter Java names");
        }

        // ⭐ the type-level sensor: a field reached through a lowered shape. A method-level agreement that
        // did not propagate to the type would be agreement about the wrong thing.
        String kHolder = typeVerdict(type(primaryTypes, "a.KHolder"));
        String jHolder = typeVerdict(type(primaryTypes, "b.JHolder"));
        report.append(String.format("%-14s kotlin: %-44s java: %s%n", "«holder»", kHolder, jHolder));
        kotlinSide.append("holder ").append(kHolder).append('\n');
        javaSide.append("holder ").append(jHolder).append('\n');

        // member extensions: every row by POSITION (the Kotlin receiver parameter is `$receiver`, the Java one is
        // named), and the type, which is where detekt's CheckstyleOutputReport moved when these were first read
        TypeInfo kReport = type(primaryTypes, "a.KReport");
        TypeInfo jReport = type(primaryTypes, "b.JReport");
        for (String name : REPORT_METHODS) {
            MethodInfo km = method(kReport, name);
            // ⛔ identity: the Kotlin member extension must BE the instance method Java declares, receiver first
            assertEquals(method(jReport, name).parameters().size(), km.parameters().size(), name + " arity");
            String kv = positionalVerdict(km);
            String jv = positionalVerdict(method(jReport, name));
            report.append(String.format("%-14s kotlin: %-44s java: %s%n", "report." + name, kv, jv));
            kotlinSide.append("report.").append(name).append(' ').append(kv).append('\n');
            javaSide.append("report.").append(name).append(' ').append(jv).append('\n');
        }
        String kReportType = immutability(kReport);
        String jReportType = immutability(jReport);
        report.append(String.format("%-14s kotlin: %-44s java: %s%n", "«report»", kReportType, jReportType));
        kotlinSide.append("report ").append(kReportType).append('\n');
        javaSide.append("report ").append(jReportType).append('\n');

        LOGGER.info("lowered shape vs hand-written Java:{}", report);
        assertEquals(javaSide.toString(), kotlinSide.toString(),
                "a lowered Kotlin shape must yield the same verdicts as the Java it claims to produce");
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in "
                        + types.stream().map(TypeInfo::fullyQualifiedName).sorted().toList()));
    }

    private static MethodInfo method(TypeInfo type, String name) {
        return type.methods().stream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " on " + type.fullyQualifiedName()));
    }

    /** The fully qualified names of the methods this method's tree holds a method reference to. */
    private static List<String> referencedMethods(MethodInfo method) {
        List<String> found = new java.util.ArrayList<>();
        method.methodBody().visit(e -> {
            if (e instanceof MethodReference mr) found.add(mr.methodInfo().fullyQualifiedName());
            return true;
        });
        return found;
    }

    /** How many times `addAndSize` stands in this method's tree. The source writes it once. */
    private static int callsTo(MethodInfo method, String name) {
        int[] n = {0};
        method.methodBody().visit(e -> {
            if (e instanceof MethodCall mc && name.equals(mc.methodInfo().name())) n[0]++;
            return true;
        });
        return n[0];
    }

    /** Type-level: immutability, plus whether the method that walks a lowered chain modifies the field. */
    private static String typeVerdict(TypeInfo type) {
        Value.Immutable immutable = type.analysis()
                .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        String level = immutable.isImmutable() ? "IMMUTABLE"
                : immutable.isAtLeastImmutableHC() ? "IMMUTABLE_HC"
                : immutable.isFinalFields() ? "FINAL_FIELDS" : "MUTABLE";
        boolean touchModifies = !method(type, "touch").analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        return "type=" + level + " touch.modifies=" + touchModifies;
    }

    private static String immutability(TypeInfo type) {
        Value.Immutable immutable = type.analysis()
                .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        return immutable.isImmutable() ? "IMMUTABLE" : immutable.isAtLeastImmutableHC() ? "IMMUTABLE_HC"
                : immutable.isFinalFields() ? "FINAL_FIELDS" : "MUTABLE";
    }

    /** As [verdict], for every parameter by position rather than name. */
    private static String positionalVerdict(MethodInfo method) {
        boolean nonModifying = method.analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        String params = method.parameters().stream()
                .map(p -> "p" + p.index() + ".unmodified=" + p.analysis()
                        .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                .collect(Collectors.joining(" "));
        return "nonModifying=" + nonModifying + " " + params;
    }

    /** The two sensors a mis-shaped tree actually moves. */
    private static String verdict(MethodInfo method) {
        boolean nonModifying = method.analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        // every Box parameter, by name: the off-spine probes carry two, and the duplicated call is in the
        // SECOND one. Reporting only the first would have missed exactly the question being asked.
        String params = method.parameters().stream()
                .filter(p -> "s.Box".equals(String.valueOf(p.parameterizedType().typeInfo())))
                .map(p -> p.simpleName() + ".unmodified=" + p.analysis()
                        .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                .collect(Collectors.joining(" "));
        return "nonModifying=" + nonModifying + " " + params;
    }
}
