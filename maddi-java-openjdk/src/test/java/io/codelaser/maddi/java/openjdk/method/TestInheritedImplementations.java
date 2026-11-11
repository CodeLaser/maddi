/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.java.openjdk.method;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.util.MethodFamilies;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * {@link TypeInfo#inheritedImplementations()} and {@link MethodFamilies}, on code javac accepts: that a concrete
 * class compiles says every abstract method of its interfaces has an implementation, so the cases below have the
 * compiler's answer in them.
 */
public class TestInheritedImplementations extends CommonTest {

    @Language("java")
    private static final String INHERITED = """
            package a.b;
            import java.util.List;
            class X {
                interface Plain { boolean d(); }
                static class Base { public boolean d() { return true; } }
                // B.d() implements I.d() in Sub, and overrides nothing
                static class Sub extends Base implements Plain { }
                // its own method overrides both: nothing is inherited
                static class Own extends Base implements Plain { @Override public boolean d() { return false; } }

                interface Typed<T> { void take(T t); void all(List<T> ts); }
                static class Overloads {
                    public void take(Integer i) { }
                    public void take(String s) { }
                    public void take(Object o) { }
                    public void all(List<String> ts) { }
                }
                // the argument picks the overload: take(String), not the first of the name
                static class OfString extends Overloads implements Typed<String> { }

                static class Generic<U> { public void take(U u) { } public void all(List<U> us) { } }
                // the superclass's type parameter is the class's argument too
                static class Both extends Generic<Integer> implements Typed<Integer> { }
                static class Passing<V> extends Generic<V> implements Typed<V> { }

                interface Arrays { void fill(int[] a, String... rest); }
                static class Filler { public void fill(int[] a, String[] rest) { } public void fill(int a, String[] rest) { } }
                static class Filled extends Filler implements Arrays { }

                interface Narrow extends Plain { }
                static class Middle extends Base { }
                // through an interface's superinterface, and a superclass's superclass
                static class Far extends Middle implements Narrow { }

                static abstract class Declares { public abstract boolean d(); }
                // an abstract method takes the place too: both leave d() to the subclasses
                static abstract class Leaves extends Declares implements Plain { }

                static class Implements implements Plain { public boolean d() { return true; } }
                // the superclass implements the interface itself: overrides() has it
                static class Again extends Implements implements Plain { }

                interface Bounded { <N extends Number> void put(N n); }
                static class Puts { public <M extends Number> void put(M m) { } public void put(Object o) { } }
                static class Put extends Puts implements Bounded { }
            }
            """;

    private static String inherited(TypeInfo cls) {
        return cls.inheritedImplementations().entrySet().stream()
                .map(e -> e.getKey() + " <- " + e.getValue()).sorted().collect(Collectors.joining("; "));
    }

    @Test
    public void inheritedImplementations() {
        TypeInfo X = scan("a.b.X", INHERITED);
        assertEquals("a.b.X.Plain.d() <- a.b.X.Base.d()", inherited(X.findSubType("Sub")));
        assertEquals("", inherited(X.findSubType("Own")));
        assertEquals("", inherited(X.findSubType("Base")));
        assertEquals("", inherited(X.findSubType("Plain")));
        assertEquals("a.b.X.Typed.all(java.util.List) <- a.b.X.Overloads.all(java.util.List); "
                     + "a.b.X.Typed.take(Object) <- a.b.X.Overloads.take(String)", inherited(X.findSubType("OfString")));
        assertEquals("a.b.X.Typed.all(java.util.List) <- a.b.X.Generic.all(java.util.List); "
                     + "a.b.X.Typed.take(Object) <- a.b.X.Generic.take(Object)", inherited(X.findSubType("Both")));
        assertEquals("a.b.X.Typed.all(java.util.List) <- a.b.X.Generic.all(java.util.List); "
                     + "a.b.X.Typed.take(Object) <- a.b.X.Generic.take(Object)", inherited(X.findSubType("Passing")));
        assertEquals("a.b.X.Arrays.fill(int[],String[]) <- a.b.X.Filler.fill(int[],String[])",
                inherited(X.findSubType("Filled")));
        assertEquals("a.b.X.Plain.d() <- a.b.X.Base.d()", inherited(X.findSubType("Far")));
        assertEquals("a.b.X.Plain.d() <- a.b.X.Declares.d()", inherited(X.findSubType("Leaves")));
        assertEquals("", inherited(X.findSubType("Again")));
        assertEquals("a.b.X.Bounded.put(Number) <- a.b.X.Puts.put(Number)", inherited(X.findSubType("Put")));
    }

    @Language("java")
    private static final String FAMILIES = """
            package a.b;
            class X {
                interface Shape { double area(); String name(); }
                static class Square implements Shape {
                    public double area() { return 1; }
                    public String name() { return "square"; }
                }
                static class Circle implements Shape {
                    public double area() { return 3; }
                    public String name() { return "circle"; }
                }
                static class Named { public String name() { return "named"; } public String other() { return ""; } }
                // Named.name() joins Shape.name()'s family through Blob only
                static class Blob extends Named implements Shape { public double area() { return 2; } }
                static class Unrelated extends Named { @Override public String name() { return "u"; } }

                static Shape anonymous() {
                    return new Shape() {
                        public double area() { return 4; }
                        public String name() { return "anonymous"; }
                    };
                }
                static Shape local() {
                    class Local extends Square { @Override public double area() { return 5; } }
                    return new Local();
                }
                static class Task implements Runnable { public void run() { } static void help() { } }
            }
            """;

    private static String names(Set<MethodInfo> methods) {
        return methods.stream().map(m -> m.typeInfo().simpleName() + "." + m.name()).collect(Collectors.joining(", "));
    }

    @Test
    public void families() {
        TypeInfo X = scan("a.b.X", FAMILIES);
        MethodFamilies families = MethodFamilies.of(MethodFamilies.allTypes(Set.of(X)));
        TypeInfo shape = X.findSubType("Shape");
        MethodInfo area = shape.findUniqueMethod("area", 0);
        MethodInfo name = shape.findUniqueMethod("name", 0);
        TypeInfo square = X.findSubType("Square");
        MethodInfo squareArea = square.findUniqueMethod("area", 0);
        TypeInfo named = X.findSubType("Named");
        MethodInfo namedName = named.findUniqueMethod("name", 0);

        // the anonymous and the local class are in it; the same set, in the same order, from any member
        String areas = "$0.area, Local.area, Blob.area, Circle.area, Shape.area, Square.area";
        assertEquals(areas, names(families.family(area)));
        assertEquals(areas, names(families.family(squareArea)));
        assertEquals(areas, names(families.family(X.findSubType("Circle").findUniqueMethod("area", 0))));

        // Named.name() is of the family, and through it Unrelated.name(), which no Shape is
        assertEquals("$0.name, Circle.name, Named.name, Shape.name, Square.name, Unrelated.name",
                names(families.family(name)));
        assertEquals(families.family(name), families.family(namedName));

        // downwards only: a call of Shape.name() reaches Named.name() in a Blob, not Unrelated's
        assertEquals("$0.name, Circle.name, Named.name, Square.name", names(families.overriders(name)));
        assertEquals("Unrelated.name", names(families.overriders(namedName)));
        assertEquals("Local.area", names(families.overriders(squareArea)));
        assertEquals("", names(families.overriders(named.findUniqueMethod("other", 0))));

        // alone: nothing overrides it; a static method
        assertEquals("Named.other", names(families.family(named.findUniqueMethod("other", 0))));
        TypeInfo task = X.findSubType("Task");
        assertEquals("Task.help", names(families.family(task.findUniqueMethod("help", 0))));

        // Runnable.run() is not the program's to edit
        MethodInfo run = task.findUniqueMethod("run", 0);
        assertEquals("Task.run, Runnable.run", names(families.family(run)));
        java.util.function.Predicate<TypeInfo> inside = t -> X.equals(t.primaryType());
        assertEquals(true, families.reachesOutside(run, inside));
        assertEquals(false, families.reachesOutside(area, inside));
        assertEquals(3, families.families().size());
    }

    @Language("java")
    private static final String NESTING = """
            package a.b;
            class X {
                static class Member { }
                Runnable run() {
                    class Local { class Inner { } }
                    return new Runnable() { public void run() { } };
                }
            }
            """;

    /** {@link TypeInfo#enclosingType()}, {@link TypeInfo#nearestEnclosingMethod()}, {@code parsedFromSource()}. */
    @Test
    public void enclosingAndSource() {
        TypeInfo X = scan("a.b.X", NESTING);
        TypeInfo member = X.findSubType("Member");
        MethodInfo run = X.findUniqueMethod("run", 0);
        java.util.List<TypeInfo> inRun = X.localTypeStream().toList();
        TypeInfo local = inRun.stream().filter(t -> "Local".equals(t.simpleName())).findFirst().orElseThrow();
        TypeInfo anonymous = inRun.stream().filter(TypeInfo::isAnonymous).findFirst().orElseThrow();
        TypeInfo inner = local.findSubType("Inner");

        assertEquals(null, X.enclosingType());
        assertEquals(X, member.enclosingType());
        assertEquals(X, local.enclosingType());
        assertEquals(X, anonymous.enclosingType());
        assertEquals(local, inner.enclosingType());

        assertEquals(null, member.nearestEnclosingMethod());
        assertEquals(run, local.nearestEnclosingMethod());
        assertEquals(run, anonymous.nearestEnclosingMethod());
        // a member of a local class has no enclosing method of its own
        assertEquals(null, inner.enclosingMethod());
        assertEquals(run, inner.nearestEnclosingMethod());

        assertEquals(true, X.parsedFromSource());
        assertEquals(true, run.parsedFromSource());
        assertEquals(true, anonymous.parsedFromSource());
        MethodInfo runnableRun = anonymous.findUniqueMethod("run", 0).overrides().iterator().next();
        assertEquals("java.lang.Runnable.run()", runnableRun.toString());
        assertEquals(false, runnableRun.parsedFromSource());
        assertEquals(false, runnableRun.typeInfo().parsedFromSource());
    }
}
