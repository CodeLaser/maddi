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

package org.e2immu.parser.java.erasure;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Objects;
import java.util.Set;

public class ConstructorCallErasure extends ErasureExpressionImpl {
    private final ParameterizedType formalType;

    public ConstructorCallErasure(Runtime runtime, Source source, ParameterizedType formalType) {
        super(runtime, source);
        this.formalType = formalType;
    }

    @Override
    public Expression withSource(Source source) {
        return new ConstructorCallErasure(runtime, source, formalType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstructorCallErasure that)) return false;
        return Objects.equals(formalType, that.formalType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(formalType);
    }

    @Override
    public Set<ParameterizedType> erasureTypes() {
        return Set.of(formalType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return formalType;
    }

    @Override
    public Precedence precedence() {
        return runtime.precedenceTop();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return runtime.newOutputBuilder().add(runtime.newText("<constructor call erasure, type " + formalType + ">"));
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        throw new UnsupportedOperationException();
    }
}
