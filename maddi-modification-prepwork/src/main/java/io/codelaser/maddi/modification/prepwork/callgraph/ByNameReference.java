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

package io.codelaser.maddi.modification.prepwork.callgraph;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;

/**
 * <b>One place where a string literal names a type the compiler will never see it name.</b> Produced by
 * {@link ComputeCallGraph} for each call to a declared {@link ByNameSink} whose class argument reads as a name that
 * the parse resolves.
 *
 * <h2>⭐ The row carries the SITE, which is the whole point</h2>
 * The graph says <em>{@code from} names {@code targetType}</em> — enough to stop a dead-code pass deleting a type
 * that only a string reaches, and no use at all to a rename, which has to edit the literal. {@code siteSource} is
 * that literal's exact range, so the row answers both questions. This is the same split the Kotlin front end drew
 * between the CST ("what the code does") and its reference index ("where an editor has to write").
 *
 * <h2>⚠ {@code from} and {@code siteSource} can be in different members</h2>
 * When the name arrives through a constant ({@code Class.forName(TARGET)} with
 * {@code private static final String TARGET = "a.b.X"}), {@code from} is the member holding the CALL and
 * {@code siteSource} is in the FIELD's initialiser. A verb that moves or renames must follow {@code siteSource},
 * never {@code from}; {@code viaConstant} says when the two part company.
 *
 * @param from         the member whose code calls the sink: the vertex the graph's by-name edge starts at
 * @param sink         which declared sink matched
 * @param targetType   the type the binary name resolved to; never null
 * @param binaryName   the literal's value, as written
 * @param siteSource   the range of the literal that holds {@code binaryName} — what a rewrite edits
 * @param viaConstant  the literal was reached through a {@code static final} field rather than written at the call
 * @param memberName   the member name written beside it, or null when the sink names no member
 * @param memberSource the range of that member literal, or null
 * @param targetMember the member it resolves to, when exactly one of that name exists on {@code targetType}; null
 *                     when the name is absent, ambiguous, or the sink names no member. <b>Ambiguity is left to the
 *                     reader</b>: the rules that decide it (arity, a MethodType, "the only public static one") are
 *                     the SINK's, and belong to whatever verifies bindings, not to a graph producer
 */
public record ByNameReference(Info from, ByNameSink sink, TypeInfo targetType, String binaryName, Source siteSource,
                              boolean viaConstant, String memberName, Source memberSource, Info targetMember) {

    /** {@code a.b.C.m() -> a.b.X#instance (FIELD)}, for a log line or a test's expected value. */
    @Override
    public String toString() {
        return from.fullyQualifiedName() + " -> " + binaryName + (memberName == null ? "" : "#" + memberName)
               + " (" + sink.kind() + (viaConstant ? ", via constant" : "") + ")";
    }
}
