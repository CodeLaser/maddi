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

package io.codelaser.maddi.cst.print.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kotlin has no {@code ;}, so statements must end up on lines of their own, also in a block the formatter would
 * otherwise put on one line. It did not: {@code { map.put(key, size()) add(element)lstKeys.add(key) }} (fernflower's
 * VBStyleCollection), because the formatter honoured a sub-block's trailing newline only at one split level.
 */
public class TestStatementSeparation extends CommonJavaToKotlin {

    @Language("java")
    private static final String SHORT_BLOCK = """
            package a;
            class C {
                void m(java.util.List<String> list, String s) { list.add(s); list.add(s); list.clear(); }
            }
            class V<E, K> extends java.util.ArrayList<E> {
                private java.util.HashMap<K, Integer> map = new java.util.HashMap<>();
                private java.util.ArrayList<K> lstKeys = new java.util.ArrayList<>();
                public void addWithKey(E element, K key) {
                    map.put(key, size());
                    super.add(element);
                    lstKeys.add(key);
                }
            }
            """;

    @Test
    public void shortBlock() {
        String kotlin = kotlin(SHORT_BLOCK);
        assertTrue(kotlin.contains("""
                        map.put(key, size)
                """), kotlin);
        assertTrue(kotlin.contains("""
                        lstKeys.add(key)
                    }
                """), kotlin);
    }

    @Language("java")
    private static final String NOT_NULL_AT_A_LINE_END = """
            package a;
            import java.util.*;
            abstract class AbstractDatasetForTheSession<Solution_> { }
            abstract class AbstractLeftDatasetForTheSession<Solution_, A> extends AbstractDatasetForTheSession<Solution_> { }
            abstract class AbstractDatasetInstanceOfTheSession<Solution_, T> { }
            abstract class AbstractLeftDatasetInstanceOfTheSession<Solution_, T>
                    extends AbstractDatasetInstanceOfTheSession<Solution_, T> { }
            final class UniTupleOfTheSession<A> { }
            final class DatasetSession<Solution_> {
                private final Map<AbstractDatasetForTheSession<Solution_>, AbstractDatasetInstanceOfTheSession<Solution_, ?>>
                        leftDatasetInstanceMap = new HashMap<>();
                @SuppressWarnings("unchecked")
                public <A> AbstractLeftDatasetInstanceOfTheSession<Solution_, UniTupleOfTheSession<A>> getInstance(
                        AbstractLeftDatasetForTheSession<Solution_, A> dataset) {
                    return (AbstractLeftDatasetInstanceOfTheSession<Solution_, UniTupleOfTheSession<A>>) Objects
                            .requireNonNull(leftDatasetInstanceMap.get((AbstractDatasetForTheSession<Solution_>) dataset));
                }
            }
            """;

    /** timefold's DatasetSession: a line break in front of {@code !!} starts a statement with it, which kotlinc refuses. */
    @Test
    public void noLineBreakBeforeNotNull() {
        String kotlin = kotlin(NOT_NULL_AT_A_LINE_END);
        assertTrue(kotlin.lines().noneMatch(l -> l.strip().startsWith("!!")), kotlin);
        compiles(kotlin);
    }

    @Language("java")
    private static final String NOT_AFTER_A_STAR_CAST = """
            package a;
            interface View<S> { }
            interface Director<S, X> extends View<S> { boolean isPinned(Object descriptor, Object entity); }
            interface Predicate2<S, E> { boolean test(View<S> view, E entity); }
            final class Descriptor<S> {
                private final Predicate2<S, Object> movable = (view, entity) -> {
                    var director = (Director<S, ?>) view;
                    return !director.isPinned(this, entity);
                };
                boolean movable(View<S> view, Object entity) { return movable.test(view, entity); }
            }
            """;

    /** timefold's EntityDescriptor: a lambda's last statement {@code !x.m()} after a cast to a star-projected type. */
    @Test
    public void notAfterAStarProjectedCast() {
        String kotlin = kotlin(NOT_AFTER_A_STAR_CAST);
        compiles(kotlin);
    }
}
