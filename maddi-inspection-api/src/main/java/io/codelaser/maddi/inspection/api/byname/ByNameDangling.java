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

package io.codelaser.maddi.inspection.api.byname;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Info;

/**
 * <b>A sink was called, the class argument read perfectly, and it names nothing in this parse.</b>
 *
 * <h2>⭐ Why this is recorded rather than skipped</h2>
 * It used to be skipped, with the reasoning "we read it and it is simply not ours" — a JDK class, a type in
 * another project, a typo. That is right for a CENSUS and wrong for a GATE.
 * <p>
 * A broken by-name binding does not turn into an invalid {@link ByNameReference}: <b>it stops being one.</b> A
 * row exists only because the literal resolved, so deleting or renaming the target without editing the string
 * makes the row VANISH. Nothing downstream can detect an absence — a gate comparing counts would need a
 * remembered baseline, which a corpus several people are committing to cannot supply. This record is the
 * positive evidence in its place: <em>here is a literal, at this range, that resolves to nothing.</em>
 *
 * <h2>⚠ Most of these are perfectly healthy</h2>
 * {@code Class.forName("java.util.ArrayList")} produces one, and so does every binding into a project this
 * parse does not contain. A dangling literal is <b>not by itself a defect</b> — it is a fact a reader must
 * judge. The judgement belongs to whoever verifies bindings: a literal naming something in one of the
 * project's own packages is a broken binding, one naming {@code java.*} is not. That is the same split
 * {@link ByNameReference}'s {@code targetMember} already draws — the producer resolves, the reader rules.
 *
 * @param from        the member whose code calls the sink
 * @param sink        which declared sink matched
 * @param binaryName  the literal's value, as written: the name that resolves to nothing
 * @param siteSource  the range of that literal — what a repair would edit
 * @param siteOwner   the member whose source text contains the range; {@code from} unless the name arrived
 *                    through a constant, in which case it is the field. Never null
 * @param viaConstant the literal was reached through a {@code static final} field
 * @param memberName  the member name written beside it, or null when the sink names no member
 */
public record ByNameDangling(Info from, ByNameSink sink, String binaryName, Source siteSource, Info siteOwner,
                             boolean viaConstant, String memberName) {

    /** {@code a.b.C.m() -> a.b.Gone#instance (FIELD, unresolved)}, for a log line or a test's expected value. */
    @Override
    public String toString() {
        return from.fullyQualifiedName() + " -> " + binaryName + (memberName == null ? "" : "#" + memberName)
               + " (" + sink.kind() + ", unresolved" + (viaConstant ? ", via constant" : "") + ")";
    }
}
