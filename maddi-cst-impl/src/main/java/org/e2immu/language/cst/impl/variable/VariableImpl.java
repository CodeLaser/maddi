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

package org.e2immu.language.cst.impl.variable;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.SourceImpl;
import org.e2immu.language.cst.impl.output.QualificationImpl;

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
    private String cachedFqn;
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
