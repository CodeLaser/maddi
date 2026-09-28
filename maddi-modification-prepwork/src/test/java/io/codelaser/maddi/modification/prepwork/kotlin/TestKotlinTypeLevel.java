package io.codelaser.maddi.modification.prepwork.kotlin;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeCallGraph;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static io.codelaser.maddi.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 The type-level half of prep on Kotlin declarations: the call graph and analysis order, part of construction and
 final fields, and the getter/setter classification of the accessors K2 synthesizes for properties. Kotlin's
 shapes have no one-to-one Java twin here (a property is a field plus accessors, a companion a nested type plus a
 static field), so these are pinned values, each checked by hand against what kotlinc emits.
 */
public class TestKotlinTypeLevel extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            interface I { fun f(): Int }
            class Del(d: I) : I by d
            data class D(val a: Int, var b: String)
            enum class E(val code: Int) { A(1), B(2) { override fun g() = 9 }; open fun g() = code }
            object O { var hits = 0; fun hit() { hits++ } }
            class X(val start: Int) {
                private val list = ArrayList<String>()
                var count = 0
                lateinit var name: String
                @JvmField val jf = 1
                val size: Int get() = list.size
                init { initList(start) }
                constructor() : this(0)
                private fun initList(n: Int) { for (i in 0 until n) list.add("x") }
                fun add(s: String) { list.add(s); count++ }
                fun withDefault(a: Int, b: Int = 3) = a + b
                companion object { const val C = 3; fun make() = X(C) }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, "package j;\nclass X {}\n");
    }

    private static TypeInfo type(String fqn) {
        return find(p.kotlinTypes(), fqn);
    }

    private static String partOfConstruction(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrNull(PART_OF_CONSTRUCTION, ValueImpl.SetOfInfoImpl.class).infoSet().stream()
                .map(Info::fullyQualifiedName).sorted().collect(Collectors.joining(", "));
    }

    private static String finalFields(TypeInfo typeInfo) {
        return typeInfo.fields().stream().map(f -> f.name() + "=" + f.analysis()
                        .getOrDefault(PropertyImpl.FINAL_FIELD, ValueImpl.BoolImpl.FALSE).isTrue())
                .collect(Collectors.joining(", "));
    }

    /* each method with the field it gets or sets, `(setter)` for a setter; methods that are neither are left out */
    private static String getSet(TypeInfo typeInfo) {
        return typeInfo.constructorAndMethodStream()
                .filter(m -> m.getSetField().field() != null)
                .map(m -> m.name() + "->" + m.getSetField().field().name() + (m.getSetField().setter() ? " (setter)" : ""))
                .collect(Collectors.joining(", "));
    }

    @Test
    public void callGraphAndAnalysisOrder() {
        G<Info> graph = new ComputeCallGraph(p.runtime(), p.kotlinX()).go().graph();
        // ⚠ maddi#73: `const val C` is a field of X (as kotlinc emits it) AND of X.Companion, and make() reads
        // the companion's. kotlinc has only X.C.
        assertEquals("""
                k.X->S->k.X.<init>(), k.X->S->k.X.<init>(int), k.X->S->k.X.C, k.X->S->k.X.Companion, \
                k.X->S->k.X.Companion, k.X->S->k.X.add(String), k.X->S->k.X.count, k.X->S->k.X.getCount(), \
                k.X->S->k.X.getName(), k.X->S->k.X.getSize(), k.X->S->k.X.getStart(), k.X->S->k.X.initList(int), \
                k.X->S->k.X.jf, k.X->S->k.X.list, k.X->S->k.X.name, k.X->S->k.X.setCount(int), \
                k.X->S->k.X.setName(String), k.X->S->k.X.start, k.X->S->k.X.withDefault$default(int,int,int), \
                k.X->S->k.X.withDefault(int,int), k.X.<init>()->SR->k.X.<init>(int), \
                k.X.<init>(int)->R->k.X.initList(int), k.X.Companion->D->k.X.Companion, \
                k.X.Companion->S->k.X.Companion.<init>(), k.X.Companion->S->k.X.Companion.C, \
                k.X.Companion->S->k.X.Companion.make(), k.X.Companion.C->R->k.X.Companion.make(), \
                k.X.Companion.make()->R->k.X.<init>(int), k.X.count->R->k.X.add(String), \
                k.X.count->R->k.X.getCount(), k.X.count->R->k.X.setCount(int), k.X.list->R->k.X.add(String), \
                k.X.list->R->k.X.getSize(), k.X.list->R->k.X.initList(int), k.X.name->R->k.X.getName(), \
                k.X.name->R->k.X.setName(String), k.X.start->R->k.X.<init>(int), k.X.start->R->k.X.getStart(), \
                k.X.withDefault$default(int,int,int)->R->k.X.withDefault(int,int)\
                """, ComputeCallGraph.print(graph));
        // the $default bridge after the method it calls; the secondary constructor after the primary one
        assertEquals("""
                C, <init>, add, getCount, getName, getSize, getStart, initList, jf, setCount, setName, withDefault, \
                <init>, count, list, name, withDefault$default, <init>, make, start, C, Companion, Companion, X\
                """, new ComputeAnalysisOrder().go(graph).stream().map(Info::simpleName)
                .collect(Collectors.joining(", ")));
    }

    @Test
    public void partOfConstructionAndFinalFields() {
        // the init block is part of the primary constructor; initList is called from it only
        assertEquals("k.X.<init>(), k.X.<init>(int), k.X.initList(int)", partOfConstruction(p.kotlinX()));
        assertEquals("start=true, list=true, count=false, name=false, jf=true, Companion=true, C=true",
                finalFields(p.kotlinX()));
        assertEquals("a=true, b=false", finalFields(type("k.D")));
        assertEquals("k.D.<init>(int,String)", partOfConstruction(type("k.D")));
        assertEquals("code=true, A=true, B=true", finalFields(type("k.E")));
        assertEquals("hits=false, INSTANCE=true", finalFields(type("k.O")));
        // class delegation: the delegate is a synthetic final field; kotlinc assigns it in the constructor, K2's
        // lowering does not (maddi#90, see delegationConstructor)
        assertEquals("$$delegate_0=true", finalFields(type("k.Del")));
    }

    @Test
    public void accessors() {
        // val -> getter; var and lateinit var -> getter + setter; a custom getter over another field's content is
        // neither (as `int size() { return list.size(); }` is not, in Java); @JvmField has no accessors
        assertEquals("getStart->start, getCount->count, setCount->count (setter), getName->name, "
                     + "setName->name (setter)", getSet(p.kotlinX()));
        // data class: componentN are getters of their property, as the accessors are
        assertEquals("getA->a, getB->b, setB->b (setter), component1->a, component2->b", getSet(type("k.D")));
        // `open fun g() = code` returns the field: a getter by shape, as in Java
        assertEquals("getCode->code, g->code", getSet(type("k.E")));
        assertEquals("getHits->hits, setHits->hits (setter)", getSet(type("k.O")));
    }

    /*
     The bodies K2 gives synthesized members, which prep analyses like any other: a delegation forwarder calls
     the delegate, a `$default` bridge assigns the defaulted parameter and calls the method.
     */
    @Test
    public void synthesizedBodies() {
        assertEquals("{return this.$$delegate_0.f();}", body(type("k.Del"), "f"));
        assertEquals("{if(($mask&2)!=0){b=3;}return withDefault(a,b);}", body(p.kotlinX(), "withDefault$default"));
        assertEquals("{return new D(a,b);}", body(type("k.D"), "copy"));
        assertEquals("{this.start=start;{initList(start);}}", p.kotlinX().findConstructor(1)
                .methodBody().print(p.runtime().qualificationSimpleNames()).toString());
        // the `$default` bridge reassigns its parameter; prep sees an ordinary conditional parameter assignment
        MethodInfo bridge = method(p.kotlinX(), "withDefault$default");
        assertEquals("""
                $mask: D:-, A:[] | R 0-E
                a: D:-, A:[] | R 1
                b: D:-, A:[0.0.0] | R 1
                return withDefault$default: D:-, A:[1] | R -""", summary(bridge));
    }

    /*
     Members whose body is EMPTY: prep has nothing to read in them, and the analyzer reads an explicitly empty body
     as "modifies nothing" (docs/kotlin-parity-study-2026-09-26.md §5.3). For a data class's equals/hashCode/
     toString that is the same decision as for a Java record; the enum's members are the JDK's own.
     */
    @Test
    public void emptyBodies() {
        assertEquals("equals, hashCode, toString", emptyBodies(type("k.D")));
        assertEquals("name, values, valueOf, getEntries, <static_0>", emptyBodies(type("k.E")));
        assertEquals("<init>", emptyBodies(type("k.O")));
        // since #85 (2026-09-28) the delegate is assigned IN the constructor (`this.$$delegate_0 = d` reads the
        // constructor parameter), so Del's constructor is no longer empty (#90)
        assertEquals("", emptyBodies(type("k.Del")));
    }

    /*
     #90 (fixed by #85, 2026-09-28): kotlinc's constructor is `{this.$$delegate_0=d;}`, and so is K2's; before, the
     lowering left it empty, `d` was read nowhere, the field assigned nowhere, and prep had no variable data for
     the constructor at all.
     */
    @Test
    public void delegationConstructor() {
        MethodInfo constructor = type("k.Del").findConstructor(1);
        assertEquals("{this.$$delegate_0=d;}", constructor.methodBody().print(p.runtime().qualificationSimpleNames()).toString());
    }

    private static String emptyBodies(TypeInfo typeInfo) {
        return typeInfo.constructorAndMethodStream()
                .filter(m -> !m.isAbstract() && m.methodBody().isEmpty())
                .map(MethodInfo::name).collect(Collectors.joining(", "));
    }

    private static String body(TypeInfo typeInfo, String name) {
        return method(typeInfo, name).methodBody().print(p.runtime().qualificationSimpleNames()).toString();
    }
}
