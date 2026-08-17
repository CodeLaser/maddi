/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

/**
 * Support types for the eventually-immutable style: an object is built up, reaches a marked transition,
 * and is immutable from then on. Each type here carries the {@code @Mark} / {@code @Only} / {@code @TestMark}
 * contracts the analyzer needs, because as a jar leaf none of it can be computed.
 *
 * <h2>Which type for which slot</h2>
 * <ul>
 *     <li>{@link io.codelaser.maddi.support.SetOnce} — a slot written exactly once, read only after
 *     ({@code @Mark("t")} on {@code set}, {@code @Only(after="t")} on {@code get}). The transition is
 *     observable, and the type's immutability is genuinely eventual.</li>
 *     <li>{@link io.codelaser.maddi.support.EventuallyFinal} / {@link io.codelaser.maddi.support.EventuallyFinalOnDemand}
 *     — a slot that may be rewritten while variable and is then frozen; the on-demand variant computes
 *     its value on first read, which is why the eventually-non-modifying layer had to exist.</li>
 *     <li>{@link io.codelaser.maddi.support.Memo} / {@link io.codelaser.maddi.support.IntMemo} — an idempotent lazy cache.
 *     <b>Not</b> an eventual transition: the write is observationally invisible, so the class is disclaimed
 *     with {@code @IgnoreModifications} and a field of that type inherits the disclaimer.</li>
 *     <li>{@link io.codelaser.maddi.support.Freezable}, {@link io.codelaser.maddi.support.AddOnceSet},
 *     {@link io.codelaser.maddi.support.SetOnceMap} — collections with the same discipline.</li>
 * </ul>
 *
 * <h2>Resolve-once: a pattern, not a type</h2>
 * A list that is accumulated during resolution and fixed afterwards needs no dedicated class. Use a
 * {@code SetOnce<List<T>>}: accumulate in a local, commit once, and read with a default.
 *
 * <pre>{@code
 * private final SetOnce<List<TypeInfo>> implementationsResolved = new SetOnce<>();
 *
 * public void setImplementationsResolved(List<TypeInfo> resolved) {
 *     implementationsResolved.set(List.copyOf(resolved));      // commit once, immutably
 * }
 * public List<TypeInfo> implementationsResolved() {
 *     return implementationsResolved.getOrDefault(List.of());  // the not-yet-resolved read
 * }
 * }</pre>
 *
 * The shape to avoid is a final {@code ArrayList} plus an {@code addX} adder. It reads as a permanently
 * mutable container, and because a mutable supertype sinks every subtype, one such field can cost a whole
 * hierarchy its immutability verdict — which is exactly what happened to {@code ModuleInfoImpl.ProvidesImpl}
 * and, through it, to the entire {@code Element} hierarchy. {@code TestEventualConformance} enforces this.
 */
package io.codelaser.maddi.support;
