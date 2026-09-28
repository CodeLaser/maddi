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
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Eclipse Collections' {@code UnifiedSet.remove(Object)} family (2026-09-27): an {@code instanceof} if-chain with
 * early returns over a bucket whose four slots {@code zero..three} are shuffled by {@code removeLast}, and a
 * {@code do/while} that walks {@code oldBucket = (ChainedBucket) oldBucket.three}.
 * <p>
 * {@code removeLast}'s {@code return null} is one constant marker ({@code $_ce12}) in its summary, and the same
 * marker at each of the four call sites; the engine composed {@code bucket.zero ← $_ce12} with
 * {@code $_ce12 → bucket.one} into {@code bucket.zero ≡ bucket.one}: all four slots "identical" because each
 * was once assigned null, and from there mirror faces without bound ({@code bucket.one → this.occupied.zero.two}).
 * {@code removeFromChain} carried 1,500-4,500 summary links at 3-26M work, {@code remove} tripped the ceiling.
 * A constant marker is never the middle of a composition (LinkComputerImpl's engine predicate): 115 links, 15k.
 */
public class TestRemoveChainWork extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X<T> {
                private static final Object NULL_KEY = new Object();
                protected Object[] table = new Object[16];
                protected int occupied;

                static final class ChainedBucket {
                    private Object zero;
                    private Object one;
                    private Object two;
                    private Object three;

                    private ChainedBucket() {
                    }

                    private ChainedBucket(Object first, Object second) {
                        this.zero = first;
                        this.one = second;
                    }

                    public Object removeLast(int cur) {
                        if (this.three instanceof ChainedBucket) {
                            return this.removeLast(this);
                        }
                        if (this.three != null) {
                            Object result = this.three;
                            this.three = null;
                            return cur == 3 ? null : result;
                        }
                        if (this.two != null) {
                            Object result = this.two;
                            this.two = null;
                            return cur == 2 ? null : result;
                        }
                        if (this.one != null) {
                            Object result = this.one;
                            this.one = null;
                            return cur == 1 ? null : result;
                        }
                        this.zero = null;
                        return null;
                    }

                    private Object removeLast(ChainedBucket oldBucket) {
                        do {
                            ChainedBucket bucket = (ChainedBucket) oldBucket.three;
                            if (bucket.three instanceof ChainedBucket) {
                                oldBucket = bucket;
                                continue;
                            }
                            if (bucket.three != null) {
                                Object result = bucket.three;
                                bucket.three = null;
                                return result;
                            }
                            if (bucket.two != null) {
                                Object result = bucket.two;
                                bucket.two = null;
                                return result;
                            }
                            if (bucket.one != null) {
                                Object result = bucket.one;
                                bucket.one = null;
                                return result;
                            }
                            Object result = bucket.zero;
                            oldBucket.three = null;
                            return result;
                        } while (true);
                    }
                }

                protected int index(Object key) {
                    int h = key == null ? 0 : key.hashCode();
                    h ^= h >>> 20 ^ h >>> 12;
                    h ^= h >>> 7 ^ h >>> 4;
                    return h & (this.table.length - 1);
                }

                private boolean nonNullTableObjectEquals(Object cur, T key) {
                    return cur == key || (cur == NULL_KEY ? key == null : cur.equals(key));
                }

                public boolean remove(Object key) {
                    int index = this.index(key);
                    Object cur = this.table[index];
                    if (cur == null) {
                        return false;
                    }
                    if (cur instanceof ChainedBucket) {
                        return this.removeFromChain((ChainedBucket) cur, (T) key, index);
                    }
                    if (this.nonNullTableObjectEquals(cur, (T) key)) {
                        this.table[index] = null;
                        this.occupied--;
                        return true;
                    }
                    return false;
                }

                private boolean removeFromChain(ChainedBucket bucket, T key, int index) {
                    if (this.nonNullTableObjectEquals(bucket.zero, key)) {
                        bucket.zero = bucket.removeLast(0);
                        if (bucket.zero == null) {
                            this.table[index] = null;
                        }
                        this.occupied--;
                        return true;
                    }
                    if (bucket.one == null) {
                        return false;
                    }
                    if (this.nonNullTableObjectEquals(bucket.one, key)) {
                        bucket.one = bucket.removeLast(1);
                        this.occupied--;
                        return true;
                    }
                    if (bucket.two == null) {
                        return false;
                    }
                    if (this.nonNullTableObjectEquals(bucket.two, key)) {
                        bucket.two = bucket.removeLast(2);
                        this.occupied--;
                        return true;
                    }
                    if (bucket.three == null) {
                        return false;
                    }
                    if (bucket.three instanceof ChainedBucket) {
                        return this.removeDeepChain(bucket, key);
                    }
                    if (this.nonNullTableObjectEquals(bucket.three, key)) {
                        bucket.three = bucket.removeLast(3);
                        this.occupied--;
                        return true;
                    }
                    return false;
                }

                private boolean removeDeepChain(ChainedBucket oldBucket, T key) {
                    do {
                        ChainedBucket bucket = (ChainedBucket) oldBucket.three;
                        if (this.nonNullTableObjectEquals(bucket.zero, key)) {
                            bucket.zero = bucket.removeLast(0);
                            if (bucket.zero == null) {
                                oldBucket.three = null;
                            }
                            this.occupied--;
                            return true;
                        }
                        if (bucket.one == null) {
                            return false;
                        }
                        if (this.nonNullTableObjectEquals(bucket.one, key)) {
                            bucket.one = bucket.removeLast(1);
                            this.occupied--;
                            return true;
                        }
                        if (bucket.two == null) {
                            return false;
                        }
                        if (this.nonNullTableObjectEquals(bucket.two, key)) {
                            bucket.two = bucket.removeLast(2);
                            this.occupied--;
                            return true;
                        }
                        if (bucket.three == null) {
                            return false;
                        }
                        if (bucket.three instanceof ChainedBucket) {
                            oldBucket = bucket;
                            continue;
                        }
                        if (this.nonNullTableObjectEquals(bucket.three, key)) {
                            bucket.three = bucket.removeLast(3);
                            this.occupied--;
                            return true;
                        }
                        return false;
                    } while (true);
                }
            }
            """;

    @DisplayName("the remove chain's summaries stay bounded and stable over recomputations")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        TypeInfo bucketType = X.findSubType("ChainedBucket");
        MethodInfo[] methods = {
                X.findUniqueMethod("remove", 1),
                X.findUniqueMethod("removeFromChain", 3),
                X.findUniqueMethod("removeDeepChain", 2),
                bucketType.methodStream().filter(m -> "removeLast".equals(m.name())
                                                      && m.parameters().getFirst().parameterizedType().typeInfo() != bucketType)
                        .findFirst().orElseThrow(),
                bucketType.methodStream().filter(m -> "removeLast".equals(m.name())
                                                      && m.parameters().getFirst().parameterizedType().typeInfo() == bucketType)
                        .findFirst().orElseThrow(),
        };
        String previous = null;
        for (int round = 1; round <= 3; round++) {
            long start = System.currentTimeMillis();
            tlc.doPrimaryType(X);
            StringBuilder sb = new StringBuilder();
            for (MethodInfo m : methods) {
                MethodLinkedVariables mlv = m.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                        MethodLinkedVariablesImpl.class);
                sb.append("@@@ ").append(m.name()).append('(').append(m.parameters().size()).append(") ").append(mlv)
                        .append('\n');
            }
            String s = sb.toString();
            System.out.println("### round " + round + ": " + (System.currentTimeMillis() - start) + " ms\n" + s);
            MethodLinkedVariables rfc = methods[1].analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            long links = rfc.ofReturnValue().stream().count()
                         + rfc.ofParameters().stream().mapToLong(l -> l.stream().count()).sum();
            assertTrue(links < 200, "round " + round + ": removeFromChain has " + links + " links");
            // the pathology itself: two different slots of the bucket identical to each other
            int r = round;
            rfc.ofParameters().forEach(l -> l.forEach(link -> assertFalse(
                    link.linkNature().isIdenticalTo()
                    && link.from() instanceof FieldReference f1 && link.to() instanceof FieldReference f2
                    && f1.scopeVariable() != null && f1.scopeVariable().equals(f2.scopeVariable())
                    && !f1.fieldInfo().equals(f2.fieldInfo()),
                    "round " + r + ": sibling slots identical: " + link)));
            // remove(Object) is content-free: it returns a boolean
            assertEquals("[-] --> -", String.valueOf(methods[0].analysis().getOrNull(
                    MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class)));
            if (round >= 2) assertEquals(previous, s, "round " + round + " differs from round " + (round - 1));
            previous = s;
        }
    }
}
