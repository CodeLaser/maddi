package io.codelaser.maddi.modification.analyzer.field;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFieldAnalyzerFinalField extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
            }
            """;

    @DisplayName("constructor and getter")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse("a.b.B", INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        LinksImpl links = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set←0:set,this.set.§m≡0:set.§m,this.set→getSet", links.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isUnmodified());
        assertTrue(set.isFinal());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
                public void add(C c) {
                    set.add(c);
                }
            }
            """;

    @DisplayName("constructor, getter, add")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse("a.b.B", INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set←0:set,this.set.§m≡0:set.§m,this.set→getSet,this.set.§cs∋0:c", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified());
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isModified()); // via add!
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = new HashSet<>(set);
                }
                public Set<C> getSet() {
                    return set;
                }
                public void add(C c) {
                    set.add(c);
                }
            }
            """;

    @DisplayName("constructor copy, getter, add")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse("a.b.B", INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set.§cs⊆0:set.§cs,this.set→getSet,this.set.§cs∋0:c", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified()); // via add
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isUnmodified()); // a copy of the parameter is made
        assertTrue(pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT)
                .isIndependentHc());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B {
                public static class M { int i; public void setI(int i) { this.i = i; } }
            
                private final Set<M> set;
            
                B(Set<M> set) {
                    this.set = new HashSet<>(set);
                }
                public Set<M> getSet() {
                    return set;
                }
                public void add(M m) {
                    set.add(m);
                }
            }
            """;

    @DisplayName("constructor copy, getter, add, modifiable")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse("a.b.B", INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo M = B.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertTrue(M.analysis().getOrDefault(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.DEPENDENT).isIndependent());

        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set.§$s⊆0:set.§$s,this.set→getSet,this.set.§$s∋0:m", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified()); // via add
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isUnmodified()); // a copy is made
        assertTrue(pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static Go callGo(int i) {
                    M m = new M(i);
                    return new Go(m);
                }
                static class Go {
                    private M m;
                    Go(M m) {
                        this.m = m;
                    }
                    void inc() {
                        this.m.i++;
                    }
                }
                static class Go2 {
                    private M m;
                    Go2(M m) {
                        this.m = m;
                    }
                    int get() {
                        return this.m.i;
                    }
                }
            }
            """;

    @DisplayName("does the modification travel via the field?")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            TypeInfo go = X.findSubType("Go");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertFalse(p0.isUnmodified());
        }
        {
            TypeInfo go = X.findSubType("Go2");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertTrue(p0.isUnmodified());
        }
    }

    @Language("java")
    private static final String BRANCH_LAST = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B {
                private final Set<String> set;
                private long t;
                B(Set<String> set, long t) {
                    if (set != null) {
                        this.set = set;
                    } else {
                        this.set = new HashSet<>();
                    }
                }
                public Set<String> getSet() {
                    return set;
                }
            }
            """;

    @Language("java")
    private static final String BRANCH_THEN_A_STATEMENT = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class C {
                private final Set<String> set;
                private long t;
                C(Set<String> set, long t) {
                    if (set != null) {
                        this.set = set;
                    } else {
                        this.set = new HashSet<>();
                    }
                    this.t = t;
                }
                public Set<String> getSet() {
                    return set;
                }
            }
            """;

    @DisplayName("a field assigned a parameter in one branch stays linked to it after a later statement")
    @Test
    public void testBranchThenAStatement() {
        // found through jfocus diagnose (nacos' naming FuzzyWatchSyncNotifyTask): with the if/else as the last
        // statement of the constructor the parameter is DEPENDENT; with any statement after it, INDEPENDENT --
        // a link lost, the unsafe direction (an alias read as none)
        TypeInfo last = javaInspector.parse("a.b.B", BRANCH_LAST);
        analyzer.go(prepWork(last));
        String lastLinks = String.valueOf(last.getFieldByName("set", true).analysis()
                .getOrNull(LinksImpl.LINKS, LinksImpl.class));
        Value.Independent lastIndependent = last.findConstructor(2).parameters().getFirst().analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);

        TypeInfo then = javaInspector.parse("a.b.C", BRANCH_THEN_A_STATEMENT);
        analyzer.go(prepWork(then));
        FieldInfo set = then.getFieldByName("set", true);
        String thenLinks = String.valueOf(set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class));
        ParameterInfo pi = then.findConstructor(2).parameters().getFirst();
        Value.Independent thenIndependent = pi.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);
        String report = "last: " + lastLinks + " / " + lastIndependent + "\nthen: " + thenLinks + " / "
                        + thenIndependent;
        assertTrue(lastIndependent != null && lastIndependent.isDependent(), report);
        assertTrue(thenIndependent != null && thenIndependent.isDependent(), report);
    }

    @Language("java")
    private static final String RETURN_IN_A_BRANCH = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class D {
                private List<String> arguments;
                private int argc;
                List<String> getArguments() {
                    arguments = new ArrayList<>(argc);
                    if (argc == 0) {
                        return arguments;
                    }
                    return arguments;
                }
            }
            """;

    @Language("java")
    private static final String RETURN_IN_A_BRANCH_LIKE_JENKINS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.List;
            class E {
                private List<String> arguments;
                private int argc;
                synchronized List<String> getArguments() {
                    if (arguments != null)
                        return arguments;
                    arguments = new ArrayList<>(argc);
                    if (argc == 0) {
                        return arguments;
                    }
                    arguments = Collections.unmodifiableList(arguments);
                    return arguments;
                }
            }
            """;

    @Language("java")
    private static final String ASSIGNED_AND_RETURNED_IN_A_BRANCH = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class F {
                private List<String> arguments;
                List<String> getArguments(List<String> in, boolean b) {
                    if (b) {
                        arguments = in;
                        return arguments;
                    } else {
                        arguments = new ArrayList<>();
                    }
                    return arguments;
                }
            }
            """;

    @DisplayName("a field returned inside a branch and again after it stays linked to the return value")
    @Test
    public void testReturnInABranchThenReturn() {
        // jenkins' ProcessTree.SolarisProcess/AIXProcess getArguments(): a first version of the fix of
        // testBranchThenAStatement emptied the summary ('[] --> -'), so the field read as @Independent. Object
        // creations must be tracked, as in a corpus run: the fresh 'oc:' source is what the fix put back
        var tracking = new io.codelaser.maddi.modification.analyzer.impl.SingleIterationAnalyzerImpl(javaInspector,
                new io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setModificationViaReachability(true).build());
        StringBuilder report = new StringBuilder();
        boolean ok = true;
        for (String[] fixture : new String[][]{{"a.b.D", RETURN_IN_A_BRANCH},
                {"a.b.E", RETURN_IN_A_BRANCH_LIKE_JENKINS}, {"a.b.F", ASSIGNED_AND_RETURNED_IN_A_BRANCH}}) {
            TypeInfo type = javaInspector.parse(fixture[0], fixture[1]);
            List<Info> ao = prepWork(type);
            tracking.go(ao);
            tracking.go(ao);
            MethodInfo get = type.methods().stream().filter(m -> m.name().equals("getArguments")).findFirst()
                    .orElseThrow();
            String mlv = String.valueOf(get.analysis().getOrNull(
                    io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS,
                    io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.class));
            Value.Independent independent = type.getFieldByName("arguments", true).analysis()
                    .getOrNull(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.class);
            report.append(fixture[0]).append(": ").append(mlv).append(" / ").append(independent).append('\n');
            ok &= mlv.contains("getArguments←this") && independent != null && independent.isDependent();
        }
        assertTrue(ok, report.toString());
    }

    @Language("java")
    private static final String DETACHED_THEN_MODIFIED = """
            package a.b;
            class Chain {
                static class Entry { Entry pred; Entry succ; }
                Entry first;
                Entry last;
                void succeeds(Entry pred, Entry succ) {
                    if (pred == null) {
                        first = succ;
                    } else {
                        pred.succ = succ;
                    }
                    if (succ == null) {
                        last = pred;
                    } else {
                        succ.pred = pred;
                    }
                }
                void delete(Entry entry) {
                    succeeds(entry.pred, entry.succ);
                }
                void append(Entry entry) {
                    succeeds(last, entry);
                    last = entry;
                }
            }
            """;

    @DisplayName("a field assigned in one branch is modified by a later modification of its source")
    @Test
    public void testDetachedThenModified() {
        // guava's LinkedHashMultimap.MultimapIterationChain.firstEntry: on the path through the first then-block,
        // 'first' holds the object 'succ.pred = pred' modifies; linked apart and joined, 'first' left succ's group
        // (it is succ's object in one alternative only) and the later modification no longer reached it
        var tracking = new io.codelaser.maddi.modification.analyzer.impl.SingleIterationAnalyzerImpl(javaInspector,
                new io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setModificationViaReachability(true).build());
        TypeInfo type = javaInspector.parse("a.b.Chain", DETACHED_THEN_MODIFIED);
        List<Info> ao = prepWork(type);
        tracking.go(ao);
        tracking.go(ao);
        tracking.go(ao);
        MethodInfo succeeds = type.findUniqueMethod("succeeds", 2);
        String mlv = String.valueOf(succeeds.analysis().getOrNull(
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS,
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.class));
        Value.Bool unmodified = type.getFieldByName("first", true).analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
        assertTrue(mlv.contains("this.first*") && unmodified != null && unmodified.isFalse(),
                "succeeds: " + mlv + " / first unmodified=" + unmodified);
    }

    @Language("java")
    private static final String FACE_AFTER_JOIN = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            class Counter {
                static class H {
                    final String value;
                    int count;
                    H(String value) {
                        this.value = value;
                    }
                }
                private final Map<String, H> counts = new HashMap<>();
                public H add(String v) {
                    H h = counts.get(v);
                    if (h == null) {
                        h = new H(v);
                        counts.put(v, h);
                    } else {
                        h.count++;
                    }
                    return h;
                }
            }
            """;

    @DisplayName("a field face of a variable assigned in one branch survives the join")
    @Test
    public void testFaceAfterJoin() {
        // timefold's ToMapPerKeyCounter.add: linked apart and joined, 'h' left the group of the new object (it is
        // that object in one alternative only); the join edge 'h ← rep' did not carry the faces ('h.value ← 0:v'),
        // and once carried, 'return h' grouped them under the return's rep, which the extraction did not read
        var tracking = new io.codelaser.maddi.modification.analyzer.impl.SingleIterationAnalyzerImpl(javaInspector,
                new io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setModificationViaReachability(true).build());
        TypeInfo type = javaInspector.parse("a.b.Counter", FACE_AFTER_JOIN);
        List<Info> ao = prepWork(type);
        tracking.go(ao);
        tracking.go(ao);
        tracking.go(ao);
        MethodInfo add = type.findUniqueMethod("add", 1);
        String mlv = String.valueOf(add.analysis().getOrNull(
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS,
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.class));
        assertTrue(mlv.contains("add.value←0:v"), "add: " + mlv);
    }

    @Language("java")
    private static final String REASSIGNED_PARAMETER = """
            package a.b;
            class Interceptors {
                interface Broker { }
                static class Wrap implements Broker {
                    final Broker b;
                    Wrap(Broker b) {
                        this.b = b;
                    }
                }
                private boolean flag;
                private boolean other;
                Broker add(Broker broker) {
                    if (flag) {
                        Wrap w = new Wrap(broker);
                        broker = w;
                    }
                    if (other) {
                        broker = new Wrap(broker);
                    }
                    return broker;
                }
            }
            """;

    @DisplayName("a reassigned parameter does not receive the faces of what it was assigned in one branch")
    @Test
    public void testReassignedParameterFaces() {
        // activemq's BrokerService.addInterceptors: 'broker = new X(broker)' in several ifs. Copying the faces of the
        // group 'broker' left at the join onto 'broker' itself made the summary claim fields of the caller's object
        // ('0:broker.b.b ← oc'); the member's name after the join also stands for the caller's object
        var tracking = new io.codelaser.maddi.modification.analyzer.impl.SingleIterationAnalyzerImpl(javaInspector,
                new io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setModificationViaReachability(true).build());
        TypeInfo type = javaInspector.parse("a.b.Interceptors", REASSIGNED_PARAMETER);
        List<Info> ao = prepWork(type);
        tracking.go(ao);
        tracking.go(ao);
        tracking.go(ao);
        MethodInfo add = type.findUniqueMethod("add", 1);
        String mlv = String.valueOf(add.analysis().getOrNull(
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS,
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.class));
        assertTrue(!mlv.contains("0:broker.b.b") && !mlv.contains("add.b.b"), "add: " + mlv);
    }
}
