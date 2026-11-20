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

import java.util.Map;

import io.codelaser.maddi.cst.api.info.EnclosingInstance;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link EnclosingInstance} and {@link TypeInfo#usesEnclosingInstance()}, on the shapes corpora found. */
public class TestEnclosingInstance extends CommonTest {

    @Language("java")
    private static final String OWNER = """
            package a.b;
            public class Owner extends Base {
                private int base;
                private int scale() { return 2; }
                class Unused {
                    private int n;
                    int twice() { return n * 2; }
                }
                class Scaled {
                    int viaField() { return base; }
                    int viaMethod() { return scale(); }
                    Owner self() { return Owner.this; }
                }
                // a member Owner inherits: the This is bound to Base, which encloses nothing
                class Inherited {
                    int read() { return inherited; }
                }
                // its own inherited member: its own this
                class OwnInherited extends Base {
                    int read() { return inherited; }
                }
                class Worker {
                    private final int seed;
                    Worker() { this.seed = base; }
                    Runnable task() {
                        return new Runnable() {
                            public void run() { int x = base; }
                        };
                    }
                }
            }
            """;

    @Language("java")
    private static final String BASE = """
            package a.b;
            public class Base {
                protected int inherited;
            }
            """;

    @Language("java")
    private static final String SOURCE = """
            package a.b;
            public abstract class Source {
                public abstract String open();
                private final class Sliced extends Source {
                    @Override
                    public String open() { return Source.this.open().substring(1); }
                }
                private final class Own extends Source {
                    @Override
                    public String open() { return "x"; }
                    String twice() { return open() + open(); }
                }
            }
            """;

    @Language("java")
    private static final String METRICS = """
            package a.b;
            import java.util.List;
            import java.util.function.Supplier;
            public class Metrics<T> {
                private T value;
                class Counter { }
                static class Plain { }
                class ByNew { Object make() { return new Counter(); } }
                class ByAnonymous { Object make() { return new Counter() { }; } }
                class ByReference { Supplier<Counter> counters = Counter::new; }
                class OwnInner {
                    class Part { }
                    Object make() { return new Part(); }
                }
                class Qualified { Object make(Metrics<T> other) { return other.new Counter(); } }
                class StaticSibling { Object make() { return new Plain(); } }
                class NamesT { T get(List<String> l) { return null; } }
                class NamesNothing { int size(List<String> l) { return l.size(); } }
            }
            """;

    @Test
    public void enclosingInstance() {
        Map<String, TypeInfo> types = scan(false, "a.b.Owner", OWNER, "a.b.Base", BASE, "a.b.Source", SOURCE,
                "a.b.Metrics", METRICS);
        TypeInfo owner = types.get("a.b.Owner");
        assertFalse(owner.findSubType("Unused").usesEnclosingInstance());
        // an unqualified field read, an unqualified call and a written Owner.this
        assertEquals(3, EnclosingInstance.uses(owner.findSubType("Scaled")).size());
        // elasticsearch's AdditionalStats: a member the enclosing type inherits
        assertTrue(owner.findSubType("Inherited").usesEnclosingInstance());
        assertFalse(owner.findSubType("OwnInherited").usesEnclosingInstance());
        // in a constructor, and inside an anonymous class declared in the inner class
        assertEquals(2, EnclosingInstance.uses(owner.findSubType("Worker")).size());

        TypeInfo source = types.get("a.b.Source");
        // guava's SlicedByteSource: a written Source.this names the enclosing instance, whatever the class extends
        assertEquals(1, EnclosingInstance.uses(source.findSubType("Sliced")).size());
        assertFalse(source.findSubType("Own").usesEnclosingInstance());

        TypeInfo metrics = types.get("a.b.Metrics");
        // pulsar's PrometheusCounter::new: creating a sibling inner class passes the enclosing instance
        assertTrue(metrics.findSubType("ByNew").usesEnclosingInstance());
        assertTrue(metrics.findSubType("ByAnonymous").usesEnclosingInstance());
        assertTrue(metrics.findSubType("ByReference").usesEnclosingInstance());
        assertFalse(metrics.findSubType("OwnInner").usesEnclosingInstance());
        assertFalse(metrics.findSubType("Qualified").usesEnclosingInstance());
        assertFalse(metrics.findSubType("StaticSibling").usesEnclosingInstance());
        assertFalse(metrics.findSubType("Plain").usesEnclosingInstance());

        assertTrue(EnclosingInstance.usesTypeParameters(metrics.findSubType("NamesT")));
        assertFalse(EnclosingInstance.usesTypeParameters(metrics.findSubType("NamesNothing")));
        assertTrue(EnclosingInstance.usesTypeParameters(metrics.findSubType("Qualified")),
                "Metrics<T> in a parameter names Metrics's T: it counts");
    }
}
