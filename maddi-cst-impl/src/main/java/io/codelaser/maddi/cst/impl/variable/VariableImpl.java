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

package io.codelaser.maddi.cst.impl.variable;

import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.SourceImpl;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class VariableImpl implements Variable {

    private final ParameterizedType parameterizedType;
    // equals/hashCode are on the analysis hot path (HashMap probes in the per-statement link graphs), and
    // subclasses rebuild the fully qualified name recursively on every call (top leaf frames of a corpus
    // jstack profile). All implementations are immutable value objects with construction-time-stable FQNs
    // (LocalVariableImpl.name is final; FieldReference/DependentVariable components are final), so the FQN
    // is memoized here. Benign race: the computation is idempotent.
    // @IgnoreModifications (road §050): idempotent memo state, disclaimed -- both the writes and the
    // slot's assignability are invisible to the modification/immutability analysis
    //
    // NOT io.codelaser.maddi.support.Memo/IntMemo, and this is the canonical note for the five slots that
    // mimic them (the two here, plus AndImpl.hash, OrImpl.hash, UnaryOperatorImpl.hash). Those classes
    // exist so the disclaimer is declared once on a type rather than on every field, and for a NEW memo
    // slot that is the right trade. It is the wrong trade here, twice over:
    //
    //   - memory. These five sit on the highest-cardinality objects in the engine. A wrapper turns a
    //     4-byte inline int into a reference plus a 16-byte object, on types instantiated per variable per
    //     statement across a whole corpus. Memory, not time, is what this engine runs out of first.
    //   - allocation. Memo.get(Supplier) and IntMemo.get(IntSupplier) build their supplier on EVERY call,
    //     hits included -- and a memo is by construction almost all hits. IntMemo cannot dodge it: it
    //     deliberately exposes no way to read the slot, so there is no non-allocating fast path to fall
    //     back on. The cost is measured, not feared: SharedVariable.assignmentSources carries the A/B
    //     where this same shape (calls outnumbering misses 265:1) cost 22% on TestBuilderChainBench.
    //
    // So: reach for Memo/IntMemo when writing a new memo slot; do not convert these five.
    @IgnoreModifications
    private String cachedFqn;
    @IgnoreModifications
    private int cachedHash;

    public VariableImpl(ParameterizedType parameterizedType) {
        this.parameterizedType = parameterizedType;
    }

    private String fqnForEquality() {
        String fqn = cachedFqn;
        if (fqn == null) {
            fqn = Objects.requireNonNull(fullyQualifiedName());
            cachedFqn = fqn;
        }
        return fqn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl variable)) return false;
        // cached-hash fast reject before the O(length) string compare: FQNs are long (deep faces carry a
        // full method signature), and the engine does many direct equals calls outside hash structures
        if (hashCode() != variable.hashCode()) return false;
        return fqnForEquality().equals(variable.fqnForEquality());
    }

    @Override
    public int hashCode() {
        int h = cachedHash;
        if (h == 0) {
            h = fqnForEquality().hashCode();
            if (h == 0) h = 1;
            cachedHash = h;
        }
        return h;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return variables(DescendModeEnum.YES);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return variables(DescendModeEnum.NO);
    }

    @Override
    public String toString() {
        return print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString();
    }

    @Override
    public Source source() {
        return SourceImpl.NO_SOURCE;
    }
}
