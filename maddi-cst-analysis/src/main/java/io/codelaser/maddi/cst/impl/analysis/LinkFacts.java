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

package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;

import java.util.Set;

/**
 * What the modification analysis's links say about a method's result, for a printer whose types state ownership
 * (Rust): a new object or one that existed before the call, and what of the receiver and the parameters it is or
 * holds. The independence verdicts do not answer this: a builder's {@code build()} returning {@code new R(...)} is
 * independent apart from hidden content, as is a getter handing out part of the receiver; a parameter kept in the
 * result of a method without fields ({@code new Items(name, items)}) is independent, there being no field of
 * {@code this} to link to; and {@code first(List<String> l)} returning {@code l.get(0)} says nothing about the
 * receiver at all.
 * <p>
 * Readings: {@link Result#NEW} with {@link #keptInResult}: an owned value, those parameters moved in, or borrowed for
 * as long as a result that borrows them; {@link Result#EXISTING} with {@link #receiverInResult}: a borrow tied to the
 * receiver; {@link Result#EXISTING} with {@link #keptInResult}: a borrow tied to those parameters; both: one lifetime
 * over all of them.
 * <p>
 * maddi-mod computes them from {@code METHOD_LINKS} ({@code AnalysisEngine.linkFacts}) in the run that analyses the
 * source; links are not written to the results files, and the library's hints carry none. Links over-approximate: an
 * answer may give an owned value or a lifetime where a borrow would do, never less. Every answer defaults to
 * "unknown", and the printer then reads the verdicts as before.
 */
public interface LinkFacts {

    /** Where a method's result comes from. */
    enum Result {
        /** a new object, created in the method or by a method it calls: the caller owns it */
        NEW,
        /** an object that existed before the call, or something inside one: a borrow */
        EXISTING
    }

    /** No facts. */
    LinkFacts NONE = new LinkFacts() {
    };

    /** Where {@code methodInfo}'s result comes from; {@code null} when not known (no links, a library method). */
    default Result result(MethodInfo methodInfo) {
        return null;
    }

    /** The result is, or holds, the receiver or (part of) its contents. False when not known. */
    default boolean receiverInResult(MethodInfo methodInfo) {
        return false;
    }

    /**
     * The parameters whose object, or hidden content, the result is or holds (a constructor argument stored in the
     * object returned, {@code l.get(0)} of a list parameter): the result lives no longer than they do. Empty when none,
     * or not known.
     */
    default Set<ParameterInfo> keptInResult(MethodInfo methodInfo) {
        return Set.of();
    }
}
