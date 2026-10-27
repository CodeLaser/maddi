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

package io.codelaser.maddi.parser.java.erasure;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

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
    public Expression rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException();
    }
}
