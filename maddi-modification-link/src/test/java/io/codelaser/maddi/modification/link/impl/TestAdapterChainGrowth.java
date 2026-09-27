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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Eclipse Collections' {@code CollectionAdapter.wrapList(Iterable)} (2026-09-26): a chain of {@code instanceof}
 * alternatives that return either the argument itself or an adapter whose {@code delegate} field holds it. The
 * return value may be the argument OR hold the argument in a field, so {@code rv.delegate} may alias {@code rv},
 * and from there {@code rv.delegate.delegate} may alias {@code rv.delegate} ... With the alternatives linked apart
 * and joined (0d755fabd), and a statement's links following the latest computation, the field paths grew
 * without bound: 69 -> 31,638 characters of links for {@code iterable} within one computation, and the method
 * ground the 30M work ceiling for 72 minutes.
 */
public class TestAdapterChainGrowth extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.RandomAccess;
            class X {
                interface MutableList<E> extends Iterable<E> { int size(); void add(E e); }
                interface MutableListFactory { <E> MutableList<E> withAll(Iterable<E> items); }
                static class MutableListFactoryImpl implements MutableListFactory {
                    @Override public <E> MutableList<E> withAll(Iterable<E> items) { return FastList.newList(items); }
                }
                static class Lists { static final MutableListFactory mutable = new MutableListFactoryImpl(); }
                static class ListAdapter<E> implements MutableList<E> {
                    final List<E> delegate;
                    ListAdapter(List<E> delegate) {
                        if (delegate == null) { throw new NullPointerException("may not wrap null"); }
                        if (delegate instanceof RandomAccess) { throw new IllegalArgumentException("use the other"); }
                        this.delegate = delegate;
                    }
                    @Override public int size() { return delegate.size(); }
                    @Override public void add(E e) { delegate.add(e); }
                    @Override public java.util.Iterator<E> iterator() { return delegate.iterator(); }
                    static <E> MutableList<E> adapt(List<E> list) {
                        if (list instanceof MutableList) {
                            return (MutableList<E>) list;
                        }
                        if (list instanceof ArrayList) {
                            return ArrayListAdapter.adapt((ArrayList<E>) list);
                        }
                        if (list instanceof RandomAccess) {
                            return new RandomAccessListAdapter<>(list);
                        }
                        return new ListAdapter<>(list);
                    }
                }
                static class ArrayListAdapter<E> implements MutableList<E> {
                    final ArrayList<E> delegate;
                    ArrayListAdapter(ArrayList<E> delegate) {
                        if (delegate == null) { throw new NullPointerException("may not wrap null"); }
                        this.delegate = delegate;
                    }
                    @Override public int size() { return delegate.size(); }
                    @Override public void add(E e) { delegate.add(e); }
                    @Override public java.util.Iterator<E> iterator() { return delegate.iterator(); }
                    static <E> ArrayListAdapter<E> adapt(ArrayList<E> list) { return new ArrayListAdapter<>(list); }
                }
                static class RandomAccessListAdapter<E> implements MutableList<E> {
                    final List<E> delegate;
                    RandomAccessListAdapter(List<E> delegate) {
                        if (delegate == null) { throw new NullPointerException("may not wrap null"); }
                        if (!(delegate instanceof RandomAccess)) { throw new IllegalArgumentException("not random access"); }
                        this.delegate = delegate;
                    }
                    @Override public int size() { return delegate.size(); }
                    @Override public void add(E e) { delegate.add(e); }
                    @Override public java.util.Iterator<E> iterator() { return delegate.iterator(); }
                    static <E> RandomAccessListAdapter<E> adapt(List<E> list) { return new RandomAccessListAdapter<>(list); }
                }
                static class FastList<E> implements MutableList<E> {
                    final ArrayList<E> items = new ArrayList<>();
                    @Override public int size() { return items.size(); }
                    @Override public void add(E e) { items.add(e); }
                    @Override public java.util.Iterator<E> iterator() { return items.iterator(); }
                    static <E> FastList<E> newList(Iterable<E> iterable) {
                        FastList<E> list = new FastList<>();
                        for (E e : iterable) list.items.add(e);
                        return list;
                    }
                }
                static class CollectionAdapter<E> implements MutableList<E> {
                    final java.util.Collection<E> delegate;
                    CollectionAdapter(java.util.Collection<E> delegate) {
                        if (delegate == null) { throw new NullPointerException("may not wrap null"); }
                        this.delegate = delegate;
                    }
                    @Override public int size() { return delegate.size(); }
                    @Override public void add(E e) { delegate.add(e); }
                    @Override public java.util.Iterator<E> iterator() { return delegate.iterator(); }
                    static <E> MutableList<E> wrapList(Iterable<E> iterable) {
                        if (iterable instanceof MutableList) {
                            return (MutableList<E>) iterable;
                        }
                        if (iterable instanceof ArrayList) {
                            return ArrayListAdapter.adapt((ArrayList<E>) iterable);
                        }
                        if (iterable instanceof RandomAccess) {
                            return RandomAccessListAdapter.adapt((List<E>) iterable);
                        }
                        if (iterable instanceof List) {
                            return ListAdapter.adapt((List<E>) iterable);
                        }
                        return Lists.mutable.withAll(iterable);
                    }
                    static <E> MutableList<E> adapt(java.util.Collection<E> collection) {
                        if (collection instanceof MutableList) {
                            return (MutableList<E>) collection;
                        }
                        if (collection instanceof List) {
                            return CollectionAdapter.wrapList(collection);
                        }
                        return new CollectionAdapter<>(collection);
                    }
                }
            }
            """;

    @DisplayName("the summary of a chain of adapter alternatives stays bounded over recomputations")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo wrapList = X.findSubType("CollectionAdapter").findUniqueMethod("wrapList", 1);
        String previous = null;
        for (int round = 1; round <= 4; round++) {
            long start = System.currentTimeMillis();
            tlc.doPrimaryType(X);
            MethodLinkedVariables mlv = wrapList.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            String s = String.valueOf(mlv);
            long links = mlv.ofReturnValue().stream().count()
                         + mlv.ofParameters().stream().mapToLong(l -> l.stream().count()).sum();
            System.out.println("### round " + round + ": " + links + " links, " + s.length() + " chars, "
                               + (System.currentTimeMillis() - start) + " ms\n" + s);
            if (round == 1) {
                for (String spec : new String[]{"ListAdapter.adapt", "ArrayListAdapter.adapt", "RandomAccessListAdapter.adapt",
                        "MutableListFactoryImpl.withAll", "FastList.newList", "CollectionAdapter.adapt", "ListAdapter.<init>",
                        "ArrayListAdapter.<init>", "RandomAccessListAdapter.<init>", "CollectionAdapter.<init>"}) {
                    String[] parts = spec.split("\\.");
                    TypeInfo t = X.findSubType(parts[0]);
                    MethodInfo m = parts[1].equals("<init>") ? t.constructors().getFirst() : t.findUniqueMethod(parts[1], 1);
                    System.out.println("@@@ " + spec + " " + m.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                            MethodLinkedVariablesImpl.class));
                }
            }
            assertTrue(links < 40, "round " + round + ": " + links + " links");
            // the pathology itself: a link between the return value and its own field path ('rv → rv.delegate'),
            // or a variable linked to itself
            int r = round;
            mlv.ofReturnValue().forEach(link -> assertTrue(
                    !link.from().equals(link.to())
                    && !io.codelaser.maddi.modification.prepwork.Util.scopeVariables(link.to()).contains(link.from())
                    && !io.codelaser.maddi.modification.prepwork.Util.scopeVariables(link.from()).contains(link.to()),
                    "round " + r + ": self-field link " + link));
            // and the two alternatives both survive: rv may be the argument, or hold it
            assertTrue(s.contains("wrapList←Λ0:iterable"), "rv may be the argument: " + s);
            assertTrue(s.contains("wrapList.delegate←Λ0:iterable"), "rv may hold the argument: " + s);
            if (round >= 3) assertEquals(previous, s, "round " + round + " differs from round " + (round - 1));
            previous = s;
        }
    }
}
