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
